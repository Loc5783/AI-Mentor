package com.example.asm_final.learning

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AiPracticeViewModelTest {

    private class FakeAiPracticeRepository : AiPracticeRepository {
        var answerDeferred: CompletableDeferred<AiAnswerResult>? = null
        var explanationDeferred: CompletableDeferred<String>? = null
        var resumable: AiPracticeSession? = null
        override suspend fun getResumableSession(): AiPracticeSession? = resumable
        var getConfigCalls = 0
        val creationKeys = mutableListOf<String?>()
        val answerKeys = mutableListOf<String>()
        var failAnswerWith: Exception? = null
        var createSessionCalls = 0
        var submitAnswerCalls = 0
        var skipQuestionCalls = 0
        var completeSessionCalls = 0
        var reportQuestionCalls = 0
        var explainMoreCalls = 0

        var failCreateWith: Exception? = null
        var createDeferred: CompletableDeferred<AiPracticeSession>? = null

        val sampleQuestions = listOf(
            AiQuestion(
                id = "ai_q_1",
                order = 1,
                prompt = "Đạo hàm của hàm số y = sin(x) là gì?",
                options = mapOf(
                    "A" to "cos(x)",
                    "B" to "-cos(x)",
                    "C" to "tan(x)",
                    "D" to "-sin(x)"
                ),
                difficulty = "MEDIUM",
                correctOption = "A",
                explanation = "Theo công thức đạo hàm cơ bản, (sin x)' = cos x."
            ),
            AiQuestion(
                id = "ai_q_2",
                order = 2,
                prompt = "Đạo hàm của hàm số y = cos(x) là gì?",
                options = mapOf(
                    "A" to "-sin(x)",
                    "B" to "sin(x)",
                    "C" to "cos(x)",
                    "D" to "-tan(x)"
                ),
                difficulty = "MEDIUM",
                correctOption = "A",
                explanation = "Theo công thức đạo hàm cơ bản, (cos x)' = -sin x."
            )
        )

        var sessionAnswers = mutableMapOf<String, AiAnswerRecord>()
        var firstTryTracker = mutableMapOf<String, Boolean>()

        override suspend fun getConfig(): AiPracticeConfig {
            getConfigCalls++
            return AiPracticeConfig(
                configured = true,
                subjects = listOf("math", "physics", "chemistry"),
                difficulties = listOf("EASY", "MEDIUM", "HARD"),
                minQuestions = 1,
                maxQuestions = 10,
                defaultQuestions = 5,
                provider = "fake"
            )
        }

        override suspend fun createSession(
            subjectId: String,
            grade: Int,
            topic: String,
            difficulty: String,
            questionCount: Int,
            idempotencyKey: String?
        ): AiPracticeSession {
            createSessionCalls++
            creationKeys.add(idempotencyKey)
            failCreateWith?.let { throw it }
            createDeferred?.let { return it.await() }
            return AiPracticeSession(
                id = "sess_ai_101",
                subjectId = subjectId,
                subjectName = "Toán",
                grade = grade,
                topic = topic,
                difficulty = difficulty,
                questionCount = questionCount,
                currentIndex = 0,
                state = "ACTIVE",
                questions = sampleQuestions,
                answers = sessionAnswers
            )
        }

        override suspend fun getSession(sessionId: String): AiPracticeSession {
            resumable?.let { if (it.id == sessionId) return it }
            return AiPracticeSession(
                id = sessionId,
                subjectId = "math",
                subjectName = "Toán",
                grade = 10,
                topic = "Đạo hàm",
                difficulty = "MEDIUM",
                questionCount = 2,
                currentIndex = 0,
                state = "ACTIVE",
                questions = sampleQuestions,
                answers = sessionAnswers
            )
        }

        override suspend fun submitAnswer(
            sessionId: String,
            questionId: String,
            option: String
        ): AiAnswerResult {
            submitAnswerCalls++
            val q = sampleQuestions.first { it.id == questionId }
            val isCorrect = (q.correctOption == option)
            val firstTry = firstTryTracker.getOrPut(questionId) { isCorrect }

            val rec = AiAnswerRecord(
                selectedOption = option,
                isCorrect = isCorrect,
                firstTryCorrect = firstTry,
                attemptsCount = 1,
                revealed = true,
                skipped = false
            )
            sessionAnswers[questionId] = rec

            return AiAnswerResult(
                questionId = questionId,
                isCorrect = isCorrect,
                selectedOption = option,
                correctOption = q.correctOption ?: "A",
                explanation = q.explanation ?: "",
                hint = q.hint,
                firstTryCorrect = firstTry,
                attemptsCount = 1,
                isSkipped = false
            )
        }

        override suspend fun submitAnswerOperation(sessionId: String, questionId: String, option: String, submissionId: String): AiAnswerResult {
            answerKeys.add(submissionId)
            answerDeferred?.let { return kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) { it.await() } }
            failAnswerWith?.let { throw it }
            return submitAnswer(sessionId, questionId, option)
        }

        override suspend fun skipQuestion(sessionId: String, questionId: String): AiAnswerResult {
            skipQuestionCalls++
            val q = sampleQuestions.first { it.id == questionId }
            firstTryTracker[questionId] = false

            val rec = AiAnswerRecord(
                selectedOption = null,
                isCorrect = false,
                firstTryCorrect = false,
                attemptsCount = 0,
                revealed = true,
                skipped = true
            )
            sessionAnswers[questionId] = rec

            return AiAnswerResult(
                questionId = questionId,
                isCorrect = false,
                selectedOption = null,
                correctOption = q.correctOption ?: "A",
                explanation = q.explanation ?: "",
                hint = q.hint,
                firstTryCorrect = false,
                attemptsCount = 0,
                isSkipped = true
            )
        }

        override suspend fun completeSession(sessionId: String): AiPracticeSession {
            completeSessionCalls++
            val total = sampleQuestions.size
            val firstTryCount = sessionAnswers.values.count { it.firstTryCorrect && it.isCorrect }
            val assistedCount = sessionAnswers.values.count { !it.firstTryCorrect && it.isCorrect }
            val skipped = sessionAnswers.values.count { it.skipped }
            val incorrect = total - (firstTryCount + assistedCount + skipped)
            val accuracy = if (total > 0) (firstTryCount.toDouble() / total.toDouble() * 100.0) else 0.0

            val summary = AiPracticeSummary(
                totalQuestions = total,
                firstTryCorrectCount = firstTryCount,
                assistedCount = assistedCount,
                incorrectCount = incorrect,
                skippedCount = skipped,
                accuracyFirstTry = accuracy
            )

            return AiPracticeSession(
                id = sessionId,
                subjectId = "math",
                subjectName = "Toán",
                grade = 10,
                topic = "Đạo hàm",
                difficulty = "MEDIUM",
                questionCount = total,
                currentIndex = 1,
                state = "COMPLETED",
                questions = sampleQuestions,
                answers = sessionAnswers,
                summary = summary
            )
        }

        override suspend fun reportQuestion(
            sessionId: String,
            questionId: String,
            reason: String,
            details: String
        ): String {
            reportQuestionCalls++
            return "Đã ghi nhận báo cáo câu hỏi $questionId: $reason"
        }

        override suspend fun explainMore(
            sessionId: String,
            questionId: String,
            query: String
        ): String {
            explainMoreCalls++
            explanationDeferred?.let { return kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) { it.await() } }
            return "Lời giải thích mở rộng cho câu hỏi $questionId theo yêu cầu: $query"
        }
    }

    private lateinit var fakeRepo: FakeAiPracticeRepository
    private var currentVm: AiPracticeViewModel? = null

    @Before
    fun setUp() {
        fakeRepo = FakeAiPracticeRepository()
        currentVm = null
    }

    @After
    fun tearDown() {
        currentVm?.onCleared()
        currentVm = null
    }

    @Test
    fun testInitialStateAndConfigLoad() = runTest {
        val vm = AiPracticeViewModel(fakeRepo, backgroundScope)
        currentVm = vm
        runCurrent()

        assertEquals(AiPracticeScreenState.SETUP, vm.screenState)
        assertEquals(1, fakeRepo.getConfigCalls)
        assertNotNull(vm.config)
        assertTrue(vm.config?.configured == true)
        assertEquals(3, vm.config?.subjects?.size)
    }

    @Test
    fun testStartPracticeBlankTopicValidation() = runTest {
        val vm = AiPracticeViewModel(fakeRepo, backgroundScope)
        currentVm = vm
        runCurrent()

        vm.startPractice(
            subjectId = "math",
            grade = 10,
            topic = "   ",
            difficulty = "MEDIUM"
        )
        runCurrent()

        assertEquals(AiPracticeScreenState.SETUP, vm.screenState)
        assertNotNull(vm.errorMessage)
        assertEquals(0, fakeRepo.createSessionCalls)
    }

    @Test
    fun testStartPracticeSuccessNavigatesToPracticing() = runTest {
        val vm = AiPracticeViewModel(fakeRepo, backgroundScope)
        currentVm = vm
        runCurrent()

        vm.startPractice(
            subjectId = "math",
            grade = 10,
            topic = "Đạo hàm lượng giác",
            difficulty = "MEDIUM",
            questionCount = 2
        )
        runCurrent()

        assertEquals(AiPracticeScreenState.PRACTICING, vm.screenState)
        assertEquals(1, fakeRepo.createSessionCalls)
        assertNotNull(vm.session)
        assertEquals(0, vm.currentQuestionIndex)
        assertNotNull(vm.currentQuestion)
        assertEquals("ai_q_1", vm.currentQuestion?.id)
    }

    @Test
    fun testCancelGenerationResetsToSetup() = runTest {
        val deferred = CompletableDeferred<AiPracticeSession>()
        fakeRepo.createDeferred = deferred

        val vm = AiPracticeViewModel(fakeRepo, backgroundScope)
        currentVm = vm
        runCurrent()

        vm.startPractice(
            subjectId = "math",
            grade = 10,
            topic = "Sóng cơ",
            difficulty = "MEDIUM"
        )
        runCurrent()

        assertEquals(AiPracticeScreenState.GENERATING, vm.screenState)

        vm.cancelGeneration()
        assertEquals(AiPracticeScreenState.SETUP, vm.screenState)

        // Delayed response arrives after cancellation
        deferred.complete(
            AiPracticeSession(
                id = "late_sess",
                subjectId = "math",
                subjectName = "Toán",
                grade = 10,
                topic = "Sóng cơ",
                difficulty = "MEDIUM",
                questionCount = 2,
                currentIndex = 0,
                state = "ACTIVE",
                questions = fakeRepo.sampleQuestions,
                answers = emptyMap()
            )
        )
        runCurrent()

        // State remains SETUP, late session ignored
        assertEquals(AiPracticeScreenState.SETUP, vm.screenState)
        assertNull(vm.session)
    }

    @Test
    fun testAnswerSubmissionFirstTryCorrect() = runTest {
        val vm = AiPracticeViewModel(fakeRepo, backgroundScope)
        currentVm = vm
        runCurrent()

        vm.startPractice(
            subjectId = "math",
            grade = 10,
            topic = "Đạo hàm lượng giác",
            difficulty = "MEDIUM"
        )
        runCurrent()

        // Select option A (correct)
        vm.selectOption("A")
        assertEquals("A", vm.selectedOption)

        vm.submitAnswer()
        runCurrent()

        assertEquals(AiPracticeScreenState.FEEDBACK, vm.screenState)
        assertNotNull(vm.lastResult)
        assertTrue(vm.lastResult?.isCorrect == true)
        assertTrue(vm.lastResult?.firstTryCorrect == true)
        assertEquals("A", vm.lastResult?.correctOption)
    }

    @Test
    fun testAnswerWrongFeedbackAndRetry() = runTest {
        val vm = AiPracticeViewModel(fakeRepo, backgroundScope)
        currentVm = vm
        runCurrent()

        vm.startPractice(
            subjectId = "math",
            grade = 10,
            topic = "Đạo hàm lượng giác",
            difficulty = "MEDIUM"
        )
        runCurrent()

        // Select option B (wrong)
        vm.selectOption("B")
        vm.submitAnswer()
        runCurrent()

        assertEquals(AiPracticeScreenState.FEEDBACK, vm.screenState)
        assertFalse(vm.lastResult?.isCorrect == true)

        // Retry question
        vm.retryQuestion()
        assertEquals(AiPracticeScreenState.PRACTICING, vm.screenState)
        assertNull(vm.selectedOption)
        assertNull(vm.lastResult)

        // Select option A (correct on retry)
        vm.selectOption("A")
        vm.submitAnswer()
        runCurrent()

        assertEquals(AiPracticeScreenState.FEEDBACK, vm.screenState)
        assertTrue(vm.lastResult?.isCorrect == true)
        assertFalse(vm.lastResult?.firstTryCorrect == true) // Retry is NOT firstTryCorrect
    }

    @Test
    fun testSkipQuestionMovesToFeedback() = runTest {
        val vm = AiPracticeViewModel(fakeRepo, backgroundScope)
        currentVm = vm
        runCurrent()

        vm.startPractice(
            subjectId = "math",
            grade = 10,
            topic = "Đạo hàm lượng giác",
            difficulty = "MEDIUM"
        )
        runCurrent()

        vm.skipCurrentQuestion()
        runCurrent()

        assertEquals(AiPracticeScreenState.FEEDBACK, vm.screenState)
        assertNotNull(vm.lastResult)
        assertTrue(vm.lastResult?.isSkipped == true)
        assertFalse(vm.lastResult?.isCorrect == true)
        assertEquals(1, fakeRepo.skipQuestionCalls)
    }

    @Test
    fun testNextQuestionAndSessionCompletion() = runTest {
        val vm = AiPracticeViewModel(fakeRepo, backgroundScope)
        currentVm = vm
        runCurrent()

        vm.startPractice(
            subjectId = "math",
            grade = 10,
            topic = "Đạo hàm lượng giác",
            difficulty = "MEDIUM"
        )
        runCurrent()

        // Answer Q1 correctly
        vm.selectOption("A")
        vm.submitAnswer()
        runCurrent()

        // Next to Q2
        vm.nextQuestion()
        assertEquals(AiPracticeScreenState.PRACTICING, vm.screenState)
        assertEquals(1, vm.currentQuestionIndex)
        assertEquals("ai_q_2", vm.currentQuestion?.id)

        // Skip Q2
        vm.skipCurrentQuestion()
        runCurrent()

        // Next on last question triggers finishSession
        vm.nextQuestion()
        runCurrent()

        assertEquals(AiPracticeScreenState.SUMMARY, vm.screenState)
        assertEquals(1, fakeRepo.completeSessionCalls)
        val summary = vm.session?.summary
        assertNotNull(summary)
        assertEquals(2, summary?.totalQuestions)
        assertEquals(1, summary?.firstTryCorrectCount)
        assertEquals(1, summary?.skippedCount)
        assertEquals(50, summary?.accuracyPercent)
    }

    @Test
    fun testReportQuestionAndAskAiMore() = runTest {
        val vm = AiPracticeViewModel(fakeRepo, backgroundScope)
        currentVm = vm
        runCurrent()

        vm.startPractice(
            subjectId = "math",
            grade = 10,
            topic = "Đạo hàm lượng giác",
            difficulty = "MEDIUM"
        )
        runCurrent()

        vm.selectOption("A")
        vm.submitAnswer()
        runCurrent()

        // Report question
        vm.reportQuestion("Sai kiến thức", "Đạo hàm sin phải ra cos.")
        runCurrent()
        assertEquals(1, fakeRepo.reportQuestionCalls)
        assertNotNull(vm.reportStatus)
        assertTrue(vm.reportStatus?.contains("ai_q_1") == true)

        // Ask AI more
        vm.askAiMore("Giải thích sâu hơn")
        runCurrent()
        assertEquals(1, fakeRepo.explainMoreCalls)
        assertNotNull(vm.explainMoreText)
        assertTrue(vm.explainMoreText?.contains("Giải thích sâu hơn") == true)
    }

    @Test
    fun testResetToSetup() = runTest {
        val vm = AiPracticeViewModel(fakeRepo, backgroundScope)
        currentVm = vm
        runCurrent()

        vm.startPractice(
            subjectId = "math",
            grade = 10,
            topic = "Đạo hàm lượng giác",
            difficulty = "MEDIUM"
        )
        runCurrent()

        vm.resetToSetup()
        assertEquals(AiPracticeScreenState.SETUP, vm.screenState)
        assertNull(vm.session)
        assertEquals(0, vm.currentQuestionIndex)
        assertNull(vm.selectedOption)
        assertNull(vm.lastResult)
        assertNull(vm.errorMessage)
    }

    @Test
    fun testGenerationRetryKeepsOperationKey() = runTest {
        val repo = FakeAiPracticeRepository()
        val vm = AiPracticeViewModel(repo, backgroundScope)
        runCurrent()
        repo.failCreateWith = java.io.IOException("response lost")
        vm.startPractice("math", 10, "Topic", "EASY", 3)
        runCurrent()
        repo.failCreateWith = null
        vm.startPractice("math", 10, "Topic", "EASY", 3)
        runCurrent()
        assertNotNull(repo.creationKeys.first())
        assertEquals(repo.creationKeys[0], repo.creationKeys[1])
        vm.startPractice("physics", 10, "Other topic", "EASY", 3)
        runCurrent()
        assertNotEquals(repo.creationKeys[1], repo.creationKeys[2])
        vm.onCleared()
    }

    @Test
    fun testAnswerTransportRetryKeepsKeyButDeliberatePracticeGetsNewKey() = runTest {
        val repo = FakeAiPracticeRepository()
        val vm = AiPracticeViewModel(repo, backgroundScope)
        vm.startPractice("math", 10, "Topic", "EASY", 3)
        runCurrent()
        vm.selectOption("B")
        repo.failAnswerWith = java.io.IOException("response lost")
        vm.submitAnswer()
        runCurrent()
        repo.failAnswerWith = null
        vm.submitAnswer()
        runCurrent()
        assertEquals(repo.answerKeys[0], repo.answerKeys[1])
        vm.retryQuestion()
        vm.selectOption("B")
        vm.submitAnswer()
        runCurrent()
        assertNotEquals(repo.answerKeys[1], repo.answerKeys[2])
        vm.onCleared()
    }

    @Test
    fun testSummaryUsesServerPercentageWithoutMultiplyingAgain() {
        val summary = AiPracticeSummary(2, 1, 0, 1, 0, 50.0)
        assertEquals(50, summary.accuracyPercent)
    }

    @Test
    fun testLateAnswerCannotRestoreFeedbackAfterReset() = runTest {
        val repo = FakeAiPracticeRepository()
        val vm = AiPracticeViewModel(repo, backgroundScope)
        vm.startPractice("math", 10, "Topic", "EASY", 3)
        runCurrent()
        repo.answerDeferred = CompletableDeferred()
        vm.selectOption("A")
        vm.submitAnswer()
        runCurrent()
        vm.resetToSetup()
        repo.answerDeferred!!.complete(AiAnswerResult("ai_q_1", true, "A", "A", "Explanation", firstTryCorrect = true))
        runCurrent()
        assertEquals(AiPracticeScreenState.SETUP, vm.screenState)
        assertNull(vm.lastResult)
        assertNull(vm.session)
        assertFalse(vm.isSubmitting)
        vm.onCleared()
    }

    @Test
    fun testLateExplanationCannotAppearOnNextQuestion() = runTest {
        val repo = FakeAiPracticeRepository()
        val vm = AiPracticeViewModel(repo, backgroundScope)
        vm.startPractice("math", 10, "Topic", "EASY", 3)
        runCurrent()
        vm.selectOption("B")
        vm.submitAnswer()
        runCurrent()
        repo.explanationDeferred = CompletableDeferred()
        vm.askAiMore("Explain")
        runCurrent()
        vm.nextQuestion()
        repo.explanationDeferred!!.complete("Old explanation")
        runCurrent()
        assertEquals("ai_q_2", vm.currentQuestion?.id)
        assertNull(vm.explainMoreText)
        assertFalse(vm.isExplainingMore)
        vm.onCleared()
    }

    @Test
    fun testClearingAiControllerDoesNotCancelCallerScope() = runTest {
        val repo = FakeAiPracticeRepository()
        val vm = AiPracticeViewModel(repo, backgroundScope)
        runCurrent()
        vm.onCleared()
        assertTrue(backgroundScope.coroutineContext[kotlinx.coroutines.Job]!!.isActive)
    }

    @Test
    fun testResumeLoadsServerSessionAndFirstUnansweredQuestion() = runTest {
        val repo = FakeAiPracticeRepository()
        val saved = repo.createSession("math", 10, "Saved topic", "EASY", 2, "saved")
        repo.resumable = saved.copy(answers = mapOf("ai_q_1" to AiAnswerRecord("A", true, true, 1, true)))
        val vm = AiPracticeViewModel(repo, backgroundScope)
        runCurrent()
        assertNotNull(vm.resumableSession)
        val callsBefore = repo.createSessionCalls
        vm.resumePractice()
        runCurrent()
        assertEquals(callsBefore, repo.createSessionCalls)
        assertEquals(saved.id, vm.session?.id)
        assertEquals("ai_q_2", vm.currentQuestion?.id)
        assertEquals(AiPracticeScreenState.PRACTICING, vm.screenState)
        vm.onCleared()
    }

    @Test
    fun testDefiniteProviderFailureAllowsNewGenerationOperation() = runTest {
        val repo = FakeAiPracticeRepository()
        val vm = AiPracticeViewModel(repo, backgroundScope)
        repo.failCreateWith = com.example.asm_final.backend.BackendClient.Failure("AI_REQUEST_FAILED", 409)
        vm.startPractice("math", 10, "Topic", "EASY", 3)
        runCurrent()
        repo.failCreateWith = null
        vm.startPractice("math", 10, "Topic", "EASY", 3)
        runCurrent()
        assertNotEquals(repo.creationKeys[0], repo.creationKeys[1])
        assertEquals(AiPracticeScreenState.PRACTICING, vm.screenState)
        vm.onCleared()
    }

    @Test
    fun testAccuracyPercentRoundingNearest() {
        val summary1 = AiPracticeSummary(
            totalQuestions = 3,
            firstTryCorrectCount = 2,
            assistedCount = 0,
            incorrectCount = 1,
            skippedCount = 0,
            accuracyFirstTry = 66.7
        )
        assertEquals(67, summary1.accuracyPercent)

        val summary2 = summary1.copy(accuracyFirstTry = 66.4)
        assertEquals(66, summary2.accuracyPercent)

        val summary3 = summary1.copy(accuracyFirstTry = 0.0)
        assertEquals(0, summary3.accuracyPercent)

        val summary4 = summary1.copy(accuracyFirstTry = 100.0)
        assertEquals(100, summary4.accuracyPercent)

        val summaryUnder = summary1.copy(accuracyFirstTry = -5.0)
        assertEquals(0, summaryUnder.accuracyPercent)

        val summaryOver = summary1.copy(accuracyFirstTry = 120.0)
        assertEquals(100, summaryOver.accuracyPercent)
    }
}
