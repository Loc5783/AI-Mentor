package com.example.mentor.schedule.domain

import org.junit.Assert.*
import org.junit.Test

class ScheduleLogicTest {
    
    @Test
    fun testTimeValidation() {
        // Valid
        val entry = ScheduleEntry(accountId = "1", title = "Math", dayOfWeek = 1, startTimeMin = 600, endTimeMin = 660)
        assertTrue(entry.endTimeMin > entry.startTimeMin)
        
        // Formatter test
        assertEquals("10:00", entry.getFormattedStartTime())
        assertEquals("11:00", entry.getFormattedEndTime())
    }
    
    @Test
    fun testOverlapLogic() {
        val entry1 = ScheduleEntry(id = 1, accountId = "1", title = "Math", dayOfWeek = 1, startTimeMin = 600, endTimeMin = 660)
        val entry2 = ScheduleEntry(id = 2, accountId = "1", title = "Science", dayOfWeek = 1, startTimeMin = 630, endTimeMin = 700)
        
        // Check if 630 to 700 overlaps with 600 to 660
        val overlaps = (entry2.startTimeMin < entry1.endTimeMin && entry2.endTimeMin > entry1.startTimeMin)
        assertTrue(overlaps)
    }

    @Test
    fun testNonOverlapLogic() {
        val entry1 = ScheduleEntry(id = 1, accountId = "1", title = "Math", dayOfWeek = 1, startTimeMin = 600, endTimeMin = 660)
        val entry3 = ScheduleEntry(id = 3, accountId = "1", title = "History", dayOfWeek = 1, startTimeMin = 660, endTimeMin = 720)
        
        val overlaps = (entry3.startTimeMin < entry1.endTimeMin && entry3.endTimeMin > entry1.startTimeMin)
        assertFalse(overlaps) // Adjoining times do not overlap
    }
}