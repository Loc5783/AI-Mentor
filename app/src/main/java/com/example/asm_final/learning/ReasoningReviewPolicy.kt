package com.example.asm_final.learning

/** A learner's own confidence report. It must never be inferred from a score. */
enum class AnswerConfidence { LOW, MEDIUM, HIGH }

/** Skill mapping and independence must be supplied by reviewed question metadata. */
internal data class ReasoningObservation(
    val evidence: LearningEvidence,
    val skillId: String?,
    val confidence: AnswerConfidence?,
    val independentVariant: Boolean = false,
    val assisted: Boolean = false,
    val skillMapVersion: String? = null
)

internal enum class ReasoningNextStep {
    MAP_QUESTIONS,
    WAIT_FOR_GRADED_ANSWER,
    ASK_CONFIDENCE,
    CHECK_POSSIBLE_MISCONCEPTION,
    TRY_UNSEEN_QUESTION,
    REVIEW_EVIDENCE
}

/** Descriptive signals only: this is not a mastery score or a reward decision. */
internal data class ReasoningReview(
    val gradedAnswers: Int,
    val highConfidenceErrors: Int,
    val lowConfidenceCorrect: Int,
    val independentCorrect: Int,
    val nextStep: ReasoningNextStep
)

internal object ReasoningReviewPolicy {
    fun review(
        ownerId: Int,
        subjectId: String,
        grade: Int,
        skillId: String,
        skillMapVersion: String,
        observations: List<ReasoningObservation>
    ): ReasoningReview {
        require(ownerId > 0 && subjectId.isNotBlank() && grade in 10..12 && skillId.isNotBlank())
        require(skillMapVersion.isNotBlank())
        require(observations.all { it.evidence.ownerId == ownerId }) {
            "Reasoning observations belong to another account"
        }
        require(observations.map { it.evidence.key }.distinct().size == observations.size) {
            "Duplicate question observations must be reconciled before review"
        }

        val matching = observations.filter {
            it.skillId == skillId && it.skillMapVersion == skillMapVersion &&
                it.evidence.subjectId == subjectId && it.evidence.grade == grade &&
                it.evidence.observation == LearningEvidence.Observation.ANSWERED_QUESTION
        }
        val graded = matching.filter { it.evidence.correct != null }
        val errors = graded.count { it.evidence.correct == false && it.confidence == AnswerConfidence.HIGH }
        val guesses = graded.count { it.evidence.correct == true && it.confidence == AnswerConfidence.LOW }
        val independent = graded.count {
            it.evidence.correct == true && it.independentVariant && !it.assisted
        }
        val next = when {
            matching.isEmpty() -> ReasoningNextStep.MAP_QUESTIONS
            graded.isEmpty() -> ReasoningNextStep.WAIT_FOR_GRADED_ANSWER
            errors > 0 -> ReasoningNextStep.CHECK_POSSIBLE_MISCONCEPTION
            guesses > 0 -> ReasoningNextStep.TRY_UNSEEN_QUESTION
            graded.all { it.confidence == null } -> ReasoningNextStep.ASK_CONFIDENCE
            independent == 0 -> ReasoningNextStep.TRY_UNSEEN_QUESTION
            else -> ReasoningNextStep.REVIEW_EVIDENCE
        }
        return ReasoningReview(graded.size, errors, guesses, independent, next)
    }
}
