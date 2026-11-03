package com.example.asm_final.learning

import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExamPrepViewModelTest {

    private class FakeExamRepository : ExamRepository {
        override val endpoint: String = "http://127.0.0.1:18080"
        override val ownerId: Int = 101

        var listExamsCallCount = 0
        var startSessionCallCount = 0
        var saveAnswerCallCount = 0
        var submitSessionCallCount = 0
        var getSessionCallCount = 0

        var failStartWith: Exception? = null
        var failSubmitWith: Exception? = null
        var failSaveWith: Exception? = null
        var submitCommittedOnServerBeforeNetworkError = false
        var lastSavedOption: String? = null
        var lastSavedRevision: Int = 0

        var listExamsDeferred: CompletableDeferred<List<ExamSummary>>? = null
        var startSessionDeferred: CompletableDeferred<ExamSession>? = null
        var getSessionDeferred: CompletableDeferred<ExamSession>? = null
        var saveAnswerDeferred: CompletableDeferred<ExamSession>? = null
        var submitSessionDeferred: CompletableDeferred<ExamSession>? = null

        val sampleExams = listOf(
            ExamSummary(
                id = "exam_phy10_mid",
                title = "Đề thi Giữa kỳ 1 Vật lí 10",
                subjectId = "physics",
                grade = 10,
                examType = "MIDTERM",
                semester = 1,
                durationMinutes = 45,
                questionCount = 2,
                totalPoints = 20,
                status = "PUBLISHED",
                version = 1
            ),
            ExamSummary(
                id = "exam_phy10_fin",
                title = "Đề thi Cuối kỳ 1 Vật lí 10",
                subjectId = "physics",
                grade = 10,
                examType = "FINAL",
                semester = 1,
                durationMinutes = 60,
                questionCount = 3,
                totalPoints = 30,
                status = "PUBLISHED",
                version = 1
            )
        )

        val questions = listOf(
            ExamQuestionSnapshot(
                id = "Q1",
                code = "PHYS-10-01",
                prompt = "Vận tốc trong chuyển động thẳng đều là gì?",
                optionA = "v = s/t",
                optionB = "v = a*t",
                optionC = "v = s*t",
                optionD = "v = 1/2 a*t2",
                points = 10
            ),
            ExamQuestionSnapshot(
                id = "Q2",
                code = "PHYS-10-02",
                prompt = "Gia tốc trong chuyển động thẳng biến đổi đều là gì?",
                optionA = "a = (v - v0)/t",
                optionB = "a = v*t",
                optionC = "a = s/t",
                optionD = "a = 0",
                points = 10
            )
        )

        var activeSession = ExamSession(
            id = "sess_001",
            examId = "exam_phy10_mid",
            examTitle = "Đề thi Giữa kỳ 1 Vật lí 10",
            subjectId = "physics",
            grade = 10,
            examType = "MIDTERM",
            semester = 1,
            durationMinutes = 45,
            state = "IN_PROGRESS",
            startedAt = 1700000000L,
            deadlineAt = 1700000000L + 45 * 60L,
            revision = 1,
            questions = questions,
            answers = emptyMap()
        )

        override suspend fun listExams(
            subjectId: String?,
            grade: Int?,
            examType: String?,
            semester: Int?
        ): List<ExamSummary> {
            listExamsCallCount++
            listExamsDeferred?.let { return it.await() }
            return sampleExams.filter {
                (subjectId == null || it.subjectId == subjectId) &&
                (grade == null || it.grade == grade) &&
                (examType == null || it.examType == examType) &&
                (semester == null || it.semester == semester)
            }
        }

        override suspend fun getExam(examId: String): ExamSummary {
            return sampleExams.first { it.id == examId }
        }

        override suspend fun listUserSessions(subjectId: String?, grade: Int?): List<ExamSession> {
            return listOf(activeSession)
        }

        override suspend fun startSession(examId: String): ExamSession {
            startSessionCallCount++
            startSessionDeferred?.let { return it.await() }
            failStartWith?.let { throw it }
            return activeSession
        }

        override suspend fun getSession(sessionId: String): ExamSession {
            getSessionCallCount++
            getSessionDeferred?.let { return it.await() }
            return activeSession
        }

        override suspend fun saveAnswer(
            sessionId: String,
            questionId: String,
            selectedOption: String?,
            revision: Int
        ): ExamSession {
            saveAnswerCallCount++
            saveAnswerDeferred?.let { return it.await() }
            failSaveWith?.let { throw it }
            lastSavedOption = selectedOption
            lastSavedRevision = revision

            val updatedAnswers = activeSession.answers.toMutableMap()
            val updatedQRevs = activeSession.questionRevisions.toMutableMap()
            updatedQRevs[questionId] = revision
            if (selectedOption != null) {
                updatedAnswers[questionId] = selectedOption
            } else {
                updatedAnswers.remove(questionId)
            }

            activeSession = activeSession.copy(
                answers = updatedAnswers,
                questionRevisions = updatedQRevs,
                revision = activeSession.revision + 1
            )
            return activeSession
        }

        override suspend fun submitSession(sessionId: String): ExamSession {
            submitSessionCallCount++
            submitSessionDeferred?.let { return it.await() }
            if (submitCommittedOnServerBeforeNetworkError) {
                activeSession = activeSession.copy(
                    state = "SUBMITTED",
                    rawScore = 20,
                    score10 = 10.0,
                    correctCount = 2,
                    totalCount = 2
                )
                failSubmitWith?.let { throw it }
            } else {
                failSubmitWith?.let { throw it }
            }

            val q1Correct = activeSession.answers["Q1"] == "A"
            val q2Correct = activeSession.answers["Q2"] == "A"
            var raw = 0
            var correct = 0
            if (q1Correct) { raw += 10; correct += 1 }
            if (q2Correct) { raw += 10; correct += 1 }

            activeSession = activeSession.copy(
                state = "SUBMITTED",
                rawScore = raw,
                score10 = (raw.toDouble() / 20.0) * 10.0,
                correctCount = correct,
                totalCount = 2
            )
            return activeSession
        }
    }

    private lateinit var fakeRepo: FakeExamRepository
    private var currentVm: ExamPrepViewModel? = null
    private val baseEpochSeconds = 1700000000L

    @Before
    fun setUp() {
        fakeRepo = FakeExamRepository()
        currentVm = null
    }

    @After
    fun tearDown() {
        currentVm?.clear()
        currentVm = null
    }

    @Test
    fun testInitialStateAndLoadExams() = runTest {
        val vm = ExamPrepViewModel(
            fakeRepo,
            this,
            clockProvider = { baseEpochSeconds + (testScheduler.currentTime / 1000L) }
        )
        currentVm = vm

        vm.init(initialSubjectId = "physics", initialGrade = 10)
        runCurrent()

        val state = vm.uiState.value
        assertEquals(ExamScreenState.LIST, state.screenState)
        assertEquals("physics", state.selectedSubjectId)
        assertEquals(10, state.selectedGrade)
        assertEquals(2, state.exams.size)
        // Check active session detection
        assertEquals(1, state.activeSessionsByExamId.size)
        assertTrue(state.activeSessionsByExamId.containsKey("exam_phy10_mid"))
    }

    @Test
    fun testSelectExamNavigatesToDetail() = runTest {
        val vm = ExamPrepViewModel(
            fakeRepo,
            this,
            clockProvider = { baseEpochSeconds + (testScheduler.currentTime / 1000L) }
        )
        currentVm = vm

        vm.init(initialSubjectId = "physics", initialGrade = 10)
        runCurrent()

        val exam = vm.uiState.value.exams.first()
        vm.selectExam(exam)

        val state = vm.uiState.value
        assertEquals(ExamScreenState.DETAIL, state.screenState)
        assertEquals(exam.id, state.selectedExam?.id)
        assertNull(state.startSessionError)

        // Back to list
        vm.backToList()
        assertEquals(ExamScreenState.LIST, vm.uiState.value.screenState)
        assertNull(vm.uiState.value.selectedExam)
    }

    @Test
    fun testStartExamTransitionsToInProgressWithAuthoritativeTimer() = runTest {
        val vm = ExamPrepViewModel(
            fakeRepo,
            this,
            clockProvider = { baseEpochSeconds + (testScheduler.currentTime / 1000L) }
        )
        currentVm = vm

        vm.init(initialSubjectId = "physics", initialGrade = 10)
        runCurrent()

        val exam = vm.uiState.value.exams.first()
        vm.selectExam(exam)
        vm.startOrResumeExam()
        runCurrent()

        val state = vm.uiState.value
        assertEquals(ExamScreenState.IN_PROGRESS, state.screenState)
        assertNotNull(state.currentSession)
        assertEquals(2, state.currentSession?.questions?.size)
        assertEquals(45 * 60L, state.remainingSeconds) // 2700 seconds
        assertFalse(state.isTimerCritical)
        assertEquals(2, state.unansweredCount)

        // Advance simulated virtual time by 2500 seconds (remaining = 200s < 300s -> critical!)
        advanceTimeBy(2500_000L)
        runCurrent()

        val updated = vm.uiState.value
        assertEquals(200L, updated.remainingSeconds)
        assertTrue(updated.isTimerCritical)

        vm.clear()
    }

    @Test
    fun testAnswerSelectionToggleAndRevisionProtection() = runTest {
        val vm = ExamPrepViewModel(
            fakeRepo,
            this,
            clockProvider = { baseEpochSeconds + (testScheduler.currentTime / 1000L) }
        )
        currentVm = vm

        vm.init(initialSubjectId = "physics", initialGrade = 10)
        runCurrent()

        vm.selectExam(vm.uiState.value.exams.first())
        vm.startOrResumeExam()
        runCurrent()

        // 1. Select answer 'A' for Q1
        vm.selectAnswer("Q1", "A")
        assertEquals("A", vm.uiState.value.localAnswers["Q1"])
        assertEquals(1, vm.uiState.value.unansweredCount)

        runCurrent()
        assertEquals("A", fakeRepo.lastSavedOption)
        assertEquals(1, fakeRepo.lastSavedRevision)

        // 2. Select answer 'B' for Q1 (replaces 'A')
        vm.selectAnswer("Q1", "B")
        assertEquals("B", vm.uiState.value.localAnswers["Q1"])

        runCurrent()
        assertEquals("B", fakeRepo.lastSavedOption)
        assertEquals(2, fakeRepo.lastSavedRevision)

        // 3. Toggle: click 'B' again -> unselects (clears answer)
        vm.selectAnswer("Q1", "B")
        assertNull(vm.uiState.value.localAnswers["Q1"])
        assertEquals(2, vm.uiState.value.unansweredCount)

        runCurrent()
        assertNull(fakeRepo.lastSavedOption)
        assertEquals(3, fakeRepo.lastSavedRevision)

        vm.clear()
    }

    @Test
    fun testSubmitConfirmationAndFinalGradingResult() = runTest {
        val vm = ExamPrepViewModel(
            fakeRepo,
            this,
            clockProvider = { baseEpochSeconds + (testScheduler.currentTime / 1000L) }
        )
        currentVm = vm

        vm.init(initialSubjectId = "physics", initialGrade = 10)
        runCurrent()

        vm.selectExam(vm.uiState.value.exams.first())
        vm.startOrResumeExam()
        runCurrent()

        // Answer Q1 = A (Correct)
        vm.selectAnswer("Q1", "A")
        runCurrent()
        // Leave Q2 unanswered

        // Request submit -> shows confirmation dialog with unanswered warning
        vm.requestSubmitConfirmation()
        assertTrue(vm.uiState.value.showSubmitConfirmDialog)
        assertEquals(1, vm.uiState.value.unansweredCount)

        // Confirm submit (cancels timer automatically)
        vm.submitSession()
        runCurrent()

        val state = vm.uiState.value
        assertEquals(ExamScreenState.RESULT, state.screenState)
        assertFalse(state.showSubmitConfirmDialog)
        assertNotNull(state.completedSession)

        val completed = state.completedSession!!
        assertEquals("SUBMITTED", completed.state)
        assertEquals(10, completed.rawScore)
        assertEquals(5.0, completed.score10, 0.01) // 10/20 * 10 = 5.0
        assertEquals(1, completed.correctCount)

        // Review navigation
        vm.setReviewQuestionIndex(1)
        assertEquals(1, vm.uiState.value.reviewQuestionIndex)

        // Finish review returns to list
        vm.finishReview()
        runCurrent()
        assertEquals(ExamScreenState.LIST, vm.uiState.value.screenState)
        assertNull(vm.uiState.value.completedSession)
    }

    @Test
    fun testAutoSubmitWhenCountdownExpires() = runTest {
        val vm = ExamPrepViewModel(
            fakeRepo,
            this,
            clockProvider = { baseEpochSeconds + (testScheduler.currentTime / 1000L) }
        )
        currentVm = vm

        vm.init(initialSubjectId = "physics", initialGrade = 10)
        runCurrent()

        vm.selectExam(vm.uiState.value.exams.first())
        vm.startOrResumeExam()
        runCurrent()

        assertEquals(ExamScreenState.IN_PROGRESS, vm.uiState.value.screenState)

        // Advance virtual time past deadline (45 mins = 2700s)
        advanceTimeBy(2701_000L)
        runCurrent()

        assertEquals(ExamScreenState.RESULT, vm.uiState.value.screenState)
        assertEquals(1, fakeRepo.submitSessionCallCount)

        vm.clear()
    }

    @Test
    fun testMonotonicClockShieldsFromWallClockAdjustment() = runTest {
        var fakeWallClock = baseEpochSeconds
        val vm = ExamPrepViewModel(
            repository = fakeRepo,
            scope = this,
            monotonicClockProvider = { testScheduler.currentTime / 1000L },
            wallClockProvider = { fakeWallClock }
        )
        currentVm = vm

        vm.init(initialSubjectId = "physics", initialGrade = 10)
        runCurrent()

        vm.selectExam(vm.uiState.value.exams.first())
        vm.startOrResumeExam()
        runCurrent()

        assertEquals(ExamScreenState.IN_PROGRESS, vm.uiState.value.screenState)
        assertEquals(2700L, vm.uiState.value.remainingSeconds)

        // 1. User changes phone wall clock 2 hours forward (fraud attempt)
        fakeWallClock += 7200L
        advanceTimeBy(10_000L) // 10 virtual monotonic seconds pass
        runCurrent()

        // Remaining seconds must ONLY decrease by 10s (2700 - 10 = 2690L), NOT affected by wall clock!
        assertEquals(2690L, vm.uiState.value.remainingSeconds)

        // 2. User changes phone wall clock 3 hours backward
        fakeWallClock -= 10800L
        advanceTimeBy(10_000L) // another 10s pass
        runCurrent()

        assertEquals(2680L, vm.uiState.value.remainingSeconds)
        vm.clear()
    }

    @Test
    fun testAnswerSaveFailureShowsUnsavedStateAndAllowsSafeRetry() = runTest {
        val vm = ExamPrepViewModel(
            fakeRepo,
            this,
            clockProvider = { baseEpochSeconds + (testScheduler.currentTime / 1000L) }
        )
        currentVm = vm

        vm.init(initialSubjectId = "physics", initialGrade = 10)
        runCurrent()

        vm.selectExam(vm.uiState.value.exams.first())
        vm.startOrResumeExam()
        runCurrent()

        // Simulate network failure during answer save
        fakeRepo.failSaveWith = IOException("Connection lost to server")
        vm.selectAnswer("Q1", "B")
        assertEquals("B", vm.uiState.value.localAnswers["Q1"])

        runCurrent()

        // Verification: Save failed -> FAILED_UNSAVED state, unsavedAnswersCount = 1
        val state = vm.uiState.value
        assertEquals(AnswerSaveStatus.FAILED_UNSAVED, state.answerSaveStatuses["Q1"])
        assertEquals(1, state.unsavedAnswersCount)
        assertNotNull(state.answerSaveErrors["Q1"])

        // User changes choice to 'C' and network recovers
        fakeRepo.failSaveWith = null
        vm.selectAnswer("Q1", "C")
        runCurrent()

        val recovered = vm.uiState.value
        assertEquals("C", recovered.localAnswers["Q1"])
        assertEquals("C", recovered.confirmedAnswers["Q1"])
        assertEquals(AnswerSaveStatus.CONFIRMED, recovered.answerSaveStatuses["Q1"])
        assertEquals(0, recovered.unsavedAnswersCount)
        vm.clear()
    }

    @Test
    fun testRetrySaveAnswerUsesCurrentSelectionNotOldFailedChoice() = runTest {
        val vm = ExamPrepViewModel(
            fakeRepo,
            this,
            clockProvider = { baseEpochSeconds + (testScheduler.currentTime / 1000L) }
        )
        currentVm = vm

        vm.init(initialSubjectId = "physics", initialGrade = 10)
        runCurrent()

        vm.selectExam(vm.uiState.value.exams.first())
        vm.startOrResumeExam()
        runCurrent()

        // Save fails with 'A'
        fakeRepo.failSaveWith = IOException("Drop 1")
        vm.selectAnswer("Q1", "A")
        runCurrent()
        assertEquals(AnswerSaveStatus.FAILED_UNSAVED, vm.uiState.value.answerSaveStatuses["Q1"])

        // User changes to 'B' while network is still failing
        vm.selectAnswer("Q1", "B")
        runCurrent()
        assertEquals("B", vm.uiState.value.localAnswers["Q1"])

        // Network recovers, user clicks retry
        fakeRepo.failSaveWith = null
        vm.retrySaveAnswer("Q1")
        runCurrent()

        // MUST save CURRENT choice 'B', not old 'A'!
        assertEquals("B", fakeRepo.lastSavedOption)
        assertEquals("B", vm.uiState.value.confirmedAnswers["Q1"])
        assertEquals(AnswerSaveStatus.CONFIRMED, vm.uiState.value.answerSaveStatuses["Q1"])
        vm.clear()
    }

    @Test
    fun testSubmitFailsNetworkResumesTimerIfTimeRemains() = runTest {
        val vm = ExamPrepViewModel(
            fakeRepo,
            this,
            clockProvider = { baseEpochSeconds + (testScheduler.currentTime / 1000L) }
        )
        currentVm = vm

        vm.init(initialSubjectId = "physics", initialGrade = 10)
        runCurrent()

        vm.selectExam(vm.uiState.value.exams.first())
        vm.startOrResumeExam()
        runCurrent()

        // Submit fails due to socket timeout
        fakeRepo.failSubmitWith = IOException("Socket timeout")
        vm.submitSession()
        runCurrent()

        val state = vm.uiState.value
        assertEquals(ExamScreenState.IN_PROGRESS, state.screenState)
        assertFalse(state.isSubmitting)
        assertNotNull(state.submitError)

        // Timer is restored and ticking because time remains!
        advanceTimeBy(5_000L)
        runCurrent()
        assertEquals(2695L, vm.uiState.value.remainingSeconds)

        // Network recovers -> submit succeeds
        fakeRepo.failSubmitWith = null
        vm.submitSession()
        runCurrent()

        assertEquals(ExamScreenState.RESULT, vm.uiState.value.screenState)
        vm.clear()
    }

    @Test
    fun testSubmitCommittedOnServerButResponseLostRecoversViaReadBack() = runTest {
        val vm = ExamPrepViewModel(
            fakeRepo,
            this,
            clockProvider = { baseEpochSeconds + (testScheduler.currentTime / 1000L) }
        )
        currentVm = vm

        vm.init(initialSubjectId = "physics", initialGrade = 10)
        runCurrent()

        vm.selectExam(vm.uiState.value.exams.first())
        vm.startOrResumeExam()
        runCurrent()

        // Server committed submission, but client threw network error receiving response
        fakeRepo.submitCommittedOnServerBeforeNetworkError = true
        fakeRepo.failSubmitWith = IOException("Connection reset by peer after commit")

        vm.submitSession()
        runCurrent()

        // Read-back recovers finalization and navigates to RESULT
        val state = vm.uiState.value
        assertEquals(ExamScreenState.RESULT, state.screenState)
        assertNotNull(state.completedSession)
        assertEquals("SUBMITTED", state.completedSession?.state)
        assertEquals(20, state.completedSession?.rawScore)
        vm.clear()
    }

    @Test
    fun testReproduction_submitSessionResponseFromSessionADoesNotOverwriteSessionB() = runTest {
        val vm = ExamPrepViewModel(
            fakeRepo,
            this,
            clockProvider = { baseEpochSeconds + (testScheduler.currentTime / 1000L) }
        )
        currentVm = vm

        vm.init(initialSubjectId = "physics", initialGrade = 10)
        runCurrent()

        // 1. Enter Session A
        val sessionA = fakeRepo.activeSession.copy(id = "sess_A", examTitle = "Đề thi A")
        vm.resumeSession(sessionA)
        runCurrent()

        assertEquals("sess_A", vm.uiState.value.currentSession?.id)
        assertEquals(ExamScreenState.IN_PROGRESS, vm.uiState.value.screenState)

        // 2. Prepare deferred response for submitSession("sess_A")
        val deferredA = CompletableDeferred<ExamSession>()
        fakeRepo.submitSessionDeferred = deferredA

        // 3. Trigger submit for Session A (it suspends waiting for deferredA)
        vm.submitSession()
        runCurrent()
        assertTrue(vm.uiState.value.isSubmitting)

        // 4. In the meantime, user switches to Session B (which is also IN_PROGRESS)
        val sessionB = fakeRepo.activeSession.copy(
            id = "sess_B",
            examTitle = "Đề thi B",
            answers = mapOf("Q1" to "D"),
            questionRevisions = mapOf("Q1" to 2)
        )
        vm.resumeSession(sessionB)
        runCurrent()

        assertEquals("sess_B", vm.uiState.value.currentSession?.id)
        assertEquals(ExamScreenState.IN_PROGRESS, vm.uiState.value.screenState)
        assertEquals("D", vm.uiState.value.localAnswers["Q1"])
        assertFalse(vm.uiState.value.isSubmitting)

        // 5. Complete Session A's submit response
        val completedA = sessionA.copy(state = "SUBMITTED", rawScore = 10)
        fakeRepo.submitSessionDeferred = null
        deferredA.complete(completedA)
        runCurrent()

        // 6. ASSERTIONS: Session B must NOT be overwritten!
        // In BUGGY code: condition `|| screenState == IN_PROGRESS` caused Session A to overwrite Session B
        assertEquals("sess_B", vm.uiState.value.currentSession?.id)
        assertEquals(ExamScreenState.IN_PROGRESS, vm.uiState.value.screenState)
        assertNull(vm.uiState.value.completedSession)
        assertEquals("D", vm.uiState.value.localAnswers["Q1"])
        assertEquals("D", vm.uiState.value.confirmedAnswers["Q1"])
        assertFalse(vm.uiState.value.isSubmitting)

        vm.clear()
    }

    @Test
    fun testReproduction_clearCancelsPendingJobsAndInvalidatesCallbacks() = runTest {
        val vm = ExamPrepViewModel(
            fakeRepo,
            this,
            clockProvider = { baseEpochSeconds + (testScheduler.currentTime / 1000L) }
        )
        currentVm = vm

        // 1. Prepare deferred response for loadExams
        val deferredList = CompletableDeferred<List<ExamSummary>>()
        fakeRepo.listExamsDeferred = deferredList

        // 2. Start loadExams (suspends on deferredList)
        vm.loadExams()
        runCurrent()
        assertTrue(vm.uiState.value.isLoadingExams)

        // 3. Call clear() on the ViewModel (lifecycle ends or reset)
        vm.clear()
        runCurrent()

        // 4. Complete response for loadExams after clear
        fakeRepo.listExamsDeferred = null
        deferredList.complete(fakeRepo.sampleExams)
        runCurrent()

        // 5. ASSERTION: A cleared ViewModel must NOT accept stale completions!
        // In BUGGY code: loadExams still updated exams and isLoadingExams!
        // Moreover, caller scope must remain active!
        assertTrue(this.isActive)
        assertFalse(vm.uiState.value.isLoadingExams)
        assertTrue("Cleared ViewModel must not receive late exams result", vm.uiState.value.exams.isEmpty())
    }
}
