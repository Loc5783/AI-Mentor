package com.example.asm_final.chat

import org.junit.Assert.*
import org.junit.Test

class ScheduleProposalTest {
    private fun block(rows: String) = "Draft\n```ai_schedule\n{\"entries\":[$rows]}\n```"
    @Test fun validExtractionAndDisplay() {
        val text = block("""{"day":1,"period":6,"subject":"Hóa học"}""")
        assertEquals(ScheduleProposal.Slot(1,6,"Hóa học"), ScheduleProposal.parse(text)!!.single())
        assertEquals("Draft", ScheduleProposal.displayText(text))
    }
    @Test fun rejectsInvalidDaysPeriodsAndTypes() {
        for (row in listOf("""{"day":0,"period":1,"subject":"A"}""", """{"day":1,"period":11,"subject":"A"}""", """{"day":1.5,"period":1,"subject":"A"}""", """{"day":"1","period":1,"subject":"A"}""", """{"day":1,"period":1,"subject":" "}""")) assertNull(ScheduleProposal.parse(block(row)))
    }
    @Test fun rejectsDuplicateSlotsAndMultipleBlocks() {
        val row = """{"day":2,"period":1,"subject":"Lý"}"""
        assertNull(ScheduleProposal.parse(block("$row,$row")))
        assertNull(ScheduleProposal.parse(block(row)+block(row)))
    }
    @Test fun ignoresOrdinaryTextAndMalformedOutput() {
        assertNull(ScheduleProposal.parse("Học Toán thứ hai"))
        assertNull(ScheduleProposal.parse("```ai_schedule\nnot json```"))
        assertNull(ScheduleProposal.parse(block("")))
    }
}

