package com.example.asm_final.learning

import org.junit.Assert.*
import org.junit.Test

class AdaptivePracticePlannerTest {
    private val blueprint = PracticeBlueprint("math", 10, "skills-v1", listOf(
        PracticeSlot("linear", ExamQuestionKind.SINGLE_CHOICE, "MEDIUM")))
    private fun candidate(id: String) = PracticeCandidate(id, "math", 10, "linear", "skills-v1",
        ExamQuestionKind.SINGLE_CHOICE, "MEDIUM", published = true, reviewed = true)
    private fun evidence(id: String, source: LearningEvidence.Source = LearningEvidence.Source.LIBRARY) =
        LearningEvidence(7, source, "session-1", id, "math", 10,
            LearningEvidence.Observation.ANSWERED_QUESTION, false)

    @Test fun combinesActualLibraryHistoryWithReasoningAndSelection() {
        val question = PracticeQuestion("old", "", "", "", "P", "A", "B", "C", "D",
            userAnswer = "A", isCorrect = false)
        val session = PracticeSession("s1", "quiz", "math", 10, "COMPLETED", 0, 10, 0, 1,
            listOf(question))
        val history = LearningEvidenceReader.fromPractice(7, session)
        val observation = ReasoningObservation(history.single(), "linear", AnswerConfidence.HIGH,
            skillMapVersion = "skills-v1")
        val plan = AdaptivePracticePlanner.planFromLoadedLibraryHistory(7, 7, blueprint,
            listOf(candidate("old"), candidate("new")), history, listOf(observation))
        assertEquals(listOf("new"), plan.selection.questionIds)
        assertEquals(setOf("old"), plan.excludedLoadedLibraryQuestionIds)
        assertEquals(ReasoningNextStep.CHECK_POSSIBLE_MISCONCEPTION,
            plan.reviewBySkill.getValue("linear").nextStep)
    }

    @Test fun identicalIdsFromOtherSourcesGradesAndSubjectsDoNotHideLibraryQuestions() {
        val history = listOf(evidence("q1", LearningEvidence.Source.AI_PRACTICE),
            evidence("q1", LearningEvidence.Source.EXAM), evidence("q1").copy(grade = 11),
            evidence("q1").copy(subjectId = "physics"))
        val plan = AdaptivePracticePlanner.planFromLoadedLibraryHistory(7, 7, blueprint,
            listOf(candidate("q1")), history, emptyList())
        assertEquals(listOf("q1"), plan.selection.questionIds)
        assertTrue(plan.excludedLoadedLibraryQuestionIds.isEmpty())
    }

    @Test fun seenQuestionStaysExcludedEvenIfItHasNoSkillMapping() {
        val plan = AdaptivePracticePlanner.planFromLoadedLibraryHistory(7, 7, blueprint,
            listOf(candidate("q1")), listOf(evidence("q1")), emptyList())
        assertTrue(plan.selection.questionIds.isEmpty())
        assertEquals(listOf(0), plan.selection.unfilledSlots)
        assertEquals(ReasoningNextStep.MAP_QUESTIONS, plan.reviewBySkill.getValue("linear").nextStep)
    }

    @Test fun anotherAccountAndAccountSwitchAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            AdaptivePracticePlanner.planFromLoadedLibraryHistory(7, 8, blueprint,
                emptyList(), emptyList(), emptyList())
        }
        assertThrows(IllegalArgumentException::class.java) {
            AdaptivePracticePlanner.planFromLoadedLibraryHistory(7, 7, blueprint,
                emptyList(), listOf(evidence("q1").copy(ownerId = 8)), emptyList())
        }
    }

    @Test fun fabricatedReasoningResultWithoutMatchingHistoryIsRejected() {
        val observation = ReasoningObservation(evidence("q1"), "linear", AnswerConfidence.HIGH,
            skillMapVersion = "skills-v1")
        assertThrows(IllegalArgumentException::class.java) {
            AdaptivePracticePlanner.planFromLoadedLibraryHistory(7, 7, blueprint,
                listOf(candidate("q2")), emptyList(), listOf(observation))
        }
    }

    @Test fun oldSkillMapCannotBecomeCurrentReasoningEvidence() {
        val history = listOf(evidence("q1"))
        val observation = ReasoningObservation(history.single(), "linear", AnswerConfidence.HIGH,
            skillMapVersion = "skills-v0")
        val plan = AdaptivePracticePlanner.planFromLoadedLibraryHistory(7, 7, blueprint,
            listOf(candidate("q2")), history, listOf(observation))
        assertEquals(0, plan.reviewBySkill.getValue("linear").gradedAnswers)
        assertEquals(ReasoningNextStep.MAP_QUESTIONS, plan.reviewBySkill.getValue("linear").nextStep)
    }

    @Test fun conflictingAnswerCannotBeSelectedAsTheReasoningTruth() {
        val first = evidence("q1")
        val observation = ReasoningObservation(first, "linear", AnswerConfidence.HIGH,
            skillMapVersion = "skills-v1")
        assertThrows(IllegalArgumentException::class.java) {
            AdaptivePracticePlanner.planFromLoadedLibraryHistory(7, 7, blueprint,
                listOf(candidate("q2")), listOf(first, first.copy(correct = true)), listOf(observation))
        }
    }
}
