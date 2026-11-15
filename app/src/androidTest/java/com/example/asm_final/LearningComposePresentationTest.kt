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
import com.example.mentor.ui.design.MentorTheme
import org.json.JSONArray
import org.json.JSONObject
import org.junit.*

class LearningComposePresentationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun skippedNodesAreHiddenAndReviewsKeepTheirOwnReward() {
        val units = JSONArray()
        repeat(6) { index ->
            units.put(JSONObject().put("id", "unit-$index").put("title", "A long lesson title that must wrap without hiding the action")
                .put("kind", if (index == 5) "FIVE_LESSON_REVIEW" else "LESSON")
                .put("xp", if (index == 5) 400 else 200)
                .put("state", when (index) { 0 -> "COMPLETED"; 1 -> "AVAILABLE"; 2 -> "SKIPPED"; else -> "LOCKED" })
                .put("canOpen", index != 2))
        }
        var dark by mutableStateOf(false)
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.3f)) {
                MentorTheme(dark) {
                    Column(Modifier.width(320.dp).verticalScroll(rememberScrollState())) {
                        PathContent(LessonPathData(units)) { }
                    }
                }
            }
        }
        compose.onNodeWithTag("path-unit:unit-0").performScrollTo().assertExists()
        compose.onNodeWithTag("path-unit:unit-1").performScrollTo().assertExists()
        compose.onNodeWithTag("path-unit:unit-2").assertDoesNotExist()
        compose.runOnIdle { dark = true }
        compose.onNodeWithTag("path-unit:unit-5").performScrollTo().assertExists()
        Assert.assertEquals(5, LessonPathData(units).totalLessons)
        Assert.assertEquals(1, LessonPathData(units).skippedLessons)
        Assert.assertEquals(400, LessonPathData(units).units.last().xp)
    }
}
