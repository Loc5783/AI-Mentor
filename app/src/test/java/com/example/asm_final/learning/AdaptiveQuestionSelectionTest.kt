package com.example.asm_final.learning

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AdaptiveQuestionSelectionTest {
    private val blueprint = PracticeBlueprint("math", 10, "skills-v1", listOf(
        PracticeSlot("linear-equations", ExamQuestionKind.SINGLE_CHOICE, "MEDIUM"),
        PracticeSlot("linear-equations", ExamQuestionKind.SINGLE_CHOICE, "MEDIUM")
    ))

    private fun candidate(id: String, subject: String = "math", grade: Int = 10,
                          skill: String? = "linear-equations", version: String? = "skills-v1",
                          published: Boolean = true, reviewed: Boolean = true) =
        PracticeCandidate(id, subject, grade, skill, version, ExamQuestionKind.SINGLE_CHOICE,
            "MEDIUM", published, reviewed)

    @Test fun selectsDistinctReviewedUnseenQuestionsInStableOrder() {
        val result = AdaptiveQuestionSelection.select(blueprint,
            listOf(candidate("q3"), candidate("q1"), candidate("q2")), setOf("q2"))
        assertEquals(mapOf(0 to "q1", 1 to "q3"), result.selectedBySlot)
        assertEquals(emptyList<Int>(), result.unfilledSlots)
    }

    @Test fun doesNotBorrowAnotherGradeSubjectOrSkillVersion() {
        val result = AdaptiveQuestionSelection.select(blueprint, listOf(
            candidate("other-grade", grade = 11), candidate("other-subject", subject = "physics"),
            candidate("old-map", version = "skills-v0"), candidate("unmapped", skill = null)
        ), emptySet())
        assertEquals(emptyList<String>(), result.questionIds)
        assertEquals(listOf(0, 1), result.unfilledSlots)
    }

    @Test fun unpublishedAndUnreviewedQuestionsNeverFillARequest() {
        val result = AdaptiveQuestionSelection.select(blueprint,
            listOf(candidate("draft", published = false), candidate("unchecked", reviewed = false)),
            emptySet())
        assertEquals(listOf(0, 1), result.unfilledSlots)
    }

    @Test fun anotherQuestionFormatOrDifficultyCannotBeSubstituted() {
        val wrongFormat = candidate("format").copy(kind = ExamQuestionKind.ESSAY)
        val wrongDifficulty = candidate("difficulty").copy(difficulty = "EASY")
        val result = AdaptiveQuestionSelection.select(blueprint,
            listOf(wrongFormat, wrongDifficulty), emptySet())
        assertEquals(listOf(0, 1), result.unfilledSlots)
    }

    @Test fun missingSlotIsReportedWithoutShiftingTheNextSlot() {
        val varied = blueprint.copy(slots = listOf(
            PracticeSlot("missing", ExamQuestionKind.SHORT_ANSWER, "HARD"),
            PracticeSlot("linear-equations", ExamQuestionKind.SINGLE_CHOICE, "MEDIUM")
        ))
        val result = AdaptiveQuestionSelection.select(varied, listOf(candidate("q1")), emptySet())
        assertEquals(mapOf(1 to "q1"), result.selectedBySlot)
        assertEquals(listOf(0), result.unfilledSlots)
    }

    @Test fun duplicateCatalogIdsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            AdaptiveQuestionSelection.select(blueprint, listOf(candidate("q1"), candidate("q1")),
                emptySet())
        }
    }

    @Test fun emptyBlueprintIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            AdaptiveQuestionSelection.select(blueprint.copy(slots = emptyList()),
                listOf(candidate("q1")), emptySet())
        }
    }
}
