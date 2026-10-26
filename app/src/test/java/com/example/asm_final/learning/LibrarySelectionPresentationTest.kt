package com.example.asm_final.learning

import com.example.asm_final.backend.BackendClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class LibrarySelectionPresentationTest {

    private class TestPresentationRepository : LibraryRepository {
        var dynamicEndpoint: String = "http://10.0.2.2:8000"
        var dynamicOwnerId: Int = 42

        override val endpoint: String get() = dynamicEndpoint
        override val ownerId: Int get() = dynamicOwnerId

        var quizzesList = mutableListOf<QuizSummary>()
        var activeSessions = mutableListOf<SessionSummary>()
        var sessionsMap = mutableMapOf<String, PracticeSession>()

        var startSessionCallCount = 0
        var getSessionCallCount = 0
        var lastGetSessionId: String? = null
        var failListActiveWith: Exception? = null
        var getSessionDeferred: CompletableDeferred<PracticeSession>? = null

        override suspend fun loadQuizzes(subjectWireId: String, grade: Int): List<QuizSummary> {
            return quizzesList.filter { it.subjectId == subjectWireId && it.grade == grade }
        }

        override suspend fun listActiveSessions(subjectWireId: String, grade: Int): List<SessionSummary> {
            failListActiveWith?.let { throw it }
            return activeSessions.filter { it.subjectId == subjectWireId && it.grade == grade }
        }

        override suspend fun getSession(sessionId: String): PracticeSession {
            getSessionCallCount++
            lastGetSessionId = sessionId
            getSessionDeferred?.let { return it.await() }
            return sessionsMap[sessionId] ?: throw BackendClient.Failure("SESSION_NOT_FOUND", 404)
        }

        override suspend fun startSession(quizId: String): PracticeSession {
            startSessionCallCount++
            throw UnsupportedOperationException("startSession must not be called in resume flow")
        }

        override suspend fun submitAnswer(
            sessionId: String,
            questionId: String,
            selectedOption: String
        ): PracticeSession {
            throw UnsupportedOperationException("Not needed in presentation test")
        }

        override suspend fun completeSession(sessionId: String): PracticeSession {
            throw UnsupportedOperationException("Not needed in presentation test")
        }

        override suspend fun loadWrongQuestions(
            subjectWireId: String?,
            grade: Int?,
            state: String?
        ): List<WrongQuestionEntry> {
            return emptyList()
        }

        override suspend fun retryWrongQuestion(
            entryId: String,
            selectedOption: String
        ): WrongQuestionRetryResult {
            throw UnsupportedOperationException("Not needed in presentation test")
        }
    }

    private fun samplePracticeSession(
        id: String,
        quizId: String,
        state: String = "IN_PROGRESS",
        answeredCount: Int = 2,
        firstUnansweredIndex: Int = 2
    ) = PracticeSession(
        id = id,
        quizId = quizId,
        subjectId = "physics",
        grade = 10,
        state = state,
        score = 20,
        maxScore = 50,
        correctCount = 2,
        totalCount = 5,
        questions = emptyList(),
        answeredCount = answeredCount,
        firstUnansweredIndex = firstUnansweredIndex
    )

    // 1. quizzes=[] + có IN_PROGRESS: Khu vực phiên dở được hiển thị, có nút Tiếp tục
    @Test
    fun test1_EmptyQuizzesWithInProgressSession_ShowsActiveSessionsAreaAndContinueAction(): Unit = runBlocking {
        val repo = TestPresentationRepository()
        repo.quizzesList.clear() // 0 public quizzes
        repo.activeSessions.add(
            SessionSummary(
                id = "sess_hidden_01",
                quizId = "q_unpublished_01",
                subjectId = "physics",
                grade = 10,
                state = "IN_PROGRESS",
                score = 20,
                correctCount = 2,
                totalCount = 5,
                answeredCount = 2,
                firstUnansweredIndex = 2,
                updatedAt = "2026-10-26T08:00:00Z"
            )
        )

        val scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())
        val controller = LibraryPracticeController(repo, scope)
        controller.loadQuizzes("physics", 10).join()

        val presentation = controller.selectionState.value.toPresentation()

        // Standalone active session area must be present
        assertEquals(1, presentation.standaloneActiveSessions.size)
        val card = presentation.standaloneActiveSessions[0]
        assertEquals("sess_hidden_01", card.sessionId)
        assertEquals("q_unpublished_01", card.quizId)
        assertEquals(2, card.answeredCount)
        assertEquals(5, card.totalCount)
        assertTrue(card.isUnpublishedOrCatalogEmpty)
        assertFalse(card.isResuming)

        // Catalog section is Empty, but notes active sessions above
        assertTrue(presentation.catalogSection is CatalogSectionPresentation.Empty)
        val emptyCatalog = presentation.catalogSection as CatalogSectionPresentation.Empty
        assertTrue(emptyCatalog.hasActiveSessionsAbove)
        assertFalse("Must not show global empty state when in-progress sessions exist", presentation.showGlobalEmptyState)
    }

    // 2. Bấm Tiếp tục: Gửi getSession đúng ID, không gọi startSession, chuyển đúng firstUnansweredIndex
    @Test
    fun test2_ClickingContinueFromActiveSessionCard_InvokesGetSessionAndDoesNotCallStartSession(): Unit = runBlocking {
        val repo = TestPresentationRepository()
        val sessionObj = samplePracticeSession("sess_hidden_01", "q_unpublished_01", firstUnansweredIndex = 3)
        repo.sessionsMap["sess_hidden_01"] = sessionObj
        repo.activeSessions.add(
            SessionSummary(
                id = "sess_hidden_01",
                quizId = "q_unpublished_01",
                subjectId = "physics",
                grade = 10,
                state = "IN_PROGRESS",
                score = 20,
                correctCount = 2,
                totalCount = 5,
                answeredCount = 3,
                firstUnansweredIndex = 3,
                updatedAt = "2026-10-26T08:00:00Z"
            )
        )

        val scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())
        val controller = LibraryPracticeController(repo, scope)
        controller.loadQuizzes("physics", 10).join()

        val presentation = controller.selectionState.value.toPresentation()
        val card = presentation.standaloneActiveSessions[0]

        // User clicks Continue on this card
        controller.resumeSessionDirect(card.session)?.join()

        assertEquals("Must invoke getSession with exact sessionId", 1, repo.getSessionCallCount)
        assertEquals("sess_hidden_01", repo.lastGetSessionId)
        assertEquals("Must NEVER invoke startSession when resuming", 0, repo.startSessionCallCount)

        assertNotNull("Session practice state must be populated", controller.sessionState.value)
        assertEquals("sess_hidden_01", controller.sessionState.value?.session?.id)
        assertEquals("Must jump to firstUnansweredIndex", 3, controller.sessionState.value?.currentStep)
    }

    // 3. quizzes=[] + active sessions tải lỗi: Có cảnh báo và retry, không hiện kết luận “không có bài dở”
    @Test
    fun test3_EmptyQuizzesWithActiveSessionsSyncFailure_ShowsWarningAndDoesNotConcludeNoActiveSessions(): Unit = runBlocking {
        val repo = TestPresentationRepository()
        repo.quizzesList.clear()
        repo.failListActiveWith = BackendClient.Failure("SERVER_TIMEOUT", 504)

        val scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())
        val controller = LibraryPracticeController(repo, scope)
        controller.loadQuizzes("physics", 10).join()

        val presentation = controller.selectionState.value.toPresentation()

        assertNotNull("Must show active sessions sync warning banner", presentation.activeSessionsSyncWarning)
        assertTrue(presentation.activeSessionsSyncWarning!!.canRetry)

        assertTrue(presentation.catalogSection is CatalogSectionPresentation.Empty)
        val emptySection = presentation.catalogSection as CatalogSectionPresentation.Empty
        assertFalse("Message must NOT claim there are no active sessions", emptySection.message.contains("không có bài dở"))
        assertFalse("Must not show global empty state on transient error", presentation.showGlobalEmptyState)
    }

    // 4. Retry tiến độ: Không tạo session, sau thành công cập nhật đúng khu vực
    @Test
    fun test4_RetryActiveSessionsProgress_DoesNotCreateSessionAndUpdatesAreaUponSuccess(): Unit = runBlocking {
        val repo = TestPresentationRepository()
        repo.quizzesList.clear()
        repo.failListActiveWith = BackendClient.Failure("NETWORK_ERROR", 0)

        val scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())
        val controller = LibraryPracticeController(repo, scope)
        controller.loadQuizzes("physics", 10).join()

        // Initial state has warning
        val presBefore = controller.selectionState.value.toPresentation()
        assertNotNull(presBefore.activeSessionsSyncWarning)

        // Clear network error and inject session
        repo.failListActiveWith = null
        repo.activeSessions.add(
            SessionSummary(
                id = "sess_retried_01",
                quizId = "q_unpublished_01",
                subjectId = "physics",
                grade = 10,
                state = "IN_PROGRESS",
                score = 10,
                correctCount = 1,
                totalCount = 5,
                answeredCount = 1,
                firstUnansweredIndex = 1,
                updatedAt = "2026-10-26T08:05:00Z"
            )
        )

        // Retry progress
        controller.retryLoadActiveSessions("physics", 10).join()

        assertEquals("Retry active sessions must NEVER call startSession", 0, repo.startSessionCallCount)

        val presAfter = controller.selectionState.value.toPresentation()
        assertNull("Warning banner must be cleared after successful retry", presAfter.activeSessionsSyncWarning)
        assertEquals("Active session area must now display the recovered session", 1, presAfter.standaloneActiveSessions.size)
        assertEquals("sess_retried_01", presAfter.standaloneActiveSessions[0].sessionId)
    }

    // 5. quizzes=[] + active sessions=[] đã đồng bộ: Hiển thị trạng thái trống đúng
    @Test
    fun test5_EmptyQuizzesAndEmptyActiveSessions_ShowsCorrectCleanEmptyState(): Unit = runBlocking {
        val repo = TestPresentationRepository()
        repo.quizzesList.clear()
        repo.activeSessions.clear()

        val scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())
        val controller = LibraryPracticeController(repo, scope)
        controller.loadQuizzes("physics", 10).join()

        val presentation = controller.selectionState.value.toPresentation()

        assertTrue(presentation.standaloneActiveSessions.isEmpty())
        assertNull(presentation.activeSessionsSyncWarning)
        assertTrue(presentation.catalogSection is CatalogSectionPresentation.Empty)
        assertFalse((presentation.catalogSection as CatalogSectionPresentation.Empty).hasActiveSessionsAbove)
        assertTrue("Must show clean global empty state when verified empty", presentation.showGlobalEmptyState)
    }

    // 6. Có đề công khai và phiên dở: Không nhân đôi card phiên ngoài chủ đích
    @Test
    fun test6_PublicQuizzesAndActiveSession_DoesNotDuplicateSessionCards(): Unit = runBlocking {
        val repo = TestPresentationRepository()
        repo.quizzesList.add(QuizSummary("q_phy_01", "Vật lý 10 - Động học", "physics", 10, "Chương 1", "Cơ", "EASY", 5))
        repo.quizzesList.add(QuizSummary("q_phy_02", "Vật lý 10 - Động lực học", "physics", 10, "Chương 2", "Lực", "MEDIUM", 5))

        // Session belongs to public quiz q_phy_01
        repo.activeSessions.add(
            SessionSummary(
                id = "sess_phy_01",
                quizId = "q_phy_01",
                subjectId = "physics",
                grade = 10,
                state = "IN_PROGRESS",
                score = 10,
                correctCount = 1,
                totalCount = 5,
                answeredCount = 1,
                firstUnansweredIndex = 1,
                updatedAt = "2026-10-26T08:00:00Z"
            )
        )

        val scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())
        val controller = LibraryPracticeController(repo, scope)
        controller.loadQuizzes("physics", 10).join()

        val presentation = controller.selectionState.value.toPresentation()

        // Since q_phy_01 is published, its active session MUST NOT appear in standalone list
        assertTrue("Public quiz session must not be duplicated into standalone active sessions", presentation.standaloneActiveSessions.isEmpty())

        assertTrue(presentation.catalogSection is CatalogSectionPresentation.Available)
        val catalog = presentation.catalogSection as CatalogSectionPresentation.Available
        assertEquals(2, catalog.quizzes.size)

        val card1 = catalog.quizzes.find { it.quiz.id == "q_phy_01" }!!
        assertTrue("Card for q_phy_01 must have active session embedded", card1.hasActiveSession)
        assertEquals("sess_phy_01", card1.activeSession?.id)

        val card2 = catalog.quizzes.find { it.quiz.id == "q_phy_02" }!!
        assertFalse("Card for q_phy_02 must not have active session", card2.hasActiveSession)
    }

    // 7. Phiên COMPLETED: Không xuất hiện ở khu vực đang làm
    @Test
    fun test7_CompletedSession_NeverAppearsInActiveSessionsArea(): Unit = runBlocking {
        val repo = TestPresentationRepository()
        repo.quizzesList.clear()
        // Completed session
        repo.activeSessions.add(
            SessionSummary(
                id = "sess_completed_01",
                quizId = "q_phy_01",
                subjectId = "physics",
                grade = 10,
                state = "COMPLETED",
                score = 50,
                correctCount = 5,
                totalCount = 5,
                answeredCount = 5,
                firstUnansweredIndex = 5,
                updatedAt = "2026-10-26T08:00:00Z"
            )
        )

        val scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())
        val controller = LibraryPracticeController(repo, scope)
        controller.loadQuizzes("physics", 10).join()

        val presentation = controller.selectionState.value.toPresentation()

        assertTrue("Completed session must NEVER appear in standalone active sessions", presentation.standaloneActiveSessions.isEmpty())
    }

    // 8. Giữ test response cũ / ownership liên quan khi resume và anti-double-click
    @Test
    fun test8_StaleResponseAndOwnershipGuard_MaintainedDuringResume(): Unit = runBlocking {
        val repo = TestPresentationRepository()
        val sessionObj = samplePracticeSession("sess_01", "q_01")
        val deferred = CompletableDeferred<PracticeSession>()
        repo.getSessionDeferred = deferred

        repo.activeSessions.add(
            SessionSummary(
                id = "sess_01",
                quizId = "q_01",
                subjectId = "physics",
                grade = 10,
                state = "IN_PROGRESS",
                score = 0,
                correctCount = 0,
                totalCount = 5,
                answeredCount = 1,
                firstUnansweredIndex = 1,
                updatedAt = "2026-10-26T08:00:00Z"
            )
        )

        val scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())
        val controller = LibraryPracticeController(repo, scope)
        controller.loadQuizzes("physics", 10).join()

        val card = controller.selectionState.value.toPresentation().standaloneActiveSessions[0]

        // Launch resume 1
        val job1 = controller.resumeSessionDirect(card.session)
        assertNotNull(job1)

        // While job1 is pending, anti-double-click guard blocks second call
        val job2 = controller.resumeSessionDirect(card.session)
        assertNull("Anti-double-click guard must block concurrent resume", job2)

        // Switch user identity before response arrives
        repo.dynamicOwnerId = 999

        // Release job1
        deferred.complete(sessionObj)
        job1?.join()

        // Stale response must be rejected due to identity mismatch
        assertNull("Session state must not be set when identity context changed", controller.sessionState.value)
    }
}
