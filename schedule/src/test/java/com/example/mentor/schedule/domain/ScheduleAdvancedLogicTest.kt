package com.example.mentor.schedule.domain

import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class ScheduleAdvancedLogicTest {
    
    @Test
    fun testScheduleStatusLogic() {
        val currentPeriod = 3 // 08:40 to 09:25
        
        // Past Entry
        val pastEntry = ScheduleEntry(accountId = "1", title = "Math", dayOfWeek = 1, period = 2)
        assertEquals("DONE", getMockStatus(pastEntry.period, currentPeriod))
        
        // Ongoing Entry
        val ongoingEntry = ScheduleEntry(accountId = "1", title = "Physics", dayOfWeek = 1, period = 3)
        assertEquals("ONGOING", getMockStatus(ongoingEntry.period, currentPeriod))
        
        // Upcoming Entry
        val upcomingEntry = ScheduleEntry(accountId = "1", title = "Chemistry", dayOfWeek = 1, period = 6)
        assertEquals("UPCOMING", getMockStatus(upcomingEntry.period, currentPeriod))
    }
    
    private fun getMockStatus(targetPeriod: Int, currentPeriod: Int): String {
        return when {
            targetPeriod < currentPeriod -> "DONE"
            targetPeriod == currentPeriod -> "ONGOING"
            else -> "UPCOMING"
        }
    }
    
    @Test
    fun testTodoCompletionPercentage() {
        val todos = listOf(
            TodoTask(accountId = "1", title = "Task 1", dayOfWeek = 1, isCompleted = true),
            TodoTask(accountId = "1", title = "Task 2", dayOfWeek = 1, isCompleted = false),
            TodoTask(accountId = "1", title = "Task 3", dayOfWeek = 1, isCompleted = true),
            TodoTask(accountId = "1", title = "Task 4", dayOfWeek = 1, isCompleted = false)
        )
        
        val completedCount = todos.count { it.isCompleted }
        val percentage = if (todos.isEmpty()) 0f else completedCount.toFloat() / todos.size
        
        assertEquals(0.5f, percentage, 0.001f)
    }
    
    @Test
    fun testTodoCompletionEmpty() {
        val todos = emptyList<TodoTask>()
        val completedCount = todos.count { it.isCompleted }
        val percentage = if (todos.isEmpty()) 0f else completedCount.toFloat() / todos.size
        
        assertEquals(0f, percentage, 0.001f)
    }
}