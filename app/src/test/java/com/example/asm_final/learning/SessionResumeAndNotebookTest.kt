package com.example.asm_final.learning

import com.example.asm_final.backend.BackendClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class SessionResumeAndNotebookTest {

    private class TestLibraryRepository : LibraryRepository {
        var dynamicEndpoint: String = "http://10.0.2.2:8000"
        var dynamicOwnerId: Int = 42

        override val endpoint: String get() = dynamicEndpoint
        override val ownerId: Int get() = dynamicOwnerId

        var activeSessions = mutableListOf<SessionSummary>()
        var sessionsMap = mutableMapOf<String, PracticeSession>()
        var wrongQuestions = mutableListOf<WrongQuestionEntry>()

        var startSessionCallCount = 0
        var getSessionCallCount = 0
        var submitAnswerCallCount = 0
        var completeSessionCallCount = 0
        var retryCallCount = 0

        var startSessionDeferred: CompletableDeferred<PracticeSession>? = null
        var submitDeferred: CompletableDeferred<PracticeSession>? = null
        var completeDeferred: CompletableDeferred<PracticeSession>? = null
        var retryNotebookDeferred: CompletableDeferred<WrongQuestionRetryResult>? = null
        var failListActiveWith: Exception? = null
        var simulateSubmitTimeoutWithServerSideRecorded: Boolean = false
        var simulateCompleteTimeoutWithServerSideRecorded: Boolean = false

        override suspend fun loadQuizzes(subjectWireId: String, grade: Int): List<QuizSummary> {
            return if (subjectWireId == "math") {
                listOf(QuizSummary("q_math_01", "Toán 12 - Giải tích", "math", 12, "Chương 1", "Hàm số", "HARD", 3))
            } else {
                listOf(
                    QuizSummary("q_phy_01", "Vật lý 10 - Động học", "physics", 10, "Chương 1", "Cơ học", "EASY", 3),
                    QuizSummary("q_phy_02", "Vật lý 10 - Động lực học", "physics", 10, "Chương 2", "Lực", "MEDIUM", 3)
                )
            }
        }

        override suspend fun listActiveSessions(subjectWireId: String, grade: Int): List<SessionSummary> {
            failListActiveWith?.let { throw it }
            return activeSessions.filter { it.subjectId == subjectWireId && it.grade == grade }
        }

        override suspend fun getSession(sessionId: String): PracticeSession {
            getSessionCallCount++
            return sessionsMap[sessionId] ?: throw BackendClient.Failure("SESSION_NOT_FOUND", 404)
        }

        override suspend fun startSession(quizId: String): PracticeSession {
            startSessionCallCount++
            startSessionDeferred?.let { return it.await() }
            val session = PracticeSession(
                id = "sess_${quizId}_${System.currentTimeMillis()}",
                quizId = quizId,
                subjectId = "physics",
                grade = 10,
                state = "IN_PROGRESS",
                score = 0,
                maxScore = 30,
                correctCount = 0,
                totalCount = 3,
                questions = listOf(
                    PracticeQuestion("q1", "Q1", "Ch1", "T1", "Câu 1?", "A", "B", "C", "D", 10),
                    PracticeQuestion("q2", "Q2", "Ch1", "T1", "Câu 2?", "A", "B", "C", "D", 10),
                    PracticeQuestion("q3", "Q3", "Ch1", "T1", "Câu 3?", "A", "B", "C", "D", 10)
                ),
                answeredCount = 0,
                firstUnansweredIndex = 0
            )
            sessionsMap[session.id] = session
            return session
        }

        override suspend fun submitAnswer(
            sessionId: String,
            questionId: String,
            selectedOption: String
        ): PracticeSession {
            submitAnswerCallCount++
            submitDeferred?.let { return it.await() }

            val cur = sessionsMap[sessionId] ?: throw BackendClient.Failure("SESSION_NOT_FOUND", 404)
            val updatedQs = cur.questions.map { q ->
                if (q.id == questionId) {
                    q.copy(userAnswer = selectedOption, isCorrect = selectedOption == "B", correctOption = "B")
                } else q
            }
            val answered = updatedQs.count { it.userAnswer != null }
            val firstUnans = updatedQs.indexOfFirst { it.userAnswer == null }.let { if (it >= 0) it else 0 }
            val updated = cur.copy(
                questions = updatedQs,
                answeredCount = answered,
                firstUnansweredIndex = firstUnans
            )
            sessionsMap[sessionId] = updated

            if (simulateSubmitTimeoutWithServerSideRecorded) {
                throw BackendClient.Failure("TIMEOUT", 408)
            }
            return updated
        }

        override suspend fun completeSession(sessionId: String): PracticeSession {
            completeSessionCallCount++
            completeDeferred?.let { return it.await() }

            val cur = sessionsMap[sessionId] ?: throw BackendClient.Failure("SESSION_NOT_FOUND", 404)
            val completed = cur.copy(state = "COMPLETED")
            sessionsMap[sessionId] = completed

            if (simulateCompleteTimeoutWithServerSideRecorded) {
                throw BackendClient.Failure("TIMEOUT", 408)
            }
            return completed
        }

        override suspend fun loadWrongQuestions(
            subjectWireId: String?,
            grade: Int?,
            state: String?
        ): List<WrongQuestionEntry> {
            return wrongQuestions.filter { item ->
                (subjectWireId == null || item.subjectId == subjectWireId) &&
                (grade == null || item.grade == grade) &&
                (state == null || state == "ALL" || item.state == state)
            }
        }

        override suspend fun retryWrongQuestion(
            entryId: String,
            selectedOption: String
        ): WrongQuestionRetryResult {
            retryCallCount++
            retryNotebookDeferred?.let { return it.await() }

            val item = wrongQuestions.find { it.id == entryId }
                ?: throw BackendClient.Failure("ENTRY_NOT_FOUND", 404)
            if (!item.hasSnapshot) {
                throw BackendClient.Failure("LEGACY_ENTRY_CANNOT_RETRY", 400)
            }
            val isCorr = selectedOption.uppercase() == item.correctOption.uppercase()
            val newState = if (isCorr) "RESOLVED" else "NEEDS_REVIEW"
            val idx = wrongQuestions.indexOfFirst { it.id == entryId }
            if (idx >= 0) {
                wrongQuestions[idx] = item.copy(state = newState, selectedOption = selectedOption)
            }
            return WrongQuestionRetryResult(
                entryId = entryId,
                questionId = item.questionId,
                selectedOption = selectedOption,
                correctOption = item.correctOption,
                isCorrect = isCorr,
                state = newState,
                explanation = item.explanation,
                updatedAt = "2026-10-24T12:00:00Z"
            )
        }
    }

    // -------------------------------------------------------------
    // 1. Session Pointer Scoping & Isolation
    // -------------------------------------------------------------
    @Test
    fun testSessionPointerScopingAndIsolation() {
        val store = InMemorySessionPointerStore()

        // Setting a pointer for User 42 on Prod endpoint
        store.setActiveSessionId("https://api.aimentor.vn", 42, "physics", 10, "quiz_01", "sess_prod_42")

        // 1. Same user on Staging endpoint: must be null (no leakage)
        val stagingPtr = store.getActiveSessionId("https://staging.aimentor.vn", 42, "physics", 10, "quiz_01")
        assertNull("Cross-endpoint leakage detected!", stagingPtr)

        // 2. Different user (User 99) on Prod endpoint: must be null (no cross-account leakage)
        val user99Ptr = store.getActiveSessionId("https://api.aimentor.vn", 99, "physics", 10, "quiz_01")
        assertNull("Cross-user leakage detected!", user99Ptr)

        // 3. Different subject/grade/quiz: must be null
        assertNull(store.getActiveSessionId("https://api.aimentor.vn", 42, "math", 10, "quiz_01"))
        assertNull(store.getActiveSessionId("https://api.aimentor.vn", 42, "physics", 11, "quiz_01"))
        assertNull(store.getActiveSessionId("https://api.aimentor.vn", 42, "physics", 10, "quiz_02"))

        // 4. Exact matching scope: returns correct session ID
        assertEquals("sess_prod_42", store.getActiveSessionId("https://api.aimentor.vn", 42, "physics", 10, "quiz_01"))

        // 5. Clearing does not affect other scopes
        store.setActiveSessionId("https://api.aimentor.vn", 42, "physics", 10, "quiz_02", "sess_prod_quiz2")
        store.clearActiveSessionId("https://api.aimentor.vn", 42, "physics", 10, "quiz_01")
        assertNull(store.getActiveSessionId("https://api.aimentor.vn", 42, "physics", 10, "quiz_01"))
        assertEquals("sess_prod_quiz2", store.getActiveSessionId("https://api.aimentor.vn", 42, "physics", 10, "quiz_02"))
    }

    // -------------------------------------------------------------
    // 2. Stale Submit Response Does Not Overwrite New Session (Test A)
    // -------------------------------------------------------------
    @Test
    fun testStaleSubmitResponseDoesNotOverwriteNewSession(): Unit = runBlocking {
        val repo = TestLibraryRepository()
        val pointerStore = InMemorySessionPointerStore()
        val scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())
        val controller = LibraryPracticeController(repo, scope, pointerStore)

        val quizA = QuizSummary("q_phy_01", "Đề A", "physics", 10, "Ch1", "Cơ", "EASY", 3)
        val quizB = QuizSummary("q_phy_02", "Đề B", "physics", 10, "Ch2", "Lực", "MEDIUM", 3)

        // 1. Open Session A
        controller.startQuiz(quizA)?.join()
        val sessionAId = controller.sessionState.value!!.session.id

        // 2. Choose option and submit, response held pending
        val deferredSubmitA = CompletableDeferred<PracticeSession>()
        repo.submitDeferred = deferredSubmitA

        controller.selectOption("A")
        val submitJob = controller.submitAnswer()
        assertTrue("Submit job should be active", submitJob?.isActive == true)

        // 3. Back to list
        controller.backToList()
        assertNull("Backing out must clear active practice state", controller.sessionState.value)

        // 4. Open Session B
        repo.submitDeferred = null
        controller.startQuiz(quizB)?.join()
        val sessionBId = controller.sessionState.value!!.session.id
        assertNotEquals(sessionAId, sessionBId)
        assertEquals("Đề B", controller.sessionState.value?.quizTitle)

        // 5. Response of A completes now
        val sessionAUpdated = repo.sessionsMap[sessionAId]!!.copy(
            score = 10,
            answeredCount = 1
        )
        deferredSubmitA.complete(sessionAUpdated)
        submitJob?.join()

        // 6. Assert Session B was NOT overwritten by Session A!
        val curState = controller.sessionState.value
        assertNotNull(curState)
        assertEquals("Session B must not be overwritten by delayed response of A", sessionBId, curState?.session?.id)
        assertEquals("Đề B", curState?.quizTitle)
        assertFalse("isSubmittingAnswer must not be stuck", curState!!.isSubmittingAnswer)
    }

    // -------------------------------------------------------------
    // 3. Stale Complete Response Does Not Overwrite New Session Or Pointer (Test B)
    // -------------------------------------------------------------
    @Test
    fun testStaleCompleteResponseDoesNotOverwriteNewSessionOrPointer(): Unit = runBlocking {
        val repo = TestLibraryRepository()
        val pointerStore = InMemorySessionPointerStore()
        val scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())
        val controller = LibraryPracticeController(repo, scope, pointerStore)

        val quizA = QuizSummary("q_phy_01", "Đề A", "physics", 10, "Ch1", "Cơ", "EASY", 3)
        val quizB = QuizSummary("q_phy_02", "Đề B", "physics", 10, "Ch2", "Lực", "MEDIUM", 3)

        controller.startQuiz(quizA)?.join()
        val sessionAId = controller.sessionState.value!!.session.id

        // Answer all questions of A
        controller.selectOption("B")
        controller.submitAnswer()?.join()
        controller.nextQuestion()
        controller.selectOption("B")
        controller.submitAnswer()?.join()
        controller.nextQuestion()
        controller.selectOption("B")
        controller.submitAnswer()?.join()

        // Call completeSession with pending deferred
        val deferredCompleteA = CompletableDeferred<PracticeSession>()
        repo.completeDeferred = deferredCompleteA
        val completeJob = controller.completeSession()

        // Back out and start Session B
        controller.backToList()
        repo.completeDeferred = null
        controller.startQuiz(quizB)?.join()
        val sessionBId = controller.sessionState.value!!.session.id

        // Pointer for Quiz B must exist
        assertEquals(sessionBId, pointerStore.getActiveSessionId(repo.endpoint, repo.ownerId, "physics", 10, quizB.id))

        // Complete A response arrives now
        val sessionACompleted = repo.sessionsMap[sessionAId]!!.copy(state = "COMPLETED")
        deferredCompleteA.complete(sessionACompleted)
        completeJob?.join()

        // Session B must remain IN_PROGRESS, not marked completed!
        val curState = controller.sessionState.value
        assertEquals(sessionBId, curState?.session?.id)
        assertEquals("IN_PROGRESS", curState?.session?.state)
        assertFalse(curState!!.isCompleting)

        // Pointer for Quiz B must NOT be cleared by Session A completion!
        assertEquals("Session B pointer must be preserved", sessionBId,
            pointerStore.getActiveSessionId(repo.endpoint, repo.ownerId, "physics", 10, quizB.id)
        )
    }

    // -------------------------------------------------------------
    // 4. Stale Retry Practice Guard & Anti Double-Click (Test C)
    // -------------------------------------------------------------
    @Test
    fun testStaleRetryPracticeAndAntiDoubleClick(): Unit = runBlocking {
        val repo = TestLibraryRepository()
        val scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())
        val controller = LibraryPracticeController(repo, scope)

        val quiz = QuizSummary("q_phy_01", "Đề A", "physics", 10, "Ch1", "Cơ", "EASY", 3)

        // Hold startSession pending
        val deferred = CompletableDeferred<PracticeSession>()
        repo.startSessionDeferred = deferred

        val job1 = controller.startQuiz(quiz)
        assertNotNull(job1)

        // While job1 is pending, second call must be blocked by anti-double-click guard
        val job2 = controller.startQuiz(quiz)
        assertNull("Second concurrent startQuiz call must be ignored by anti-double-click guard", job2)

        // Release job1
        val session = PracticeSession(
            id = "sess_q1",
            quizId = quiz.id,
            subjectId = "physics",
            grade = 10,
            state = "IN_PROGRESS",
            score = 0,
            maxScore = 30,
            correctCount = 0,
            totalCount = 3,
            questions = emptyList()
        )
        deferred.complete(session)
        job1?.join()
    }

    // -------------------------------------------------------------
    // 5. Stale Notebook Retry Does Not Inject Into New Context (Test D)
    // -------------------------------------------------------------
    @Test
    fun testStaleNotebookRetryDoesNotInjectIntoNewContext(): Unit = runBlocking {
        val repo = TestLibraryRepository()
        val scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())
        val controller = WrongQuestionsNotebookController(repo, scope)

        val entryPhysics = WrongQuestionEntry(
            id = "wq_phy_01",
            questionId = "q_phy_1",
            subjectId = "physics",
            grade = 10,
            chapter = "Ch1",
            topic = "Cơ",
            prompt = "Vận tốc?",
            options = listOf(WrongQuestionOption("A", "Sai"), WrongQuestionOption("B", "Đúng")),
            selectedOption = "A",
            correctOption = "B",
            explanation = "Giải thích",
            difficulty = "EASY",
            state = "NEEDS_REVIEW",
            hasSnapshot = true,
            canRetry = true,
            createdAt = "2026-10-24T10:00:00Z",
            updatedAt = null
        )
        val entryMath = WrongQuestionEntry(
            id = "wq_math_01",
            questionId = "q_math_1",
            subjectId = "math",
            grade = 12,
            chapter = "Ch1",
            topic = "Hàm số",
            prompt = "Đạo hàm?",
            options = listOf(WrongQuestionOption("A", "Sai"), WrongQuestionOption("B", "Đúng")),
            selectedOption = "A",
            correctOption = "B",
            explanation = "Giải thích toán",
            difficulty = "HARD",
            state = "NEEDS_REVIEW",
            hasSnapshot = true,
            canRetry = true,
            createdAt = "2026-10-24T10:00:00Z",
            updatedAt = null
        )
        repo.wrongQuestions.add(entryPhysics)
        repo.wrongQuestions.add(entryMath)

        // 1. Load physics notebook
        controller.loadNotebook("physics", 10).join()
        assertEquals(1, controller.state.value.items?.size)

        // 2. Submit retry for physics entry with pending deferred
        val deferredRetry = CompletableDeferred<WrongQuestionRetryResult>()
        repo.retryNotebookDeferred = deferredRetry

        controller.selectRetryOption(entryPhysics.id, "B")
        val retryJob = controller.submitRetry(entryPhysics)

        // 3. Student switches to Math notebook
        repo.retryNotebookDeferred = null
        controller.loadNotebook("math", 12).join()
        assertEquals(1, controller.state.value.items?.size)
        assertEquals("wq_math_01", controller.state.value.items?.first()?.id)

        // 4. Stale Physics retry response arrives now
        deferredRetry.complete(
            WrongQuestionRetryResult(
                entryId = entryPhysics.id,
                questionId = entryPhysics.questionId,
                selectedOption = "B",
                correctOption = "B",
                isCorrect = true,
                state = "RESOLVED",
                explanation = "Giải thích",
                updatedAt = "2026-10-24T12:00:00Z"
            )
        )
        retryJob?.join()

        // 5. Verify the Math notebook is NOT corrupted with Physics retry result
        val curItems = controller.state.value.items
        assertEquals("Math notebook items must not be modified by stale Physics retry", 1, curItems?.size)
        assertEquals("wq_math_01", curItems?.first()?.id)
        assertFalse("Physics retry result must not leak into Math state", controller.state.value.retryResults.containsKey(entryPhysics.id))
    }

    // -------------------------------------------------------------
    // 6. Active Sessions Failure Shows Warning And Preserves Quizzes (Test E)
    // -------------------------------------------------------------
    @Test
    fun testActiveSessionsFailureShowsWarningAndPreservesQuizzes(): Unit = runBlocking {
        val repo = TestLibraryRepository()
        val scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())
        val controller = LibraryPracticeController(repo, scope)

        // Simulate listActiveSessions failure
        repo.failListActiveWith = BackendClient.Failure("CONNECT_ERROR", 0)

        controller.loadQuizzes("physics", 10).join()

        val state = controller.selectionState.value
        // Quizzes must be preserved!
        assertNotNull("Quizzes list must be preserved when active session fetch fails", state.quizzes)
        assertEquals(2, state.quizzes?.size)

        // Warning state must be set
        assertFalse(state.activeSessionsSynced)
        assertNotNull(state.activeSessionsErrorMessage)
        assertTrue(state.activeSessionsErrorMessage!!.contains("Chưa thể đồng bộ tiến độ"))

        // Now test retryLoadActiveSessions
        val initialStartCount = repo.startSessionCallCount
        repo.failListActiveWith = null

        controller.retryLoadActiveSessions("physics", 10).join()

        val updatedState = controller.selectionState.value
        assertTrue("Active sessions should now be synced", updatedState.activeSessionsSynced)
        assertNull("Error message should be cleared", updatedState.activeSessionsErrorMessage)
        assertEquals("Retry active sessions must NOT call startSession", initialStartCount, repo.startSessionCallCount)
    }

    // -------------------------------------------------------------
    // 7. Timeout After Server Recorded Answer With Read-Back (Test F)
    // -------------------------------------------------------------
    @Test
    fun testSubmitTimeoutWithServerSideRecordedReadBack(): Unit = runBlocking {
        val repo = TestLibraryRepository()
        val scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())
        val controller = LibraryPracticeController(repo, scope)

        val quiz = QuizSummary("q_phy_01", "Đề A", "physics", 10, "Ch1", "Cơ", "EASY", 3)
        controller.startQuiz(quiz)?.join()

        // Enable simulated timeout where server recorded the answer before dropping response
        repo.simulateSubmitTimeoutWithServerSideRecorded = true

        controller.selectOption("B")
        controller.submitAnswer()?.join()

        // Read-back should have reconciled the state automatically
        val curState = controller.sessionState.value
        assertNotNull(curState)
        assertEquals("Answer should be confirmed via read-back", "B", curState?.session?.questions?.first()?.userAnswer)
        assertEquals("No network error shown when server recorded answer", null, curState?.notificationMessage)
        assertFalse(curState!!.isSubmittingAnswer)
    }

    // -------------------------------------------------------------
    // 8. Timeout After Server Recorded Complete With Read-Back (Test F2)
    // -------------------------------------------------------------
    @Test
    fun testCompleteTimeoutWithServerSideRecordedReadBack(): Unit = runBlocking {
        val repo = TestLibraryRepository()
        val pointerStore = InMemorySessionPointerStore()
        val scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())
        val controller = LibraryPracticeController(repo, scope, pointerStore)

        val quiz = QuizSummary("q_phy_01", "Đề A", "physics", 10, "Ch1", "Cơ", "EASY", 3)
        controller.startQuiz(quiz)?.join()

        // Answer all questions
        controller.selectOption("B")
        controller.submitAnswer()?.join()
        controller.nextQuestion()
        controller.selectOption("B")
        controller.submitAnswer()?.join()
        controller.nextQuestion()
        controller.selectOption("B")
        controller.submitAnswer()?.join()

        repo.simulateCompleteTimeoutWithServerSideRecorded = true
        controller.completeSession()?.join()

        val curState = controller.sessionState.value
        assertNotNull(curState)
        assertEquals("COMPLETED", curState?.session?.state)
        assertFalse(curState!!.isCompleting)

        // Pointer must be cleared
        assertNull(pointerStore.getActiveSessionId(repo.endpoint, repo.ownerId, "physics", 10, quiz.id))
    }

    // -------------------------------------------------------------
    // 9. Multiple Sessions for Same Quiz & Unpublished Quiz Access (Test G)
    // -------------------------------------------------------------
    @Test
    fun testMultipleSessionsPicksMostRecentAndUnpublishedAccessible(): Unit = runBlocking {
        val repo = TestLibraryRepository()
        val scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())
        val controller = LibraryPracticeController(repo, scope)

        // Multiple sessions for q_phy_01: older vs newer
        repo.activeSessions.add(
            SessionSummary("sess_old", "q_phy_01", "physics", 10, "IN_PROGRESS", 0, 0, 3, 0, 0, "2026-10-24T10:00:00Z")
        )
        repo.activeSessions.add(
            SessionSummary("sess_new", "q_phy_01", "physics", 10, "IN_PROGRESS", 10, 1, 3, 1, 1, "2026-10-24T12:00:00Z")
        )

        // Session for an unpublished/hidden quiz
        val unpublishedSummary = SessionSummary("sess_hidden", "q_hidden_99", "physics", 10, "IN_PROGRESS", 10, 1, 3, 1, 1, "2026-10-24T11:00:00Z")
        repo.activeSessions.add(unpublishedSummary)

        repo.sessionsMap["sess_hidden"] = PracticeSession(
            id = "sess_hidden",
            quizId = "q_hidden_99",
            subjectId = "physics",
            grade = 10,
            state = "IN_PROGRESS",
            score = 10,
            maxScore = 30,
            correctCount = 1,
            totalCount = 3,
            questions = listOf(
                PracticeQuestion("q1", "Q1", "Ch1", "T1", "Câu 1?", "A", "B", "C", "D", 10, userAnswer = "B", isCorrect = true),
                PracticeQuestion("q2", "Q2", "Ch1", "T1", "Câu 2?", "A", "B", "C", "D", 10),
                PracticeQuestion("q3", "Q3", "Ch1", "T1", "Câu 3?", "A", "B", "C", "D", 10)
            ),
            answeredCount = 1,
            firstUnansweredIndex = 1
        )

        controller.loadQuizzes("physics", 10).join()

        // 1. Verify most recent session is selected for q_phy_01
        val selectedActive = controller.selectionState.value.activeSessionsByQuizId["q_phy_01"]
        assertEquals("sess_new", selectedActive?.id)
        assertEquals("2026-10-24T12:00:00Z", selectedActive?.updatedAt)

        // 2. Verify unpublished quiz session can be resumed directly via snapshot
        controller.resumeSessionDirect(unpublishedSummary)?.join()
        assertEquals("sess_hidden", controller.sessionState.value?.session?.id)
        assertEquals(1, controller.sessionState.value?.currentStep)
    }

    // -------------------------------------------------------------
    // 10. Controller Recreation With Same Store & Identity Isolation (Test G2)
    // -------------------------------------------------------------
    @Test
    fun testControllerRecreationWithSameStoreAndIdentityIsolation(): Unit = runBlocking {
        val repo = TestLibraryRepository()
        val pointerStore = InMemorySessionPointerStore()
        val scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())

        val quiz = QuizSummary("q_phy_01", "Đề A", "physics", 10, "Ch1", "Cơ", "EASY", 3)

        // Controller 1 creates session
        val controller1 = LibraryPracticeController(repo, scope, pointerStore)
        controller1.startQuiz(quiz)?.join()
        val sessId = controller1.sessionState.value!!.session.id

        // Controller 2 created (simulating screen recreation or app restart)
        val controller2 = LibraryPracticeController(repo, scope, pointerStore)
        assertNull(controller2.sessionState.value)

        // Controller 2 restores session using pointer store
        val restoreJob = controller2.restoreActiveSessionIfAny(quiz)
        assertNotNull("Must find pointer and return job", restoreJob)
        restoreJob?.join()
        assertEquals(sessId, controller2.sessionState.value?.session?.id)

        // Now change identity: different owner
        repo.dynamicOwnerId = 99
        val controllerDiffUser = LibraryPracticeController(repo, scope, pointerStore)
        val diffJob = controllerDiffUser.restoreActiveSessionIfAny(quiz)
        assertNull("Different user must NOT restore previous user's session pointer", diffJob)
    }
}
