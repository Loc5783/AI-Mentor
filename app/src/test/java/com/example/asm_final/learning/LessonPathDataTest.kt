package com.example.asm_final.learning

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonPathDataTest {
    private fun unit(id: String, state: String, review: Boolean = false): JSONObject =
        JSONObject().put("id", id).put("title", id)
            .put("kind", if (review) "FIVE_LESSON_REVIEW" else "LESSON")
            .put("state", state).put("xp", if (review) 400 else 200)

    @Test fun skippedLessonsAndReviewDisappearWithoutMovingLessonsAcrossStages() {
        val values = JSONArray()
        repeat(5) { values.put(unit("lesson-${it + 1}", "SKIPPED")) }
        values.put(unit("review-1", "SKIPPED", review = true))
        values.put(unit("lesson-6", "AVAILABLE"))
        values.put(unit("lesson-7", "LOCKED"))
        values.put(unit("lesson-8", "COMPLETED"))
        values.put(unit("review-2", "LOCKED", review = true))

        val path = LessonPathData(values)

        assertEquals(8, path.totalLessons)
        assertEquals(5, path.skippedLessons)
        assertEquals(1, path.completedLessons)
        assertEquals(1, path.visibleStages.size)
        assertEquals(2, path.visibleStages[0].number)
        assertEquals(listOf("lesson-6", "lesson-7", "lesson-8", "review-2"),
            path.visibleStages[0].units.map { it.id })
        assertEquals(6, path.visibleStages[0].units[0].ordinal)
    }

    @Test fun allMasteredPathHasNoEmptyStage() {
        val path = LessonPathData(JSONArray().put(unit("lesson-1", "SKIPPED"))
            .put(unit("review-1", "SKIPPED", review = true)))

        assertEquals(1, path.skippedLessons)
        assertTrue(path.visibleStages.isEmpty())
    }
}
