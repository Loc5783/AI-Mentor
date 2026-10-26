package com.example.mentor.schedule.domain

interface ScheduleRepository {
    fun getEntries(accountId: String, dayOfWeek: Int): List<ScheduleEntry>
    fun insertEntry(entry: ScheduleEntry): Long
    fun updateEntry(entry: ScheduleEntry)
    fun deleteEntry(id: Long, accountId: String)
}