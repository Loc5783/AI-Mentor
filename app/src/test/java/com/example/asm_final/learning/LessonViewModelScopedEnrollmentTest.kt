package com.example.asm_final.learning

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.Executor

/**
 * Reproduction & verification tests for scoped task enrollment key and SavedState safety.
 * Verifies that background tasks do not use mutated enrollment keys across rebinds,
 * legacy fallbacks use scoped keys, and SavedState mutations are guarded.
 */
class LessonViewModelScopedEnrollmentTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    class FakeLessonApiClient(val name: String = "client") : LessonViewModel.LessonApiClient {
        data class RequestRecord(
            val clientName: String,
            val method: String,
            val path: String,
            val body: JSONObject?,
            val key: String?,
            val revision: Int?
        )

        val recordedRequests = mutableListOf<RequestRecord>()
        var configuredResult = true
        var signedInResult = true
        var customHandler: ((method: String, path: String, body: JSONObject?, key: String?) -> JSONObject)? = null

        override fun configured(): Boolean = configuredResult

        override fun signedIn(): Boolean = signedInResult

        override fun signIn(identifier: String?, password: String?) {}

        override fun request(
            method: String,
            path: String,
            body: JSONObject?,
            key: String?,
            revision: Int?
        ): JSONObject {
            val record = RequestRecord(
                clientName = name,
                method = method,
                path = path,
                body = body,
                key = key,
                revision = revision
            )
            recordedRequests.add(record)
            customHandler?.let { return it(method, path, body, key) }

            if (path == "/enrollments" && method == "POST") {
                return JSONObject().put("id", "enr_" + (body?.optString("subjectId") ?: "default"))
            }
            if (path.contains("/path") && method == "GET") {
                return JSONObject().apply {
                    put("enrollment", JSONObject().put("id", "enr_default"))
                    put("curriculumVersion", "v1")
                    put("lessons", JSONArray())
                }
            }
            return JSONObject()
        }

        fun countCalls(method: String, pathPrefix: String): Int {
            return recordedRequests.count { it.method == method && it.path.startsWith(pathPrefix) }
        }
    }

    class ControllableExecutor : Executor {
        val tasks = mutableListOf<Runnable>()

        override fun execute(command: Runnable) {
            tasks.add(command)
        }

        fun runNext(): Boolean {
            if (tasks.isNotEmpty()) {
                val r = tasks.removeAt(0)
                r.run()
                return true
            }
            return false
        }

        fun runAll() {
            while (runNext()) {}
        }
    }

    private val app = android.app.Application()

    // =========================================================================
    // A. Worker cũ sau đổi môn/lớp
    // =========================================================================
    @Test
    fun testA_OldWorkerAfterSubjectGradeChange_MustNotUseNewScopeEnrollmentKey() {
        val saved = SavedStateHandle()
        val workerExec = ControllableExecutor()
        val mainExec = ControllableExecutor()
        val fakeClient = FakeLessonApiClient()

        val viewModel = LessonViewModel(app, saved, fakeClient, workerExec, mainExec) { 1 }

        // 1. Bind Physics 10 -> Worker Physics được xếp hàng
        viewModel.bind(1, "physics", 10)
        val physicsEnrollKey = saved.get<String>("1:physics:10:enrollKey")
        assertNotNull("Scope Physics 10 phải có enrollKey ổn định", physicsEnrollKey)
        assertEquals(1, workerExec.tasks.size)

        // 2. Bind Math 12 -> ViewModel chuyển scope
        viewModel.bind(1, "math", 12)
        val mathEnrollKey = saved.get<String>("1:math:12:enrollKey")
        assertNotNull("Scope Math 12 phải có enrollKey ổn định", mathEnrollKey)
        assertNotEquals("Key Physics và Math phải khác nhau", physicsEnrollKey, mathEnrollKey)
        assertEquals(2, workerExec.tasks.size)

        // 3. Cho worker Physics cũ chạy
        val ranOldWorker = workerExec.runNext()
        assertTrue("Worker cũ phải được chạy", ranOldWorker)

        // 4. Xác minh: Hoặc tác vụ bị hủy trước request, hoặc POST /enrollments dùng đúng key Physics 10
        val enrollRequests = fakeClient.recordedRequests.filter { it.method == "POST" && it.path == "/enrollments" }
        if (enrollRequests.isNotEmpty()) {
            val phyReq = enrollRequests.first()
            assertEquals("physics", phyReq.body?.optString("subjectId"))
            assertEquals(10, phyReq.body?.optInt("grade"))
            assertEquals(
                "Old Physics work must retain Physics key",
                physicsEnrollKey,
                phyReq.key
            )
            assertNotEquals(
                "Old Physics work must NOT use Math 12 key",
                mathEnrollKey,
                phyReq.key
            )
        }
    }

    // =========================================================================
    // B. Worker scope mới & Idempotency retry
    // =========================================================================
    @Test
    fun testB_NewScopeWorker_UsesOwnKeyAndMaintainsIdempotencyOnRetry() {
        val saved = SavedStateHandle()
        val workerExec = ControllableExecutor()
        val mainExec = ControllableExecutor()
        val fakeClient = FakeLessonApiClient()

        val viewModel = LessonViewModel(app, saved, fakeClient, workerExec, mainExec) { 1 }

        // Bind Math 12
        viewModel.bind(1, "math", 12)
        val mathEnrollKey = saved.get<String>("1:math:12:enrollKey")
        assertNotNull(mathEnrollKey)

        // Chạy worker Math 12
        workerExec.runNext()
        mainExec.runNext()

        val mathReq = fakeClient.recordedRequests.firstOrNull { it.path == "/enrollments" }
        assertNotNull("Math 12 phải gửi request enrollment", mathReq)
        assertEquals("math", mathReq?.body?.optString("subjectId"))
        assertEquals(12, mathReq?.body?.optInt("grade"))
        assertEquals("Math 12 request phải dùng đúng mathEnrollKey", mathEnrollKey, mathReq?.key)

        // Retry / reload cùng scope phải giữ đúng chính sách key ổn định
        fakeClient.recordedRequests.clear()
        viewModel.reload()
        workerExec.runNext()
        mainExec.runNext()

        val retryReq = fakeClient.recordedRequests.firstOrNull { it.path == "/enrollments" }
        assertNotNull("Reload phải gửi request enrollment", retryReq)
        assertEquals("Retry cùng scope phải giữ nguyên idempotency key", mathEnrollKey, retryReq?.key)
    }

    // =========================================================================
    // C. Fallback xác minh legacy qua POST /enrollments
    // =========================================================================
    @Test
    fun testC_LegacyFallbackVerification_MustNotReadMutableEnrollKeyOfNewScope() {
        val saved = SavedStateHandle()
        // Đặt attempt legacy thiếu metadata subject/grade để buộc vào nhánh xác minh
        saved.set("attemptId", "legacy_att_unknown")
        saved.set("savedOwner", 1)

        val workerExec = ControllableExecutor()
        val mainExec = ControllableExecutor()
        val fakeClient = FakeLessonApiClient()

        fakeClient.customHandler = { method, path, _, _ ->
            if (method == "GET" && path == "/attempts/legacy_att_unknown") {
                // Trả về attempt có enrollmentId nhưng thiếu subject và grade
                JSONObject().apply {
                    put("id", "legacy_att_unknown")
                    put("enrollmentId", "enr_legacy_prev")
                    put("questions", JSONArray())
                }
            } else if (method == "POST" && path == "/enrollments") {
                JSONObject().put("id", "enr_new_scope")
            } else if (method == "GET" && path.contains("/path")) {
                JSONObject().apply {
                    put("enrollment", JSONObject().put("id", "enr_new_scope"))
                    put("curriculumVersion", "v1")
                }
            } else {
                JSONObject()
            }
        }

        val viewModel = LessonViewModel(app, saved, fakeClient, workerExec, mainExec) { 1 }

        // Bind Physics 10 -> Cần xác minh legacy qua POST /enrollments
        viewModel.bind(1, "physics", 10)
        val physicsEnrollKey = saved.get<String>("1:physics:10:enrollKey")

        // Rebind sang Math 12 trước khi worker chạy
        viewModel.bind(1, "math", 12)
        val mathEnrollKey = saved.get<String>("1:math:12:enrollKey")

        // Chạy worker Physics cũ (trong đó có fallback POST /enrollments)
        workerExec.runNext()

        val enrollReq = fakeClient.recordedRequests.firstOrNull { it.method == "POST" && it.path == "/enrollments" }
        assertNotNull("Phải gọi POST /enrollments để xác minh scope", enrollReq)
        assertEquals("physics", enrollReq?.body?.optString("subjectId"))
        assertEquals(10, enrollReq?.body?.optInt("grade"))
        assertEquals(
            "Legacy fallback enrollment request must retain Physics key",
            physicsEnrollKey,
            enrollReq?.key
        )
        assertNotEquals(
            "Legacy fallback enrollment request must NOT use Math key",
            mathEnrollKey,
            enrollReq?.key
        )

        // Chạy callback cũ trên main thread: không được nhận attempt legacy sai môn/lớp vào scope hiện tại (Math 12)
        mainExec.runAll()
        assertNull("Math 12 không nhận attempt của Vật lý", viewModel.attempt)
        assertNull(saved.get("1:math:12:attemptId"))
    }

    // =========================================================================
    // D. Callback cũ sau rebind không làm hỏng state scope mới
    // =========================================================================
    @Test
    fun testD_StaleCallbackAfterRebind_DoesNotMutateNewScopeState() {
        val saved = SavedStateHandle()
        val workerExec = ControllableExecutor()
        val mainExec = ControllableExecutor()
        val fakeClient = FakeLessonApiClient()

        val viewModel = LessonViewModel(app, saved, fakeClient, workerExec, mainExec) { 1 }

        // 1. Bind Physics 10 và cho worker chạy (nhưng giữ callback main lại)
        viewModel.bind(1, "physics", 10)
        workerExec.runNext()
        assertEquals(1, mainExec.tasks.size) // Physics main callback đang đợi

        // 2. Rebind sang Math 12 và hoàn thành Math 12
        viewModel.bind(1, "math", 12)
        workerExec.runNext() // worker Math 12
        assertEquals(2, mainExec.tasks.size)
        // Task 0: Physics callback cũ. Task 1: Math callback mới.
        val mathCallback = mainExec.tasks.removeAt(1)
        mathCallback.run()

        assertNotNull("Math 12 đã tải path thành công", viewModel.path)
        val mathPath = viewModel.path
        assertFalse(viewModel.busy)
        assertNull(viewModel.error)

        // Thiết lập pending mutation trong scope Math 12
        saved.set("1:math:12:pendingPath", "/attempts/att_math_pend")

        // 3. Bây giờ chạy Physics callback cũ (Task 0)
        val oldPhysicsCallback = mainExec.tasks.removeAt(0)
        oldPhysicsCallback.run()

        // KIỂM CHỨNG:
        // a. Không đổi path / attempt của scope mới
        assertSame("Path của Math 12 không bị thay đổi bởi callback cũ", mathPath, viewModel.path)
        assertNull("Attempt của Math 12 không bị gán", viewModel.attempt)
        // b. Không đổi busy / loading / error của scope mới
        assertFalse(viewModel.busy)
        assertNull(viewModel.error)
        // c. Không xóa hoặc ghi đè pending mutation của scope mới
        assertEquals("/attempts/att_math_pend", saved.get("1:math:12:pendingPath"))
    }

    // =========================================================================
    // E. Đổi tài khoản (Owner A -> Owner B)
    // =========================================================================
    @Test
    fun testE_SwitchAccount_IsolatesClientKeyAndSavedState() {
        val saved = SavedStateHandle()
        val workerExec = ControllableExecutor()
        val mainExec = ControllableExecutor()

        val clientA = FakeLessonApiClient("clientA")
        val clientB = FakeLessonApiClient("clientB")

        var activeUserId = 1
        val apiFactory = LessonViewModel.LessonApiClientFactory { acc ->
            if (acc == 1) clientA else clientB
        }

        val viewModel = LessonViewModel(
            app,
            saved,
            apiFactory,
            workerExec,
            mainExec,
            { activeUserId }
        )

        // 1. Queue tác vụ owner 1 (Physics 10)
        viewModel.bind(1, "physics", 10)
        val keyOwner1 = saved.get<String>("1:physics:10:enrollKey")
        assertNotNull(keyOwner1)

        // 2. Chuyển sang owner 2 (Physics 10)
        activeUserId = 2
        viewModel.bind(2, "physics", 10)
        val keyOwner2 = saved.get<String>("2:physics:10:enrollKey")
        assertNotNull(keyOwner2)
        assertNotEquals("Hai owner phải có enrollKey độc lập", keyOwner1, keyOwner2)

        // 3. Chạy worker cũ của Owner 1
        workerExec.runNext()

        // Xác minh: Request của Owner 1 phải gửi qua clientA, dùng keyOwner1, KHÔNG dùng clientB/keyOwner2
        assertEquals("clientB không được nhận request của Owner 1", 0, clientB.recordedRequests.size)
        val reqA = clientA.recordedRequests.firstOrNull { it.path == "/enrollments" }
        assertNotNull("Owner 1 phải gửi request enrollment qua clientA", reqA)
        assertEquals("physics", reqA?.body?.optString("subjectId"))
        assertEquals(
            "Owner 1 worker must retain Owner 1 key",
            keyOwner1,
            reqA?.key
        )
        assertNotEquals(
            "Owner 1 worker must NOT use Owner 2 key",
            keyOwner2,
            reqA?.key
        )

        // 4. Chạy callback cũ của Owner 1 trên main thread
        mainExec.runNext()
        // Callback owner 1 bị guard chặn, không cập nhật sang owner 2
        assertNull("Owner 2 không nhận dữ liệu từ callback Owner 1", viewModel.path)
        assertNull(saved.get("2:physics:10:attemptId"))
    }

    // =========================================================================
    // F. Metadata legacy và bảo toàn SavedState
    // =========================================================================
    @Test
    fun testF1_LegacyMismatchAfterRebind_MustNotWriteInappropriateMetadataToSavedState() {
        val saved = SavedStateHandle()
        saved.set("attemptId", "candidate_mismatch_att")
        saved.set("savedOwner", 1)

        val workerExec = ControllableExecutor()
        val mainExec = ControllableExecutor()
        val fakeClient = FakeLessonApiClient()

        fakeClient.customHandler = { method, path, _, _ ->
            if (method == "GET" && path == "/attempts/candidate_mismatch_att") {
                // Server trả về môn Chemistry lớp 11 (không khớp Physics 10)
                JSONObject().apply {
                    put("id", "candidate_mismatch_att")
                    put("subjectId", "chemistry")
                    put("grade", 11)
                    put("questions", JSONArray())
                }
            } else if (method == "GET" && path.contains("/path")) {
                JSONObject().apply {
                    put("enrollment", JSONObject().put("id", "enr_default"))
                    put("curriculumVersion", "v1")
                }
            } else {
                JSONObject()
            }
        }

        val viewModel = LessonViewModel(app, saved, fakeClient, workerExec, mainExec) { 1 }

        // Bind Physics 10
        viewModel.bind(1, "physics", 10)

        // Rebind sang Math 12 trước khi worker chạy
        viewModel.bind(1, "math", 12)

        // Chạy worker Physics cũ
        workerExec.runNext()

        // Khi chạy stale callback:
        mainExec.runAll()
        // Kiểm tra stale callback không ghi candidate vào SavedState nếu generation đã đổi:
        assertNull(
            "Metadata candidate không được ghi vào SavedState bởi tác vụ/callback đã stale",
            saved.get<String>("legacy_candidate_subj:1:candidate_mismatch_att")
        )
    }

    @Test
    fun testF2_LegacyCandidate_PreservedWhenOpeningAnotherSubject() {
        val saved = SavedStateHandle()
        saved.set("attemptId", "legacy_phy_candidate")
        saved.set("savedSubject", "physics")
        saved.set("savedGrade", 10)
        saved.set("savedOwner", 1)

        val fakeClient = FakeLessonApiClient()
        val directExecutor = Executor { it.run() }
        val viewModel = LessonViewModel(app, saved, fakeClient, directExecutor, directExecutor) { 1 }

        // Mở Math 12 trước
        viewModel.bind(1, "math", 12)

        // Candidate của Physics 10 không bị mất
        val candidateSubj = saved.get<String>("legacy_candidate_subj:1:legacy_phy_candidate")
        val candidateGrade = saved.get<Int>("legacy_candidate_grade:1:legacy_phy_candidate")
        assertEquals("physics", candidateSubj)
        assertEquals(Integer.valueOf(10), candidateGrade)

        // Khi bind sang Physics 10, candidate được tìm thấy và sử dụng
        fakeClient.recordedRequests.clear()
        fakeClient.customHandler = { method, path, _, _ ->
            if (path == "/attempts/legacy_phy_candidate") {
                JSONObject().apply {
                    put("id", "legacy_phy_candidate")
                    put("subjectId", "physics")
                    put("grade", 10)
                    put("questions", JSONArray())
                }
            } else JSONObject()
        }

        viewModel.bind(1, "physics", 10)
        assertEquals(1, fakeClient.countCalls("GET", "/attempts/legacy_phy_candidate"))
        assertEquals("legacy_phy_candidate", viewModel.attempt?.optString("id"))
    }
}
