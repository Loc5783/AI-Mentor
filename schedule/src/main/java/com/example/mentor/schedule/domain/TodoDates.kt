package com.example.mentor.schedule.domain

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Converts the weekly day selector into the next actual local calendar date. */
object TodoDates {
    private fun formatter() = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply { isLenient = false }

    fun today(now: Long = System.currentTimeMillis()): String = formatter().format(now)

    fun nextOccurrence(dayOfWeek: Int, fromDate: String = today()): String {
        require(dayOfWeek in 1..7)
        val parsed = requireNotNull(runCatching { formatter().parse(fromDate) }.getOrNull())
        require(formatter().format(parsed) == fromDate)
        val calendar = Calendar.getInstance().apply { time = parsed }
        val current = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7 + 1
        calendar.add(Calendar.DAY_OF_YEAR, (dayOfWeek - current + 7) % 7)
        return formatter().format(calendar.time)
    }
}
