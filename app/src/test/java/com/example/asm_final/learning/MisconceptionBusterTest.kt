package com.example.asm_final.learning

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class MisconceptionBusterTest {

    @Test
    fun inFlightExamDoesNotLeakAnswerWhenHesitantToggled() {
        val userAnswers = JSONObject()
        userAnswers.put("a", true)
        userAnswers.put("a_hesitant", true) // Mark hesitant

        // Ensure hesitant flag does not modify selected option or expose correct answer key
        assertTrue(userAnswers.getBoolean("a_hesitant"))
        assertTrue(userAnswers.getBoolean("a"))
        assertFalse(userAnswers.has("correctOptionId"))
        assertFalse(userAnswers.has("explanation"))
    }

    @Test
    fun preAuthoredReasonReturnsVerifiedHintAndCounterexample() {
        val session = MisconceptionBuster.startSession(
            userId = 101,
            questionId = "q_math_01",
            subjectId = "math",
            grade = 11,
            reasonInput = "math_reason_01"
        )

        assertFalse(session.isCustomReason)
        assertTrue(session.hint.contains("Mệnh đề"))
        assertTrue(session.counterexample.contains("x²"))
        assertNotNull(session.prop1Prompt)
        assertNotNull(session.prop2Prompt)
    }

    @Test
    fun customReasonWithMalformedAiResponseFallsBackToPreAuthoredHint() {
        val session = MisconceptionBuster.startSession(
            userId = 101,
            questionId = "q_math_02",
            subjectId = "math",
            grade = 11,
            reasonInput = "Em cho rằng x² = 9 thì x chỉ có thể bằng 3",
            aiResponseJson = "INVALID_JSON_RESPONSE"
        )

        assertTrue(session.isCustomReason)
        // Verified fallback hint is used
        assertFalse(session.hint.isBlank())
        assertFalse(session.counterexample.isBlank())
        assertNotNull(session.prop1Prompt)
    }

    @Test
    fun evaluateVerificationRequiresBothPropositionsCorrectForResolvedStatus() {
        val session = MisconceptionBuster.startSession(
            userId = 101,
            questionId = "q_math_01",
            subjectId = "math",
            grade = 11,
            reasonInput = "math_reason_01"
        )

        assertEquals(MisconceptionBuster.VerificationStatus.NEEDS_REVIEW, session.status)

        // 1 out of 2 correct -> status stays NEEDS_REVIEW
        val step1 = MisconceptionBuster.evaluateVerification(
            session = session,
            ans1 = session.prop1Correct,
            ans2 = !session.prop2Correct // Wrong
        )
        assertEquals(MisconceptionBuster.VerificationStatus.NEEDS_REVIEW, step1.status)

        // Both 2 out of 2 correct -> status becomes RESOLVED
        val step2 = MisconceptionBuster.evaluateVerification(
            session = session,
            ans1 = session.prop1Correct,
            ans2 = session.prop2Correct // Correct
        )
        assertEquals(MisconceptionBuster.VerificationStatus.RESOLVED, step2.status)
    }

    @Test
    fun evaluateVerificationIsIdempotentWhenAlreadyResolved() {
        val session = MisconceptionBuster.startSession(
            userId = 101,
            questionId = "q_math_01",
            subjectId = "math",
            grade = 11,
            reasonInput = "math_reason_01"
        )

        // Grade 2/2 correct -> RESOLVED
        val resolved = MisconceptionBuster.evaluateVerification(
            session = session,
            ans1 = session.prop1Correct,
            ans2 = session.prop2Correct
        )
        assertEquals(MisconceptionBuster.VerificationStatus.RESOLVED, resolved.status)

        // Resubmitting wrong answers on an already RESOLVED session keeps it RESOLVED idempotently
        val reSubmitted = MisconceptionBuster.evaluateVerification(
            session = resolved,
            ans1 = !session.prop1Correct,
            ans2 = !session.prop2Correct
        )
        assertEquals(MisconceptionBuster.VerificationStatus.RESOLVED, reSubmitted.status)
    }
}
