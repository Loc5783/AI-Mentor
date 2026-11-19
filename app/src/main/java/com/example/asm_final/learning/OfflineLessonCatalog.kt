package com.example.asm_final.learning

import org.json.JSONObject

/** Bundled reading content. Offline answers never update server progress or rewards. */
data class OfflineLesson(
    val id: String,
    val title: String,
    val guidance: List<String>,
    val examples: List<String>
)

data class OfflineCourse(val subjectId: String, val grade: Int, val lessons: List<OfflineLesson>)

object OfflineLessonCatalog {
    fun parse(source: String, subjectId: String, grade: Int): OfflineCourse {
        val root = JSONObject(source)
        require(root.getString("subjectId") == subjectId && root.getInt("grade") == grade)
        val values = root.getJSONArray("lessons")
        val lessons = (0 until values.length()).map { index ->
            val row = values.getJSONObject(index)
            fun strings(name: String): List<String> {
                val values = row.optJSONArray(name) ?: return emptyList()
                return (0 until values.length()).mapNotNull { i ->
                    values.optString(i).trim().takeIf(String::isNotBlank)
                }
            }
            OfflineLesson(row.getString("id"), row.getString("title"),
                strings("guidance"), strings("examples"))
        }
        require(lessons.isNotEmpty())
        return OfflineCourse(subjectId, grade, lessons)
    }
}
