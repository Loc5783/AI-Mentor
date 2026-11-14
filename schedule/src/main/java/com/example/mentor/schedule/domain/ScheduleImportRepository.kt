package com.example.mentor.schedule.domain

/** Short-lived import session, acquired by the application composition root. */
interface ScheduleImportRepository : AutoCloseable {
    fun importVacantEntries(accountId: String, entries: List<ScheduleEntry>): Int
    /** Adds reviewed tasks in one transaction, skipping tasks already present for that day. */
    fun importTodos(accountId: String, tasks: List<TodoTask>): Int
    override fun close()
}
