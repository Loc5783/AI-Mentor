package com.example.asm_final.learning

import com.example.asm_final.backend.BackendClient
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

    private class TestRepo(
        override val ownerId: Int,
        var shouldSimulateOffline: Boolean = false,
        var serverHasResolvedConflict: Boolean = false,
        var rejectStart: Boolean = false,
        var rejectVerify: Boolean = false
    ) : MisconceptionRepository {
        private val catalog = listOf(
            MisconceptionReasonItem(
                "math_reason_01", "math", 10, "Grade 10 reason", "Hint 10",
                "Counterexample 10", "Proposition 1", "Proposition 2"
            ),
            MisconceptionReasonItem(
                "math_reason_03", "math", 11, "Grade 11 reason", "Hint 11",
                "Counterexample 11", "Proposition 1", "Proposition 2"
            )
        )
        val sessions = mutableMapOf<String, MisconceptionBusterSession>()
        val drafts = mutableMapOf<String, MisconceptionBusterSession>()

        override suspend fun getVerifiedReasons(subjectId: String, grade: Int): List<MisconceptionReasonItem> =
            catalog.filter { it.subjectId == subjectId && it.grade == grade }

        override suspend fun startSession(
            id: String,
            sessionId: String,
            questionId: String,
            subjectId: String,
            grade: Int,
            questionPrompt: String,
            selectedOption: String?,
            reason: MisconceptionReasonItem
        ): MisconceptionBusterSession {
            if (rejectStart) throw BackendClient.Failure("QUESTION_NOT_ANSWERED_WRONGLY", 409)
            require(reason.subjectId == subjectId && reason.grade == grade)
            val session = MisconceptionBusterSession(
                id = id,
                ownerId = ownerId,
                sessionId = sessionId,
                questionId = questionId,
                reasonId = reason.reasonId,
                subjectId = subjectId,
                grade = grade,
                questionPrompt = questionPrompt,
                selectedOption = selectedOption,
                misconceptionReason = reason.reasonText,
                reasonType = MisconceptionReasonType.PRE_AUTHORED,
                aiHint = reason.verifiedHint,
                aiCounterexample = reason.counterexample,
                prop1Prompt = reason.prop1Prompt,
                prop2Prompt = reason.prop2Prompt,
                isDraft = shouldSimulateOffline
            )
            sessions[id] = session
            drafts["${sessionId}_$questionId"] = session
            return session
        }

        override suspend fun verifySession(
            id: String,
            prop1Ans: Boolean,
            prop2Ans: Boolean
        ): MisconceptionBusterSession {
            if (rejectVerify) throw BackendClient.Failure("SESSION_NOT_FOUND", 404)
            val current = sessions[id] ?: throw BackendClient.Failure("SESSION_NOT_FOUND", 404)
            val verified = current.copy(
                prop1UserAns = prop1Ans,
                prop2UserAns = prop2Ans,
                verificationStatus = if (!shouldSimulateOffline && !rejectVerify &&
                    prop1Ans == false && prop2Ans == true
                ) MisconceptionVerificationStatus.RESOLVED
                else MisconceptionVerificationStatus.NEEDS_REVIEW,
                answeredAt = System.currentTimeMillis(),
                isDraft = shouldSimulateOffline
            )
            sessions[id] = verified
            return verified
        }

        override suspend fun getSession(id: String): MisconceptionBusterSession? {
            val session = sessions[id] ?: return null
            return if (serverHasResolvedConflict) {
                session.copy(verificationStatus = MisconceptionVerificationStatus.RESOLVED, isDraft = false)
            } else session
        }

        override suspend fun getActiveDraft(sessionId: String, questionId: String): MisconceptionBusterSession? =
            drafts["${sessionId}_$questionId"]

        override suspend fun syncDraft(session: MisconceptionBusterSession): MisconceptionBusterSession {
            if (serverHasResolvedConflict) {
                return session.copy(verificationStatus = MisconceptionVerificationStatus.RESOLVED, isDraft = false)
                    .also { sessions[session.id] = it }
            }
            return session.copy(isDraft = false).also { sessions[session.id] = it }
        }
    }

    @Test
    fun flowUsesOnlyMatchingServerCatalogReasonAndServerStatus() = runTest(testDispatcher) {
        val repo = TestRepo(ownerId = 101)
        val vm = MisconceptionViewModel(
            repository = repo,
            questionId = "q_test_01",
            sessionId = "sess_01",
            subjectId = "math",
            grade = 10,
            questionPrompt = "Tìm tập xác định của...",
            selectedOption = "B",
            correctOption = "A",
            explanation = "Chi tiết lời giải...",
            externalScope = testScope
        )

        assertEquals(MisconceptionStep.SELECT_REASON, vm.currentStep)
        assertEquals(listOf("math_reason_01"), vm.verifiedReasons.map { it.reasonId })

        vm.selectedReasonId = "math_reason_01"
        vm.proceedToHint()
        assertEquals(MisconceptionStep.REVIEW_HINT, vm.currentStep)
        assertEquals(MisconceptionReasonType.PRE_AUTHORED, vm.activeSession?.reasonType)

        vm.proceedToTest()
        vm.prop1Answer = false
        vm.prop2Answer = true
        vm.submitVerifications()

        assertEquals(MisconceptionStep.RESULT, vm.currentStep)
        assertEquals(MisconceptionVerificationStatus.RESOLVED, vm.activeSession?.verificationStatus)
    }

    @Test
    fun rejectedStartDoesNotBecomeAnOfflineDraft() = runTest(testDispatcher) {
        val repo = TestRepo(ownerId = 101, rejectStart = true)
        val vm = MisconceptionViewModel(
            repository = repo,
            questionId = "q_rejected",
            sessionId = "sess_rejected",
            subjectId = "math",
            grade = 10,
            questionPrompt = "Prompt",
            selectedOption = "B",
            correctOption = "A",
            explanation = "Explanation",
            externalScope = testScope
        )

        vm.selectedReasonId = "math_reason_01"
        vm.proceedToHint()

        assertNull(vm.activeSession)
        assertEquals(MisconceptionStep.SELECT_REASON, vm.currentStep)
        assertTrue(vm.errorMessage?.contains("Máy chủ từ chối") == true)
    }

    @Test
    fun rejectedVerificationDoesNotShowAnOfflineResult() = runTest(testDispatcher) {
        val repo = TestRepo(ownerId = 101, rejectVerify = true)
        val vm = MisconceptionViewModel(
            repository = repo,
            questionId = "q_rejected",
            sessionId = "sess_rejected",
            subjectId = "math",
            grade = 10,
            questionPrompt = "Prompt",
            selectedOption = "B",
            correctOption = "A",
            explanation = "Explanation",
            externalScope = testScope
        )

        vm.selectedReasonId = "math_reason_01"
        vm.proceedToHint()
        vm.proceedToTest()
        vm.prop1Answer = false
        vm.prop2Answer = true
        vm.submitVerifications()

        assertEquals(MisconceptionStep.TEST_PROPOSITIONS, vm.currentStep)
        assertFalse(vm.activeSession?.isDraft == true)
        assertTrue(vm.errorMessage?.contains("Không tìm thấy phiên") == true)
    }

    @Test
    fun offlineVerificationRemainsPendingUntilSync() = runTest(testDispatcher) {
        val repo = TestRepo(ownerId = 101, shouldSimulateOffline = true)
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
        vm.prop1Answer = false
        vm.prop2Answer = true
        vm.submitVerifications()

        assertEquals(MisconceptionStep.RESULT, vm.currentStep)
        assertEquals(MisconceptionVerificationStatus.NEEDS_REVIEW, vm.activeSession?.verificationStatus)
        assertTrue(vm.activeSession?.isDraft == true)
    }

    @Test
    fun syncKeepsNewerServerResolvedState() = runTest(testDispatcher) {
        val repo = TestRepo(ownerId = 101, shouldSimulateOffline = true, serverHasResolvedConflict = true)
        val vm = MisconceptionViewModel(
            repository = repo,
            questionId = "q_conflict",
            sessionId = "sess_conflict",
            subjectId = "math",
            grade = 10,
            questionPrompt = "Prompt",
            selectedOption = "A",
            correctOption = "B",
            explanation = "Explanation",
            externalScope = testScope
        )

        vm.selectedReasonId = "math_reason_01"
        vm.proceedToHint()
        assertTrue(vm.activeSession?.isDraft == true)

        vm.triggerSyncDraft()

        assertEquals(MisconceptionVerificationStatus.RESOLVED, vm.activeSession?.verificationStatus)
        assertFalse(vm.activeSession?.isDraft == true)
    }

    @Test
    fun localDraftsAreIsolatedByAccountOwner() = runTest(testDispatcher) {
        val repoUser1 = TestRepo(ownerId = 101)
        val repoUser2 = TestRepo(ownerId = 102)
        val user1 = MisconceptionViewModel(
            repoUser1, "q_isolated", "sess_isolated", "math", 10,
            "Prompt", "A", "B", "Explanation", externalScope = testScope
        )

        user1.selectedReasonId = "math_reason_01"
        user1.proceedToHint()

        assertNotNull(repoUser1.getActiveDraft("sess_isolated", "q_isolated"))
        assertNull(repoUser2.getActiveDraft("sess_isolated", "q_isolated"))
    }
}
