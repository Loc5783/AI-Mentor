package com.example.mentor.schedule.domain

import org.junit.Assert.*
import org.junit.Test

class ScheduleAiContextTest {
    @Test fun includesOnlySelectedDayAndUnfinishedTasks() {
        val result = ScheduleAiContext.forDay(2,
            listOf(ScheduleEntry(accountId = "1", title = "Physics", dayOfWeek = 2, period = 3),
                ScheduleEntry(accountId = "1", title = "Chemistry", dayOfWeek = 3, period = 1)),
            listOf(TodoTask(accountId = "1", title = "Review force", dayOfWeek = 2),
                TodoTask(accountId = "1", title = "Old task", dayOfWeek = 2, isCompleted = true)))
        assertTrue(result.contains("Physics"))
        assertTrue(result.contains("Review force"))
        assertFalse(result.contains("Chemistry"))
        assertFalse(result.contains("Old task"))
    }
}
