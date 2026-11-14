package com.example.mentor.schedule.domain

/** Only the chosen day's loaded data is sent when the learner asks the assistant to plan. */
object ScheduleAiContext {
    fun forDay(day: Int, entries: List<ScheduleEntry>, tasks: List<TodoTask>): String {
        require(day in 1..7)
        val lessons = entries.filter { it.dayOfWeek == day }.sortedBy { it.period }.take(10)
            .joinToString("; ") { "period ${it.period}: ${it.title.take(80)}" }
        val unfinished = tasks.filter { it.dayOfWeek == day && !it.isCompleted }.take(10)
            .joinToString("; ") { it.title.take(100) }
        return "Only selected weekday $day was loaded (Monday=1, Sunday=7); other days are unknown. " +
            "Recorded classes: ${lessons.ifEmpty { "none" }}. " +
            "Unfinished to-dos: ${unfinished.ifEmpty { "none" }}. " +
            "Use these as context only; ask me about goals or deadlines if needed."
    }
}
