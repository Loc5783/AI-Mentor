package com.example.asm_final.learning

import org.json.JSONObject
import java.util.UUID

/**
 * Xưởng Phá Mệnh Đề (Misconception Buster)
 * 3-phase flow for diagnosing and resolving student misconceptions on propositions & math questions.
 */
object MisconceptionBuster {

    enum class VerificationStatus {
        NEEDS_REVIEW,
        RESOLVED
    }

    data class PreAuthoredReason(
        val id: String,
        val text: String,
        val verifiedHint: String,
        val counterexample: String,
        val prop1Prompt: String,
        val prop1Correct: Boolean,
        val prop2Prompt: String,
        val prop2Correct: Boolean
    )

    data class MisconceptionSession(
        val id: String,
        val userId: Int,
        val questionId: String,
        val subjectId: String,
        val grade: Int,
        val misconceptionReason: String,
        val isCustomReason: Boolean,
        val hint: String,
        val counterexample: String,
        val prop1Prompt: String,
        val prop1Correct: Boolean,
        var prop1UserAns: Boolean? = null,
        val prop2Prompt: String,
        val prop2Correct: Boolean,
        var prop2UserAns: Boolean? = null,
        var status: VerificationStatus = VerificationStatus.NEEDS_REVIEW
    )

    // Pre-verified Math misconceptions catalog
    val MATH_CATALOG = listOf(
        PreAuthoredReason(
            id = "math_reason_01",
            text = "Nhầm lẫn giữa mệnh đề đảo và mệnh đề phản đảo (Không suy ra hai chiều)",
            verifiedHint = "Mệnh đề P ⇒ Q không có nghĩa Q ⇒ P luôn đúng. Phản ví dụ: x = 2 ⇒ x² = 4, nhưng x² = 4 ⇒ x = 2 hoặc x = -2.",
            counterexample = "Nếu x² = 9 thì x có nhất thiết bằng 3 không? Không, vì x có thể bằng -3.",
            prop1Prompt = "Mệnh đề 'Nếu x > 2 thì x² > 4' có mệnh đề đảo luôn đúng?",
            prop1Correct = false,
            prop2Prompt = "Nếu x² > 4 thì x có nhất thiết phải lớn hơn 2 không?",
            prop2Correct = false
        ),
        PreAuthoredReason(
            id = "math_reason_02",
            text = "Quên trường hợp hệ số a = 0 khi xét phương trình bậc hai ax² + bx + c = 0",
            verifiedHint = "Khi hệ số a chứa tham số m, nếu a = 0 phương trình trở thành bậc nhất bx + c = 0, có thể có 1 nghiệm thay vì vô nghiệm hoặc 2 nghiệm.",
            counterexample = "Xét (m-1)x² + 2x + 1 = 0 với m = 1: phương trình thành 2x + 1 = 0, có nghiệm x = -1/2.",
            prop1Prompt = "Phương trình (m-1)x² + 2x + 1 = 0 luôn là phương trình bậc hai với mọi m?",
            prop1Correct = false,
            prop2Prompt = "Khi m = 1, phương trình (m-1)x² + 2x + 1 = 0 có đúng 1 nghiệm?",
            prop2Correct = true
        ),
        PreAuthoredReason(
            id = "math_reason_03",
            text = "Nhầm lẫn tập giá trị của sin x và cos x khi giải phương trình",
            verifiedHint = "Với mọi x ∈ R, ta luôn có -1 ≤ sin x ≤ 1 và -1 ≤ cos x ≤ 1. Phương trình sin x = 2 vô nghiệm.",
            counterexample = "sin x = 1.5 không có nghiệm thực nào.",
            prop1Prompt = "Phương trình sin x = -1.2 có nghiệm thực x?",
            prop1Correct = false,
            prop2Prompt = "Tập giá trị của hàm số y = 2 cos x là [-2; 2]?",
            prop2Correct = true
        )
    )

    /**
     * Phase 1 & 2: Start a Misconception Buster session.
     * Uses pre-authored verified content if matching reason ID, or AI / fallback.
     */
    fun startSession(
        userId: Int,
        questionId: String,
        subjectId: String,
        grade: Int,
        reasonInput: String,
        aiResponseJson: String? = null
    ): MisconceptionSession {
        val trimmedReason = reasonInput.trim().take(150)
        val preAuthored = MATH_CATALOG.find { it.id == trimmedReason || it.text == trimmedReason }

        if (preAuthored != null) {
            return MisconceptionSession(
                id = UUID.randomUUID().toString(),
                userId = userId,
                questionId = questionId,
                subjectId = subjectId,
                grade = grade,
                misconceptionReason = preAuthored.text,
                isCustomReason = false,
                hint = preAuthored.verifiedHint,
                counterexample = preAuthored.counterexample,
                prop1Prompt = preAuthored.prop1Prompt,
                prop1Correct = preAuthored.prop1Correct,
                prop2Prompt = preAuthored.prop2Prompt,
                prop2Correct = preAuthored.prop2Correct
            )
        }

        // Custom reason: try parsing AI response JSON if valid, else fallback to catalog[0]
        val parsedAi = runCatching {
            if (aiResponseJson.isNullOrBlank()) null else JSONObject(aiResponseJson)
        }.getOrNull()

        val hint = parsedAi?.optString("hint")?.takeIf { it.isNotBlank() }
            ?: MATH_CATALOG[0].verifiedHint
        val counterexample = parsedAi?.optString("counterexample")?.takeIf { it.isNotBlank() }
            ?: MATH_CATALOG[0].counterexample
        val p1 = parsedAi?.optString("prop1Prompt")?.takeIf { it.isNotBlank() }
            ?: MATH_CATALOG[0].prop1Prompt
        val p1Ans = parsedAi?.optBoolean("prop1Correct") ?: MATH_CATALOG[0].prop1Correct
        val p2 = parsedAi?.optString("prop2Prompt")?.takeIf { it.isNotBlank() }
            ?: MATH_CATALOG[0].prop2Prompt
        val p2Ans = parsedAi?.optBoolean("prop2Correct") ?: MATH_CATALOG[0].prop2Correct

        return MisconceptionSession(
            id = UUID.randomUUID().toString(),
            userId = userId,
            questionId = questionId,
            subjectId = subjectId,
            grade = grade,
            misconceptionReason = trimmedReason,
            isCustomReason = true,
            hint = hint,
            counterexample = counterexample,
            prop1Prompt = p1,
            prop1Correct = p1Ans,
            prop2Prompt = p2,
            prop2Correct = p2Ans
        )
    }

    /**
     * Phase 3: Submit student answers for the two test propositions.
     * RESOLVED status is granted ONLY IF BOTH propositions are answered correctly!
     * Submitting is idempotent.
     */
    fun evaluateVerification(
        session: MisconceptionSession,
        ans1: Boolean,
        ans2: Boolean
    ): MisconceptionSession {
        // If already resolved, return without double-updating
        if (session.status == VerificationStatus.RESOLVED) {
            return session
        }

        session.prop1UserAns = ans1
        session.prop2UserAns = ans2

        val correct1 = (ans1 == session.prop1Correct)
        val correct2 = (ans2 == session.prop2Correct)

        if (correct1 && correct2) {
            session.status = VerificationStatus.RESOLVED
        } else {
            session.status = VerificationStatus.NEEDS_REVIEW
        }

        return session
    }
}
