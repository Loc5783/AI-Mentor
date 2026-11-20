package com.example.mentor.schedule.domain

interface ScheduleRepository {
    fun getEntries(accountId: String, dayOfWeek: Int): List<ScheduleEntry>
    fun insertEntry(entry: ScheduleEntry): Long
    fun updateEntry(entry: ScheduleEntry)
    fun saveEntries(entries: List<ScheduleEntry>, isEdit: Boolean) {
        require(entries.isNotEmpty())
        if (isEdit) {
            require(entries.size == 1)
            updateEntry(entries.single())
        } else {
            entries.forEach(::insertEntry)
        }
    }
    fun deleteEntry(id: Long, accountId: String)

    fun getTodos(accountId: String, dayOfWeek: Int): List<TodoTask>
    fun getAllTodos(accountId: String): List<TodoTask> = (1..7).flatMap { getTodos(accountId, it) }
    fun insertTodo(task: TodoTask): Long
    fun updateTodo(task: TodoTask)
    fun deleteTodo(id: Long, accountId: String)
    fun updateTodoStatus(id: Long, accountId: String, isCompleted: Boolean, completedAt: Long?)

    fun getLogs(accountId: String, limit: Int = 50): List<ScheduleActivityLog>
    fun insertLog(log: ScheduleActivityLog)
    fun clearLogs(accountId: String)
}

class DuplicateScheduleEntryException(
    val dayOfWeek: Int,
    val period: Int,
    val existingEntries: List<ScheduleEntry>
) : IllegalStateException("Period $period on day $dayOfWeek already has a scheduled class")