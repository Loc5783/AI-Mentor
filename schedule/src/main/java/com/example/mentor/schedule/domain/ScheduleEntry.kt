package com.example.mentor.schedule.domain

data class ScheduleEntry(
    val id: Long = 0,
    val accountId: String,
    val title: String,
    val dayOfWeek: Int, // 1 = Mon, ..., 7 = Sun
    val startTimeMin: Int, // 1 to 10
    val teacher: String? = null,
    val notes: String? = null
) {
    fun getFormattedStartTime(): String {
        return formatTime(startTimeMin)
    }

    fun getFormattedEndTime(): String {
        return formatTime(startTimeMin, isEnd = true)
    }

    private fun formatTime(period: Int, isEnd: Boolean = false): String {
        val mapping = mapOf(
            1 to Pair("07:00", "07:45"),
            2 to Pair("07:50", "08:35"),
            3 to Pair("08:40", "09:25"),
            4 to Pair("09:35", "10:20"),
            5 to Pair("10:25", "11:10"),
            6 to Pair("13:00", "13:45"),
            7 to Pair("13:50", "14:35"),
            8 to Pair("14:40", "15:25"),
            9 to Pair("15:35", "16:20"),
            10 to Pair("16:25", "17:10")
        )
        val pair = mapping[period] ?: Pair("00:00", "00:00")
        return if (isEnd) pair.second else pair.first
    }
}