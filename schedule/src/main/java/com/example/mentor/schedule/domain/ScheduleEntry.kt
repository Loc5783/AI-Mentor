package com.example.mentor.schedule.domain

data class ScheduleEntry(
    val id: Long = 0,
    val accountId: String,
    val title: String,
    val dayOfWeek: Int, // 1 = Mon, ..., 7 = Sun
    val startTimeMin: Int, // 0 to 1439
    val endTimeMin: Int,   // 0 to 1439, must be > startTimeMin
    val room: String? = null,
    val teacher: String? = null,
    val notes: String? = null,
    val colorLabel: Int = 0 // Some index mapping to predefined colors
) {
    fun getFormattedStartTime(): String {
        return formatTime(startTimeMin)
    }

    fun getFormattedEndTime(): String {
        return formatTime(endTimeMin)
    }

    private fun formatTime(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return String.format("%02d:%02d", h, m)
    }
}