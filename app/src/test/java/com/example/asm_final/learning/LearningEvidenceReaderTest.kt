package com.example.asm_final.learning

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.Executor

class LearningEvidenceReaderTest {
    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Test fun lessonProgressDoesNotInventQuestionScores() {
        val path = LessonPathData(JSONArray("""[
          {"id":"lesson-1","title":"Bài 1","state":"COMPLETED","kind":"LESSON","xp":10},
          {"id":"lesson-2","title":"Bài 2","state":"SKIPPED","kind":"LESSON","xp":0},
          {"id":"lesson-3","title":"Bài 3","state":"AVAILABLE","kind":"LESSON","xp":0}
        ]"""))
        val items = LearningEvidenceReader.fromLessonPath(7, "math", 10, path)
        assertEquals(2, items.size)
        assertEquals(LearningEvidence.Observation.COMPLETED_LESSON, items[0].observation)
        assertEquals(LearningEvidence.Observation.SKIPPED_LESSON, items[1].observation)
        assertTrue(items.all { it.correct == null })
    }

    @Test fun examAnswerRemainsUngradedUntilSubmission() {
        val question = ExamQuestionSnapshot("q1", "", "P", "A", "B", "C", "D", isCorrect = true)
        val pending = ExamSession("s1", "e1", "Exam", "math", 12, "MIDTERM", 1, 45,
            "IN_PROGRESS", questions = listOf(question), answers = mapOf("q1" to "A"))
        assertNull(LearningEvidenceReader.fromExam(7, pending).single().correct)
        assertEquals(true, LearningEvidenceReader.fromExam(7, pending.copy(state = "SUBMITTED")).single().correct)
    }

    @Test fun sameQuestionInDifferentSessionsIsDistinctAndAccountsCannotMix() {
        val question = PracticeQuestion("q1", "", "", "", "P", "A", "B", "C", "D",
            userAnswer = "A", isCorrect = true)
        val session = PracticeSession("s1", "quiz", "math", 10, "COMPLETED", 10, 10, 1, 1, listOf(question))
        val first = LearningEvidenceReader.fromPractice(7, session)
        val second = LearningEvidenceReader.fromPractice(7, session.copy(id = "s2"))
        assertEquals(2, LearningEvidenceReader.combine(7, first, second).size)
        assertEquals(1, LearningEvidenceReader.combine(7, first, first).size)
        assertThrows(IllegalArgumentException::class.java) {
            LearningEvidenceReader.combine(7, first, LearningEvidenceReader.fromPractice(8, session))
        }
    }

    @Test fun detailedExamSessionWinsOverListSummary() {
        val question = ExamQuestionSnapshot("q1", "", "P", "A", "B", "C", "D", isCorrect = true)
        val summary = ExamSession("s1", "e1", "Exam", "math", 12, "MIDTERM", 1, 45, "SUBMITTED")
        val full = summary.copy(questions = listOf(question), answers = mapOf("q1" to "A"))
        val result = LearningEvidenceReader.fromExamSessions(7, listOf(summary, full))
        assertEquals(1, result.size)
        assertEquals(true, result.single().correct)
    }

    @Test fun newerExamRevisionWinsWhenSnapshotsHaveTheSameQuestionCount() {
        val question = ExamQuestionSnapshot("q1", "", "P", "A", "B", "C", "D")
        val old = ExamSession("s1", "e1", "Exam", "math", 12, "MIDTERM", 1, 45,
            "IN_PROGRESS", revision = 1, questions = listOf(question), answers = mapOf("q1" to "A"))
        val newer = old.copy(state = "SUBMITTED", revision = 2,
            questions = listOf(question.copy(isCorrect = false)))
        assertEquals(false, LearningEvidenceReader.fromExamSessions(7, listOf(newer, old)).single().correct)
    }

    @Test fun newerSummaryCannotReviveAnOlderDetailedAnswer() {
        val question = ExamQuestionSnapshot("q1", "", "P", "A", "B", "C", "D", isCorrect = true)
        val old = ExamSession("s1", "e1", "Exam", "math", 12, "MIDTERM", 1, 45,
            "SUBMITTED", revision = 1, questions = listOf(question), answers = mapOf("q1" to "A"))
        val newer = old.copy(revision = 2, questions = emptyList(), answers = emptyMap())
        assertTrue(LearningEvidenceReader.fromExamSessions(7, listOf(old, newer)).isEmpty())
        assertTrue(LearningEvidenceReader.fromExamSessions(7, listOf(newer, old)).isEmpty())
    }

    @Test fun contradictorySameRevisionExamSnapshotsAreNotUsed() {
        val question = ExamQuestionSnapshot("q1", "", "P", "A", "B", "C", "D", isCorrect = true)
        val first = ExamSession("s1", "e1", "Exam", "math", 12, "MIDTERM", 1, 45,
            "SUBMITTED", revision = 2, questions = listOf(question), answers = mapOf("q1" to "A"))
        val second = first.copy(questions = listOf(question.copy(isCorrect = false)))
        assertTrue(LearningEvidenceReader.fromExamSessions(7, listOf(first, second)).isEmpty())
        assertTrue(LearningEvidenceReader.fromExamSessions(7, listOf(second, first)).isEmpty())
    }

    @Test fun identicalExamSnapshotsCountOnce() {
        val question = ExamQuestionSnapshot("q1", "", "P", "A", "B", "C", "D", isCorrect = true)
        val full = ExamSession("s1", "e1", "Exam", "math", 12, "MIDTERM", 1, 45,
            "SUBMITTED", revision = 2, questions = listOf(question), answers = mapOf("q1" to "A"))
        assertEquals(1, LearningEvidenceReader.fromExamSessions(7, listOf(full, full)).size)
    }

    @Test fun pendingEssayIsNotEvidenceOfAnIncorrectAnswer() {
        val question = ExamQuestionSnapshot("q1", "", "P", "", "", "", "", isCorrect = false,
            questionType = "ESSAY")
        val session = ExamSession("s1", "e1", "Exam", "literature", 12, "FINAL", 1, 90,
            "SUBMITTED", questions = listOf(question), answers = mapOf("q1" to "Bài làm"),
            gradingStatus = "PENDING")
        assertNull(LearningEvidenceReader.fromExam(7, session).single().correct)
        val graded = session.copy(questions = listOf(question.copy(essayPoints = 0)))
        assertEquals(false, LearningEvidenceReader.fromExam(7, graded).single().correct)
    }

    @Test fun conflictingCombinedObservationsAreExcludedInEitherOrder() {
        val correct = LearningEvidence(7, LearningEvidence.Source.LIBRARY, "s1", "q1", "math", 10,
            LearningEvidence.Observation.ANSWERED_QUESTION, true)
        val incorrect = correct.copy(correct = false)
        assertTrue(LearningEvidenceReader.combine(7, listOf(correct), listOf(incorrect)).isEmpty())
        assertTrue(LearningEvidenceReader.combine(7, listOf(incorrect), listOf(correct)).isEmpty())
    }

    @Test fun aiPracticeDistinguishesAnsweredSkippedAndUnrevealedQuestions() {
        val questions = (1..3).map { AiQuestion("q$it", "P$it", emptyMap()) }
        val session = AiPracticeSession("ai-1", "physics", "Vật lý", 11, "Lực", "MEDIUM", 3, 2,
            "IN_PROGRESS", questions, mapOf(
                "q1" to AiAnswerRecord("B", false, false, 1, true),
                "q2" to AiAnswerRecord(null, false, false, 0, true, skipped = true),
                "q3" to AiAnswerRecord("A", true, true, 1, false)
            ))
        val result = LearningEvidenceReader.fromAiPractice(7, session)
        assertEquals(2, result.size)
        assertEquals(false, result[0].correct)
        assertEquals(LearningEvidence.Observation.SKIPPED_QUESTION, result[1].observation)
        assertNull(result[1].correct)
    }

    @Test fun placementRequiresCompletedMatchingAttemptAndScoredAnswers() {
        val completion = JSONObject("""{"kind":"PLACEMENT","attemptId":"placement-1"}""")
        val attempt = JSONObject("""{
          "id":"placement-1","kind":"PLACEMENT","state":"COMPLETED","subjectId":"math","grade":10,
          "questions":[{"id":"q1"},{"id":"q2"}],
          "answers":[{"questionId":"q1","selectedOptionId":"A","correct":false},
                     {"questionId":"q2","selectedOptionId":"C"},
                     {"questionId":"unknown","selectedOptionId":"B","correct":true}]
        }""")
        val result = LearningEvidenceReader.fromPlacement(7, completion, attempt)
        assertEquals(1, result.size)
        assertEquals(LearningEvidence.Source.PLACEMENT, result.single().source)
        assertEquals(false, result.single().correct)
        assertTrue(LearningEvidenceReader.fromPlacement(7, completion,
            JSONObject(attempt.toString()).put("state", "IN_PROGRESS")).isEmpty())
        assertTrue(LearningEvidenceReader.fromPlacement(7,
            JSONObject(completion.toString()).put("attemptId", "another"), attempt).isEmpty())
    }

    @Test fun lessonSnapshotIsHiddenAfterAccountSwitch() {
        var activeAccount = 7
        val api = object : LessonViewModel.LessonApiClient {
            override fun configured() = true
            override fun signedIn() = true
            override fun signIn(identifier: String?, password: String?) = Unit
            override fun request(method: String?, path: String?, body: JSONObject?, key: String?,
                revision: Int?): JSONObject = JSONObject()
        }
        val direct = Executor { it.run() }
        val model = LessonViewModel(Application(), SavedStateHandle(), api, direct, direct) { activeAccount }
        model.bind(7, "math", 10)
        model.path = JSONObject("""{
          "enrollment":{"subjectId":"math","grade":10},
          "units":[{"id":"lesson-1","title":"Bài 1","state":"COMPLETED","kind":"LESSON","xp":10}]
        }""")
        assertEquals(1, LearningEvidenceReader.fromLessonViewModel(model).size)
        activeAccount = 8
        assertTrue(LearningEvidenceReader.fromLessonViewModel(model).isEmpty())
    }
}
