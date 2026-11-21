package com.example.asm_final.learning

import java.text.Normalizer
import java.util.Locale
import java.util.UUID

/** Review policy only. Import transport, persistence and UI must call this boundary explicitly. */
object ExamPackFoundry {
    private val keys = listOf("A", "B", "C", "D")
    private fun normalize(value: String) =
        Normalizer.normalize(value, Normalizer.Form.NFC).trim().replace(Regex("\\s+"), " ")

    data class ExamPackFile(
        val fileName: String,
        val pageCount: Int = 1,
        val mimeType: String = "application/pdf"
    )

    data class ExtractedQuestion(
        val id: String = UUID.randomUUID().toString(),
        val packId: String,
        val userId: Int,
        val prompt: String,
        val options: Map<String, String>,
        val correctOption: String?,
        val explanation: String?,
        val topic: String?,
        val difficulty: String = "MEDIUM",
        val sourceFileName: String,
        val sourcePageIndex: Int = 1,
        val isApproved: Boolean = false,
        val isAnswerVerified: Boolean = false
    ) {
        // Length framing avoids delimiter collisions; ordering ignores map insertion order.
        // Preserve case, punctuation and numbers: they can change scientific meaning.
        val exactFingerprint: String
            get() = (listOf(prompt) + options.toSortedMap().flatMap { listOf(it.key, it.value) })
                .joinToString("") { value -> normalize(value).let { "${it.length}:$it" } }

        val structuralTemplate: String
            get() = normalize(prompt).replace(Regex("\\d+(\\.\\d+)?"), "#")
    }

    data class ExamPack(
        val id: String = UUID.randomUUID().toString(),
        val userId: Int,
        val title: String,
        val subjectId: String,
        val grade: Int,
        val sourceFiles: List<ExamPackFile>,
        var isApproved: Boolean = false,
        val questions: MutableList<ExtractedQuestion> = mutableListOf()
    )

    fun processExtraction(pack: ExamPack, rawQuestions: List<ExtractedQuestion>): List<ExtractedQuestion> {
        val fingerprints = pack.questions.map { it.exactFingerprint }.toMutableSet()
        val added = mutableListOf<ExtractedQuestion>()
        for (question in rawQuestions) {
            if (question.prompt.isBlank() || question.options.isEmpty()) continue
            if (!fingerprints.add(question.exactFingerprint)) continue
            // IDs and identity are application-owned, never accepted from AI.
            val draft = question.copy(
                id = UUID.randomUUID().toString(), packId = pack.id, userId = pack.userId,
                options = question.options.toMap(), isApproved = false, isAnswerVerified = false
            )
            pack.questions.add(draft)
            added.add(draft)
        }
        if (added.isNotEmpty()) pack.isApproved = false
        return added
    }

    fun approveQuestion(
        question: ExtractedQuestion,
        verifiedAnswerKey: String?,
        editedPrompt: String? = null,
        editedOptions: Map<String, String>? = null,
        editedExplanation: String? = null,
        editedTopic: String? = null
    ): ExtractedQuestion {
        val reviewed = question.copy(
            prompt = (editedPrompt ?: question.prompt).trim(),
            options = (editedOptions ?: question.options).mapValues { it.value.trim() },
            // An explicit human selection is required; never silently trust the AI key.
            correctOption = verifiedAnswerKey?.trim()?.uppercase(Locale.ROOT),
            explanation = (editedExplanation ?: question.explanation)?.trim(),
            topic = (editedTopic ?: question.topic)?.trim(),
            isApproved = false, isAnswerVerified = false
        )
        val valid = isComplete(reviewed)
        return reviewed.copy(isApproved = valid, isAnswerVerified = valid)
    }

    /** Replace the stored question atomically, including corrected answer and explanation. */
    fun reviewQuestion(
        pack: ExamPack,
        questionId: String,
        verifiedAnswerKey: String?,
        editedPrompt: String? = null,
        editedOptions: Map<String, String>? = null,
        editedExplanation: String? = null,
        editedTopic: String? = null
    ): ExtractedQuestion {
        val index = pack.questions.indexOfFirst { it.id == questionId }
        require(index >= 0) { "Question does not belong to this pack" }
        val original = pack.questions[index]
        require(original.packId == pack.id && original.userId == pack.userId)
        val reviewed = approveQuestion(original, verifiedAnswerKey, editedPrompt, editedOptions,
            editedExplanation, editedTopic)
        require(pack.questions.none { it.id != questionId && it.exactFingerprint == reviewed.exactFingerprint }) {
            "Review would duplicate an existing question"
        }
        pack.questions[index] = reviewed
        pack.isApproved = false
        return reviewed
    }

    private fun isComplete(question: ExtractedQuestion): Boolean =
        question.prompt.isNotBlank() &&
            question.options.keys == keys.toSet() &&
            keys.all { !question.options[it].isNullOrBlank() } &&
            keys.map { normalize(question.options.getValue(it)).lowercase(Locale.ROOT) }.distinct().size == 4 &&
            question.correctOption in keys &&
            !question.explanation.isNullOrBlank() && !question.topic.isNullOrBlank() &&
            question.sourceFileName.isNotBlank() && question.sourcePageIndex > 0

    private fun eligible(pack: ExamPack, question: ExtractedQuestion): Boolean =
        question.userId == pack.userId && question.packId == pack.id &&
            question.isApproved && question.isAnswerVerified && isComplete(question) &&
            pack.sourceFiles.any {
                it.fileName == question.sourceFileName && question.sourcePageIndex in 1..it.pageCount
            }

    /** Fail closed: a mixed draft/reviewed pack is not a published exam. */
    fun finalizePack(pack: ExamPack): ExamPack {
        pack.isApproved = pack.questions.isNotEmpty() &&
            pack.questions.all { eligible(pack, it) } &&
            pack.questions.map { it.exactFingerprint }.distinct().size == pack.questions.size
        return pack
    }

    /** Revalidate at the playback boundary, even if a stale approval flag was restored. */
    fun playableQuestions(pack: ExamPack): List<ExtractedQuestion> {
        check(finalizePack(pack).isApproved) { "Every question must be reviewed before practice" }
        return pack.questions.map { it.copy(options = it.options.toMap()) }
    }
}
