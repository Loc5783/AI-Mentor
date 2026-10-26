package com.example.asm_final.learning

import com.example.asm_final.backend.BackendClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class LibraryPracticeFlowTest {

    private class ControlledFakeLibraryRepository : LibraryRepository {
        var startSessionCallCount = 0
        var lastStartedQuizId: String? = null
        var completeSessionCallCount = 0
        var submitAnswerCallCount = 0

        var failLoadWith: Exception? = null
        var failStartWith: Exception? = null
        var failCompleteWith: Exception? = null
        var failSubmitWith: Exception? = null

        var startSessionDeferred: CompletableDeferred<PracticeSession>? = null
        var loadQuizzesDeferredMap = mutableMapOf<String, CompletableDeferred<List<QuizSummary>>>()

        val physicsQuizzes = listOf(
            QuizSummary("quiz_phy10_01", "Vật lý 10 - Động học", "physics", 10, "Chương 1", "Chuyển động", "EASY", 2),
            QuizSummary("quiz_phy10_02", "Vật lý 10 - Động lực học", "physics", 10, "Chương 2", "Định luật Newton", "MEDIUM", 3)
        )

        val mathQuizzes = listOf(
            QuizSummary("quiz_math12_01", "Toán 12 - Khảo sát hàm số", "math", 12, "Chương 1", "Đơn điệu", "HARD", 5)
        )

        override suspend fun loadQuizzes(subjectWireId: String, grade: Int): List<QuizSummary> {
            val key = "$subjectWireId-$grade"
            loadQuizzesDeferredMap[key]?.let { return it.await() }

            failLoadWith?.let { throw it }

            return if (subjectWireId == "math") mathQuizzes else physicsQuizzes
        }

        override suspend fun startSession(quizId: String): PracticeSession {
            startSessionCallCount++
            lastStartedQuizId = quizId

            failStartWith?.let { throw it }

            startSessionDeferred?.let { return it.await() }

            return PracticeSession(
                id = "sess_${quizId}",
                quizId = quizId,
                subjectId = "physics",
                grade = 10,
                state = "IN_PROGRESS",
                score = 0,
                maxScore = 20,
                correctCount = 0,
                totalCount = 2,
                questions = listOf(
                    PracticeQuestion("q1", "Q1", "Ch1", "T1", "Câu hỏi 1?", "A", "B", "C", "D", points = 10),
                    PracticeQuestion("q2", "Q2", "Ch1", "T1", "Câu hỏi 2?", "A", "B", "C", "D", points = 10)
                )
            )
        }

        override suspend fun submitAnswer(sessionId: String, questionId: String, selectedOption: String): PracticeSession {
            submitAnswerCallCount++
            failSubmitWith?.let { throw it }

            return PracticeSession(
                id = sessionId,
                quizId = "quiz_phy10_01",
                subjectId = "physics",
                grade = 10,
                state = "IN_PROGRESS",
                score = 10,
                maxScore = 20,
                correctCount = 1,
                totalCount = 2,
                questions = listOf(
                    PracticeQuestion("q1", "Q1", "Ch1", "T1", "Câu hỏi 1?", "A", "B", "C", "D", points = 10, userAnswer = selectedOption, isCorrect = true, correctOption = "B"),
                    PracticeQuestion("q2", "Q2", "Ch1", "T1", "Câu hỏi 2?", "A", "B", "C", "D", points = 10)
                )
            )
        }

        override suspend fun completeSession(sessionId: String): PracticeSession {
            completeSessionCallCount++
            failCompleteWith?.let { throw it }

            return PracticeSession(
                id = sessionId,
                quizId = "quiz_phy10_01",
                subjectId = "physics",
                grade = 10,
                state = "COMPLETED",
                score = 20,
                maxScore = 20,
                correctCount = 2,
                totalCount = 2,
                questions = emptyList()
            )
        }

        override val endpoint: String = "http://localhost:8000"
        override val ownerId: Int = 1

        var activeSessionsList: List<SessionSummary> = emptyList()
        var sessionStore = mutableMapOf<String, PracticeSession>()
        var wrongQuestionsList: List<WrongQuestionEntry> = emptyList()

        override suspend fun listActiveSessions(subjectWireId: String, grade: Int): List<SessionSummary> {
            return activeSessionsList
        }

        override suspend fun getSession(sessionId: String): PracticeSession {
            return sessionStore[sessionId] ?: PracticeSession(
                id = sessionId,
                quizId = "quiz_phy10_01",
                subjectId = "physics",
                grade = 10,
                state = "IN_PROGRESS",
                score = 0,
                maxScore = 20,
                correctCount = 0,
                totalCount = 2,
                questions = listOf(
                    PracticeQuestion("q1", "Q1", "Ch1", "T1", "Câu hỏi 1?", "A", "B", "C", "D", points = 10),
                    PracticeQuestion("q2", "Q2", "Ch1", "T1", "Câu hỏi 2?", "A", "B", "C", "D", points = 10)
                )
            )
        }

        override suspend fun loadWrongQuestions(subjectWireId: String?, grade: Int?, state: String?): List<WrongQuestionEntry> {
            return wrongQuestionsList
        }

        override suspend fun retryWrongQuestion(entryId: String, selectedOption: String): WrongQuestionRetryResult {
            return WrongQuestionRetryResult(
                entryId = entryId,
                questionId = "q1",
                selectedOption = selectedOption,
                correctOption = "B",
                isCorrect = selectedOption == "B",
                state = if (selectedOption == "B") "RESOLVED" else "NEEDS_REVIEW",
                explanation = "Giải thích câu 1",
                updatedAt = "2026-10-24T12:00:00Z"
            )
        }
    }

    // -------------------------------------------------------------
    // A. Start lỗi khi session còn null
    // -------------------------------------------------------------
    @Test
    fun testA_StartFailureWhileSessionNullPreservesListAndShowsActionError() = runBlocking {
        val repo = ControlledFakeLibraryRepository()
        val controller = LibraryPracticeController(repo, this)

        controller.loadQuizzes("physics", 10).join()
        assertEquals(2, controller.selectionState.value.quizzes?.size)

        val deferred = CompletableDeferred<PracticeSession>()
        repo.startSessionDeferred = deferred

        val secondQuiz = controller.selectionState.value.quizzes!![1]
        val job1 = controller.startQuiz(secondQuiz)

        // Trong lúc pending: startingQuizId đúng và double-click bị chặn
        assertEquals("quiz_phy10_02", controller.selectionState.value.startingQuizId)
        val job2 = controller.startQuiz(secondQuiz)
        assertNull(job2)
        assertEquals(1, repo.startSessionCallCount)

        // Request thất bại với NETWORK_ERROR (503)
        deferred.completeExceptionally(BackendClient.Failure("NETWORK_ERROR", 503))
        job1?.join()

        // Assertions:
        // 1. Không có session được tạo
        assertNull(controller.sessionState.value)
        // 2. Danh sách hai đề vẫn giữ nguyên
        assertEquals(2, controller.selectionState.value.quizzes?.size)
        // 3. Có thông báo lỗi action mà UI thực sự tiêu thụ
        val errorMsg = controller.selectionState.value.startErrorMessage
        assertNotNull(errorMsg)
        assertTrue(errorMsg?.contains("kết nối") == true || errorMsg?.contains("mạng") == true)
        // 4. selectionState.errorMessage vẫn là null (không thay toàn bộ danh sách bằng màn lỗi tải dữ liệu)
        assertNull(controller.selectionState.value.errorMessage)
        // 5. startingQuizId được giải phóng về null để cho phép thử lại
        assertNull(controller.selectionState.value.startingQuizId)
        // 6. Không có request start tự động bổ sung
        assertEquals(1, repo.startSessionCallCount)
    }

    // -------------------------------------------------------------
    // B. Thử lại sau khi start lỗi
    // -------------------------------------------------------------
    @Test
    fun testB_RetryAfterStartFailureSucceedsAndClearsError() = runBlocking {
        val repo = ControlledFakeLibraryRepository()
        val controller = LibraryPracticeController(repo, this)

        controller.loadQuizzes("physics", 10).join()

        // Lần 1 thất bại
        repo.failStartWith = BackendClient.Failure("NETWORK_ERROR", 503)
        val secondQuiz = controller.selectionState.value.quizzes!![1]
        controller.startQuiz(secondQuiz)?.join()

        assertNull(controller.sessionState.value)
        assertNotNull(controller.selectionState.value.startErrorMessage)
        assertEquals(1, repo.startSessionCallCount)

        // Người dùng chủ động thử lại đề thứ hai
        repo.failStartWith = null
        val retryJob = controller.startQuiz(secondQuiz)

        // Thông báo lỗi cũ phải được xóa ngay khi bắt đầu thử lại
        assertNull(controller.selectionState.value.startErrorMessage)

        retryJob?.join()

        // Assertions:
        // 1. Chỉ thêm đúng 1 request bổ sung (tổng 2)
        assertEquals(2, repo.startSessionCallCount)
        // 2. Gửi đúng quizId của đề thứ hai
        assertEquals("quiz_phy10_02", repo.lastStartedQuizId)
        // 3. Thành công chuyển sang đúng session
        assertNotNull(controller.sessionState.value)
        assertEquals("quiz_phy10_02", controller.sessionState.value?.session?.quizId)
        // 4. Thông báo lỗi cũ không còn
        assertNull(controller.selectionState.value.startErrorMessage)
    }

    // -------------------------------------------------------------
    // C. Đề không còn khả dụng / đã bị ẩn
    // -------------------------------------------------------------
    @Test
    fun testC_QuizUnavailableErrorProducesAppropriateMessageWithoutLosingList() = runBlocking {
        val repo = ControlledFakeLibraryRepository()
        repo.failStartWith = BackendClient.Failure("QUIZ_NOT_FOUND", 404)

        val controller = LibraryPracticeController(repo, this)
        controller.loadQuizzes("physics", 10).join()

        val firstQuiz = controller.selectionState.value.quizzes!![0]
        controller.startQuiz(firstQuiz)?.join()

        // Assertions:
        // 1. Không tạo session giả
        assertNull(controller.sessionState.value)
        // 2. Không mất danh sách đề
        assertEquals(2, controller.selectionState.value.quizzes?.size)
        // 3. Thông báo phù hợp cho đề bị ẩn/không tồn tại, không gọi là lỗi mạng
        val msg = controller.selectionState.value.startErrorMessage
        assertNotNull(msg)
        assertTrue(msg?.contains("không khả dụng") == true || msg?.contains("bị ẩn") == true)
        assertFalse(msg?.contains("mạng") == true)
        // 4. startingQuizId được giải phóng
        assertNull(controller.selectionState.value.startingQuizId)
    }

    // -------------------------------------------------------------
    // D. Cancellation và ngữ cảnh cũ
    // -------------------------------------------------------------
    @Test
    fun testD_CancellationDoesNotShowNetworkError() = runBlocking {
        val repo = ControlledFakeLibraryRepository()
        val deferred = CompletableDeferred<PracticeSession>()
        repo.startSessionDeferred = deferred

        val controller = LibraryPracticeController(repo, this)
        controller.loadQuizzes("physics", 10).join()

        val quiz = controller.selectionState.value.quizzes!![0]
        val job = controller.startQuiz(quiz)
        assertEquals("quiz_phy10_01", controller.selectionState.value.startingQuizId)

        // Hủy job khi đang chờ
        job?.cancel()
        job?.join()

        // Cancellation không được hiển thị thành lỗi mạng
        assertNull(controller.sessionState.value)
        assertNull(controller.selectionState.value.startErrorMessage)
        assertNull(controller.selectionState.value.startingQuizId)
    }

    @Test
    fun testD_ContextSwitchDiscardsStaleStartResponse() = runBlocking {
        val repo = ControlledFakeLibraryRepository()
        val deferredPhysicsStart = CompletableDeferred<PracticeSession>()
        repo.startSessionDeferred = deferredPhysicsStart

        val controller = LibraryPracticeController(repo, this)
        controller.loadQuizzes("physics", 10).join()
        val physicsQuiz = controller.selectionState.value.quizzes!![0]

        // Bắt đầu đề Vật lý 10
        val startJob = controller.startQuiz(physicsQuiz)

        // Người dùng đổi ngữ cảnh sang môn Toán 12
        controller.loadQuizzes("math", 12).join()
        assertEquals("quiz_math12_01", controller.selectionState.value.quizzes?.first()?.id)

        // Request cũ của Vật lý trả lời sau khi đã đổi ngữ cảnh
        deferredPhysicsStart.complete(
            PracticeSession("sess_phy", "quiz_phy10_01", "physics", 10, "IN_PROGRESS", 0, 20, 0, 2, emptyList())
        )
        startJob?.join()

        // Session cũ không được gán cho ngữ cảnh mới
        assertNull(controller.sessionState.value)
        assertNull(controller.selectionState.value.startErrorMessage)
        assertEquals("quiz_math12_01", controller.selectionState.value.quizzes?.first()?.id)
    }

    // -------------------------------------------------------------
    // E. Các test hồi quy (B1 - B8)
    // -------------------------------------------------------------
    @Test
    fun testB1_LoadingQuizListDoesNotAutoStartSession() = runBlocking {
        val repo = ControlledFakeLibraryRepository()
        val controller = LibraryPracticeController(repo, this)

        controller.loadQuizzes("physics", 10).join()

        val state = controller.selectionState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertNotNull(state.quizzes)
        assertEquals(2, state.quizzes?.size)
        assertEquals("quiz_phy10_01", state.quizzes?.get(0)?.id)
        assertEquals("quiz_phy10_02", state.quizzes?.get(1)?.id)

        // Must NEVER auto-start a session
        assertEquals(0, repo.startSessionCallCount)
        assertNull(controller.sessionState.value)
    }

    @Test
    fun testB2_SelectingSecondQuizSendsCorrectQuizId() = runBlocking {
        val repo = ControlledFakeLibraryRepository()
        val controller = LibraryPracticeController(repo, this)

        controller.loadQuizzes("physics", 10).join()

        val secondQuiz = controller.selectionState.value.quizzes!![1]
        controller.startQuiz(secondQuiz)?.join()

        assertEquals("quiz_phy10_02", repo.lastStartedQuizId)
        assertEquals(1, repo.startSessionCallCount)

        val sessionState = controller.sessionState.value
        assertNotNull(sessionState)
        assertEquals("Vật lý 10 - Động lực học", sessionState?.quizTitle)
        assertEquals("quiz_phy10_02", sessionState?.session?.quizId)
    }

    @Test
    fun testB3_DoubleClickGuardPreventsDuplicateStartSession() = runBlocking {
        val repo = ControlledFakeLibraryRepository()
        val deferred = CompletableDeferred<PracticeSession>()
        repo.startSessionDeferred = deferred

        val controller = LibraryPracticeController(repo, this)
        controller.loadQuizzes("physics", 10).join()

        val firstQuiz = controller.selectionState.value.quizzes!![0]

        // Click 1: in-flight
        val job1 = controller.startQuiz(firstQuiz)
        assertNotNull(job1)
        assertEquals("quiz_phy10_01", controller.selectionState.value.startingQuizId)

        // Click 2: while first click is in-flight -> ignored, returns null
        val job2 = controller.startQuiz(firstQuiz)
        assertNull(job2)

        // Complete the deferred
        deferred.complete(
            PracticeSession("sess_01", "quiz_phy10_01", "physics", 10, "IN_PROGRESS", 0, 20, 0, 2, emptyList())
        )
        job1?.join()

        // Guard must have ensured exactly 1 startSession call occurred
        assertEquals(1, repo.startSessionCallCount)
        assertNull(controller.selectionState.value.startingQuizId)
    }

    @Test
    fun testB4_RetryWithoutAutoStart() = runBlocking {
        val repo = ControlledFakeLibraryRepository()
        repo.failLoadWith = BackendClient.Failure("SERVER_NOT_CONFIGURED", 0)

        val controller = LibraryPracticeController(repo, this)
        controller.loadQuizzes("physics", 10).join()

        // First attempt fails
        assertNotNull(controller.selectionState.value.errorMessage)
        assertNull(controller.selectionState.value.quizzes)
        assertNull(controller.sessionState.value)

        // Retry succeeds
        repo.failLoadWith = null
        controller.loadQuizzes("physics", 10).join()

        assertNull(controller.selectionState.value.errorMessage)
        assertEquals(2, controller.selectionState.value.quizzes?.size)
        // Must still NOT auto-start session
        assertEquals(0, repo.startSessionCallCount)
        assertNull(controller.sessionState.value)
    }

    @Test
    fun testB5_ResponseGenerationOrderingIgnoresStaleResponses() = runBlocking {
        val repo = ControlledFakeLibraryRepository()
        val defPhysics = CompletableDeferred<List<QuizSummary>>()
        val defMath = CompletableDeferred<List<QuizSummary>>()

        repo.loadQuizzesDeferredMap["physics-10"] = defPhysics
        repo.loadQuizzesDeferredMap["math-12"] = defMath

        val controller = LibraryPracticeController(repo, this)

        // Request 1: Physics
        val job1 = controller.loadQuizzes("physics", 10)
        assertEquals(1, controller.requestGeneration)

        // Request 2: Math
        val job2 = controller.loadQuizzes("math", 12)
        assertEquals(2, controller.requestGeneration)

        // Request 2 (Math) resolves first
        defMath.complete(repo.mathQuizzes)
        job2.join()

        assertEquals("quiz_math12_01", controller.selectionState.value.quizzes?.first()?.id)

        // Request 1 (Physics) resolves later
        defPhysics.complete(repo.physicsQuizzes)
        job1.join()

        // Stale response must be discarded; Math must remain!
        assertEquals("quiz_math12_01", controller.selectionState.value.quizzes?.first()?.id)
    }

    @Test
    fun testB6_CompleteServerRejectionKeepsSessionInProgress() = runBlocking {
        val repo = ControlledFakeLibraryRepository()
        val controller = LibraryPracticeController(repo, this)

        controller.loadQuizzes("physics", 10).join()

        val quiz = controller.selectionState.value.quizzes!![0]
        controller.startQuiz(quiz)?.join()

        // Submit answer for question 1
        controller.selectOption("B")
        controller.submitAnswer()?.join()

        // Question 2 is still unanswered; repo throws 400 SESSION_INCOMPLETE on complete
        repo.failCompleteWith = BackendClient.Failure("SESSION_INCOMPLETE", 400)
        controller.completeSession()?.join()

        val sessionState = controller.sessionState.value
        assertNotNull(sessionState)
        // Session must remain IN_PROGRESS
        assertEquals("IN_PROGRESS", sessionState?.session?.state)
        assertFalse(sessionState!!.isCompleting)
        // Previously submitted answer must still be present
        assertEquals("B", sessionState.session.questions[0].userAnswer)
        // Must show notification warning
        assertTrue(
            sessionState.notificationMessage?.contains("trả lời đủ") == true ||
            sessionState.notificationMessage?.contains("SESSION_INCOMPLETE") == true
        )
    }

    @Test
    fun testB7_LegacySessionRequiresRestartErrorMapping() = runBlocking {
        val repo = ControlledFakeLibraryRepository()
        repo.failSubmitWith = BackendClient.Failure("LEGACY_SESSION_REQUIRES_RESTART", 409)

        val controller = LibraryPracticeController(repo, this)
        controller.loadQuizzes("physics", 10).join()

        val quiz = controller.selectionState.value.quizzes!![0]
        controller.startQuiz(quiz)?.join()

        controller.selectOption("A")
        controller.submitAnswer()?.join()

        val sessionState = controller.sessionState.value
        assertNotNull(sessionState)
        assertFalse(sessionState!!.isSubmittingAnswer)
        assertTrue(
            sessionState.notificationMessage?.contains("Phiên làm bài cũ cần được bắt đầu lại") == true
        )
    }

    @Test
    fun testB8_BackNavigationRetainsSelectionAndDoesNotAutoStart() = runBlocking {
        val repo = ControlledFakeLibraryRepository()
        val controller = LibraryPracticeController(repo, this)

        controller.loadQuizzes("physics", 10).join()

        val quiz = controller.selectionState.value.quizzes!![1]
        controller.startQuiz(quiz)?.join()

        assertNotNull(controller.sessionState.value)
        assertEquals(1, repo.startSessionCallCount)

        // User navigates back to quiz list
        controller.backToList()

        assertNull(controller.sessionState.value)
        assertEquals(1, repo.startSessionCallCount) // No extra start session call
        assertNotNull(controller.selectionState.value.quizzes)
        assertEquals(2, controller.selectionState.value.quizzes?.size)
    }
}
