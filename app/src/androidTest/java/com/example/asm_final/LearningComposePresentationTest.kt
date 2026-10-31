package com.example.asm_final

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.example.asm_final.R
import com.example.asm_final.learning.LessonPathData
import com.example.asm_final.ui.PathContent
import com.example.mentor.learning.SubjectId
import com.example.mentor.ui.design.MentorTheme
import org.json.JSONArray
import org.json.JSONObject
import org.junit.*

class LearningComposePresentationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun lockedNodesCannotLaunchAndReviewsKeepTheirOwnReward() {
        val units = JSONArray()
        repeat(6) { index ->
            units.put(JSONObject().put("id", "unit-$index").put("title", "A long lesson title that must wrap without hiding the action")
                .put("kind", if (index == 5) "FIVE_LESSON_REVIEW" else "LESSON")
                .put("xp", if (index == 5) 400 else 200)
                .put("state", when (index) { 0 -> "COMPLETED"; 1 -> "AVAILABLE"; 2 -> "SKIPPED"; else -> "LOCKED" })
                .put("canOpen", index != 2))
        }
        val opened = mutableListOf<String>()
        var dark by mutableStateOf(false)
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.3f)) {
                MentorTheme(dark) {
                    Column(Modifier.width(320.dp).verticalScroll(rememberScrollState())) {
                        PathContent(LessonPathData(units), SubjectId.MATH) { opened.add(it) }
                    }
                }
            }
        }
        compose.onNodeWithTag("path-unit:unit-0").performScrollTo().performClick()
        compose.onNodeWithTag("path-sheet-open").performClick()
        compose.onNodeWithTag("path-unit:unit-1").performScrollTo().performClick()
        compose.onNodeWithTag("path-sheet-open").performClick()
        compose.onNodeWithTag("path-unit:unit-2").performScrollTo().performClick()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.onNodeWithText(context.getString(R.string.course_placement_skipped_status)).assertExists()
        compose.onNodeWithText(context.getString(R.string.course_unlock_note)).assertExists()
        compose.onNodeWithText(context.getString(R.string.course_retake_skipped)).assertDoesNotExist()
        compose.onNodeWithText(context.getString(R.string.course_back)).performClick()
        Assert.assertEquals(listOf("unit-0", "unit-1"), opened)
        compose.runOnIdle { dark = true }
        compose.onNodeWithTag("path-unit:unit-5").performScrollTo().assertExists()
        Assert.assertEquals(5, LessonPathData(units).totalLessons)
        Assert.assertEquals(1, LessonPathData(units).skippedLessons)
        Assert.assertEquals(400, LessonPathData(units).units.last().xp)
    }
}
