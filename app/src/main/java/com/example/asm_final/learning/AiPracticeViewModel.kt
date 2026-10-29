package com.example.asm_final.learning

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.asm_final.backend.BackendClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch

enum class AiPracticeScreenState {
    SETUP,
    GENERATING,
    PRACTICING,
    FEEDBACK,
    SUMMARY
}

class AiPracticeViewModel(
    private val repository: AiPracticeRepository,
    private val scope: CoroutineScope? = null
) : ViewModel() {

    private val ownedJob = SupervisorJob((scope ?: viewModelScope).coroutineContext[Job])
    private val vmScope = CoroutineScope((scope ?: viewModelScope).coroutineContext + ownedJob)
    private var closed = false

    private fun isCurrent(generation: Long) = !closed && generation == requestGenerationToken

    private fun invalidateWork() {
        requestGenerationToken++
        ownedJob.cancelChildren()
        activeJob = null
        isSubmitting = false
        isReporting = false
        isExplainingMore = false
    }

    private var activeJob: Job? = null
    private var requestGenerationToken: Long = 0L
    private var generationPayload: List<String>? = null
    private var generationKey: String? = null
    private var answerPayload: List<String>? = null
    private var answerKey: String? = null

    var screenState by mutableStateOf(AiPracticeScreenState.SETUP)
        private set

    var config by mutableStateOf<AiPracticeConfig?>(null)
        private set
    var resumableSession by mutableStateOf<AiPracticeSession?>(null)
        private set

    var session by mutableStateOf<AiPracticeSession?>(null)
        private set

    var currentQuestionIndex by mutableIntStateOf(0)
        private set

    var selectedOption by mutableStateOf<String?>(null)

    var isSubmitting by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var lastResult by mutableStateOf<AiAnswerResult?>(null)
        private set

    var explainMoreText by mutableStateOf<String?>(null)
        private set

    var isExplainingMore by mutableStateOf(false)
        private set

    var reportStatus by mutableStateOf<String?>(null)
        private set

    var isReporting by mutableStateOf(false)
        private set

    val currentQuestion: AiQuestion?
        get() {
            val qList = session?.questions ?: return null
            if (currentQuestionIndex in qList.indices) {
                return qList[currentQuestionIndex]
            }
            return null
        }

    init {
        loadConfig()
    }

    fun loadConfig() {
        val generation = requestGenerationToken
        vmScope.launch {
            try {
                val loaded = repository.getConfig()
                if (!isCurrent(generation)) return@launch
                config = loaded
                val saved = repository.getResumableSession()
                if (!isCurrent(generation)) return@launch
                resumableSession = saved
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                if (!isCurrent(generation)) return@launch
                errorMessage = "Không thể tải cấu hình AI: ${e.message}"
            }
        }
    }

    fun startPractice(
        subjectId: String,
        grade: Int,
        topic: String,
        difficulty: String,
        questionCount: Int = 5
    ) {
        if (topic.isBlank()) {
            errorMessage = "Vui lòng nhập chủ đề cần luyện tập."
            return
        }

        if (closed) return
        invalidateWork()
        val token = requestGenerationToken
        val payload = listOf(subjectId, grade.toString(), topic.trim(), difficulty, questionCount.toString())
        if (generationPayload != payload || generationKey == null) {
            generationPayload = payload
            generationKey = java.util.UUID.randomUUID().toString()
        }
        val operationKey = generationKey!!
        errorMessage = null
        screenState = AiPracticeScreenState.GENERATING

        val generation = requestGenerationToken
        activeJob = vmScope.launch {
            try {
                val newSession = repository.createSession(
                    subjectId = subjectId,
                    grade = grade,
                    topic = topic.trim(),
                    difficulty = difficulty,
                    questionCount = questionCount,
                    idempotencyKey = operationKey
                )
                if (token == requestGenerationToken && isCurrent(generation)) {
                    generationKey = null
                    session = newSession
                    currentQuestionIndex = 0
                    selectedOption = null
                    lastResult = null
                    explainMoreText = null
                    screenState = AiPracticeScreenState.PRACTICING
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                if (!isCurrent(generation)) return@launch
                if (token == requestGenerationToken && isCurrent(generation)) {
                    if (e is BackendClient.Failure && e.code in setOf(
                            "AI_REQUEST_FAILED", "AI_PROVIDER_NOT_CONFIGURED", "AI_TIMEOUT",
                            "AI_RATE_LIMIT", "AI_MALFORMED_OUTPUT", "AI_VALIDATION_FAILED", "AI_PROVIDER_ERROR"
                        )) {
                        // A definite provider failure has no session to recover. A new
                        // user retry may start fresh; uncertain transport failures keep the key.
                        generationKey = null
                    }
                    errorMessage = e.message ?: "Tạo bài luyện tập thất bại. Vui lòng thử lại."
                    screenState = AiPracticeScreenState.SETUP
                }
            }
        }
    }

    fun cancelGeneration() {
        generationKey = null
        invalidateWork()
        errorMessage = null
        screenState = AiPracticeScreenState.SETUP
    }

    fun selectOption(option: String) {
        if (screenState == AiPracticeScreenState.PRACTICING && !isSubmitting) {
            selectedOption = option
            errorMessage = null
        }
    }

    fun resumePractice() {
        val saved = resumableSession ?: return
        if (closed || isSubmitting) return
        invalidateWork()
        val generation = requestGenerationToken
        isSubmitting = true
        vmScope.launch {
            try {
                val restored = repository.getSession(saved.id)
                if (!isCurrent(generation)) return@launch
                session = restored
                val unanswered = restored.questions.indexOfFirst { !restored.answers.containsKey(it.id) }
                currentQuestionIndex = if (unanswered >= 0) unanswered else (restored.questions.size - 1).coerceAtLeast(0)
                selectedOption = null
                lastResult = null
                errorMessage = null
                isSubmitting = false
                if (restored.state == "COMPLETED") {
                    screenState = AiPracticeScreenState.SUMMARY
                } else {
                    val q = currentQuestion
                    val answer = q?.let { restored.answers[it.id] }
                    if (q != null && answer != null && answer.revealed) {
                        lastResult = AiAnswerResult(q.id, answer.isCorrect, answer.selectedOption,
                            q.correctOption ?: "", q.explanation ?: "", q.hint,
                            answer.firstTryCorrect, answer.attemptsCount, answer.skipped)
                        screenState = AiPracticeScreenState.FEEDBACK
                    } else screenState = AiPracticeScreenState.PRACTICING
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (failure: Exception) {
                if (!isCurrent(generation)) return@launch
                isSubmitting = false
                errorMessage = "Chưa thể mở bài đang làm. Vui lòng thử lại khi có mạng."
            }
        }
    }

    fun submitAnswer() {
        if (isSubmitting) return
        val sess = session ?: return
        val q = currentQuestion ?: return
        val opt = selectedOption
        if (opt.isNullOrBlank()) {
            errorMessage = "Vui lòng chọn một phương án."
            return
        }

        val payload = listOf(sess.id, q.id, opt)
        if (answerPayload != payload || answerKey == null) {
            answerPayload = payload
            answerKey = java.util.UUID.randomUUID().toString()
        }
        val operationKey = answerKey!!

        isSubmitting = true
        errorMessage = null

        val generation = requestGenerationToken
        vmScope.launch {
            try {
                val result = repository.submitAnswerOperation(sess.id, q.id, opt, operationKey)
                if (!isCurrent(generation)) return@launch
                lastResult = result
                isSubmitting = false
                screenState = AiPracticeScreenState.FEEDBACK
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                if (!isCurrent(generation)) return@launch
                isSubmitting = false
                errorMessage = "Gửi đáp án thất bại: ${e.message}"
            }
        }
    }

    fun skipCurrentQuestion() {
        if (isSubmitting || closed) return
        val sess = session ?: return
        val q = currentQuestion ?: return

        isSubmitting = true
        errorMessage = null

        val generation = requestGenerationToken
        vmScope.launch {
            try {
                val result = repository.skipQuestion(sess.id, q.id)
                if (!isCurrent(generation)) return@launch
                lastResult = result
                isSubmitting = false
                screenState = AiPracticeScreenState.FEEDBACK
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                if (!isCurrent(generation)) return@launch
                isSubmitting = false
                errorMessage = "Bỏ qua câu hỏi thất bại: ${e.message}"
            }
        }
    }

    fun retryQuestion() {
        if (screenState == AiPracticeScreenState.FEEDBACK) {
            invalidateWork()
            answerKey = null
            selectedOption = null
            lastResult = null
            explainMoreText = null
            errorMessage = null
            screenState = AiPracticeScreenState.PRACTICING
        }
    }

    fun nextQuestion() {
        val sess = session ?: return
        val qList = sess.questions
        if (isSubmitting) return
        if (currentQuestionIndex + 1 < qList.size) {
            invalidateWork()
            currentQuestionIndex++
            selectedOption = null
            lastResult = null
            explainMoreText = null
            errorMessage = null
            screenState = AiPracticeScreenState.PRACTICING
        } else {
            finishSession()
        }
    }

    fun finishSession() {
        if (isSubmitting || closed) return
        val sess = session ?: return
        isSubmitting = true
        errorMessage = null

        val generation = requestGenerationToken
        vmScope.launch {
            try {
                val completed = repository.completeSession(sess.id)
                if (!isCurrent(generation)) return@launch
                session = completed
                isSubmitting = false
                screenState = AiPracticeScreenState.SUMMARY
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                if (!isCurrent(generation)) return@launch
                isSubmitting = false
                errorMessage = "Không thể hoàn thành phiên: ${e.message}"
            }
        }
    }

    fun reportQuestion(reason: String, details: String = "") {
        if (isReporting || closed) return
        val sess = session ?: return
        val q = currentQuestion ?: return

        isReporting = true
        reportStatus = null

        val generation = requestGenerationToken
        vmScope.launch {
            try {
                val msg = repository.reportQuestion(sess.id, q.id, reason, details)
                if (!isCurrent(generation)) return@launch
                reportStatus = msg
                isReporting = false
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                if (!isCurrent(generation)) return@launch
                isReporting = false
                reportStatus = "Gửi báo cáo thất bại: ${e.message}"
            }
        }
    }

    fun askAiMore(query: String) {
        if (isExplainingMore || closed) return
        val sess = session ?: return
        val q = currentQuestion ?: return
        if (query.isBlank()) return

        isExplainingMore = true
        explainMoreText = null

        val generation = requestGenerationToken
        vmScope.launch {
            try {
                val exp = repository.explainMore(sess.id, q.id, query.trim())
                if (!isCurrent(generation)) return@launch
                explainMoreText = exp
                isExplainingMore = false
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                if (!isCurrent(generation)) return@launch
                isExplainingMore = false
                explainMoreText = "Không thể kết nối Gia sư AI: ${e.message}"
            }
        }
    }

    fun resetToSetup() {
        invalidateWork()
        generationKey = null
        answerKey = null
        resumableSession = session?.takeIf { it.state != "COMPLETED" }
        session = null
        currentQuestionIndex = 0
        selectedOption = null
        lastResult = null
        explainMoreText = null
        errorMessage = null
        screenState = AiPracticeScreenState.SETUP
    }

    public override fun onCleared() {
        super.onCleared()
        closed = true
        invalidateWork()
        ownedJob.cancel()
    }
}
