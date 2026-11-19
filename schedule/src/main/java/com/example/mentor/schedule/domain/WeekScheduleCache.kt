package com.example.mentor.schedule.domain

/** Optional online prefetch for repositories that support offline timetable reads. */
interface WeekScheduleCache {
    fun cacheWeek(accountId: String)
}
