package com.example.mentor.schedule.domain

/** Supplied by the host application; the schedule module does not depend on app transport. */
interface ScheduleRepositoryProvider {
    fun scheduleRepository(): ScheduleRepository
}
