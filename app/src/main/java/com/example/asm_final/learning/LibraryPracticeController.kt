package com.example.asm_final.learning



import com.example.asm_final.backend.BackendClient

import kotlinx.coroutines.CancellationException

import kotlinx.coroutines.CoroutineScope

import kotlinx.coroutines.CoroutineStart

import kotlinx.coroutines.Job

import kotlinx.coroutines.NonCancellable

import kotlinx.coroutines.flow.MutableStateFlow

import kotlinx.coroutines.flow.StateFlow

import kotlinx.coroutines.flow.asStateFlow

import kotlinx.coroutines.flow.update

import kotlinx.coroutines.launch

import kotlinx.coroutines.withContext



data class QuizSelectionState(

    val quizzes: List<QuizSummary>? = null,

    val activeSessionsByQuizId: Map<String, SessionSummary> = emptyMap(),

    val isLoading: Boolean = false,

    val errorMessage: String? = null,

    val startingQuizId: String? = null,

    val resumingQuizId: String? = null,

    val startErrorMessage: String? = null,

    val confirmingRestartQuiz: QuizSummary? = null,

    val isActiveSessionsLoading: Boolean = false,

    val activeSessionsErrorMessage: String? = null,

    val activeSessionsSynced: Boolean = false

)


sealed interface CatalogSectionPresentation {
    object Loading : CatalogSectionPresentation
    data class Error(val message: String) : CatalogSectionPresentation
    data class Empty(
        val message: String,
        val detailMessage: String,
        val hasActiveSessionsAbove: Boolean
    ) : CatalogSectionPresentation
    data class Available(
        val quizzes: List<QuizCardPresentation>
    ) : CatalogSectionPresentation
}

data class QuizCardPresentation(
    val quiz: QuizSummary,
    val isStarting: Boolean,
    val isResuming: Boolean,
    val activeSession: SessionSummary?,
    val isSynced: Boolean
) {
    val hasActiveSession: Boolean
        get() = activeSession != null && activeSession.state == "IN_PROGRESS"
}

data class ActiveSessionCardPresentation(
    val session: SessionSummary,
    val sessionId: String,
    val quizId: String,
    val title: String,
    val explanation: String,
    val answeredCount: Int,
    val totalCount: Int,
    val isResuming: Boolean,
    val isSynced: Boolean,
    val isUnpublishedOrCatalogEmpty: Boolean
)

data class ActiveSessionsSyncWarning(
    val message: String,
    val canRetry: Boolean
)

data class LibrarySelectionPresentation(
    val startError: String?,
    val activeSessionsSyncWarning: ActiveSessionsSyncWarning?,
    val standaloneActiveSessions: List<ActiveSessionCardPresentation>,
    val catalogSection: CatalogSectionPresentation,
    val showGlobalEmptyState: Boolean
)

fun QuizSelectionState.toPresentation(): LibrarySelectionPresentation {
    val syncWarning = if (this.activeSessionsErrorMessage != null) {
        ActiveSessionsSyncWarning(
            message = this.activeSessionsErrorMessage,
            canRetry = !this.isActiveSessionsLoading
        )
    } else null

    val publishedQuizIds = this.quizzes?.map { it.id }?.toSet() ?: emptySet()

    val standaloneList = this.activeSessionsByQuizId.values
        .filter { it.state == "IN_PROGRESS" && it.quizId !in publishedQuizIds }
        .sortedByDescending { it.updatedAt }
        .map { s ->
            val isResuming = this.resumingQuizId == s.quizId
            val explanation = if (this.quizzes.isNullOrEmpty()) {
                "Đề thi này hiện không mở cho lượt làm mới, nhưng bài làm dở dang của bạn vẫn có thể tiếp tục từ bản snapshot đã lưu."
            } else {
                "Đề thi này hiện không hiển thị trong danh mục chính, nhưng bạn vẫn có thể hoàn thành bài làm từ bản snapshot đã lưu."
            }
            ActiveSessionCardPresentation(
                session = s,
                sessionId = s.id,
                quizId = s.quizId,
                title = "Bài luyện dở (Mã đề: ${s.quizId})",
                explanation = explanation,
                answeredCount = s.answeredCount,
                totalCount = s.totalCount,
                isResuming = isResuming,
                isSynced = this.activeSessionsSynced,
                isUnpublishedOrCatalogEmpty = true
            )
        }

    val catalogSection = when {
        this.isLoading -> CatalogSectionPresentation.Loading
        this.errorMessage != null -> CatalogSectionPresentation.Error(this.errorMessage)
        this.quizzes.isNullOrEmpty() -> {
            val hasSessionsAbove = standaloneList.isNotEmpty()
            val (msg, detail) = when {
                hasSessionsAbove -> {
                    Pair(
                        "Danh mục hiện chưa có bộ đề mở mới",
                        "Kho đề mới đang được chuẩn bị và sẽ sớm xuất bản. Bạn có thể tiếp tục hoàn thành các bài luyện dở dang ở trên."
                    )
                }
                this.activeSessionsErrorMessage != null -> {
                    Pair(
                        "Chưa có bộ đề công khai mới",
                        "Kho đề mới đang được chuẩn bị. Vui lòng bấm 'Thử lại' phía trên để kiểm tra bài làm dở dang của bạn."
                    )
                }
                this.isActiveSessionsLoading -> {
                    Pair(
                        "Chưa có bộ đề công khai mới",
                        "Đang kiểm tra tiến độ bài làm dở dang..."
                    )
                }
                else -> {
                    Pair(
                        "📌 Chưa có bộ đề trắc nghiệm",
                        "Kho đề ôn luyện cho môn học này đang được chuẩn bị và sẽ sớm xuất bản."
                    )
                }
            }
            CatalogSectionPresentation.Empty(
                message = msg,
                detailMessage = detail,
                hasActiveSessionsAbove = hasSessionsAbove
            )
        }
        else -> {
            val cards = this.quizzes.map { quiz ->
                val active = this.activeSessionsByQuizId[quiz.id]?.takeIf { it.state == "IN_PROGRESS" }
                QuizCardPresentation(
                    quiz = quiz,
                    isStarting = this.startingQuizId == quiz.id,
                    isResuming = this.resumingQuizId == quiz.id,
                    activeSession = active,
                    isSynced = this.activeSessionsSynced
                )
            }
            CatalogSectionPresentation.Available(cards)
        }
    }

    val showGlobalEmptyState = standaloneList.isEmpty() &&
        syncWarning == null &&
        catalogSection is CatalogSectionPresentation.Empty &&
        this.activeSessionsSynced

    return LibrarySelectionPresentation(
        startError = this.startErrorMessage,
        activeSessionsSyncWarning = syncWarning,
        standaloneActiveSessions = standaloneList,
        catalogSection = catalogSection,
        showGlobalEmptyState = showGlobalEmptyState
    )
}



data class SessionPracticeState(

    val session: PracticeSession,

    val quizTitle: String,

    val currentStep: Int = 0,

    val selectedOption: String? = null,

    val isSubmittingAnswer: Boolean = false,

    val isCompleting: Boolean = false,

    val notificationMessage: String? = null

)



class LibraryPracticeController(

    val repository: LibraryRepository,

    val scope: CoroutineScope,

    val pointerStore: SessionPointerStore? = null

) {

    private val _selectionState = MutableStateFlow(QuizSelectionState())

    val selectionState: StateFlow<QuizSelectionState> = _selectionState.asStateFlow()



    private val _sessionState = MutableStateFlow<SessionPracticeState?>(null)

    val sessionState: StateFlow<SessionPracticeState?> = _sessionState.asStateFlow()



    var requestGeneration: Int = 0

        private set



    fun loadQuizzes(subjectWireId: String, grade: Int): Job {

        val gen = ++requestGeneration

        val targetOwnerId = repository.ownerId

        val targetEndpoint = repository.endpoint



        _selectionState.update {

            it.copy(

                isLoading = true,

                errorMessage = null,

                startingQuizId = null,

                resumingQuizId = null,

                startErrorMessage = null,

                confirmingRestartQuiz = null,

                isActiveSessionsLoading = true,

                activeSessionsErrorMessage = null

            )

        }

        return scope.launch(start = CoroutineStart.UNDISPATCHED) {

            try {

                val list = repository.loadQuizzes(subjectWireId, grade)

                if (gen == requestGeneration && repository.ownerId == targetOwnerId && repository.endpoint == targetEndpoint) {

                    _selectionState.update {

                        it.copy(

                            quizzes = list,

                            isLoading = false,

                            errorMessage = null

                        )

                    }

                }

                // Load active sessions in parallel/sequentially without dropping quizzes if session loading fails

                try {

                    val activeSessions = repository.listActiveSessions(subjectWireId, grade)

                    val activeMap = mutableMapOf<String, SessionSummary>()

                    for (s in activeSessions) {

                        val existing = activeMap[s.quizId]

                        if (existing == null || s.updatedAt > existing.updatedAt) {

                            activeMap[s.quizId] = s

                        }

                    }

                    if (gen == requestGeneration && repository.ownerId == targetOwnerId && repository.endpoint == targetEndpoint) {

                        _selectionState.update {

                            it.copy(

                                activeSessionsByQuizId = activeMap,

                                isActiveSessionsLoading = false,

                                activeSessionsErrorMessage = null,

                                activeSessionsSynced = true

                            )

                        }

                    }

                } catch (e: CancellationException) {

                    throw e

                } catch (e: Exception) {

                    if (gen == requestGeneration && repository.ownerId == targetOwnerId && repository.endpoint == targetEndpoint) {

                        val msg = formatError(e)

                        _selectionState.update {

                            it.copy(

                                isActiveSessionsLoading = false,

                                activeSessionsErrorMessage = "Chưa thể đồng bộ tiến độ bài dở: $msg",

                                activeSessionsSynced = false

                            )

                        }

                    }

                }

            } catch (e: CancellationException) {

                throw e

            } catch (e: Exception) {

                if (gen == requestGeneration && repository.ownerId == targetOwnerId && repository.endpoint == targetEndpoint) {

                    val msg = formatError(e)

                    _selectionState.update {

                        it.copy(

                            quizzes = null,

                            isLoading = false,

                            errorMessage = msg,

                            isActiveSessionsLoading = false,

                            activeSessionsSynced = false

                        )

                    }

                }

            }

        }

    }



    fun retryLoadActiveSessions(subjectWireId: String, grade: Int): Job {

        val gen = ++requestGeneration

        val targetOwnerId = repository.ownerId

        val targetEndpoint = repository.endpoint



        _selectionState.update {

            it.copy(

                isActiveSessionsLoading = true,

                activeSessionsErrorMessage = null

            )

        }

        return scope.launch(start = CoroutineStart.UNDISPATCHED) {

            try {

                val activeSessions = repository.listActiveSessions(subjectWireId, grade)

                val activeMap = mutableMapOf<String, SessionSummary>()

                for (s in activeSessions) {

                    val existing = activeMap[s.quizId]

                    if (existing == null || s.updatedAt > existing.updatedAt) {

                        activeMap[s.quizId] = s

                    }

                }

                if (gen == requestGeneration && repository.ownerId == targetOwnerId && repository.endpoint == targetEndpoint) {

                    _selectionState.update {

                        it.copy(

                            activeSessionsByQuizId = activeMap,

                            isActiveSessionsLoading = false,

                            activeSessionsErrorMessage = null,

                            activeSessionsSynced = true

                        )

                    }

                }

            } catch (e: CancellationException) {

                throw e

            } catch (e: Exception) {

                if (gen == requestGeneration && repository.ownerId == targetOwnerId && repository.endpoint == targetEndpoint) {

                    val msg = formatError(e)

                    _selectionState.update {

                        it.copy(

                            isActiveSessionsLoading = false,

                            activeSessionsErrorMessage = "Chưa thể đồng bộ tiến độ bài dở: $msg",

                            activeSessionsSynced = false

                        )

                    }

                }

            }

        }

    }



    fun resumeSession(quiz: QuizSummary, sessionId: String, onResumed: ((PracticeSession) -> Unit)? = null): Job? {

        return resumeSessionInternal(sessionId, quiz.id, quiz.title, quiz.subjectId, quiz.grade, onResumed)

    }



    fun resumeSessionDirect(sessionSummary: SessionSummary, quizTitle: String? = null, onResumed: ((PracticeSession) -> Unit)? = null): Job? {

        val title = quizTitle ?: "Bài luyện dở (${sessionSummary.quizId})"

        return resumeSessionInternal(sessionSummary.id, sessionSummary.quizId, title, sessionSummary.subjectId, sessionSummary.grade, onResumed)

    }



    private fun resumeSessionInternal(

        sessionId: String,

        quizId: String,

        quizTitle: String,

        subjectId: String,

        grade: Int,

        onResumed: ((PracticeSession) -> Unit)? = null

    ): Job? {

        if (_selectionState.value.startingQuizId != null || _selectionState.value.resumingQuizId != null) {

            return null

        }

        val currentGen = ++requestGeneration

        val targetOwnerId = repository.ownerId

        val targetEndpoint = repository.endpoint



        _selectionState.update {

            it.copy(

                resumingQuizId = quizId,

                startErrorMessage = null,

                confirmingRestartQuiz = null

            )

        }

        return scope.launch(start = CoroutineStart.UNDISPATCHED) {

            try {

                val session = repository.getSession(sessionId)

                if (currentGen == requestGeneration && repository.ownerId == targetOwnerId && repository.endpoint == targetEndpoint) {

                    if (session.state == "COMPLETED") {

                        pointerStore?.clearActiveSessionId(targetEndpoint, targetOwnerId, subjectId, grade, quizId)

                        _selectionState.update { state ->

                            state.copy(

                                resumingQuizId = null,

                                startErrorMessage = "Bài luyện này đã hoàn thành trước đó. Vui lòng bắt đầu phiên mới.",

                                activeSessionsByQuizId = state.activeSessionsByQuizId - quizId

                            )

                        }

                        return@launch

                    }

                    pointerStore?.setActiveSessionId(targetEndpoint, targetOwnerId, subjectId, grade, quizId, session.id)

                    _selectionState.update {

                        it.copy(

                            resumingQuizId = null,

                            startErrorMessage = null

                        )

                    }

                    _sessionState.value = SessionPracticeState(

                        session = session,

                        quizTitle = quizTitle,

                        currentStep = session.firstUnansweredIndex,

                        selectedOption = null,

                        notificationMessage = null

                    )

                    onResumed?.invoke(session)

                }

            } catch (e: CancellationException) {

                throw e

            } catch (e: Exception) {

                if (currentGen == requestGeneration) {

                    val msg = formatStartError(e)

                    _selectionState.update {

                        it.copy(

                            resumingQuizId = null,

                            startErrorMessage = msg

                        )

                    }

                }

            } finally {

                withContext(NonCancellable) {

                    if (currentGen == requestGeneration) {

                        _selectionState.update { it.copy(resumingQuizId = null) }

                    }

                }

            }

        }

    }



    fun startQuiz(quiz: QuizSummary, forceRestart: Boolean = false, onStarted: ((PracticeSession) -> Unit)? = null): Job? {

        if (_selectionState.value.startingQuizId != null || _selectionState.value.resumingQuizId != null) {

            return null

        }

        val currentGen = ++requestGeneration

        val targetOwnerId = repository.ownerId

        val targetEndpoint = repository.endpoint



        _selectionState.update {

            it.copy(

                startingQuizId = quiz.id,

                startErrorMessage = null,

                confirmingRestartQuiz = null

            )

        }

        return scope.launch(start = CoroutineStart.UNDISPATCHED) {

            try {

                val session = repository.startSession(quiz.id)

                if (currentGen == requestGeneration && repository.ownerId == targetOwnerId && repository.endpoint == targetEndpoint) {

                    pointerStore?.setActiveSessionId(targetEndpoint, targetOwnerId, quiz.subjectId, quiz.grade, quiz.id, session.id)

                    val summary = SessionSummary(

                        id = session.id,

                        quizId = session.quizId,

                        subjectId = session.subjectId,

                        grade = session.grade,

                        state = session.state,

                        score = session.score,

                        correctCount = session.correctCount,

                        totalCount = session.totalCount,

                        answeredCount = session.answeredCount,

                        firstUnansweredIndex = 0,

                        updatedAt = ""

                    )

                    _selectionState.update { state ->

                        state.copy(

                            startingQuizId = null,

                            startErrorMessage = null,

                            activeSessionsByQuizId = state.activeSessionsByQuizId + (quiz.id to summary)

                        )

                    }

                    _sessionState.value = SessionPracticeState(

                        session = session,

                        quizTitle = quiz.title,

                        currentStep = 0,

                        selectedOption = null,

                        notificationMessage = null

                    )

                    onStarted?.invoke(session)

                }

            } catch (e: CancellationException) {

                throw e

            } catch (e: Exception) {

                if (currentGen == requestGeneration) {

                    val msg = formatStartError(e)

                    _selectionState.update {

                        it.copy(

                            startingQuizId = null,

                            startErrorMessage = msg

                        )

                    }

                }

            } finally {

                withContext(NonCancellable) {

                    if (currentGen == requestGeneration) {

                        _selectionState.update { it.copy(startingQuizId = null) }

                    }

                }

            }

        }

    }



    fun requestRestartConfirmation(quiz: QuizSummary) {

        _selectionState.update { it.copy(confirmingRestartQuiz = quiz) }

    }



    fun cancelRestartConfirmation() {

        _selectionState.update { it.copy(confirmingRestartQuiz = null) }

    }



    fun restoreActiveSessionIfAny(quiz: QuizSummary, onRestored: ((PracticeSession) -> Unit)? = null): Job? {

        val ptr = pointerStore?.getActiveSessionId(repository.endpoint, repository.ownerId, quiz.subjectId, quiz.grade, quiz.id)

            ?: return null

        return resumeSession(quiz, ptr, onRestored)

    }



    fun selectOption(option: String) {

        _sessionState.update { cur ->

            if (cur != null) {

                val q = cur.session.questions.getOrNull(cur.currentStep)

                if (q?.userAnswer == null && !cur.isSubmittingAnswer) {

                    cur.copy(selectedOption = option)

                } else cur

            } else null

        }

    }



    fun submitAnswer(): Job? {

        val cur = _sessionState.value ?: return null

        val q = cur.session.questions.getOrNull(cur.currentStep) ?: return null

        val opt = cur.selectedOption ?: return null

        if (q.userAnswer != null || cur.isSubmittingAnswer) return null



        val targetSessionId = cur.session.id

        val targetOwnerId = repository.ownerId

        val targetEndpoint = repository.endpoint



        _sessionState.update { it?.copy(isSubmittingAnswer = true, notificationMessage = null) }

        return scope.launch(start = CoroutineStart.UNDISPATCHED) {

            try {

                val updated = repository.submitAnswer(targetSessionId, q.id, opt)

                if (isCurrentSession(targetSessionId, targetOwnerId, targetEndpoint)) {

                    _sessionState.update { state ->

                        if (state?.session?.id == targetSessionId) {

                            state.copy(

                                session = updated,

                                isSubmittingAnswer = false,

                                notificationMessage = null

                            )

                        } else state

                    }

                }

            } catch (e: CancellationException) {

                throw e

            } catch (e: Exception) {

                var recoveredSession: PracticeSession? = null

                if (isNetworkFailure(e)) {

                    try {

                        val verified = repository.getSession(targetSessionId)

                        val ans = verified.questions.find { it.id == q.id }?.userAnswer

                        if (ans != null) {

                            recoveredSession = verified

                        }

                    } catch (readBackEx: Exception) {

                        if (readBackEx is CancellationException) throw readBackEx

                    }

                }



                if (isCurrentSession(targetSessionId, targetOwnerId, targetEndpoint)) {

                    if (recoveredSession != null) {

                        _sessionState.update { state ->

                            if (state?.session?.id == targetSessionId) {

                                state.copy(

                                    session = recoveredSession,

                                    isSubmittingAnswer = false,

                                    notificationMessage = null

                                )

                            } else state

                        }

                    } else {

                        val msg = when {

                            isNetworkFailure(e) ->

                                "Chưa xác nhận được kết quả do lỗi mạng. Bạn có thể kiểm tra kết nối và bấm gửi lại."

                            e is BackendClient.Failure && e.code == "LEGACY_SESSION_REQUIRES_RESTART" ->

                                "Phiên làm bài cũ cần được bắt đầu lại để đảm bảo tính chính xác."

                            e is BackendClient.Failure && e.code == "SESSION_ALREADY_COMPLETED" ->

                                "Bài luyện này đã hoàn thành trước đó."

                            else -> formatError(e)

                        }

                        _sessionState.update { state ->

                            if (state?.session?.id == targetSessionId) {

                                state.copy(

                                    isSubmittingAnswer = false,

                                    notificationMessage = msg

                                )

                            } else state

                        }

                    }

                }

            } finally {

                withContext(NonCancellable) {

                    _sessionState.update { state ->

                        if (state?.session?.id == targetSessionId) {

                            state.copy(isSubmittingAnswer = false)

                        } else state

                    }

                }

            }

        }

    }



    fun nextQuestion() {

        _sessionState.update { cur ->

            if (cur != null && cur.currentStep + 1 < cur.session.questions.size) {

                cur.copy(currentStep = cur.currentStep + 1, selectedOption = null)

            } else cur

        }

    }



    fun completeSession(): Job? {

        val cur = _sessionState.value ?: return null

        if (cur.isCompleting) return null

        val allAnswered = cur.session.questions.all { it.userAnswer != null }

        if (!allAnswered) {

            _sessionState.update {

                it?.copy(notificationMessage = "Bạn cần trả lời đủ tất cả các câu hỏi trước khi nộp bài.")

            }

            return null

        }



        val targetSessionId = cur.session.id

        val targetQuizId = cur.session.quizId

        val targetSubjectId = cur.session.subjectId

        val targetGrade = cur.session.grade

        val targetOwnerId = repository.ownerId

        val targetEndpoint = repository.endpoint



        _sessionState.update { it?.copy(isCompleting = true, notificationMessage = null) }

        return scope.launch(start = CoroutineStart.UNDISPATCHED) {

            try {

                val comp = repository.completeSession(targetSessionId)

                if (isCurrentSession(targetSessionId, targetOwnerId, targetEndpoint)) {

                    pointerStore?.clearActiveSessionId(

                        targetEndpoint,

                        targetOwnerId,

                        targetSubjectId,

                        targetGrade,

                        targetQuizId

                    )

                    _selectionState.update { state ->

                        state.copy(activeSessionsByQuizId = state.activeSessionsByQuizId - targetQuizId)

                    }

                    _sessionState.update { state ->

                        if (state?.session?.id == targetSessionId) {

                            state.copy(

                                session = comp,

                                isCompleting = false,

                                notificationMessage = null

                            )

                        } else state

                    }

                }

            } catch (e: CancellationException) {

                throw e

            } catch (e: Exception) {

                var recoveredComp: PracticeSession? = null

                if (isNetworkFailure(e)) {

                    try {

                        val verified = repository.getSession(targetSessionId)

                        if (verified.state == "COMPLETED") {

                            recoveredComp = verified

                        }

                    } catch (readBackEx: Exception) {

                        if (readBackEx is CancellationException) throw readBackEx

                    }

                }



                if (isCurrentSession(targetSessionId, targetOwnerId, targetEndpoint)) {

                    if (recoveredComp != null) {

                        pointerStore?.clearActiveSessionId(

                            targetEndpoint,

                            targetOwnerId,

                            targetSubjectId,

                            targetGrade,

                            targetQuizId

                        )

                        _selectionState.update { state ->

                            state.copy(activeSessionsByQuizId = state.activeSessionsByQuizId - targetQuizId)

                        }

                        _sessionState.update { state ->

                            if (state?.session?.id == targetSessionId) {

                                state.copy(

                                    session = recoveredComp,

                                    isCompleting = false,

                                    notificationMessage = null

                                )

                            } else state

                        }

                    } else {

                        val msg = when {

                            isNetworkFailure(e) ->

                                "Chưa xác nhận được việc nộp bài do lỗi mạng. Vui lòng kiểm tra kết nối và thử lại."

                            e is BackendClient.Failure && e.code == "SESSION_INCOMPLETE" ->

                                "Bạn cần trả lời đủ tất cả các câu hỏi trước khi nộp bài."

                            e is BackendClient.Failure && e.code == "LEGACY_SESSION_REQUIRES_RESTART" ->

                                "Phiên làm bài cũ cần được bắt đầu lại."

                            else -> formatError(e)

                        }

                        _sessionState.update { state ->

                            if (state?.session?.id == targetSessionId) {

                                state.copy(

                                    isCompleting = false,

                                    notificationMessage = msg

                                )

                            } else state

                        }

                    }

                }

            } finally {

                withContext(NonCancellable) {

                    _sessionState.update { state ->

                        if (state?.session?.id == targetSessionId) {

                            state.copy(isCompleting = false)

                        } else state

                    }

                }

            }

        }

    }



    fun retryPractice(quizId: String): Job? {

        if (_selectionState.value.startingQuizId != null || _selectionState.value.resumingQuizId != null) {

            return null

        }

        val targetOwnerId = repository.ownerId

        val targetEndpoint = repository.endpoint

        val gen = ++requestGeneration



        return scope.launch(start = CoroutineStart.UNDISPATCHED) {

            try {

                val newSess = repository.startSession(quizId)

                if (gen == requestGeneration && repository.ownerId == targetOwnerId && repository.endpoint == targetEndpoint) {

                    pointerStore?.setActiveSessionId(

                        targetEndpoint,

                        targetOwnerId,

                        newSess.subjectId,

                        newSess.grade,

                        newSess.quizId,

                        newSess.id

                    )

                    _sessionState.update { cur ->

                        cur?.copy(

                            session = newSess,

                            currentStep = 0,

                            selectedOption = null,

                            notificationMessage = null

                        )

                    }

                }

            } catch (e: CancellationException) {

                throw e

            } catch (e: Exception) {

                if (gen == requestGeneration) {

                    val msg = formatStartError(e)

                    _sessionState.update { it?.copy(notificationMessage = msg) }

                }

            }

        }

    }



    fun backToList() {

        _sessionState.value = null

        _selectionState.update {

            it.copy(

                startingQuizId = null,

                resumingQuizId = null,

                startErrorMessage = null,

                confirmingRestartQuiz = null

            )

        }

    }



    fun clearNotification() {

        _sessionState.update { it?.copy(notificationMessage = null) }

        _selectionState.update { it.copy(startErrorMessage = null) }

    }



    fun clearStartError() {

        _selectionState.update { it.copy(startErrorMessage = null) }

    }



    private fun isCurrentSession(sessionId: String, ownerId: Int, endpoint: String): Boolean {

        val cur = _sessionState.value ?: return false

        return cur.session.id == sessionId && repository.ownerId == ownerId && repository.endpoint == endpoint

    }



    private fun isNetworkFailure(e: Exception): Boolean {

        if (e is BackendClient.Failure) {

            val code = e.code?.uppercase() ?: ""

            return code in listOf("NETWORK_ERROR", "CONNECT_ERROR", "TIMEOUT", "TIMEOUT_ERROR", "IO_ERROR")

        }

        return e is java.io.IOException || e is java.net.SocketTimeoutException || e is java.net.ConnectException

    }



    private fun formatStartError(e: Exception): String {

        return if (e is BackendClient.Failure) {

            when {

                e.code == "QUIZ_NOT_FOUND" ->

                    "Bộ đề này hiện không khả dụng hoặc đã bị ẩn."

                e.code == "QUIZ_NOT_AVAILABLE" ->

                    "Bộ đề này hiện chưa sẵn sàng để luyện tập hoặc đang được cập nhật."

                e.code == "SERVER_NOT_CONFIGURED" ->

                    "Chưa cấu hình địa chỉ máy chủ backend."

                isNetworkFailure(e) ->

                    "Lỗi kết nối mạng khi mở bài luyện. Vui lòng kiểm tra kết nối và thử lại."

                e.status == 401 || e.code == "UNAUTHORIZED" || e.code == "AUTH_EXPIRED" ->

                    "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại."

                else ->

                    "Không thể bắt đầu bài luyện: ${e.code} (HTTP ${e.status}). Vui lòng thử lại."

            }

        } else {

            "Lỗi kết nối máy chủ: ${e.localizedMessage ?: e.javaClass.simpleName}"

        }

    }



    private fun formatError(e: Exception): String {

        return if (e is BackendClient.Failure) {

            when (e.code) {

                "SERVER_NOT_CONFIGURED" -> "Chưa cấu hình địa chỉ máy chủ backend."

                "LEGACY_SESSION_REQUIRES_RESTART" -> "Phiên làm bài cũ cần được bắt đầu lại."

                "SESSION_INCOMPLETE" -> "Bạn cần trả lời đủ tất cả các câu hỏi trước khi nộp bài."

                "QUIZ_NOT_FOUND" -> "Bộ đề này hiện không khả dụng hoặc đã bị ẩn."

                "QUIZ_NOT_AVAILABLE" -> "Bộ đề này hiện chưa sẵn sàng để luyện tập hoặc đang được cập nhật."

                "NETWORK_ERROR", "CONNECT_ERROR", "TIMEOUT" -> "Lỗi kết nối mạng. Vui lòng kiểm tra mạng và thử lại."

                else -> "Lỗi máy chủ: ${e.code} (HTTP ${e.status})"

            }

        } else {

            "Lỗi kết nối máy chủ: ${e.localizedMessage ?: e.javaClass.simpleName}"

        }

    }

}

