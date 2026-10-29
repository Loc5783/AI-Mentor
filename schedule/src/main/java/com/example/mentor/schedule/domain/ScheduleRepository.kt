package com.example.mentor.schedule.domain

interface ScheduleRepository {
    fun getEntries(accountId: String, dayOfWeek: Int): List<ScheduleEntry>
    fun insertEntry(entry: ScheduleEntry): Long
    fun updateEntry(entry: ScheduleEntry)
    fun deleteEntry(id: Long, accountId: String)

    fun getTodos(accountId: String, dayOfWeek: Int): List<TodoTask>
    fun insertTodo(task: TodoTask): Long
    fun updateTodo(task: TodoTask)
    fun deleteTodo(id: Long, accountId: String)
    fun updateTodoStatus(id: Long, accountId: String, isCompleted: Boolean, completedAt: Long?)

    fun getLogs(accountId: String, limit: Int = 50): List<ScheduleActivityLog>
    fun insertLog(log: ScheduleActivityLog)
    fun clearLogs(accountId: String)
}