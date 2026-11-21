package com.example.asm_final.learning

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
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
    val sessionId: String,
    val subjectId: String,
    val grade: Int,
    val questionPrompt: String,
    val selectedOption: String?,
    val correctOption: String?,
    val explanation: String?,
    private val context: Context? = null,
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

    var prop1Answer by mutableStateOf<Boolean?>(null)
    var prop2Answer by mutableStateOf<Boolean?>(null)

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    private val connectivity = context?.applicationContext?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    @Volatile private var wasOnline = isOnline()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = checkReconnect()
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) = checkReconnect()
    }

    private fun isOnline(): Boolean {
        val cm = connectivity ?: return false
        val active = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(active) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun checkReconnect() {
        val online = isOnline()
        if (online && !wasOnline) {
            triggerSyncDraft()
        }
        wasOnline = online
    }

    init {
        runCatching { connectivity?.registerDefaultNetworkCallback(networkCallback) }
        restoreDraftOrStart()
    }

    fun triggerSyncDraft() {
        val current = activeSession ?: return
        if (!current.isDraft || closed) return
        val generation = requestGenerationToken
        vmScope.launch {
            try {
                val synced = repository.syncDraft(current)
                if (isCurrent(generation)) {
                    activeSession = synced
                }
            } catch (_: Exception) {}
        }
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
                        selectedReasonId = MisconceptionBuster.MATH_CATALOG.find { it.text == draft.misconceptionReason }?.id
                        prop1Answer = draft.prop1UserAns
                        prop2Answer = draft.prop2UserAns

                        currentStep = when {
                            draft.verificationStatus == MisconceptionVerificationStatus.RESOLVED -> MisconceptionStep.RESULT
                            draft.prop1UserAns != null && draft.prop2UserAns != null -> MisconceptionStep.RESULT
                            else -> MisconceptionStep.REVIEW_HINT
                        }

                        // Try to sync with server if draft is pending
                        if (draft.isDraft) {
                            triggerSyncDraft()
                        }
                    }
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (_: Exception) {
                if (isCurrent(generation)) {
                    isLoading = false
                }
            }
        }
    }

    fun proceedToHint() {
        if (isLoading) return
        val reasonId = selectedReasonId

        if (reasonId.isNullOrBlank()) {
            errorMessage = "Vui lòng chọn nguyên nhân bạn nghĩ mình đã nhầm lẫn."
            return
        }

        errorMessage = null
        isLoading = true
        val generation = requestGenerationToken

        vmScope.launch {
            try {
                val sessionIdToUse = activeSession?.id ?: UUID.randomUUID().toString()

                val session = repository.startSession(
                    id = sessionIdToUse,
                    sessionId = sessionId,
                    questionId = questionId,
                    reasonId = reasonId
                )

                if (isCurrent(generation)) {
                    activeSession = session
                    isLoading = false
                    currentStep = MisconceptionStep.REVIEW_HINT
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (_: Exception) {
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
            } catch (_: Exception) {
                if (isCurrent(generation)) {
                    isLoading = false
                    errorMessage = "Chưa kết nối được máy chủ để chấm. Bản nháp đã được lưu an toàn."
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
        runCatching { connectivity?.unregisterNetworkCallback(networkCallback) }
        invalidateWork()
        super.onCleared()
    }
}
