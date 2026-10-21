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
import com.example.asm_final.learning.LessonPathData
import com.example.asm_final.ui.PathContent
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
                .put("state", if (index == 0) "COMPLETED" else if (index == 1) "AVAILABLE" else "LOCKED"))
        }
        val opened = mutableListOf<String>()
        var dark by mutableStateOf(false)
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.3f)) {
                MentorTheme(dark) {
                    Column(Modifier.width(320.dp).verticalScroll(rememberScrollState())) {
                        PathContent(LessonPathData(units)) { opened.add(it) }
                    }
                }
            }
        }
        listOf("unit-0", "unit-1").forEach { id ->
            compose.onNode(hasClickAction() and hasAnyAncestor(hasTestTag("path-unit:$id"))).performScrollTo().performClick()
        }
        compose.onNode(hasClickAction() and hasAnyAncestor(hasTestTag("path-unit:unit-2"))).performScrollTo().assertIsNotEnabled()
        Assert.assertEquals(listOf("unit-0", "unit-1"), opened)
        compose.runOnIdle { dark = true }
        compose.onNodeWithTag("path-unit:unit-5").performScrollTo().assertExists()
        Assert.assertEquals(5, LessonPathData(units).totalLessons)
        Assert.assertEquals(400, LessonPathData(units).units.last().xp)
    }
}
