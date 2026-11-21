package com.example.asm_final.learning

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MisconceptionFlowTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private class FakeMisconceptionRepository(
        override val ownerId: Int,
        var shouldSimulateOffline: Boolean = false
    ) : MisconceptionRepository {
        val storage = mutableMapOf<String, MisconceptionBusterSession>()
        val activeDrafts = mutableMapOf<String, MisconceptionBusterSession>()

        override suspend fun startSession(
            id: String,
            sessionId: String?,
            questionId: String,
            subjectId: String,
            grade: Int,
            questionPrompt: String,
            selectedOption: String?,
            misconceptionReason: String,
            reasonType: MisconceptionReasonType,
            aiHint: String,
            aiCounterexample: String,
            prop1Prompt: String,
            prop1Correct: Boolean,
            prop2Prompt: String,
            prop2Correct: Boolean
        ): MisconceptionBusterSession {
            val session = MisconceptionBusterSession(
                id = id,
                ownerId = ownerId,
                sessionId = sessionId,
                questionId = questionId,
                subjectId = subjectId,
                grade = grade,
                questionPrompt = questionPrompt,
                selectedOption = selectedOption,
                misconceptionReason = misconceptionReason,
                reasonType = reasonType,
                aiHint = aiHint,
                aiCounterexample = aiCounterexample,
                prop1Prompt = prop1Prompt,
                prop1Correct = prop1Correct,
                prop2Prompt = prop2Prompt,
                prop2Correct = prop2Correct,
                verificationStatus = MisconceptionVerificationStatus.NEEDS_REVIEW,
                isDraft = shouldSimulateOffline
            )
            storage[id] = session
            activeDrafts["${sessionId.orEmpty()}_$questionId"] = session
            return session
        }

        override suspend fun verifySession(
            id: String,
            prop1Ans: Boolean,
            prop2Ans: Boolean
        ): MisconceptionBusterSession {
            val session = storage[id] ?: throw IllegalStateException("Session not found")
            session.prop1UserAns = prop1Ans
            session.prop2UserAns = prop2Ans

            if (shouldSimulateOffline) {
                // Offline: must not grant RESOLVED
                session.verificationStatus = MisconceptionVerificationStatus.NEEDS_REVIEW
                val updated = session.copy(isDraft = true)
                storage[id] = updated
                return updated
            }

            // Server-side evaluation
            val resolved = (prop1Ans == session.prop1Correct) && (prop2Ans == session.prop2Correct)
            session.verificationStatus = if (resolved) MisconceptionVerificationStatus.RESOLVED else MisconceptionVerificationStatus.NEEDS_REVIEW
            val updated = session.copy(isDraft = false)
            storage[id] = updated
            return updated
        }

        override suspend fun getSession(id: String): MisconceptionBusterSession? = storage[id]

        override suspend fun getActiveDraft(sessionId: String?, questionId: String): MisconceptionBusterSession? =
            activeDrafts["${sessionId.orEmpty()}_$questionId"]

        override suspend fun syncDraft(session: MisconceptionBusterSession): MisconceptionBusterSession {
            val synced = session.copy(isDraft = false)
            storage[session.id] = synced
            return synced
        }
    }

    @Test
    fun flowAdvancesStepByStepWithPreAuthoredReason() = runTest(testDispatcher) {
        val repo = FakeMisconceptionRepository(ownerId = 101)
        val vm = MisconceptionViewModel(
            repository = repo,
            questionId = "q_test_01",
            sessionId = "sess_01",
            subjectId = "math",
            grade = 11,
            questionPrompt = "Tìm tập xác định của...",
            selectedOption = "B",
            correctOption = "A",
            explanation = "Chi tiết lời giải...",
            externalScope = testScope
        )

        assertEquals(MisconceptionStep.SELECT_REASON, vm.currentStep)

        // Select pre-authored reason
        vm.selectedReasonId = "math_reason_01"
        vm.proceedToHint()

        assertEquals(MisconceptionStep.REVIEW_HINT, vm.currentStep)
        assertNotNull(vm.activeSession)
        assertEquals(MisconceptionReasonType.PRE_AUTHORED, vm.activeSession?.reasonType)

        // Advance to test
        vm.proceedToTest()
        assertEquals(MisconceptionStep.TEST_PROPOSITIONS, vm.currentStep)

        // Answer propositions: both correct
        vm.prop1Answer = vm.activeSession?.prop1Correct
        vm.prop2Answer = vm.activeSession?.prop2Correct
        vm.submitVerifications()

        assertEquals(MisconceptionStep.RESULT, vm.currentStep)
        assertEquals(MisconceptionVerificationStatus.RESOLVED, vm.activeSession?.verificationStatus)
    }

    @Test
    fun partialOrIncorrectPropositionsKeepNeedsReviewStatus() = runTest(testDispatcher) {
        val repo = FakeMisconceptionRepository(ownerId = 101)
        val vm = MisconceptionViewModel(
            repository = repo,
            questionId = "q_test_02",
            sessionId = "sess_02",
            subjectId = "math",
            grade = 11,
            questionPrompt = "Giải phương trình ax^2+bx+c=0",
            selectedOption = "C",
            correctOption = "A",
            explanation = "Chú ý a=0",
            externalScope = testScope
        )

        vm.selectedReasonId = "math_reason_02"
        vm.proceedToHint()
        vm.proceedToTest()

        // 1 right, 1 wrong
        vm.prop1Answer = vm.activeSession?.prop1Correct
        vm.prop2Answer = !(vm.activeSession?.prop2Correct ?: true)
        vm.submitVerifications()

        assertEquals(MisconceptionStep.RESULT, vm.currentStep)
        assertEquals(MisconceptionVerificationStatus.NEEDS_REVIEW, vm.activeSession?.verificationStatus)
    }

    @Test
    fun offlineVerificationStaysAsDraftAndDoesNotGrantResolved() = runTest(testDispatcher) {
        val repo = FakeMisconceptionRepository(ownerId = 101, shouldSimulateOffline = true)
        val vm = MisconceptionViewModel(
            repository = repo,
            questionId = "q_test_offline",
            sessionId = "sess_offline",
            subjectId = "math",
            grade = 11,
            questionPrompt = "Phương trình sin x = 2",
            selectedOption = "A",
            correctOption = "D",
            explanation = "Vô nghiệm",
            externalScope = testScope
        )

        vm.selectedReasonId = "math_reason_03"
        vm.proceedToHint()
        vm.proceedToTest()

        vm.prop1Answer = vm.activeSession?.prop1Correct
        vm.prop2Answer = vm.activeSession?.prop2Correct
        vm.submitVerifications()

        assertEquals(MisconceptionStep.RESULT, vm.currentStep)
        // Must still be NEEDS_REVIEW and marked draft when offline!
        assertEquals(MisconceptionVerificationStatus.NEEDS_REVIEW, vm.activeSession?.verificationStatus)
        assertTrue(vm.activeSession?.isDraft == true)
    }

    @Test
    fun accountIsolationPreventsCrossUserDraftAccess() = runTest(testDispatcher) {
        val repoUser1 = FakeMisconceptionRepository(ownerId = 101)
        val repoUser2 = FakeMisconceptionRepository(ownerId = 102)

        val vmUser1 = MisconceptionViewModel(
            repository = repoUser1,
            questionId = "q_isolated",
            sessionId = "sess_isolated",
            subjectId = "math",
            grade = 11,
            questionPrompt = "Isolated Prompt",
            selectedOption = "A",
            correctOption = "B",
            explanation = "Expl",
            externalScope = testScope
        )

        vmUser1.selectedReasonId = "math_reason_01"
        vmUser1.proceedToHint()

        assertNotNull(repoUser1.getActiveDraft("sess_isolated", "q_isolated"))
        assertNull(repoUser2.getActiveDraft("sess_isolated", "q_isolated"))
    }
}
