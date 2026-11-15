package com.example.asm_final

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.SavedStateHandle
import com.example.asm_final.learning.LessonViewModel
import com.example.asm_final.ui.LearningUi
import com.example.mentor.learning.SubjectId
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.Executor

class PlacementQuizUiInteractionTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    class DirectExecutor : Executor {
        override fun execute(command: Runnable) {
            command.run()
        }
    }

    class FakeUiApiClient : LessonViewModel.LessonApiClient {
        override fun configured(): Boolean = true
        override fun signedIn(): Boolean = true
        override fun signIn(identifier: String?, password: String?) {}

        override fun request(
            method: String,
            path: String,
            body: JSONObject?,
            key: String?,
            revision: Int?
        ): JSONObject {
            return JSONObject().put("id", "placement-ui-attempt-1")
                .put("kind", "PLACEMENT")
                .put("step", 0)
                .put("questions", JSONArray().apply {
                    put(JSONObject().put("id", "q1").put("prompt", "Cau hoi 1 - Sinh hoc")
                        .put("options", JSONArray().apply {
                            put(JSONObject().put("id", "a").put("text", "Dap an A"))
                            put(JSONObject().put("id", "b").put("text", "Dap an B"))
                        }))
                    put(JSONObject().put("id", "q2").put("prompt", "Cau hoi 2 - Sinh hoc")
                        .put("options", JSONArray().apply {
                            put(JSONObject().put("id", "a").put("text", "Cau 2 Dap an A"))
                            put(JSONObject().put("id", "b").put("text", "Cau 2 Dap an B"))
                        }))
                })
                .put("answers", JSONArray())
        }
    }

    @Test
    fun testPlacementOptionSelectionAndNextQuestionUiRecomposition() {
        val context = composeTestRule.activity
        val client = FakeUiApiClient()
        val directExecutor = DirectExecutor()
        val vm = LessonViewModel(context.application as Application, SavedStateHandle(), client, directExecutor, directExecutor) { 1 }
        vm.bind(1, "biology", 10)
        vm.start("placement", true)

        composeTestRule.setContent {
            MaterialTheme {
                LearningUi.screen(context, vm, SubjectId.BIOLOGY, 10, true, {}, null, {})
            }
        }

        // Verify Question 1 prompt is visible
        composeTestRule.onNodeWithText("Cau hoi 1 - Sinh hoc").assertIsDisplayed()

        // Click Option A for Question 1
        composeTestRule.onNodeWithText("Dap an A").performClick()

        // Verify Option A is selected in ViewModel draft
        assertEquals("a", vm.placementAnswer("q1"))

        // Click Option B for Question 1
        composeTestRule.onNodeWithText("Dap an B").performClick()
        assertEquals("b", vm.placementAnswer("q1"))

        // Click Next question button
        composeTestRule.onNodeWithText("Câu tiếp theo").performClick()

        // Verify Question 2 prompt is visible
        assertEquals(1, vm.placementIndex())
        composeTestRule.onNodeWithText("Cau hoi 2 - Sinh hoc").assertIsDisplayed()
    }
}
