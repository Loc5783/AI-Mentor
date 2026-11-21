package com.example.asm_final.learning

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.asm_final.ui.MisconceptionBusterScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MisconceptionFlowDeviceTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private class TestRepo(override val ownerId: Int) : MisconceptionRepository {
        val storage = mutableMapOf<String, MisconceptionBusterSession>()

        override suspend fun startSession(
            id: String,
            sessionId: String,
            questionId: String,
            reasonId: String
        ): MisconceptionBusterSession {
            val catalogItem = MisconceptionBuster.MATH_CATALOG.find { it.id == reasonId }
                ?: MisconceptionBuster.MATH_CATALOG[0]

            val session = MisconceptionBusterSession(
                id = id,
                ownerId = ownerId,
                sessionId = sessionId,
                questionId = questionId,
                subjectId = "math",
                grade = 11,
                questionPrompt = "Tìm nghiệm phương trình sin x = 2",
                selectedOption = "A",
                misconceptionReason = catalogItem.text,
                reasonType = MisconceptionReasonType.PRE_AUTHORED,
                aiHint = catalogItem.verifiedHint,
                aiCounterexample = catalogItem.counterexample,
                prop1Prompt = catalogItem.prop1Prompt,
                prop1Correct = catalogItem.prop1Correct,
                prop2Prompt = catalogItem.prop2Prompt,
                prop2Correct = catalogItem.prop2Correct,
                verificationStatus = MisconceptionVerificationStatus.NEEDS_REVIEW
            )
            storage[id] = session
            return session
        }

        override suspend fun verifySession(
            id: String,
            prop1Ans: Boolean,
            prop2Ans: Boolean
        ): MisconceptionBusterSession {
            val s = storage[id]!!
            s.prop1UserAns = prop1Ans
            s.prop2UserAns = prop2Ans
            val resolved = (prop1Ans == s.prop1Correct) && (prop2Ans == s.prop2Correct)
            val updated = s.copy(
                prop1UserAns = prop1Ans,
                prop2UserAns = prop2Ans,
                verificationStatus = if (resolved) MisconceptionVerificationStatus.RESOLVED else MisconceptionVerificationStatus.NEEDS_REVIEW
            )
            storage[id] = updated
            return updated
        }

        override suspend fun getSession(id: String): MisconceptionBusterSession? = storage[id]
        override suspend fun getActiveDraft(sessionId: String, questionId: String): MisconceptionBusterSession? = null
        override suspend fun syncDraft(session: MisconceptionBusterSession): MisconceptionBusterSession = session
    }

    @Test
    fun completeMisconceptionFlowOnDevice() {
        val repo = TestRepo(ownerId = 1)
        val vm = MisconceptionViewModel(
            repository = repo,
            questionId = "q_device_01",
            sessionId = "sess_dev_01",
            subjectId = "math",
            grade = 11,
            questionPrompt = "Tìm nghiệm phương trình sin x = 2",
            selectedOption = "A",
            correctOption = "D",
            explanation = "Hàm sin x có miền giá trị [-1; 1] nên phương trình vô nghiệm."
        )

        var closed = false
        composeTestRule.setContent {
            MisconceptionBusterScreen(
                viewModel = vm,
                onClose = { closed = true }
            )
        }

        // 1. Step 1: Select reason
        composeTestRule.onNodeWithTag("MisconceptionBusterScreen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("MisconceptionReason_math_reason_03").performClick()
        composeTestRule.onNodeWithTag("MisconceptionProceedHintBtn").performClick()

        // 2. Step 2: Review Hint
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("2. Phân tích & Phản ví dụ").assertIsDisplayed()
        composeTestRule.onNodeWithTag("MisconceptionProceedTestBtn").performClick()

        // 3. Step 3: Test Propositions
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("3. Kiểm chứng mệnh đề").assertIsDisplayed()

        // Mệnh đề 1: Sai (False), Mệnh đề 2: Đúng (True)
        composeTestRule.onNodeWithTag("MisconceptionProp1_False").performClick()
        composeTestRule.onNodeWithTag("MisconceptionProp2_True").performClick()
        composeTestRule.onNodeWithTag("MisconceptionSubmitVerificationsBtn").performClick()

        // 4. Step 4: Result -> RESOLVED
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("MisconceptionResultBanner").assertIsDisplayed()
        composeTestRule.onNodeWithText("ĐÃ PHÁ VỠ HIỂU SAI 🎉\nBạn đã trả lời chính xác cả 2 mệnh đề kiểm chứng!").assertIsDisplayed()

        composeTestRule.onNodeWithTag("MisconceptionDoneBtn").performClick()
        assertTrue(closed)
        assertEquals(MisconceptionVerificationStatus.RESOLVED, vm.activeSession?.verificationStatus)
    }

    @Test
    fun incorrectAnswersLeadToNeedsReview() {
        val repo = TestRepo(ownerId = 2)
        val vm = MisconceptionViewModel(
            repository = repo,
            questionId = "q_device_02",
            sessionId = "sess_dev_02",
            subjectId = "math",
            grade = 11,
            questionPrompt = "Tìm nghiệm phương trình sin x = 2",
            selectedOption = "B",
            correctOption = "D",
            explanation = "Vô nghiệm"
        )

        composeTestRule.setContent {
            MisconceptionBusterScreen(
                viewModel = vm,
                onClose = {}
            )
        }

        composeTestRule.onNodeWithTag("MisconceptionReason_math_reason_03").performClick()
        composeTestRule.onNodeWithTag("MisconceptionProceedHintBtn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("MisconceptionProceedTestBtn").performClick()
        composeTestRule.waitForIdle()

        // Click Wrong answers
        composeTestRule.onNodeWithTag("MisconceptionProp1_True").performClick()
        composeTestRule.onNodeWithTag("MisconceptionProp2_False").performClick()
        composeTestRule.onNodeWithTag("MisconceptionSubmitVerificationsBtn").performClick()

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("CẦN ÔN TẬP THÊM ⚠️\nBạn chưa trả lời đúng cả 2 mệnh đề kiểm chứng. Câu hỏi vẫn được giữ trong danh sách cần ôn.").assertIsDisplayed()
        assertEquals(MisconceptionVerificationStatus.NEEDS_REVIEW, vm.activeSession?.verificationStatus)
    }
}
