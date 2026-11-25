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
import com.example.mentor.schedule.domain.TodoDates
import com.example.mentor.schedule.domain.WeekScheduleCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import java.util.Calendar
import java.util.concurrent.atomic.AtomicInteger

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
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
    val loading = mutableStateOf(true)
    private val failures = kotlinx.coroutines.CoroutineExceptionHandler { _, _ ->
        viewModelScope.launch(Dispatchers.Main) { error.value = true }
    }
    val entries = mutableStateListOf<ScheduleEntry>()
    val todos = mutableStateListOf<TodoTask>()
    val allExamPlanTodos = mutableStateListOf<TodoTask>()
    val logs = mutableStateListOf<ScheduleActivityLog>()
    val selectedDay = mutableStateOf(getCurrentDayOfWeek())
    
    val todoProgress = mutableStateOf(0f)
    
    private var lastDeletedEntry: ScheduleEntry? = null
    private var lastDeletedTodo: TodoTask? = null
    private var firstResumePending = true
    private val loadGeneration = AtomicInteger()
    private var weekCachePrimed = false
    private val connectivity = app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    @Volatile private var wasOnline = isOnline()
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = checkReconnect()
        override fun onLost(network: Network) = checkReconnect()
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) = checkReconnect()
    }

    private fun isOnline(): Boolean = connectivity.getNetworkCapabilities(connectivity.activeNetwork)
        ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true

    private fun checkReconnect() {
        val online = isOnline()
        if (online && !wasOnline) {
            weekCachePrimed = false
            loadData()
        }
        wasOnline = online
    }

    init {
        connectivity.registerDefaultNetworkCallback(networkCallback)
        loadData()
    }

    override fun onCleared() {
        connectivity.unregisterNetworkCallback(networkCallback)
        super.onCleared()
    }

    /** Refresh after returning from the schedule assistant; initial data loads in init. */
    fun refreshOnResume() {
        if (firstResumePending) firstResumePending = false else loadData()
    }

    private fun getCurrentDayOfWeek(): Int {
        val cal = Calendar.getInstance()
        var day = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0 (Sun) to 6 (Sat)
        if (day == 0) day = 7 // Map Sun to 7, Mon to 1
        return day
    }

    fun loadData() {
        val generation = loadGeneration.incrementAndGet()
        val day = selectedDay.value
        viewModelScope.launch(Dispatchers.Main) {
            if (generation == loadGeneration.get()) loading.value = true
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val entryList = repository.getEntries(accountId, day)
                val todoList = repository.getTodos(accountId, day)
                val allTodoList = runCatching { repository.getAllTodos(accountId) }.getOrDefault(emptyList())
                val examPlanList = allTodoList.filter { !it.dueDate.isNullOrBlank() || !it.planStartDate.isNullOrBlank() }
                val logList = runCatching { repository.getLogs(accountId, 20) }.getOrDefault(emptyList())
                withContext(Dispatchers.Main) {
                    if (generation != loadGeneration.get() || selectedDay.value != day) return@withContext
                    error.value = false
                    loading.value = false
                    entries.clear()
                    entries.addAll(entryList)
                    todos.clear()
                    todos.addAll(todoList)
                    allExamPlanTodos.clear()
                    allExamPlanTodos.addAll(examPlanList)
                    logs.clear()
                    logs.addAll(logList)
                    calculateTodoProgress()
                }
                if (!weekCachePrimed && repository is WeekScheduleCache) {
                    weekCachePrimed = runCatching { repository.cacheWeek(accountId) }.isSuccess
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    if (generation == loadGeneration.get() && selectedDay.value == day) {
                        error.value = true
                        loading.value = false
                    }
                }
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
        if (selectedDay.value == day) return
        selectedDay.value = day
        entries.clear()
        todos.clear()
        calculateTodoProgress()
        loading.value = true
        loadData()
    }

    fun saveEntries(
        entriesToSave: List<ScheduleEntry>,
        isEdit: Boolean,
        onComplete: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO + failures) {
            val result = try {
                val entries = entriesToSave.map {
                    if (isEdit) it else it.copy(accountId = accountId)
                }
                repository.saveEntries(entries, isEdit)
                val titles = entries.joinToString { it.title }
                logAction(
                    actionType = if (isEdit) "UPDATE" else "CREATE",
                    entityType = "SCHEDULE",
                    description = if (isEdit) "Đã cập nhật môn: $titles" else "Đã thêm môn: $titles"
                )

                try {
                    val helperClass = Class.forName("com.example.asm_final.utils.ReminderHelper")
                    val method = helperClass.getMethod("reschedule", android.content.Context::class.java)
                    method.invoke(null, getApplication<Application>())
                } catch (e: Exception) {}

                Result.success(Unit)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                Result.failure(failure)
            }
            loadData()
            withContext(Dispatchers.Main) { onComplete(result) }
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
            val planStartDate = TodoDates.today()
            val task = TodoTask(
                accountId = accountId,
                title = title,
                dayOfWeek = selectedDay.value,
                priority = priority,
                linkedSubject = linkedSubject,
                dueDate = TodoDates.nextOccurrence(selectedDay.value, planStartDate),
                planStartDate = planStartDate
            )
            repository.insertTodo(task)
            logAction("CREATE", "TODO", "Đã thêm nhiệm vụ: $title")
            loadData()
        }
    }

    fun updateTodo(task: TodoTask) {
        viewModelScope.launch(Dispatchers.IO + failures) {
            repository.updateTodo(task)
            logAction("UPDATE", "TODO", "Đã cập nhật nhiệm vụ: ${task.title}")
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

    suspend fun findOverlaps(day: Int, period: Int, ignoreId: Long = 0L): List<ScheduleEntry> {
        return withContext(Dispatchers.IO) {
            val listToCheck = repository.getEntries(accountId, day)
            listToCheck.filter {
                it.id != ignoreId && it.period == period
            }
        }
    }
}
