package com.example.mentor.schedule.ui

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mentor.schedule.domain.ScheduleEntry
import com.example.mentor.schedule.domain.ScheduleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class ScheduleViewModel(
    private val accountId: String,
    private val repository: ScheduleRepository
) : ViewModel() {

    val entries = mutableStateListOf<ScheduleEntry>()
    val allEntries = mutableStateListOf<ScheduleEntry>()
    val selectedDay = mutableStateOf(getCurrentDayOfWeek())
    
    // For Undo
    private var lastDeletedEntry: ScheduleEntry? = null

    init {
        loadEntries()
        loadAllEntriesForWeek()
    }

    private fun getCurrentDayOfWeek(): Int {
        val cal = Calendar.getInstance()
        var day = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0 (Sun) to 6 (Sat)
        if (day == 0) day = 7 // Map Sun to 7, Mon to 1
        return day
    }

    fun loadAllEntriesForWeek() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = mutableListOf<ScheduleEntry>()
            for (day in 1..7) {
                list.addAll(repository.getEntries(accountId, day))
            }
            withContext(Dispatchers.Main) {
                allEntries.clear()
                allEntries.addAll(list)
            }
        }
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

    fun saveEntries(entries: List<ScheduleEntry>, isEdit: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            entries.forEach { entry ->
                if (isEdit) {
                    repository.updateEntry(entry)
                } else {
                    repository.insertEntry(entry.copy(accountId = accountId))
                }
            }
            loadEntries()
            loadAllEntriesForWeek()
        }
    }

    fun deleteEntry(entry: ScheduleEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteEntry(entry.id, accountId)
            lastDeletedEntry = entry
            loadEntries()
            loadAllEntriesForWeek()
        }
    }

    fun undoDelete() {
        lastDeletedEntry?.let { entry ->
            viewModelScope.launch(Dispatchers.IO) {
                repository.insertEntry(entry)
                lastDeletedEntry = null
                loadEntries()
                loadAllEntriesForWeek()
            }
        }
    }

    suspend fun checkOverlap(day: Int, startTime: Int, endTime: Int, ignoreId: Long = 0L): Boolean {
        return withContext(Dispatchers.IO) {
            val listToCheck = repository.getEntries(accountId, day)
            listToCheck.any { 
                it.id != ignoreId && 
                (startTime < it.endTimeMin && endTime > it.startTimeMin)
            }
        }
    }
}