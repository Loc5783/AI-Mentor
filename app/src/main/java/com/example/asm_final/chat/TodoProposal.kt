package com.example.asm_final.chat

import org.json.JSONObject

/** Treat AI output as an untrusted suggestion until the learner reviews every task. */
object TodoProposal {
    data class Task(val day: Int, val title: String, val subject: String?, val priority: Int)

    private val block = Regex("```ai_todo\\s*([\\s\\S]*?)```", RegexOption.IGNORE_CASE)

    fun parse(text: String): List<Task>? = runCatching {
        val matches = block.findAll(text).toList()
        require(matches.size == 1)
        val rows = JSONObject(matches.single().groupValues[1]).getJSONArray("tasks")
        require(rows.length() in 1..21)
        val tasks = (0 until rows.length()).map { index ->
            val row = rows.getJSONObject(index)
            val day = row.get("day")
            val priority = row.get("priority")
            require(day is Number && day.toDouble() == day.toInt().toDouble() && day.toInt() in 1..7)
            require(priority is Number && priority.toDouble() == priority.toInt().toDouble() && priority.toInt() in 0..2)
            val title = row.get("title")
            require(title is String && title.isNotBlank() && title.length <= 160 && title.none { it.isISOControl() })
            val subject = if (row.isNull("subject")) null else row.get("subject")
            require(subject == null || subject is String && subject.length <= 80 && subject.none { it.isISOControl() })
            Task(day.toInt(), title.trim(), (subject as? String)?.trim()?.ifEmpty { null }, priority.toInt())
        }
        require(tasks.distinctBy { Triple(it.day, it.title.lowercase(), it.subject?.lowercase()) }.size == tasks.size)
        tasks
    }.getOrNull()

    fun displayText(text: String): String = block.replace(text, "").trim()
}
