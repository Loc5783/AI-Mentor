package com.example.mentor.schedule.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TodoDatesTest {
    @Test fun selectedPastWeekdayMovesToNextWeek() {
        // 2026-11-16 is Monday. A task for Sunday belongs to 22 November.
        assertEquals("2026-11-22", TodoDates.nextOccurrence(7, "2026-11-16"))
        assertEquals("2026-11-16", TodoDates.nextOccurrence(1, "2026-11-16"))
    }

    @Test fun rejectsInvalidCalendarDate() {
        assertThrows(IllegalArgumentException::class.java) {
            TodoDates.nextOccurrence(1, "2026-02-30")
        }
    }
}
