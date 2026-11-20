package com.example.asm_final.chat

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class ExamPlanProposalTest {
    private val today = LocalDate.parse("2026-11-19")
    private val exam = LocalDate.parse("2026-11-25")
    private fun block(date: String, subject: String = "Toán", minutes: Int = 30) =
        "```ai_exam_plan\n{\"tasks\":[{\"dueDate\":\"$date\",\"startTime\":\"19:00\",\"title\":\"Ôn Hàm số\",\"subject\":\"$subject\",\"minutes\":$minutes,\"priority\":1}]}\n```"

    @Test fun acceptsOnlySelectedSubjectAndDateBeforeExam() {
        assertEquals("2026-11-20", ExamPlanProposal.parse(block("2026-11-20"), today, exam, setOf("Toán"), 60)?.single()?.dueDate)
        assertNull(ExamPlanProposal.parse(block("2026-11-25"), today, exam, setOf("Toán"), 60))
        assertNull(ExamPlanProposal.parse(block("2026-11-20", "Vật lý"), today, exam, setOf("Toán"), 60))
        assertNull(ExamPlanProposal.parse(block("2026-11-20", minutes = 90), today, exam, setOf("Toán"), 60))
    }
}
