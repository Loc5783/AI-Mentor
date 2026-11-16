package com.example.asm_final.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TutorRequestHistoryTest {
    private fun message(id: String, text: String, role: String, status: String = "SENT") =
        ChatRecords.Message(id, text, role, status, "", false, emptyList())

    @Test
    fun keepsLatestTenCompletedExchangesAndCurrentQuestion() {
        val history = mutableListOf<ChatRecords.Message>()
        repeat(25) { index ->
            history += message("q$index", "Q$index", "USER")
            history += message("a$index", "A$index", "ASSISTANT")
        }
        history += message("current", "Q25", "USER", "PENDING")

        val selected = TutorRequestHistory.select(history, "current")

        assertEquals(21, selected.size)
        assertEquals("Q15", selected.first().text)
        assertEquals("A24", selected[19].text)
        assertEquals("Q25", selected.last().text)
    }

    @Test
    fun excludesFailedQuestionsAndRejectsMissingTarget() {
        val history = listOf(
            message("failed", "Old failure", "USER", "FAILED"),
            message("q1", "Q1", "USER"),
            message("a1", "A1", "ASSISTANT"),
            message("current", "Q2", "USER", "PENDING"),
        )

        assertEquals(
            listOf("Q1", "A1", "Q2"),
            TutorRequestHistory.select(history, "current").map { it.text },
        )
        assertThrows(IllegalArgumentException::class.java) {
            TutorRequestHistory.select(history, "not-found")
        }
    }
}
