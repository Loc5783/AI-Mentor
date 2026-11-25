package com.example.asm_final.learning

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LibraryHistoryControllerTest {
    private class HistoryRepository : LibraryRepository {
        override val endpoint = "test://isolated"
        override val ownerId = 7
        var summaries = emptyList<SessionSummary>()
        var detail: PracticeSession? = null

        override suspend fun listHistorySessions(subjectWireId: String, grade: Int) =
            summaries.filter { it.subjectId == subjectWireId && it.grade == grade }
        override suspend fun loadQuizzes(subjectWireId: String, grade: Int) = emptyList<QuizSummary>()
        override suspend fun listActiveSessions(subjectWireId: String, grade: Int) = emptyList<SessionSummary>()
        override suspend fun getSession(sessionId: String) = requireNotNull(detail).also {
            require(it.id == sessionId)
        }
        override suspend fun startSession(quizId: String): PracticeSession = error("unused")
        override suspend fun submitAnswer(sessionId: String, questionId: String,
            selectedOption: String): PracticeSession = error("unused")
        override suspend fun completeSession(sessionId: String): PracticeSession = error("unused")
        override suspend fun loadWrongQuestions(subjectWireId: String?, grade: Int?,
            state: String?) = emptyList<WrongQuestionEntry>()
        override suspend fun retryWrongQuestion(entryId: String,
            selectedOption: String): WrongQuestionRetryResult = error("unused")
    }

    @Test
    fun completedWorkCanBeReviewedButNeverResumedAsUnfinished() = runBlocking {
        val repo = HistoryRepository()
        val active = SessionSummary("active", "quiz", "math", 10, "IN_PROGRESS",
            0, 0, 2, 1, 1, "2026-11-23")
        val done = active.copy(id = "done", state = "COMPLETED", answeredCount = 2)
        repo.summaries = listOf(active, done, active.copy(id = "other-grade", grade = 11))
        repo.detail = PracticeSession("done", "quiz", "math", 10, "COMPLETED",
            20, 20, 2, 2, emptyList())
        val controller = LibraryPracticeController(repo, this)

        controller.loadHistory("math", 10).join()
        assertEquals(listOf(active, done), controller.selectionState.value.historySessions)
        assertNull(controller.reviewHistorySession(active, "Bài dở"))
        assertNull(controller.sessionState.value)
        controller.reviewHistorySession(done, "Bài đã nộp")?.join()
        assertEquals("COMPLETED", controller.sessionState.value?.session?.state)
        assertEquals("Bài đã nộp", controller.sessionState.value?.quizTitle)
    }
}
