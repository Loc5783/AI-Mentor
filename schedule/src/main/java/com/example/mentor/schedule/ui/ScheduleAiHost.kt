package com.example.mentor.schedule.ui

/** The app shell owns navigation to Chat AI; the schedule module only requests a task. */
interface ScheduleAiHost {
    fun openScheduleAi(action: ScheduleAiAction)
}

enum class ScheduleAiAction {
    TIMETABLE_PHOTO,
    TODO_PLAN,
}
