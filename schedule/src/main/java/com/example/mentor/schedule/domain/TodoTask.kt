package com.example.mentor.schedule.domain

data class TodoTask(
    val id: Long = 0,
    val accountId: String,
    val title: String,
    val isCompleted: Boolean = false,
    val dayOfWeek: Int, // 1 = Mon, ..., 7 = Sun
    val priority: Int = 1, // 0 = Low, 1 = Normal, 2 = High
    val linkedSubject: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)