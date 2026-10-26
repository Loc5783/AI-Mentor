package com.example.mentor.schedule.domain

data class ScheduleActivityLog(
    val id: Long = 0,
    val accountId: String,
    val actionType: String, // CREATE, UPDATE, DELETE, COMPLETE, UNCOMPLETE
    val entityType: String, // SCHEDULE, TODO
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)