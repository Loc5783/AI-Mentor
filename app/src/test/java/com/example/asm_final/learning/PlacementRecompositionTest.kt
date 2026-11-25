package com.example.asm_final.learning

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.Executor

class PlacementRecompositionTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    class DirectExecutor : Executor {
        override fun execute(command: Runnable) {
            command.run()
        }
    }

    class FakePlacementApiClient : LessonViewModel.LessonApiClient {
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
            return JSONObject().put("id", "placement-attempt-123")
                .put("kind", "PLACEMENT")
                .put("step", 0)
                .put("questions", JSONArray().apply {
                    put(JSONObject().put("id", "q1").put("prompt", "Question 1"))
                    put(JSONObject().put("id", "q2").put("prompt", "Question 2"))
                })
                .put("answers", JSONArray())
        }
    }

    private val app = Application()

    @Test
    fun testPlacementAnswerSelectionAndNavigationStatePropagation() {
        val client = FakePlacementApiClient()
        val directExecutor = DirectExecutor()
        val vm = LessonViewModel(app, SavedStateHandle(), client, directExecutor, directExecutor) { 1 }

        var revisionEmitted = 0
        vm.changes().observeForever { rev ->
            revisionEmitted = rev ?: 0
        }

        vm.bind(1, "biology", 10)
        vm.start("placement", true)

        val initialRev = revisionEmitted

        // 1. Select option A on q1
        vm.setPlacementAnswer("q1", "a")
        assertEquals("a", vm.placementAnswer("q1"))
        assertTrue("Expected revision to increase after setPlacementAnswer", revisionEmitted > initialRev)

        val revAfterQ1OptionA = revisionEmitted

        // 2. Change option on q1 to B
        vm.setPlacementAnswer("q1", "b")
        assertEquals("b", vm.placementAnswer("q1"))
        assertTrue("Expected revision to increase after changing answer to b", revisionEmitted > revAfterQ1OptionA)

        val revAfterQ1OptionB = revisionEmitted

        // 3. Advance to Q2
        vm.setPlacementIndex(1)
        assertEquals(1, vm.placementIndex())
        assertTrue("Expected revision to increase after setPlacementIndex(1)", revisionEmitted > revAfterQ1OptionB)

        val revAfterNextQuestion = revisionEmitted

        // 4. Return to Q1 and verify selection 'b' is retained
        vm.setPlacementIndex(0)
        assertEquals(0, vm.placementIndex())
        assertEquals("b", vm.placementAnswer("q1"))
        assertTrue("Expected revision to increase after setPlacementIndex(0)", revisionEmitted > revAfterNextQuestion)
    }
}
