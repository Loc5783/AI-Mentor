package com.example.mentor.schedule.ui

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.mentor.schedule.domain.ScheduleActivityLog
import com.example.mentor.schedule.domain.ScheduleEntry
import com.example.mentor.schedule.domain.ScheduleRepository
import com.example.mentor.schedule.domain.TodoTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

import android.content.Context
import androidx.lifecycle.AndroidViewModel
import android.app.Application

class ScheduleViewModelFactory(
    private val app: Application,
    private val accountId: String,
    private val repository: ScheduleRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScheduleViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ScheduleViewModel(app, accountId, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class ScheduleViewModel(
    app: Application,
    private val accountId: String,
    private val repository: ScheduleRepository
) : AndroidViewModel(app) {

    val error = mutableStateOf(false)
    private val failures = kotlinx.coroutines.CoroutineExceptionHandler { _, _ ->
        viewModelScope.launch(Dispatchers.Main) { error.value = true }
    }
    val entries = mutableStateListOf<ScheduleEntry>()
    val todos = mutableStateListOf<TodoTask>()
    val logs = mutableStateListOf<ScheduleActivityLog>()
    val selectedDay = mutableStateOf(getCurrentDayOfWeek())
    
    val todoProgress = mutableStateOf(0f)
    
    private var lastDeletedEntry: ScheduleEntry? = null
    private var lastDeletedTodo: TodoTask? = null

    init {
        loadData()
    }

    private fun getCurrentDayOfWeek(): Int {
        val cal = Calendar.getInstance()
        var day = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0 (Sun) to 6 (Sat)
        if (day == 0) day = 7 // Map Sun to 7, Mon to 1
        return day
    }

    fun loadData() {
        viewModelScope.launch(Dispatchers.IO + failures) {
            val entryList = repository.getEntries(accountId, selectedDay.value)
            val todoList = repository.getTodos(accountId, selectedDay.value)
            val logList = repository.getLogs(accountId, 20)
            
            withContext(Dispatchers.Main) {
                error.value = false
                entries.clear()
                entries.addAll(entryList)
                
                todos.clear()
                todos.addAll(todoList)
                
                logs.clear()
                logs.addAll(logList)
                
                calculateTodoProgress()
            }
        }
    }

    private fun calculateTodoProgress() {
        if (todos.isEmpty()) {
            todoProgress.value = 0f
        } else {
            val completedCount = todos.count { it.isCompleted }
            todoProgress.value = completedCount.toFloat() / todos.size
        }
    }

    fun selectDay(day: Int) {
        selectedDay.value = day
        loadData()
    }

    fun saveEntries(entriesToSave: List<ScheduleEntry>, isEdit: Boolean) {
        viewModelScope.launch(Dispatchers.IO + failures) {
            val titles = entriesToSave.joinToString { it.title }
            entriesToSave.forEach { entry ->
                if (isEdit) {
                    repository.updateEntry(entry)
                } else {
                    repository.insertEntry(entry.copy(accountId = accountId))
                }
            }
            
            logAction(
                actionType = if (isEdit) "UPDATE" else "CREATE",
                entityType = "SCHEDULE",
                description = if (isEdit) "Đã cập nhật môn: $titles" else "Đã thêm môn: $titles"
            )
            
            // Auto-reschedule notifications if changed
            try {
                val helperClass = Class.forName("com.example.asm_final.utils.ReminderHelper")
                val method = helperClass.getMethod("reschedule", android.content.Context::class.java)
                method.invoke(null, getApplication<Application>())
            } catch (e: Exception) {}
            
            loadData()
        }
    }

    fun deleteEntry(entry: ScheduleEntry) {
        viewModelScope.launch(Dispatchers.IO + failures) {
            repository.deleteEntry(entry.id, accountId)
            lastDeletedEntry = entry
            logAction("DELETE", "SCHEDULE", "Đã xóa môn: ${entry.title}")
            
            try {
                val helperClass = Class.forName("com.example.asm_final.utils.ReminderHelper")
                val method = helperClass.getMethod("reschedule", android.content.Context::class.java)
                method.invoke(null, getApplication<Application>())
            } catch (e: Exception) {}
            
            loadData()
        }
    }

    fun undoDeleteEntry() {
        lastDeletedEntry?.let { entry ->
            viewModelScope.launch(Dispatchers.IO + failures) {
                repository.insertEntry(entry)
                lastDeletedEntry = null
                logAction("CREATE", "SCHEDULE", "Đã khôi phục môn: ${entry.title}")
                
                try {
                    val helperClass = Class.forName("com.example.asm_final.utils.ReminderHelper")
                    val method = helperClass.getMethod("reschedule", android.content.Context::class.java)
                    method.invoke(null, getApplication<Application>())
                } catch (e: Exception) {}
                
                loadData()
            }
        }
    }
    
    // --- TODO LOGIC ---
    
    fun saveTodo(title: String, priority: Int, linkedSubject: String?) {
        viewModelScope.launch(Dispatchers.IO + failures) {
            val task = TodoTask(
                accountId = accountId,
                title = title,
                dayOfWeek = selectedDay.value,
                priority = priority,
                linkedSubject = linkedSubject
            )
            repository.insertTodo(task)
            logAction("CREATE", "TODO", "Đã thêm nhiệm vụ: $title")
            loadData()
        }
    }
    
    fun toggleTodo(todo: TodoTask) {
        viewModelScope.launch(Dispatchers.IO + failures) {
            val newState = !todo.isCompleted
            val completedTime = if (newState) System.currentTimeMillis() else null
            repository.updateTodoStatus(todo.id, accountId, newState, completedTime)
            
            val action = if (newState) "COMPLETE" else "UNCOMPLETE"
            val text = if (newState) "Hoàn thành" else "Đánh dấu chưa xong"
            logAction(action, "TODO", "$text: ${todo.title}")
            
            loadData()
        }
    }
    
    fun deleteTodo(todo: TodoTask) {
        viewModelScope.launch(Dispatchers.IO + failures) {
            repository.deleteTodo(todo.id, accountId)
            lastDeletedTodo = todo
            logAction("DELETE", "TODO", "Đã xóa nhiệm vụ: ${todo.title}")
            loadData()
        }
    }
    
    fun undoDeleteTodo() {
        lastDeletedTodo?.let { todo ->
            viewModelScope.launch(Dispatchers.IO + failures) {
                repository.insertTodo(todo)
                lastDeletedTodo = null
                logAction("CREATE", "TODO", "Đã khôi phục nhiệm vụ: ${todo.title}")
                loadData()
            }
        }
    }
    
    // --- LOG LOGIC ---
    
    private fun logAction(actionType: String, entityType: String, description: String) {
        repository.insertLog(
            ScheduleActivityLog(
                accountId = accountId,
                actionType = actionType,
                entityType = entityType,
                description = description
            )
        )
    }
    
    fun clearLogs() {
        viewModelScope.launch(Dispatchers.IO + failures) {
            repository.clearLogs(accountId)
            loadData()
        }
    }

    suspend fun checkOverlap(day: Int, period: Int, ignoreId: Long = 0L): Boolean {
        return withContext(Dispatchers.IO) {
            val listToCheck = repository.getEntries(accountId, day)
            listToCheck.any { 
                it.id != ignoreId && it.period == period
            }
        }
    }
}
