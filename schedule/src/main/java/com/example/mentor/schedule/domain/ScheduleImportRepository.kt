package com.example.mentor.schedule.domain

/** Short-lived import session, acquired by the application composition root. */
interface ScheduleImportRepository : AutoCloseable {
    /** Reads a day's current classes so photo imports can explain or replace existing rows. */
    fun getEntries(accountId: String, dayOfWeek: Int): List<ScheduleEntry>
    fun importVacantEntries(accountId: String, entries: List<ScheduleEntry>): Int
    /** Atomically replaces all schedule entries on the imported days. */
    fun replaceEntriesForImportedDays(accountId: String, entries: List<ScheduleEntry>): ScheduleReplaceResult
    /** Adds reviewed tasks in one transaction, skipping tasks already present for that day. */
    fun importTodos(accountId: String, tasks: List<TodoTask>): Int
    override fun close()
}

data class ScheduleReplaceResult(val removed: Int, val inserted: Int)
