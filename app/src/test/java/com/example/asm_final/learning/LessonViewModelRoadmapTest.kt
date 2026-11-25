package com.example.asm_final.learning

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import com.example.asm_final.backend.BackendClient
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.Executor

class LessonViewModelRoadmapTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    class DirectExecutor : Executor {
        override fun execute(command: Runnable) {
            command.run()
        }
    }

    class FakeRoadmapApiClient : LessonViewModel.LessonApiClient {
        val requests = mutableListOf<String>()
        var customHandler: ((method: String, path: String, body: JSONObject?, key: String?, revision: Int?) -> JSONObject)? = null

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
            requests.add("$method $path")
            customHandler?.let { return it(method, path, body, key, revision) }

            if (method == "POST" && path == "/enrollments") {
                return JSONObject().put("id", "enr_1").put("subjectId", "physics").put("grade", 10)
            }
            if (method == "GET" && path.contains("/path")) {
                return JSONObject().apply {
                    put("enrollment", JSONObject().put("id", "enr_1"))
                    put("curriculumVersion", "v1")
                    put("units", JSONArray())
                }
            }
            return JSONObject()
        }
    }

    private val app = android.app.Application()

    @Test
    fun placementAnswersCanBeRevisitedAndSubmittedTogether() {
        val client = FakeRoadmapApiClient()
        val directExecutor = DirectExecutor()
        var submitted: JSONObject? = null
        client.customHandler = { method, path, body, _, _ ->
            when ("$method $path") {
                "POST /enrollments" -> JSONObject().put("id", "enr_1")
                "GET /enrollments/enr_1/path" -> JSONObject()
                    .put("enrollment", JSONObject().put("id", "enr_1"))
                    .put("curriculumVersion", "v1")
                "POST /placements" -> JSONObject().put("id", "placement-1")
                    .put("kind", "PLACEMENT").put("step", 0).put("revision", 1)
                    .put("questions", JSONArray().put(JSONObject().put("id", "q1"))
                        .put(JSONObject().put("id", "q2")))
                    .put("answers", JSONArray())
                "PATCH /attempts/placement-1" -> {
                    submitted = body
                    JSONObject().put("id", "placement-1").put("kind", "PLACEMENT")
                        .put("step", 2).put("revision", 2)
                        .put("questions", JSONArray().put(JSONObject().put("id", "q1"))
                            .put(JSONObject().put("id", "q2")))
                        .put("answers", JSONArray())
                }
                else -> error("Unexpected request: $method $path")
            }
        }
        val vm = LessonViewModel(app, SavedStateHandle(), client, directExecutor, directExecutor) { 1 }

        vm.bind(1, "physics", 10)
        vm.start("placement", true)
        vm.setPlacementAnswer("q1", "a")
        vm.setPlacementIndex(1)
        vm.setPlacementAnswer("q2", "b")
        vm.setPlacementIndex(0)
        vm.setPlacementAnswer("q1", "c")

        assertEquals(0, vm.placementIndex())
        assertEquals("c", vm.placementAnswer("q1"))
        assertTrue(vm.placementReady())
        assertFalse(client.requests.contains("PATCH /attempts/placement-1"))

        vm.submitPlacement()

        assertEquals(2, submitted?.getInt("step"))
        val answers = submitted!!.getJSONArray("answers")
        assertEquals("c", answers.getJSONObject(0).getString("selectedOptionId"))
        assertEquals("b", answers.getJSONObject(1).getString("selectedOptionId"))
        assertFalse(vm.hasPending())
    }

    @Test
    fun placementCanSubmitWithAnUnansweredQuestion() {
        val client = FakeRoadmapApiClient()
        val directExecutor = DirectExecutor()
        var submitted: JSONObject? = null
        client.customHandler = { method, path, body, _, _ ->
            when ("$method $path") {
                "POST /enrollments" -> JSONObject().put("id", "enr_1")
                "GET /enrollments/enr_1/path" -> JSONObject()
                    .put("enrollment", JSONObject().put("id", "enr_1"))
                    .put("curriculumVersion", "v1")
                "POST /placements" -> JSONObject().put("id", "placement-2")
                    .put("kind", "PLACEMENT").put("step", 0).put("revision", 1)
                    .put("questions", JSONArray().put(JSONObject().put("id", "q1"))
                        .put(JSONObject().put("id", "q2")))
                    .put("answers", JSONArray())
                "PATCH /attempts/placement-2" -> {
                    submitted = body
                    JSONObject().put("id", "placement-2").put("kind", "PLACEMENT")
                        .put("step", 2).put("revision", 2)
                        .put("questions", JSONArray()).put("answers", JSONArray())
                }
                else -> error("Unexpected request: $method $path")
            }
        }
        val vm = LessonViewModel(app, SavedStateHandle(), client, directExecutor, directExecutor) { 1 }

        vm.bind(1, "physics", 10)
        vm.start("placement", true)
        vm.setPlacementAnswer("q1", "a")
        assertFalse(vm.placementReady())

        vm.submitPlacement()

        assertEquals(2, submitted?.getInt("step"))
        val answers = submitted!!.getJSONArray("answers")
        assertEquals(1, answers.length())
        assertEquals("q1", answers.getJSONObject(0).getString("questionId"))
    }

    @Test
    fun completedPlacementCannotStartAgain() {
        val client = FakeRoadmapApiClient()
        val directExecutor = DirectExecutor()
        client.customHandler = { method, path, _, _, _ ->
            when ("$method $path") {
                "POST /enrollments" -> JSONObject().put("id", "enr_1")
                "GET /enrollments/enr_1/path" -> JSONObject()
                    .put("enrollment", JSONObject().put("id", "enr_1")
                        .put("placement", JSONObject().put("completedAt", "2026-11-15")))
                    .put("curriculumVersion", "v1")
                    .put("units", JSONArray())
                else -> error("Unexpected request: $method $path")
            }
        }
        val vm = LessonViewModel(app, SavedStateHandle(), client, directExecutor, directExecutor) { 1 }

        vm.bind(1, "physics", 10)
        vm.start("placement", true)

        assertFalse(client.requests.contains("POST /placements"))
        assertNull(vm.error)
    }

    @Test
    fun testResumeCompletedAttemptRestoresCompletionObject() {
        val client = FakeRoadmapApiClient()
        val savedState = SavedStateHandle()
        val directExecutor = DirectExecutor()
        val vm = LessonViewModel(app, savedState, client, directExecutor, directExecutor) { 1 }

        client.customHandler = { method, path, _, _, _ ->
            if (method == "GET" && path == "/attempts/att_101") {
                JSONObject().apply {
                    put("id", "att_101")
                    put("state", "COMPLETED")
                    put("unit_id", "physics-10-unit-1")
                    put("completion", JSONObject().apply {
                        put("id", "comp_999")
                        put("attemptId", "att_101")
                        put("xpAwarded", 200)
                        put("streakDays", 1)
                    })
                }
            } else if (method == "POST" && path == "/enrollments") {
                JSONObject().put("id", "enr_1")
            } else {
                JSONObject().apply {
                    put("enrollment", JSONObject().put("id", "enr_1"))
                    put("curriculumVersion", "v1")
                }
            }
        }

        vm.bind(1, "physics", 10)
        vm.resume("att_101")

        assertNotNull(vm.attempt)
        assertEquals("att_101", vm.attempt?.optString("id"))
        assertEquals("COMPLETED", vm.attempt?.optString("state"))

        assertNotNull(vm.completion)
        assertEquals("comp_999", vm.completion?.optString("id"))
        assertEquals(200, vm.completion?.optInt("xpAwarded"))
    }

    @Test
    fun testRetryRecoversFromAttemptAlreadyCompleted409() {
        val client = FakeRoadmapApiClient()
        val savedState = SavedStateHandle()
        val directExecutor = DirectExecutor()
        val vm = LessonViewModel(app, savedState, client, directExecutor, directExecutor) { 1 }

        // Start with an attempt
        val initialAttempt = JSONObject().apply {
            put("id", "att_202")
            put("state", "IN_PROGRESS")
            put("revision", 2)
            put("questions", JSONArray())
        }

        client.customHandler = { method, path, body, key, revision ->
            if (method == "PATCH" && path == "/attempts/att_202") {
                throw BackendClient.Failure("ATTEMPT_ALREADY_COMPLETED", 409)
            } else if (method == "GET" && path == "/attempts/att_202") {
                JSONObject().apply {
                    put("id", "att_202")
                    put("state", "COMPLETED")
                    put("revision", 3)
                    put("completion", JSONObject().apply {
                        put("id", "comp_done")
                        put("xpAwarded", 200)
                    })
                }
            } else {
                JSONObject().apply {
                    put("id", "enr_1")
                    put("enrollment", JSONObject().put("id", "enr_1"))
                }
            }
        }

        vm.bind(1, "physics", 10)

        // Inject active attempt
        val attemptField = LessonViewModel::class.java.getDeclaredField("attempt")
        attemptField.isAccessible = true
        attemptField.set(vm, initialAttempt)

        // Save progress, which sets pending and triggers retry
        vm.save(2, null, null)

        // Verify that retry caught 409 ATTEMPT_ALREADY_COMPLETED and fetched /attempts/att_202
        assertFalse(vm.hasPending())
        assertNotNull(vm.attempt)
        assertEquals("COMPLETED", vm.attempt?.optString("state"))

        assertNotNull(vm.completion)
        assertEquals("comp_done", vm.completion?.optString("id"))
    }

    @Test
    fun testRetryRecoversFromRevisionConflict412() {
        val client = FakeRoadmapApiClient()
        val savedState = SavedStateHandle()
        val directExecutor = DirectExecutor()
        val vm = LessonViewModel(app, savedState, client, directExecutor, directExecutor) { 1 }

        val initialAttempt = JSONObject().apply {
            put("id", "att_303")
            put("state", "IN_PROGRESS")
            put("revision", 1)
        }

        client.customHandler = { method, path, body, key, revision ->
            if (method == "PATCH" && path == "/attempts/att_303") {
                throw BackendClient.Failure("REVISION_CONFLICT", 412)
            } else if (method == "GET" && path == "/attempts/att_303") {
                JSONObject().apply {
                    put("id", "att_303")
                    put("state", "IN_PROGRESS")
                    put("revision", 2)
                }
            } else {
                JSONObject().apply {
                    put("id", "enr_1")
                    put("enrollment", JSONObject().put("id", "enr_1"))
                }
            }
        }

        vm.bind(1, "physics", 10)

        val attemptField = LessonViewModel::class.java.getDeclaredField("attempt")
        attemptField.isAccessible = true
        attemptField.set(vm, initialAttempt)

        vm.save(1, null, null)

        assertFalse(vm.hasPending())
        assertNotNull(vm.attempt)
        assertEquals(2, vm.attempt?.optInt("revision"))
        assertNull(vm.completion)
    }

    @Test
    fun testAnimateCompletionCelebratedOnlyOnce() {
        val client = FakeRoadmapApiClient()
        val savedState = SavedStateHandle()
        val directExecutor = DirectExecutor()
        val vm = LessonViewModel(app, savedState, client, directExecutor, directExecutor) { 1 }

        vm.bind(1, "physics", 10)

        assertFalse(vm.animateCompletion())

        val completionObj = JSONObject().apply {
            put("id", "celebration_1")
            put("xpAwarded", 200)
        }

        val completionField = LessonViewModel::class.java.getDeclaredField("completion")
        completionField.isAccessible = true
        completionField.set(vm, completionObj)

        // First call should return true
        assertTrue(vm.animateCompletion())

        // Subsequent call for same completion ID should return false
        assertFalse(vm.animateCompletion())
    }
}
