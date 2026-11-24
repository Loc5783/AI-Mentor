package com.example.asm_final.learning

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ReasoningReviewPolicyTest {
    private fun observation(evidence: LearningEvidence, skillId: String?, confidence: AnswerConfidence?,
                            independentVariant: Boolean = false, assisted: Boolean = false) =
        ReasoningObservation(evidence, skillId, confidence, independentVariant, assisted, "skills-v1")
    private fun answer(id: String, correct: Boolean?, owner: Int = 7, subject: String = "math") =
        LearningEvidence(owner, LearningEvidence.Source.LIBRARY, "session-$id", id, subject, 10,
            LearningEvidence.Observation.ANSWERED_QUESTION, correct)

    @Test fun missingSkillMappingNeverBecomesAProficiencyClaim() {
        val result = ReasoningReviewPolicy.review(7, "math", 10, "linear-equations", "skills-v1",
            listOf(observation(answer("q1", true), null, AnswerConfidence.HIGH)))
        assertEquals(0, result.gradedAnswers)
        assertEquals(ReasoningNextStep.MAP_QUESTIONS, result.nextStep)
    }

    @Test fun unansweredAndUngradedWorkCannotBeDiagnosed() {
        val result = ReasoningReviewPolicy.review(7, "math", 10, "linear-equations", "skills-v1",
            listOf(observation(answer("q1", null), "linear-equations", null)))
        assertEquals(ReasoningNextStep.WAIT_FOR_GRADED_ANSWER, result.nextStep)
    }

    @Test fun confidentErrorIsOnlyAPossibleMisconception() {
        val result = ReasoningReviewPolicy.review(7, "math", 10, "linear-equations", "skills-v1",
            listOf(observation(answer("q1", false), "linear-equations", AnswerConfidence.HIGH)))
        assertEquals(1, result.highConfidenceErrors)
        assertEquals(ReasoningNextStep.CHECK_POSSIBLE_MISCONCEPTION, result.nextStep)
    }

    @Test fun guessedCorrectAnswerRequestsAnUnseenQuestion() {
        val result = ReasoningReviewPolicy.review(7, "math", 10, "linear-equations", "skills-v1",
            listOf(observation(answer("q1", true), "linear-equations", AnswerConfidence.LOW)))
        assertEquals(1, result.lowConfidenceCorrect)
        assertEquals(ReasoningNextStep.TRY_UNSEEN_QUESTION, result.nextStep)
    }

    @Test fun gradedAnswerWithoutSelfReportedConfidenceAsksForIt() {
        val result = ReasoningReviewPolicy.review(7, "math", 10, "linear-equations", "skills-v1",
            listOf(observation(answer("q1", true), "linear-equations", null)))
        assertEquals(ReasoningNextStep.ASK_CONFIDENCE, result.nextStep)
    }

    @Test fun independentCorrectAnswerRemainsEvidenceForReviewNotMastery() {
        val result = ReasoningReviewPolicy.review(7, "math", 10, "linear-equations", "skills-v1",
            listOf(observation(answer("q1", true), "linear-equations",
                AnswerConfidence.HIGH, independentVariant = true)))
        assertEquals(1, result.independentCorrect)
        assertEquals(ReasoningNextStep.REVIEW_EVIDENCE, result.nextStep)
    }

    @Test fun assistedAnswerDoesNotCountAsIndependent() {
        val result = ReasoningReviewPolicy.review(7, "math", 10, "linear-equations", "skills-v1",
            listOf(observation(answer("q1", true), "linear-equations",
                AnswerConfidence.HIGH, independentVariant = true, assisted = true)))
        assertEquals(0, result.independentCorrect)
        assertEquals(ReasoningNextStep.TRY_UNSEEN_QUESTION, result.nextStep)
    }

    @Test fun anotherAccountCannotBeMixedIntoReview() {
        assertThrows(IllegalArgumentException::class.java) {
            ReasoningReviewPolicy.review(7, "math", 10, "linear-equations", "skills-v1",
                listOf(observation(answer("q1", true, owner = 8), "linear-equations", null)))
        }
    }

    @Test fun duplicateObservationCannotSilentlyOverrideAResult() {
        val first = observation(answer("q1", true), "linear-equations", AnswerConfidence.HIGH)
        val second = first.copy(evidence = answer("q1", false))
        assertThrows(IllegalArgumentException::class.java) {
            ReasoningReviewPolicy.review(7, "math", 10, "linear-equations", "skills-v1", listOf(first, second))
        }
    }
}
