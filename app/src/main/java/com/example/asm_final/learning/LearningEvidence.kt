package com.example.asm_final.learning

import org.json.JSONObject

/** Read-only observations from existing server snapshots; no AI inference or reward decision. */
internal data class LearningEvidence(
    val ownerId: Int,
    val source: Source,
    val sourceId: String,
    val itemId: String,
    val subjectId: String,
    val grade: Int,
    val observation: Observation,
    val correct: Boolean? = null
) {
    enum class Source { LESSON_PATH, PLACEMENT, AI_PRACTICE, LIBRARY, EXAM }
    enum class Observation { COMPLETED_LESSON, SKIPPED_LESSON, ANSWERED_QUESTION, SKIPPED_QUESTION }

    /** A question identifier can recur in several sessions, but each observation is distinct. */
    val key: String get() = "${source.name}:$grade:$subjectId:$sourceId:$itemId"
}

internal object LearningEvidenceReader {
    fun fromLessonViewModel(model: LessonViewModel): List<LearningEvidence> {
        if (!model.isCurrentAccount) return emptyList()
        val snapshot = model.path ?: return emptyList()
        val units = snapshot.optJSONArray("units") ?: return emptyList()
        val subjectId = model.subject() ?: return emptyList()
        val grade = model.grade()
        if (subjectId.isBlank() || grade !in 10..12) return emptyList()
        val enrollment = snapshot.optJSONObject("enrollment") ?: return emptyList()
        if (enrollment.optString("subjectId") != subjectId || enrollment.optInt("grade") != grade)
            return emptyList()
        return runCatching { fromLessonPath(model.owner(), subjectId, grade, LessonPathData(units)) }
            .getOrDefault(emptyList())
    }

    fun fromPlacementViewModel(model: LessonViewModel): List<LearningEvidence> {
        if (!model.isCurrentAccount) return emptyList()
        val completion = model.completion ?: return emptyList()
        val attempt = completion.optJSONObject("__placementReview") ?: model.attempt ?: return emptyList()
        if (attempt.optString("subjectId") != model.subject() || attempt.optInt("grade") != model.grade())
            return emptyList()
        return fromPlacement(model.owner(), completion, attempt)
    }

    /** A placement answer is evidence only after the matching attempt is completed. */
    fun fromPlacement(ownerId: Int, completion: JSONObject, attempt: JSONObject): List<LearningEvidence> {
        if (completion.optString("kind") != "PLACEMENT" ||
            attempt.optString("kind") != "PLACEMENT" || attempt.optString("state") != "COMPLETED" ||
            completion.optString("attemptId").isBlank() ||
            completion.optString("attemptId") != attempt.optString("id")) return emptyList()
        val subjectId = attempt.optString("subjectId")
        val grade = attempt.optInt("grade")
        if (subjectId.isBlank() || grade !in 10..12) return emptyList()
        val questions = attempt.optJSONArray("questions") ?: return emptyList()
        val questionIds = (0 until questions.length()).mapNotNull { questions.optJSONObject(it)?.optString("id") }
            .filter { it.isNotBlank() }.toSet()
        val answers = attempt.optJSONArray("answers") ?: return emptyList()
        return (0 until answers.length()).mapNotNull { index ->
            val answer = answers.optJSONObject(index) ?: return@mapNotNull null
            val questionId = answer.optString("questionId")
            val correct = answer.opt("correct") as? Boolean ?: return@mapNotNull null
            if (questionId !in questionIds || answer.optString("selectedOptionId").isBlank())
                return@mapNotNull null
            LearningEvidence(ownerId, LearningEvidence.Source.PLACEMENT, attempt.getString("id"),
                questionId, subjectId, grade, LearningEvidence.Observation.ANSWERED_QUESTION,
                correct)
        }.distinctBy { it.key }
    }

    fun fromAiPractice(ownerId: Int, session: AiPracticeSession): List<LearningEvidence> =
        session.questions.mapNotNull { question ->
            val answer = session.answers[question.id] ?: return@mapNotNull null
            if (!answer.revealed) return@mapNotNull null
            val observation = if (answer.skipped) LearningEvidence.Observation.SKIPPED_QUESTION
                else LearningEvidence.Observation.ANSWERED_QUESTION
            if (!answer.skipped && answer.selectedOption.isNullOrBlank()) return@mapNotNull null
            LearningEvidence(ownerId, LearningEvidence.Source.AI_PRACTICE, session.id, question.id,
                session.subjectId, session.grade, observation,
                if (answer.skipped) null else answer.isCorrect)
        }

    fun fromLessonPath(ownerId: Int, subjectId: String, grade: Int, path: LessonPathData): List<LearningEvidence> =
        path.units.mapNotNull { unit ->
            if (unit.review) return@mapNotNull null
            val observation = when (unit.state) {
                "COMPLETED" -> LearningEvidence.Observation.COMPLETED_LESSON
                "SKIPPED" -> LearningEvidence.Observation.SKIPPED_LESSON
                else -> return@mapNotNull null
            }
            LearningEvidence(ownerId, LearningEvidence.Source.LESSON_PATH, subjectId, unit.id,
                subjectId, grade, observation)
        }

    fun fromPractice(ownerId: Int, session: PracticeSession): List<LearningEvidence> =
        session.questions.mapNotNull { question ->
            if (question.userAnswer.isNullOrBlank()) return@mapNotNull null
            LearningEvidence(ownerId, LearningEvidence.Source.LIBRARY, session.id, question.id,
                session.subjectId, session.grade, LearningEvidence.Observation.ANSWERED_QUESTION,
                question.isCorrect)
        }

    fun fromExam(ownerId: Int, session: ExamSession): List<LearningEvidence> =
        session.questions.mapNotNull { question ->
            val answer = session.answers[question.id] ?: question.selectedOption
            if (answer.isNullOrBlank()) return@mapNotNull null
        val graded = (session.state == "SUBMITTED" || session.state == "TIMED_OUT") &&
                (question.kind != ExamQuestionKind.ESSAY || question.essayPoints != null)
            LearningEvidence(ownerId, LearningEvidence.Source.EXAM, session.id, question.id,
                session.subjectId, session.grade, LearningEvidence.Observation.ANSWERED_QUESTION,
                if (graded) question.isCorrect else null)
        }

    fun fromExamSessions(ownerId: Int, sessions: List<ExamSession>): List<LearningEvidence> =
        combine(ownerId, *sessions.groupBy { it.id }.values
            .map { versions ->
                // Never resurrect answers from an older detailed snapshot after a newer summary.
                val latest = versions.filter { it.revision == versions.maxOf { version -> version.revision } }
                val detailed = latest.filter { it.questions.isNotEmpty() }
                val observations = detailed.map { fromExam(ownerId, it).toSet() }.distinct()
                // Same-revision disagreement has no trustworthy winner. Fetch fresh details later.
                if (observations.size == 1) observations.single().toList() else emptyList()
            }
            .toTypedArray())

    fun combine(ownerId: Int, vararg groups: List<LearningEvidence>): List<LearningEvidence> {
        val all = groups.flatMap { it }
        require(all.all { it.ownerId == ownerId }) { "Evidence belongs to another account" }
        // Identical repeats are harmless; conflicting observations must not depend on list order.
        return all.groupBy { it.key }.values.mapNotNull { versions -> versions.distinct().singleOrNull() }
    }
}
