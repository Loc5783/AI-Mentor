package com.example.asm_final.learning

enum class MisconceptionVerificationStatus {
    NEEDS_REVIEW,
    RESOLVED
}

enum class MisconceptionReasonType {
    PRE_AUTHORED,
    CUSTOM
}

data class MisconceptionReasonItem(
    val reasonId: String,
    val subjectId: String,
    val grade: Int,
    val reasonText: String,
    val verifiedHint: String,
    val counterexample: String,
    val prop1Prompt: String,
    val prop2Prompt: String
)

data class MisconceptionBusterSession(
    val id: String,
    val ownerId: Int,
    val sessionId: String,
    val questionId: String,
    val reasonId: String,
    val subjectId: String,
    val grade: Int,
    val questionPrompt: String,
    val selectedOption: String?,
    val misconceptionReason: String,
    val reasonType: MisconceptionReasonType,
    val aiHint: String,
    val aiCounterexample: String,
    val prop1Prompt: String,
    var prop1UserAns: Boolean? = null,
    val prop2Prompt: String,
    var prop2UserAns: Boolean? = null,
    var verificationStatus: MisconceptionVerificationStatus = MisconceptionVerificationStatus.NEEDS_REVIEW,
    val answeredAt: Long = System.currentTimeMillis(),
    val serverUpdatedAt: Long? = null,
    val isDraft: Boolean = false
)
