package com.example.mentor.schedule.domain

import org.junit.Assert.*
import org.junit.Test

class ScheduleLogicTest {
    
    @Test
    fun testTimeFormatting() {
        val entry = ScheduleEntry(accountId = "1", title = "Math", dayOfWeek = 1, period = 1)
        assertEquals("07:00", entry.getFormattedStartTime())
        assertEquals("07:45", entry.getFormattedEndTime())
    }
    
    @Test
    fun testAfternoonTimeFormatting() {
        val entry = ScheduleEntry(accountId = "1", title = "Physics", dayOfWeek = 1, period = 6)
        assertEquals("13:00", entry.getFormattedStartTime())
        assertEquals("13:45", entry.getFormattedEndTime())
    }
}