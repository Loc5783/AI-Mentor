package com.example.asm_final.learning

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.File

class OfflineLessonCatalogTest {
    private val source = """{
        "subjectId":"math","grade":10,"lessons":[
          {"id":"sets","title":"Sets","guidance":["First idea"],"examples":["A = {1,2}"]}
        ]
    }"""

    @Test fun parsesReadOnlyLesson() {
        val course = OfflineLessonCatalog.parse(source, "math", 10)
        assertEquals("sets", course.lessons.single().id)
        assertEquals("First idea", course.lessons.single().guidance.single())
    }

    @Test fun rejectsAnotherGradeOrSubject() {
        assertThrows(IllegalArgumentException::class.java) {
            OfflineLessonCatalog.parse(source, "math", 11)
        }
        assertThrows(IllegalArgumentException::class.java) {
            OfflineLessonCatalog.parse(source, "physics", 10)
        }
    }

    @Test fun everySubjectAndGradeHasReadableContentWithoutAnswerKeys() {
        val assets = File("src/main/assets/offline")
        val subjects = listOf("math", "literature", "english", "history", "geography",
            "chemistry", "physics", "biology", "economics_law")
        for (subject in subjects) for (grade in 10..12) {
            val source = File(assets, "$subject-$grade.json").readText(Charsets.UTF_8)
            val course = OfflineLessonCatalog.parse(source, subject, grade)
            org.junit.Assert.assertTrue("$subject-$grade", course.lessons.isNotEmpty())
            org.junit.Assert.assertFalse(source.contains("\"correctOption"))
            org.junit.Assert.assertFalse(source.contains("\"questions\""))
        }
    }
}
