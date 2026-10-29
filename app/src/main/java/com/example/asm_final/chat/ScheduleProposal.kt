package com.example.asm_final.chat

import org.json.JSONObject

/** Strict boundary between untrusted model output and local schedule writes. */
object ScheduleProposal {
    data class Slot(val day: Int, val period: Int, val subject: String)
    private val block = Regex("```ai_schedule\\s*([\\s\\S]*?)```", RegexOption.IGNORE_CASE)
    fun parse(text: String): List<Slot>? = runCatching {
        val matches = block.findAll(text).toList()
        require(matches.size == 1)
        val rows = JSONObject(matches.single().groupValues[1]).getJSONArray("entries")
        require(rows.length() in 1..70)
        val result = (0 until rows.length()).map { index ->
            val row = rows.getJSONObject(index)
            val day = row.get("day")
            val period = row.get("period")
            require(day is Number && day.toDouble() == day.toInt().toDouble() && day.toInt() in 1..7)
            require(period is Number && period.toDouble() == period.toInt().toDouble() && period.toInt() in 1..10)
            val subject = row.get("subject")
            require(subject is String && subject.isNotBlank() && subject.length <= 120 && subject.none { it.isISOControl() })
            Slot(day.toInt(), period.toInt(), subject.trim())
        }
        require(result.distinctBy { it.day to it.period }.size == result.size)
        result
    }.getOrNull()
    fun displayText(text: String): String = block.replace(text, "").trim()
}
