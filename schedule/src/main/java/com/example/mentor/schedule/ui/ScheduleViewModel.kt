package com.example.mentor.schedule.ui

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.mentor.schedule.domain.ScheduleEntry
import com.example.mentor.schedule.domain.ScheduleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class ScheduleViewModelFactory(
    private val accountId: String,
    private val repository: ScheduleRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScheduleViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ScheduleViewModel(accountId, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class ScheduleViewModel(
    private val accountId: String,
    private val repository: ScheduleRepository
) : ViewModel() {

    val entries = mutableStateListOf<ScheduleEntry>()
    val selectedDay = mutableStateOf(getCurrentDayOfWeek())
    
    private var lastDeletedEntry: ScheduleEntry? = null

    init {
        loadEntries()
    }

    private fun getCurrentDayOfWeek(): Int {
        val cal = Calendar.getInstance()
        var day = cal.get(Calendar.DAY_OF_WEEK) - 1
        if (day == 0) day = 7
        return day
    }

    fun loadEntries() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = repository.getEntries(accountId, selectedDay.value)
            withContext(Dispatchers.Main) {
                entries.clear()
                entries.addAll(list)
            }
        }
    }

    fun selectDay(day: Int) {
        selectedDay.value = day
        loadEntries()
    }

    fun saveEntries(entriesToSave: List<ScheduleEntry>, isEdit: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            entriesToSave.forEach { entry ->
                if (isEdit) {
                    repository.updateEntry(entry)
                } else {
                    repository.insertEntry(entry.copy(accountId = accountId))
                }
            }
            loadEntries()
        }
    }

    fun deleteEntry(entry: ScheduleEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteEntry(entry.id, accountId)
            lastDeletedEntry = entry
            loadEntries()
        }
    }

    fun undoDelete() {
        lastDeletedEntry?.let { entry ->
            viewModelScope.launch(Dispatchers.IO) {
                repository.insertEntry(entry)
                lastDeletedEntry = null
                loadEntries()
            }
        }
    }

    suspend fun checkOverlap(day: Int, period: Int, ignoreId: Long = 0L): Boolean {
        return withContext(Dispatchers.IO) {
            val listToCheck = repository.getEntries(accountId, day)
            listToCheck.any { 
                it.id != ignoreId && it.startTimeMin == period
            }
        }
    }
}