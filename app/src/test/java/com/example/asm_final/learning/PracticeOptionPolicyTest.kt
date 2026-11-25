package com.example.asm_final.learning

import org.junit.Assert.assertEquals
import org.junit.Test

class PracticeOptionPolicyTest {
    @Test fun threeChoiceLessonDoesNotOfferEmptyD() {
        val question = PracticeQuestion("q", "", "", "", "Question", "One", "Two", "Three", "")
        assertEquals(listOf("A", "B", "C"), visiblePracticeOptions(question).map { it.first })
    }

    @Test fun fourChoiceQuestionKeepsD() {
        val question = PracticeQuestion("q", "", "", "", "Question", "One", "Two", "Three", "Four")
        assertEquals(listOf("A", "B", "C", "D"), visiblePracticeOptions(question).map { it.first })
    }
}
