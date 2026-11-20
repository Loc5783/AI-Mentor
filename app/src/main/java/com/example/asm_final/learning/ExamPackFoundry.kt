package com.example.asm_final.learning

import org.json.JSONObject
import java.util.UUID

/**
 * AI ExamPack Foundry (Xưởng Đóng Đề Thi AI)
 * Handles multi-file PDF/Image import, structural question extraction, exact deduplication,
 * numerical variation protection, and account-scoped pack management.
 */
object ExamPackFoundry {

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
        val options: Map<String, String>, // A, B, C, D
        val correctOption: String?,
        val explanation: String?,
        val topic: String?,
        val difficulty: String = "MEDIUM",
        val sourceFileName: String,
        val sourcePageIndex: Int = 1,
        var isApproved: Boolean = false,
        var isAnswerVerified: Boolean = false
    ) {
        /**
         * Key for exact prompt + options text deduplication.
         */
        val exactFingerprint: String
            get() = prompt.trim().lowercase() + "||" + options.values.joinToString("|") { it.trim().lowercase() }

        /**
         * Normalized text with numbers replaced by '#', used to detect numerical variations.
         */
        val structuralTemplate: String
            get() = prompt.trim().lowercase().replace(Regex("\\d+(\\.\\d+)?"), "#")
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

    /**
     * Extracts and processes questions from file contents or AI extraction results.
     * Enforces:
     * 1. Exact text deduplication (re-importing same file won't duplicate).
     * 2. Numerical Variation Protection (questions with different numbers/answers ARE kept separate).
     * 3. AI output CANNOT self-grant isAnswerVerified = true.
     */
    fun processExtraction(
        pack: ExamPack,
        rawQuestions: List<ExtractedQuestion>
    ): List<ExtractedQuestion> {
        val existingFingerprints = pack.questions.map { it.exactFingerprint }.toMutableSet()
        val added = mutableListOf<ExtractedQuestion>()

        for (q in rawQuestions) {
            // Safety check: ensure required fields are present
            if (q.prompt.isBlank() || q.options.isEmpty()) continue

            // 1. Exact text deduplication check
            val fp = q.exactFingerprint
            if (existingFingerprints.contains(fp)) {
                continue // Skip exact duplicate
            }

            // 2. Numerical variation check:
            // If prompt matches structural template but has different numbers/correctOption,
            // it MUST be kept separate (NOT merged)!
            // Safety rule: AI output cannot self-grant isAnswerVerified = true
            val safeQuestion = q.copy(
                userId = pack.userId,
                packId = pack.id,
                isApproved = false,
                isAnswerVerified = false // AI extraction output CANNOT self-grant answer_verified
            )

            existingFingerprints.add(fp)
            pack.questions.add(safeQuestion)
            added.add(safeQuestion)
        }

        return added
    }

    /**
     * Student manual review & approval.
     * Sets isApproved = true, and if correctOption is verified by student, sets isAnswerVerified = true.
     */
    fun approveQuestion(
        question: ExtractedQuestion,
        verifiedAnswerKey: String?,
        editedPrompt: String? = null,
        editedOptions: Map<String, String>? = null
    ): ExtractedQuestion {
        val finalPrompt = editedPrompt?.trim()?.takeIf { it.isNotBlank() } ?: question.prompt
        val finalOptions = editedOptions ?: question.options
        val finalAnswer = verifiedAnswerKey?.trim()?.uppercase() ?: question.correctOption

        // If answer key is missing or conflicting, it cannot be verified
        val isVerified = !finalAnswer.isNullOrBlank() && finalOptions.containsKey(finalAnswer)

        question.isApproved = true
        question.isAnswerVerified = isVerified

        return question.copy(
            prompt = finalPrompt,
            options = finalOptions,
            correctOption = finalAnswer,
            isApproved = true,
            isAnswerVerified = isVerified
        )
    }

    /**
     * Mark entire pack as APPROVED if at least 1 question is approved.
     */
    fun finalizePack(pack: ExamPack): ExamPack {
        val approvedCount = pack.questions.count { it.isApproved }
        if (approvedCount > 0) {
            pack.isApproved = true
        }
        return pack
    }
}
