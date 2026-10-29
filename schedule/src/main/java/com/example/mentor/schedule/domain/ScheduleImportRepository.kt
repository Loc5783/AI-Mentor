package com.example.mentor.schedule.domain

/** Short-lived import session, acquired by the application composition root. */
interface ScheduleImportRepository : AutoCloseable {
    fun importVacantEntries(accountId: String, entries: List<ScheduleEntry>): Int
    override fun close()
}
