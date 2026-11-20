package com.example.asm_final.chat

import org.json.JSONObject
import java.time.LocalDate

object ExamPlanProposal {
    data class Task(val dueDate: String, val startTime: String, val title: String, val subject: String, val minutes: Int, val priority: Int)
    private val block = Regex("```ai_exam_plan\\s*([\\s\\S]*?)```", RegexOption.IGNORE_CASE)

    fun parse(text: String, today: LocalDate, examDate: LocalDate, subjects: Set<String>, dailyMinutes: Int): List<Task>? = runCatching {
        val matches = block.findAll(text).toList()
        require(matches.size == 1)
        val rows = JSONObject(matches.single().groupValues[1]).getJSONArray("tasks")
        require(rows.length() in 1..21)
        val result = (0 until rows.length()).map { index ->
            val row = rows.getJSONObject(index)
            val date = LocalDate.parse(row.getString("dueDate"))
            val title = row.getString("title").trim()
            val startTime = row.getString("startTime")
            val subject = row.getString("subject").trim()
            val minutes = row.getInt("minutes")
            val priority = row.getInt("priority")
            require(date.isAfter(today) && date.isBefore(examDate))
            require(title.isNotEmpty() && title.length <= 140 && title.none(Char::isISOControl))
            require(Regex("([01]\\d|2[0-3]):[0-5]\\d").matches(startTime))
            require(subject in subjects && minutes in 10..240 && priority in 0..2)
            Task(date.toString(), startTime, title, subject, minutes, priority)
        }
        require(result.groupBy { it.dueDate }.values.all { day -> day.sumOf { it.minutes } <= dailyMinutes })
        require(result.distinctBy { Triple(it.dueDate, it.subject.lowercase(), it.title.lowercase()) }.size == result.size)
        result
    }.getOrNull()
}
