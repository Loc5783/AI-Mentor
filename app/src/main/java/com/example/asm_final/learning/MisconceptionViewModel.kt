package com.example.asm_final.learning

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import java.util.UUID

enum class MisconceptionStep {
    SELECT_REASON,
    REVIEW_HINT,
    TEST_PROPOSITIONS,
    RESULT
}

class MisconceptionViewModel(
    private val repository: MisconceptionRepository,
    val questionId: String,
    val sessionId: String?,
    val subjectId: String,
    val grade: Int,
    val questionPrompt: String,
    val selectedOption: String?,
    val correctOption: String?,
    val explanation: String?,
    private val externalScope: CoroutineScope? = null
) : ViewModel() {

    private val ownedJob = SupervisorJob((externalScope ?: viewModelScope).coroutineContext[Job])
    private val vmScope = CoroutineScope((externalScope ?: viewModelScope).coroutineContext + ownedJob)
    private var closed = false
    private var requestGenerationToken: Long = 0L

    private fun isCurrent(generation: Long) = !closed && generation == requestGenerationToken

    private fun invalidateWork() {
        requestGenerationToken++
        ownedJob.cancelChildren()
        isLoading = false
    }

    var currentStep by mutableStateOf(MisconceptionStep.SELECT_REASON)
        private set

    var activeSession by mutableStateOf<MisconceptionBusterSession?>(null)
        private set

    var selectedReasonId by mutableStateOf<String?>(null)
    var customReasonText by mutableStateOf("")

    var prop1Answer by mutableStateOf<Boolean?>(null)
    var prop2Answer by mutableStateOf<Boolean?>(null)

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    init {
        restoreDraftOrStart()
    }

    private fun restoreDraftOrStart() {
        val generation = requestGenerationToken
        isLoading = true
        vmScope.launch {
            try {
                val draft = repository.getActiveDraft(sessionId, questionId)
                if (isCurrent(generation)) {
                    isLoading = false
                    if (draft != null) {
                        activeSession = draft
                        selectedReasonId = if (draft.reasonType == MisconceptionReasonType.PRE_AUTHORED) {
                            MisconceptionBuster.MATH_CATALOG.find { it.text == draft.misconceptionReason }?.id
                        } else null
                        customReasonText = if (draft.reasonType == MisconceptionReasonType.CUSTOM) draft.misconceptionReason else ""
                        prop1Answer = draft.prop1UserAns
                        prop2Answer = draft.prop2UserAns

                        currentStep = when {
                            draft.verificationStatus == MisconceptionVerificationStatus.RESOLVED -> MisconceptionStep.RESULT
                            draft.prop1UserAns != null && draft.prop2UserAns != null -> MisconceptionStep.RESULT
                            else -> MisconceptionStep.REVIEW_HINT
                        }
                    }
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                if (isCurrent(generation)) {
                    isLoading = false
                }
            }
        }
    }

    fun proceedToHint() {
        if (isLoading) return
        val reasonId = selectedReasonId
        val customText = customReasonText.trim()

        if (reasonId.isNullOrBlank() && customText.isBlank()) {
            errorMessage = "Vui lòng chọn hoặc nhập nguyên nhân bạn nghĩ mình đã nhầm lẫn."
            return
        }

        errorMessage = null
        isLoading = true
        val generation = requestGenerationToken

        vmScope.launch {
            try {
                val reasonInput = if (!reasonId.isNullOrBlank()) reasonId else customText
                val catalogMatch = MisconceptionBuster.MATH_CATALOG.find { it.id == reasonInput || it.text == reasonInput }

                val reasonType = if (catalogMatch != null) MisconceptionReasonType.PRE_AUTHORED else MisconceptionReasonType.CUSTOM
                val reasonTitle = catalogMatch?.text ?: customText.take(150)
                val hint = catalogMatch?.verifiedHint ?: MisconceptionBuster.MATH_CATALOG[0].verifiedHint
                val counterexample = catalogMatch?.counterexample ?: MisconceptionBuster.MATH_CATALOG[0].counterexample
                val p1Prompt = catalogMatch?.prop1Prompt ?: MisconceptionBuster.MATH_CATALOG[0].prop1Prompt
                val p1Correct = catalogMatch?.prop1Correct ?: MisconceptionBuster.MATH_CATALOG[0].prop1Correct
                val p2Prompt = catalogMatch?.prop2Prompt ?: MisconceptionBuster.MATH_CATALOG[0].prop2Prompt
                val p2Correct = catalogMatch?.prop2Correct ?: MisconceptionBuster.MATH_CATALOG[0].prop2Correct

                val sessionIdToUse = activeSession?.id ?: UUID.randomUUID().toString()

                val session = repository.startSession(
                    id = sessionIdToUse,
                    sessionId = sessionId,
                    questionId = questionId,
                    subjectId = subjectId,
                    grade = grade,
                    questionPrompt = questionPrompt,
                    selectedOption = selectedOption,
                    misconceptionReason = reasonTitle,
                    reasonType = reasonType,
                    aiHint = hint,
                    aiCounterexample = counterexample,
                    prop1Prompt = p1Prompt,
                    prop1Correct = p1Correct,
                    prop2Prompt = p2Prompt,
                    prop2Correct = p2Correct
                )

                if (isCurrent(generation)) {
                    activeSession = session
                    isLoading = false
                    currentStep = MisconceptionStep.REVIEW_HINT
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                if (isCurrent(generation)) {
                    isLoading = false
                    errorMessage = "Chưa thể khởi tạo phân tích. Vui lòng thử lại."
                }
            }
        }
    }

    fun proceedToTest() {
        if (currentStep == MisconceptionStep.REVIEW_HINT) {
            errorMessage = null
            currentStep = MisconceptionStep.TEST_PROPOSITIONS
        }
    }

    fun submitVerifications() {
        if (isLoading) return
        val sess = activeSession ?: return
        val ans1 = prop1Answer
        val ans2 = prop2Answer

        if (ans1 == null || ans2 == null) {
            errorMessage = "Vui lòng chọn Đúng hoặc Sai cho cả 2 mệnh đề kiểm chứng."
            return
        }

        isLoading = true
        errorMessage = null
        val generation = requestGenerationToken

        vmScope.launch {
            try {
                val updated = repository.verifySession(sess.id, ans1, ans2)
                if (isCurrent(generation)) {
                    activeSession = updated
                    isLoading = false
                    currentStep = MisconceptionStep.RESULT
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                if (isCurrent(generation)) {
                    isLoading = false
                    errorMessage = "Chưa kết nối được máy chủ để chấm. Bản nháp đã được lưu an toàn."
                    // In case of error, still advance to result screen showing NEEDS_REVIEW / draft state safely
                    currentStep = MisconceptionStep.RESULT
                }
            }
        }
    }

    fun retryVerification() {
        if (isLoading) return
        val sess = activeSession ?: return
        if (sess.verificationStatus == MisconceptionVerificationStatus.RESOLVED) return
        prop1Answer = null
        prop2Answer = null
        errorMessage = null
        currentStep = MisconceptionStep.TEST_PROPOSITIONS
    }

    fun backToPreviousStep() {
        when (currentStep) {
            MisconceptionStep.REVIEW_HINT -> currentStep = MisconceptionStep.SELECT_REASON
            MisconceptionStep.TEST_PROPOSITIONS -> currentStep = MisconceptionStep.REVIEW_HINT
            else -> {}
        }
    }

    override fun onCleared() {
        closed = true
        invalidateWork()
        super.onCleared()
    }
}
