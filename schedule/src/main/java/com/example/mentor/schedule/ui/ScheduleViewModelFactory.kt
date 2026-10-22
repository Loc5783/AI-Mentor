package com.example.mentor.schedule.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.mentor.schedule.domain.ScheduleRepository

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