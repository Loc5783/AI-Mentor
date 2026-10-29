package com.example.asm_final.learning

import android.os.SystemClock
import com.example.asm_final.backend.BackendClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ExamScreenState {
    LIST,
    DETAIL,
    IN_PROGRESS,
    RESULT
}

enum class AnswerSaveStatus {
    CONFIRMED,
    SAVING,
    FAILED_UNSAVED
}

data class ExamUiState(
    val screenState: ExamScreenState = ExamScreenState.LIST,
    // Filter & List state
    val selectedSubjectId: String? = null,
    val selectedGrade: Int = 10,
    val selectedExamType: String? = null, // null = all, "MIDTERM", "FINAL"
    val selectedSemester: Int? = null, // null = all, 1, 2
    val exams: List<ExamSummary> = emptyList(),
    val activeSessionsByExamId: Map<String, ExamSession> = emptyMap(),
    val isLoadingExams: Boolean = false,
    val examListError: String? = null,

    // Detail state
    val selectedExam: ExamSummary? = null,
    val isStartingSession: Boolean = false,
    val startSessionError: String? = null,

    // In-Progress state
    val currentSession: ExamSession? = null,
    val currentQuestionIndex: Int = 0,
    val localAnswers: Map<String, String> = emptyMap(), // questionId -> selectedOption (visible on UI)
    val confirmedAnswers: Map<String, String> = emptyMap(), // questionId -> confirmedOption on server
    val answerSaveStatuses: Map<String, AnswerSaveStatus> = emptyMap(), // questionId -> AnswerSaveStatus
    val answerSaveErrors: Map<String, String> = emptyMap(), // questionId -> error message
    val savingAnswers: Map<String, Boolean> = emptyMap(), // questionId -> isSaving (backward compat)
    val questionRevisions: Map<String, Int> = emptyMap(), // questionId -> revision
    val remainingSeconds: Long = 0L,
    val isTimerCritical: Boolean = false,
    val showSubmitConfirmDialog: Boolean = false,
    val isSubmitting: Boolean = false,
    val submitError: String? = null,

    // Result & Review state
    val completedSession: ExamSession? = null,
    val reviewQuestionIndex: Int = 0
) {
    val unansweredCount: Int
        get() {
            val session = currentSession ?: return 0
            val total = session.questions.size
            val answered = session.questions.count { q ->
                val ans = localAnswers[q.id]
                !ans.isNullOrBlank()
            }
            return (total - answered).coerceAtLeast(0)
        }

    val unsavedAnswersCount: Int
        get() = answerSaveStatuses.count { it.value == AnswerSaveStatus.FAILED_UNSAVED }

    val inFlightSavingCount: Int
        get() = answerSaveStatuses.count { it.value == AnswerSaveStatus.SAVING }
}

class ExamPrepViewModel(
    private val repository: ExamRepository,
    private val scope: CoroutineScope,
    private val monotonicClockProvider: () -> Long = { SystemClock.elapsedRealtime() / 1000L },
    private val wallClockProvider: () -> Long = { System.currentTimeMillis() / 1000L }
) {
    // Secondary constructor for backward compatibility with tests/callers specifying clockProvider
    constructor(
        repository: ExamRepository,
        scope: CoroutineScope,
        clockProvider: () -> Long
    ) : this(
        repository = repository,
        scope = scope,
        monotonicClockProvider = clockProvider,
        wallClockProvider = clockProvider
    )

    private val _uiState = MutableStateFlow(ExamUiState())
    val uiState: StateFlow<ExamUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var requestGeneration = 0
    private var controllerGeneration = 0
    private var isCleared = false

    private var loadExamsJob: Job? = null
    private var startExamJob: Job? = null
    private var submitJob: Job? = null
    private val saveAnswerJobs = mutableMapOf<String, Job>()

    // Monotonic server-sync anchors
    private var serverBaseEpochSeconds: Long = 0L
    private var localBaseElapsedSeconds: Long = 0L
    private var deadlineEpochSeconds: Long = 0L

    fun calculateRemainingSeconds(): Long {
        if (deadlineEpochSeconds <= 0L) return 0L
        val elapsedSinceSync = (monotonicClockProvider() - localBaseElapsedSeconds).coerceAtLeast(0L)
        val estimatedServerNow = serverBaseEpochSeconds + elapsedSinceSync
        return (deadlineEpochSeconds - estimatedServerNow).coerceAtLeast(0L)
    }

    fun init(initialSubjectId: String? = null, initialGrade: Int = 10) {
        isCleared = false
        _uiState.update {
            it.copy(
                selectedSubjectId = initialSubjectId,
                selectedGrade = if (initialGrade in 10..12) initialGrade else 10
            )
        }
        loadExams()
    }

    fun setSubjectFilter(subjectId: String?) {
        _uiState.update { it.copy(selectedSubjectId = subjectId) }
        loadExams()
    }

    fun setGradeFilter(grade: Int) {
        if (grade in 10..12) {
            _uiState.update { it.copy(selectedGrade = grade) }
            loadExams()
        }
    }

    fun setExamTypeFilter(examType: String?) {
        _uiState.update { it.copy(selectedExamType = examType) }
        loadExams()
    }

    fun setSemesterFilter(semester: Int?) {
        _uiState.update { it.copy(selectedSemester = semester) }
        loadExams()
    }

    fun loadExams(): Job {
        loadExamsJob?.cancel()
        val gen = ++requestGeneration
        val callGen = controllerGeneration
        val state = _uiState.value
        _uiState.update { it.copy(isLoadingExams = true, examListError = null) }

        val job = scope.launch {
            try {
                val exams = repository.listExams(
                    subjectId = state.selectedSubjectId,
                    grade = state.selectedGrade,
                    examType = state.selectedExamType,
                    semester = state.selectedSemester
                )
                val userSessions = try {
                    repository.listUserSessions(
                        subjectId = state.selectedSubjectId,
                        grade = state.selectedGrade
                    )
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    emptyList()
                }

                val activeMap = mutableMapOf<String, ExamSession>()
                for (s in userSessions) {
                    if (s.state == "IN_PROGRESS") {
                        activeMap[s.examId] = s
                    }
                }

                if (gen == requestGeneration && callGen == controllerGeneration && !isCleared) {
                    _uiState.update {
                        it.copy(
                            exams = exams,
                            activeSessionsByExamId = activeMap,
                            isLoadingExams = false,
                            examListError = null
                        )
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                if (gen == requestGeneration && callGen == controllerGeneration && !isCleared) {
                    _uiState.update {
                        it.copy(
                            isLoadingExams = false,
                            examListError = e.message ?: "Không thể tải danh sách đề thi"
                        )
                    }
                }
            } finally {
                if (loadExamsJob === coroutineContext[Job]) {
                    loadExamsJob = null
                }
            }
        }
        loadExamsJob = job
        return job
    }

    fun selectExam(exam: ExamSummary) {
        _uiState.update {
            it.copy(
                selectedExam = exam,
                screenState = ExamScreenState.DETAIL,
                startSessionError = null
            )
        }
    }

    fun backToList() {
        stopTimer()
        startExamJob?.cancel()
        startExamJob = null
        submitJob?.cancel()
        submitJob = null
        saveAnswerJobs.values.forEach { it.cancel() }
        saveAnswerJobs.clear()

        _uiState.update {
            it.copy(
                screenState = ExamScreenState.LIST,
                selectedExam = null,
                startSessionError = null,
                isSubmitting = false,
                submitError = null
            )
        }
        loadExams()
    }

    fun startOrResumeExam(): Job? {
        val exam = _uiState.value.selectedExam ?: return null
        startExamJob?.cancel()
        val gen = ++requestGeneration
        val callGen = controllerGeneration
        _uiState.update { it.copy(isStartingSession = true, startSessionError = null) }

        val job = scope.launch {
            try {
                val session = repository.startSession(exam.id)
                if (gen != requestGeneration || callGen != controllerGeneration || isCleared) return@launch

                if (session.state == "SUBMITTED" || session.state == "TIMED_OUT") {
                    // Session already completed
                    _uiState.update {
                        it.copy(
                            isStartingSession = false,
                            completedSession = session,
                            screenState = ExamScreenState.RESULT
                        )
                    }
                } else {
                    // IN_PROGRESS session
                    enterSession(session)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                if (gen == requestGeneration && callGen == controllerGeneration && !isCleared) {
                    _uiState.update {
                        it.copy(
                            isStartingSession = false,
                            startSessionError = e.message ?: "Không thể bắt đầu đề thi"
                        )
                    }
                }
            } finally {
                if (startExamJob === coroutineContext[Job]) {
                    startExamJob = null
                }
            }
        }
        startExamJob = job
        return job
    }

    fun resumeSession(session: ExamSession) {
        enterSession(session)
    }

    private fun enterSession(session: ExamSession) {
        stopTimer()
        submitJob?.cancel()
        submitJob = null
        saveAnswerJobs.values.forEach { it.cancel() }
        saveAnswerJobs.clear()

        val currentAnswers = mutableMapOf<String, String>()
        for (q in session.questions) {
            val ans = q.selectedOption ?: session.answers[q.id]
            if (!ans.isNullOrBlank()) {
                currentAnswers[q.id] = ans
            }
        }

        // Initialize question revisions and confirmed statuses from server
        val initialRevisions = mutableMapOf<String, Int>()
        val initialStatuses = mutableMapOf<String, AnswerSaveStatus>()
        session.questions.forEach { q ->
            val rev = session.questionRevisions[q.id] ?: 0
            initialRevisions[q.id] = rev
            if (currentAnswers.containsKey(q.id)) {
                initialStatuses[q.id] = AnswerSaveStatus.CONFIRMED
            }
        }

        serverBaseEpochSeconds = if (session.serverNow > 0L) session.serverNow else wallClockProvider()
        localBaseElapsedSeconds = monotonicClockProvider()
        deadlineEpochSeconds = session.deadlineAt

        val remaining = calculateRemainingSeconds()

        _uiState.update {
            it.copy(
                screenState = ExamScreenState.IN_PROGRESS,
                currentSession = session,
                currentQuestionIndex = 0,
                localAnswers = currentAnswers,
                confirmedAnswers = currentAnswers,
                answerSaveStatuses = initialStatuses,
                answerSaveErrors = emptyMap(),
                savingAnswers = emptyMap(),
                questionRevisions = initialRevisions,
                remainingSeconds = remaining,
                isTimerCritical = remaining < 300L,
                showSubmitConfirmDialog = false,
                isSubmitting = false,
                submitError = null,
                isStartingSession = false
            )
        }

        if (remaining <= 0L) {
            // Already expired at entry
            submitSession(isAutoSubmit = true)
        } else {
            startTimer()
        }
    }

    private fun startTimer() {
        stopTimer()
        if (isCleared) return
        val session = _uiState.value.currentSession ?: return
        if (session.state != "IN_PROGRESS") return
        val timerGen = controllerGeneration

        timerJob = scope.launch {
            while (isActive) {
                delay(1000L)
                if (timerGen != controllerGeneration || isCleared) break
                val curSession = _uiState.value.currentSession
                if (curSession == null || curSession.id != session.id || _uiState.value.screenState != ExamScreenState.IN_PROGRESS) {
                    break
                }

                val rem = calculateRemainingSeconds()
                val isCritical = rem in 1..300

                _uiState.update {
                    it.copy(
                        remainingSeconds = rem,
                        isTimerCritical = isCritical
                    )
                }

                if (rem <= 0L) {
                    // Time's up! Authoritative auto submit
                    submitSession(isAutoSubmit = true)
                    break
                }
            }
        }
    }

    fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    fun clear() {
        isCleared = true
        controllerGeneration++
        stopTimer()
        loadExamsJob?.cancel()
        loadExamsJob = null
        startExamJob?.cancel()
        startExamJob = null
        submitJob?.cancel()
        submitJob = null
        saveAnswerJobs.values.forEach { it.cancel() }
        saveAnswerJobs.clear()
        _uiState.update {
            it.copy(
                isLoadingExams = false,
                isStartingSession = false,
                isSubmitting = false
            )
        }
    }

    fun selectQuestionIndex(index: Int) {
        val session = _uiState.value.currentSession ?: return
        if (index in 0 until session.questions.size) {
            _uiState.update { it.copy(currentQuestionIndex = index) }
        }
    }

    fun nextQuestion() {
        val session = _uiState.value.currentSession ?: return
        val next = _uiState.value.currentQuestionIndex + 1
        if (next < session.questions.size) {
            _uiState.update { it.copy(currentQuestionIndex = next) }
        }
    }

    fun previousQuestion() {
        val prev = _uiState.value.currentQuestionIndex - 1
        if (prev >= 0) {
            _uiState.update { it.copy(currentQuestionIndex = prev) }
        }
    }

    fun selectAnswer(questionId: String, option: String): Job? {
        val session = _uiState.value.currentSession ?: return null
        if (_uiState.value.isSubmitting || session.state != "IN_PROGRESS") return null

        val currentSelected = _uiState.value.localAnswers[questionId]
        // Toggle: if clicking already selected option, unselect (clear) it
        val targetSelection: String? = if (currentSelected == option) null else option

        return performSaveAnswer(session, questionId, targetSelection)
    }

    fun retrySaveAnswer(questionId: String): Job? {
        val session = _uiState.value.currentSession ?: return null
        if (_uiState.value.isSubmitting || session.state != "IN_PROGRESS") return null

        // Re-sends the current local selection with the next revision
        val currentSelected = _uiState.value.localAnswers[questionId]
        return performSaveAnswer(session, questionId, currentSelected)
    }

    private fun performSaveAnswer(session: ExamSession, questionId: String, targetSelection: String?): Job {
        val sessionId = session.id
        val callGen = controllerGeneration
        val currentRev = _uiState.value.questionRevisions[questionId] ?: 0
        val nextRev = currentRev + 1

        val newRevisions = _uiState.value.questionRevisions.toMutableMap()
        newRevisions[questionId] = nextRev

        val updatedAnswers = _uiState.value.localAnswers.toMutableMap()
        if (targetSelection != null) {
            updatedAnswers[questionId] = targetSelection
        } else {
            updatedAnswers.remove(questionId)
        }

        val updatedStatuses = _uiState.value.answerSaveStatuses.toMutableMap()
        updatedStatuses[questionId] = AnswerSaveStatus.SAVING

        val updatedSaving = _uiState.value.savingAnswers.toMutableMap()
        updatedSaving[questionId] = true

        val updatedErrors = _uiState.value.answerSaveErrors.toMutableMap()
        updatedErrors.remove(questionId)

        _uiState.update {
            it.copy(
                localAnswers = updatedAnswers,
                questionRevisions = newRevisions,
                answerSaveStatuses = updatedStatuses,
                savingAnswers = updatedSaving,
                answerSaveErrors = updatedErrors
            )
        }

        // Cancel previous save job for this question
        saveAnswerJobs[questionId]?.cancel()

        // Save asynchronously with revision guard
        val job = scope.launch {
            try {
                val updatedSession = repository.saveAnswer(sessionId, questionId, targetSelection, nextRev)
                if (callGen != controllerGeneration || isCleared) return@launch

                _uiState.update { state ->
                    if (state.screenState != ExamScreenState.IN_PROGRESS || state.currentSession?.id != sessionId) {
                        return@update state
                    }
                    val latestRev = state.questionRevisions[questionId] ?: 0
                    // Response cũ không được đè trạng thái mới hoặc xóa cờ đang lưu của request mới
                    if (nextRev >= latestRev) {
                        val newConfirmed = state.confirmedAnswers.toMutableMap()
                        if (targetSelection != null) {
                            newConfirmed[questionId] = targetSelection
                        } else {
                            newConfirmed.remove(questionId)
                        }
                        val newStatuses = state.answerSaveStatuses.toMutableMap()
                        newStatuses[questionId] = AnswerSaveStatus.CONFIRMED

                        val newSaving = state.savingAnswers.toMutableMap()
                        newSaving.remove(questionId)

                        val newErrs = state.answerSaveErrors.toMutableMap()
                        newErrs.remove(questionId)

                        state.copy(
                            currentSession = updatedSession,
                            confirmedAnswers = newConfirmed,
                            answerSaveStatuses = newStatuses,
                            savingAnswers = newSaving,
                            answerSaveErrors = newErrs
                        )
                    } else {
                        state
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                if (callGen != controllerGeneration || isCleared) return@launch

                _uiState.update { state ->
                    if (state.screenState != ExamScreenState.IN_PROGRESS || state.currentSession?.id != sessionId) {
                        return@update state
                    }
                    val latestRev = state.questionRevisions[questionId] ?: 0
                    if (nextRev >= latestRev) {
                        val newStatuses = state.answerSaveStatuses.toMutableMap()
                        newStatuses[questionId] = AnswerSaveStatus.FAILED_UNSAVED

                        val newSaving = state.savingAnswers.toMutableMap()
                        newSaving.remove(questionId)

                        val newErrs = state.answerSaveErrors.toMutableMap()
                        newErrs[questionId] = "Chưa lưu được đáp án"

                        state.copy(
                            answerSaveStatuses = newStatuses,
                            savingAnswers = newSaving,
                            answerSaveErrors = newErrs
                        )
                    } else {
                        state
                    }
                }

                if (e.message?.contains("EXAM_SESSION_EXPIRED") == true) {
                    if (callGen == controllerGeneration && !isCleared && _uiState.value.currentSession?.id == sessionId) {
                        submitSession(isAutoSubmit = true)
                    }
                }
            } finally {
                if (saveAnswerJobs[questionId] === coroutineContext[Job]) {
                    saveAnswerJobs.remove(questionId)
                }
            }
        }
        saveAnswerJobs[questionId] = job
        return job
    }

    fun requestSubmitConfirmation() {
        _uiState.update { it.copy(showSubmitConfirmDialog = true) }
    }

    fun dismissSubmitConfirmation() {
        _uiState.update { it.copy(showSubmitConfirmDialog = false) }
    }

    fun submitSession(isAutoSubmit: Boolean = false): Job? {
        if (isCleared) return null
        val session = _uiState.value.currentSession ?: return null
        if (_uiState.value.isSubmitting) return null

        stopTimer()
        val targetSessionId = session.id
        val callGen = controllerGeneration
        _uiState.update {
            it.copy(
                isSubmitting = true,
                showSubmitConfirmDialog = false,
                submitError = null
            )
        }

        val job = scope.launch {
            try {
                val resultSession = repository.submitSession(targetSessionId)
                if (callGen == controllerGeneration && !isCleared && _uiState.value.currentSession?.id == targetSessionId) {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            screenState = ExamScreenState.RESULT,
                            completedSession = resultSession,
                            currentSession = null,
                            reviewQuestionIndex = 0
                        )
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e

                if (callGen != controllerGeneration || isCleared || _uiState.value.currentSession?.id != targetSessionId) {
                    return@launch
                }

                // Read-back to check if already submitted/finalized on server
                var finalSession: ExamSession? = null
                try {
                    finalSession = repository.getSession(targetSessionId)
                } catch (readBackEx: Exception) {
                    if (readBackEx is CancellationException) throw readBackEx
                }

                if (callGen != controllerGeneration || isCleared || _uiState.value.currentSession?.id != targetSessionId) {
                    return@launch
                }

                if (finalSession != null && (finalSession.state == "SUBMITTED" || finalSession.state == "TIMED_OUT")) {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            screenState = ExamScreenState.RESULT,
                            completedSession = finalSession,
                            currentSession = null,
                            reviewQuestionIndex = 0
                        )
                    }
                    return@launch
                }

                // If session is still IN_PROGRESS and time remains, resume timer
                val remaining = calculateRemainingSeconds()
                if (remaining > 0L) {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            submitError = "Chưa thể nộp bài do sự cố kết nối. Bài thi vẫn đang tiếp tục, bạn có thể kiểm tra và nộp lại."
                        )
                    }
                    startTimer()
                } else {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            submitError = "Thời gian làm bài đã kết thúc nhưng chưa kết nối được máy chủ. Vui lòng bấm Nộp bài lại khi có mạng."
                        )
                    }
                }
            } finally {
                if (submitJob === coroutineContext[Job]) {
                    submitJob = null
                }
            }
        }
        submitJob = job
        return job
    }

    fun setReviewQuestionIndex(index: Int) {
        val completed = _uiState.value.completedSession ?: return
        if (index in 0 until completed.questions.size) {
            _uiState.update { it.copy(reviewQuestionIndex = index) }
        }
    }

    fun finishReview() {
        stopTimer()
        submitJob?.cancel()
        submitJob = null
        saveAnswerJobs.values.forEach { it.cancel() }
        saveAnswerJobs.clear()

        _uiState.update {
            it.copy(
                screenState = ExamScreenState.LIST,
                completedSession = null,
                selectedExam = null,
                isSubmitting = false,
                submitError = null
            )
        }
        loadExams()
    }
}
