package com.example.asm_final.learning

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.util.concurrent.Executor

/**
 * Unit tests verifying LessonViewModel product contract, Intent handoff contract,
 * attempt preservation behavior, request ownership isolation across rebinds,
 * and LiveData emissions without catching RuntimeException.
 */
class LessonViewModelHandoffTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    // --- Fake API Client for deterministic testing ---
    class FakeLessonApiClient : LessonViewModel.LessonApiClient {
        data class RequestRecord(
            val method: String,
            val path: String,
            val body: JSONObject?,
            val key: String?,
            val revision: Int?
        )

        val recordedRequests = mutableListOf<RequestRecord>()
        var configuredResult = true
        var signedInResult = true
        var responseToReturn: JSONObject? = null
        var exceptionToThrow: Exception? = null
        var customHandler: ((method: String, path: String, body: JSONObject?) -> JSONObject)? = null

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
            recordedRequests.add(
                RequestRecord(
                    method = method,
                    path = path,
                    body = body,
                    key = key,
                    revision = revision
                )
            )
            exceptionToThrow?.let { throw it }
            customHandler?.let { return it(method, path, body) }
            if (path == "/enrollments" && method == "POST") {
                val enrollId = responseToReturn?.optJSONObject("enrollment")?.optString("id")
                    ?: responseToReturn?.optString("id")
                    ?: "enr_default"
                return JSONObject().put("id", enrollId)
            }
            return responseToReturn ?: JSONObject()
        }

        fun countCalls(method: String, pathPrefix: String): Int {
            return recordedRequests.count { it.method == method && it.path.startsWith(pathPrefix) }
        }
    }

    // --- Controllable Executor for queueing and stepping callbacks ---
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

    private val directExecutor = Executor { it.run() }
    private val app = android.app.Application()

    // 1. Kiểm tra hợp đồng Intent handoff thống nhất (Single Source of Truth)
    @Test
    fun test1_HandoffContract_UnifiedSingleSourceOfTruth() {
        val threadId = "thread_phy10_optics_01"
        val username = "student_99"

        val contract = LessonChatHandoff.createContract(username, threadId)
        val map = contract.toExtraMap()

        assertEquals(threadId, map[LessonChatHandoff.EXTRA_CHAT_THREAD_ID])
        assertEquals(true, map[LessonChatHandoff.EXTRA_LESSON_RETURN])
        assertEquals(username, map[LessonChatHandoff.EXTRA_USERNAME])

        val legacyMap = LessonChatHandoff.buildHandoffMap(username, threadId)
        assertEquals(map, legacyMap)

        val anonymousContract = LessonChatHandoff.createContract(null, threadId)
        val anonMap = anonymousContract.toExtraMap()
        assertFalse(anonMap.containsKey(LessonChatHandoff.EXTRA_USERNAME))
        assertEquals(threadId, anonMap[LessonChatHandoff.EXTRA_CHAT_THREAD_ID])

        assertFalse(map.containsKey("TOKEN"))
        assertFalse(map.containsKey("PASSWORD"))
        assertFalse(map.containsKey("SECRET"))
    }

    // 2. Khi khôi phục phiên học từ SavedStateHandle: ViewModel gọi GET /attempts/<id>, KHÔNG gọi POST /attempts
    @Test
    fun test2_ResumeAttempt_OnBindWithSavedState_FetchesExistingAttempt() {
        val saved = SavedStateHandle()
        val attemptId = "att_test_123"
        saved.set("attemptId", attemptId)

        val fakeClient = FakeLessonApiClient()
        val attemptJson = JSONObject().apply {
            put("id", attemptId)
            put("subject", "physics")
            put("grade", 10)
            put("step", 2)
            put("questions", JSONArray())
        }
        fakeClient.responseToReturn = attemptJson

        val viewModel = LessonViewModel(
            app,
            saved,
            fakeClient,
            directExecutor,
            directExecutor,
            { 1 }
        )

        viewModel.bind(1, "physics", 10)

        assertEquals(1, fakeClient.countCalls("GET", "/attempts/$attemptId"))
        assertEquals("Không được tạo attempt mới", 0, fakeClient.countCalls("POST", "/attempts"))
        assertEquals("Không được hoàn thành tự động", 0, fakeClient.countCalls("POST", "/completions"))

        assertNotNull(viewModel.attempt)
        assertEquals(attemptId, viewModel.attempt.optString("id"))
        assertNull(viewModel.error)
        assertFalse(viewModel.busy)
    }

    // 3. Khi LessonActivity quay lại từ Chat (onResume gọi reload()):
    //    Client gửi GET /attempts/<id>, KHÔNG gửi POST /attempts (không tạo attempt mới)
    //    và KHÔNG gửi POST /completions (không cộng lặp EXP).
    @Test
    fun test3_ReturningFromChat_ReloadPreservesAttempt_NoNewAttemptOrExp() {
        val saved = SavedStateHandle()
        val attemptId = "att_chat_return_456"
        saved.set("attemptId", attemptId)

        val fakeClient = FakeLessonApiClient()
        val attemptJson = JSONObject().apply {
            put("id", attemptId)
            put("subject", "chemistry")
            put("grade", 11)
            put("step", 3)
            put("questions", JSONArray().put(JSONObject().put("id", "q1")))
            put("answers", JSONObject().put("q1", "B"))
        }
        fakeClient.responseToReturn = attemptJson

        val viewModel = LessonViewModel(
            app,
            saved,
            fakeClient,
            directExecutor,
            directExecutor,
            { 1 }
        )

        viewModel.bind(1, "chemistry", 11)
        fakeClient.recordedRequests.clear()

        // Giả lập quay lại từ Chat AI (LessonActivity.onResume() gọi model.reload())
        viewModel.reload()

        assertEquals(1, fakeClient.countCalls("GET", "/attempts/$attemptId"))
        assertEquals("Tuyệt đối không được gọi POST /attempts khi reload", 0, fakeClient.countCalls("POST", "/attempts"))
        assertEquals("Tuyệt đối không được gọi POST /completions khi reload", 0, fakeClient.countCalls("POST", "/completions"))

        assertNotNull(viewModel.attempt)
        assertEquals(attemptId, viewModel.attempt.optString("id"))
        assertEquals(3, viewModel.attempt.optInt("step"))
        assertEquals("B", viewModel.attempt.optJSONObject("answers")?.optString("q1"))
        assertEquals(attemptId, saved.get<String>("attemptId"))
    }

    // 4. Nếu reload gặp lỗi mạng: ViewModel KHÔNG xóa attemptId đã lưu và KHÔNG tự khởi tạo attempt mới
    @Test
    fun test4_ReloadNetworkFailure_PreservesAttemptIdAndDoesNotCreateNewAttempt() {
        val saved = SavedStateHandle()
        val attemptId = "att_err_test_789"
        saved.set("attemptId", attemptId)

        val fakeClient = FakeLessonApiClient()
        val initialAttempt = JSONObject().apply {
            put("id", attemptId)
            put("subject", "math")
            put("grade", 12)
            put("step", 1)
            put("questions", JSONArray())
        }
        fakeClient.responseToReturn = initialAttempt

        val viewModel = LessonViewModel(
            app,
            saved,
            fakeClient,
            directExecutor,
            directExecutor,
            { 1 }
        )

        viewModel.bind(1, "math", 12)
        fakeClient.recordedRequests.clear()

        fakeClient.exceptionToThrow = IOException("Simulated network timeout")
        viewModel.reload()

        assertEquals("NETWORK_ERROR", viewModel.error)
        assertEquals("attemptId không được bị xóa khi mất mạng", attemptId, saved.get<String>("attemptId"))
        assertNotNull("Attempt data cũ phải được giữ lại", viewModel.attempt)
        assertEquals(attemptId, viewModel.attempt.optString("id"))
        assertEquals(0, fakeClient.countCalls("POST", "/attempts"))
    }

    // 5. Khôi phục trạng thái qua SavedStateHandle sau khi tái tạo Activity (Process death simulation)
    @Test
    fun test5_ProcessRecreation_RestoresAttemptWithoutCreatingNew() {
        val saved = SavedStateHandle()
        val attemptId = "att_process_recreation_999"
        saved.set("attemptId", attemptId)

        val fakeClient = FakeLessonApiClient()
        fakeClient.responseToReturn = JSONObject().apply {
            put("id", attemptId)
            put("subject", "biology")
            put("grade", 10)
            put("step", 4)
            put("questions", JSONArray())
        }

        val newViewModel = LessonViewModel(
            app,
            saved,
            fakeClient,
            directExecutor,
            directExecutor,
            { 1 }
        )

        newViewModel.bind(1, "biology", 10)

        assertEquals(1, fakeClient.countCalls("GET", "/attempts/$attemptId"))
        assertEquals(0, fakeClient.countCalls("POST", "/attempts"))
        assertEquals(attemptId, newViewModel.attempt.optString("id"))
    }

    // 6. Kiểm tra bảo vệ danh tính (Identity Guard): Người dùng khác không được nhận kết quả của phiên
    @Test
    fun test6_IdentityGuard_ActiveUserMismatch_DropsTaskResult() {
        val saved = SavedStateHandle()
        val fakeClient = FakeLessonApiClient()
        fakeClient.responseToReturn = JSONObject().apply {
            put("id", "att_user_switch")
            put("questions", JSONArray())
        }

        var currentUser = 999

        val viewModel = LessonViewModel(
            app,
            saved,
            fakeClient,
            directExecutor,
            directExecutor,
            { currentUser }
        )

        viewModel.bind(1, "literature", 10)
        assertNull("Khi activeUser != owner, kết quả bài học không được nhận", viewModel.attempt)

        currentUser = 1
        viewModel.reload()
        assertTrue(fakeClient.countCalls("POST", "/enrollments") > 0)
    }

    // 7. Yêu cầu 7A: Callback thành công cũ sau rebind không được nhận vào model B
    @Test
    fun test7_OldOwnerSuccessCallback_DoesNotPolluteReboundModel() {
        val saved = SavedStateHandle()
        val mainExecutor = ControllableExecutor()
        val fakeClient = FakeLessonApiClient()

        val attemptA = JSONObject().apply {
            put("id", "att_owner_A")
            put("subjectId", "physics")
            put("grade", 10)
            put("questions", JSONArray())
        }
        fakeClient.responseToReturn = attemptA

        var currentUser = 1
        val viewModel = LessonViewModel(
            app,
            saved,
            fakeClient,
            directExecutor,
            mainExecutor,
            { currentUser }
        )

        // 1. Bind A, worker hoàn tất, callback cập nhật UI của A đang chờ trong mainExecutor queue
        saved.set("attemptId", "att_owner_A")
        viewModel.bind(1, "physics", 10)
        assertEquals(1, mainExecutor.tasks.size)
        assertTrue(viewModel.busy)

        // 2. Đổi sang user B và bind B
        currentUser = 2
        val attemptB = JSONObject().apply {
            put("id", "att_owner_B")
            put("subjectId", "physics")
            put("grade", 10)
            put("questions", JSONArray())
        }
        fakeClient.responseToReturn = attemptB
        viewModel.bind(2, "physics", 10)
        // Queue hiện có callback của A và callback của B
        assertEquals(2, mainExecutor.tasks.size)
        assertTrue("Model phải đang busy với request của B", viewModel.busy)

        // 3. Chạy callback cũ của A
        val ranA = mainExecutor.runNext()
        assertTrue(ranA)

        // 4. KIỂM CHỨNG:
        // Response A KHÔNG được gán vào model B!
        assertNull("Dữ liệu của owner A tuyệt đối không được gán vào model của B", viewModel.attempt)
        // Callback A KHÔNG được xóa busy của request B!
        assertTrue("Callback của A không được làm mất cờ busy của B", viewModel.busy)

        // 5. Chạy tiếp callback của B
        val ranB = mainExecutor.runNext()
        assertTrue(ranB)
        assertNotNull(viewModel.attempt)
        assertEquals("att_owner_B", viewModel.attempt.optString("id"))
        assertFalse("Sau khi B hoàn tất, busy mới trở về false", viewModel.busy)
    }

    // 8. Yêu cầu 7B: Callback lỗi cũ không ghi vào B và không xóa cờ busy của B
    @Test
    fun test8_OldOwnerFailureCallback_DoesNotClearBusyOrSetErrorOnReboundModel() {
        val saved = SavedStateHandle()
        val mainExecutor = ControllableExecutor()
        val fakeClient = FakeLessonApiClient()

        fakeClient.exceptionToThrow = IOException("Connection lost for owner A")

        var currentUser = 1
        val viewModel = LessonViewModel(
            app,
            saved,
            fakeClient,
            directExecutor,
            mainExecutor,
            { currentUser }
        )

        // 1. Bind A gặp lỗi, error callback của A được đưa vào mainExecutor queue
        viewModel.bind(1, "physics", 10)
        assertEquals(1, mainExecutor.tasks.size)
        assertTrue(viewModel.busy)

        // 2. Chuyển sang user B, API client phục hồi
        currentUser = 2
        fakeClient.exceptionToThrow = null
        fakeClient.responseToReturn = JSONObject().apply {
            put("id", "att_B_success")
            put("questions", JSONArray())
        }
        viewModel.bind(2, "physics", 10)
        assertEquals(2, mainExecutor.tasks.size)
        assertTrue("Model đang busy với request của B", viewModel.busy)

        // 3. Thực thi callback lỗi của A
        mainExecutor.runNext()

        // 4. KIỂM CHỨNG:
        // Lỗi của A không được gán vào error của model B
        assertNull("Lỗi của A không được ghi vào model B", viewModel.error)
        // Cờ busy của B không bị xóa
        assertTrue("Callback lỗi của A không được xóa busy của B", viewModel.busy)

        // 5. Thực thi callback của B
        mainExecutor.runNext()
        assertFalse(viewModel.busy)
        assertNull(viewModel.error)
        assertEquals("att_B_success", viewModel.attempt.optString("id"))
    }

    // 9. Yêu cầu 7C: API client / credential được scope theo đúng account, không lây lan SavedState
    @Test
    fun test9_ApiClientAndSavedState_ScopedPerOwner() {
        val clientA = FakeLessonApiClient()
        val clientB = FakeLessonApiClient()

        val factory = LessonViewModel.LessonApiClientFactory { acc ->
            if (acc == 1) clientA else clientB
        }

        val saved = SavedStateHandle()
        saved.set("attemptId", "att_owner_1_private")

        var currentUser = 1
        val viewModel = LessonViewModel(
            app,
            saved,
            factory,
            directExecutor,
            directExecutor,
            { currentUser }
        )

        // Bind account 1
        clientA.responseToReturn = JSONObject().apply {
            put("id", "att_owner_1_private")
            put("subjectId", "chemistry")
            put("grade", 11)
            put("questions", JSONArray())
        }
        viewModel.bind(1, "chemistry", 11)

        assertEquals(1, clientA.countCalls("GET", "/attempts/att_owner_1_private"))
        assertEquals(0, clientB.recordedRequests.size)

        // Rebind account 2
        currentUser = 2
        clientB.responseToReturn = JSONObject().apply {
            val enroll = JSONObject().put("id", "enr_2")
            put("enrollment", enroll)
            put("curriculumVersion", "v1")
        }
        viewModel.bind(2, "chemistry", 11)

        // Client A không nhận thêm request nào
        assertEquals(1, clientA.recordedRequests.size)

        // Client B được gọi cho account 2
        assertTrue(clientB.recordedRequests.size > 0)
        // Saved attemptId của A đã bị xóa khi rebind account 2, client B KHÔNG truy vấn attempt của A!
        assertEquals("Client B không được truy vấn attempt của account 1", 0, clientB.countCalls("GET", "/attempts/att_owner_1_private"))
    }

    // 10. Yêu cầu 7D: Bind lại cùng ngữ cảnh trong vòng đời là idempotent
    @Test
    fun test10_SameContextBind_IsIdempotentAcrossLifecycle() {
        val saved = SavedStateHandle()
        val fakeClient = FakeLessonApiClient()
        val attempt = JSONObject().apply {
            put("id", "att_idempotent")
            put("subjectId", "math")
            put("grade", 12)
            put("questions", JSONArray())
        }
        fakeClient.responseToReturn = attempt

        val viewModel = LessonViewModel(
            app,
            saved,
            fakeClient,
            directExecutor,
            directExecutor,
            { 1 }
        )

        saved.set("attemptId", "att_idempotent")
        viewModel.bind(1, "math", 12)
        assertEquals(1, fakeClient.countCalls("GET", "/attempts/att_idempotent"))

        // Bind lại cùng owner, subject, grade
        viewModel.bind(1, "math", 12)

        // Không tạo thêm request nào
        assertEquals(1, fakeClient.countCalls("GET", "/attempts/att_idempotent"))
        assertEquals(0, fakeClient.countCalls("POST", "/attempts"))
        assertEquals("att_idempotent", viewModel.attempt.optString("id"))
    }

    // 11. Yêu cầu 7E: Pending save của A không được retry bởi B
    @Test
    fun test11_PendingRequest_ScopedToOwner_NotRetriedByDifferentOwner() {
        val saved = SavedStateHandle()
        saved.set("pendingMethod", "PATCH")
        saved.set("pendingPath", "/attempts/att_A")
        saved.set("pendingBody", "{\"step\":1}")
        saved.set("pendingRevision", 1)
        saved.set("pendingKey", "key_A")
        saved.set("savedOwner", 1)

        val clientA = FakeLessonApiClient()
        val clientB = FakeLessonApiClient()
        val factory = LessonViewModel.LessonApiClientFactory { acc -> if (acc == 1) clientA else clientB }

        var currentUser = 1
        val viewModel = LessonViewModel(
            app,
            saved,
            factory,
            directExecutor,
            directExecutor,
            { currentUser }
        )

        // Owner 1 hasPending là true
        viewModel.bind(1, "physics", 10)
        // Khi chuyển sang owner 2:
        currentUser = 2
        viewModel.bind(2, "physics", 10)

        // hasPending của owner 2 phải là false
        assertFalse("Owner 2 không được thừa kế pending của owner 1", viewModel.hasPending())

        // Thử gọi retry() dưới owner 2
        viewModel.retry()

        // Client B tuyệt đối không được gửi PATCH /attempts/att_A
        assertEquals(0, clientB.countCalls("PATCH", "/attempts/att_A"))
    }

    // 12. Yêu cầu 7F: LiveData phát tín hiệu cập nhật mà không cần try-catch nuốt RuntimeException
    @Test
    fun test12_LiveData_EmitsProperlyWithoutCatchingRuntimeException() {
        val saved = SavedStateHandle()
        val fakeClient = FakeLessonApiClient()
        val attempt = JSONObject().apply {
            put("id", "att_livedata_test")
            put("questions", JSONArray())
        }
        fakeClient.responseToReturn = attempt

        val viewModel = LessonViewModel(
            app,
            saved,
            fakeClient,
            directExecutor,
            directExecutor,
            { 1 }
        )

        val observedVersions = mutableListOf<Int>()
        viewModel.changes().observeForever { version ->
            observedVersions.add(version)
        }

        saved.set("attemptId", "att_livedata_test")
        viewModel.bind(1, "physics", 10)

        // Observer nhận được version cập nhật (từ emit khi bắt đầu task và khi nhận kết quả)
        assertTrue("LiveData observer phải nhận được ít nhất 1 lần cập nhật", observedVersions.size > 1)
        assertTrue("Version phải tăng dần", observedVersions.last() > 0)
    }

    // 13. Yêu cầu 7A: Đổi môn - Owner 1, Vật lý 10 có attempt A; Bind Toán 10 không GET attempt A
    @Test
    fun test13_ChangeSubject_DoesNotFetchAttemptOfOldSubject() {
        val saved = SavedStateHandle()
        val fakeClient = FakeLessonApiClient()

        val attemptPhysics = JSONObject().apply {
            put("id", "att_physics_10")
            put("subject", "physics")
            put("grade", 10)
            put("questions", JSONArray())
        }
        val mathPath = JSONObject().apply {
            put("enrollment", JSONObject().put("id", "enr_math_10"))
            put("curriculumVersion", "v1")
        }

        var nextResp: JSONObject = attemptPhysics
        fakeClient.responseToReturn = nextResp

        val viewModel = LessonViewModel(
            app,
            saved,
            fakeClient,
            directExecutor,
            directExecutor,
            { 1 }
        )

        // 1. Khởi tạo và tải attempt môn Vật lý 10
        saved.set("attemptId", "att_physics_10")
        viewModel.bind(1, "physics", 10)
        assertEquals(1, fakeClient.countCalls("GET", "/attempts/att_physics_10"))
        assertEquals("att_physics_10", viewModel.attempt?.optString("id"))

        // 2. Chuyển sang môn Toán 10
        fakeClient.recordedRequests.clear()
        fakeClient.responseToReturn = mathPath
        viewModel.bind(1, "math", 10)

        // KIỂM CHỨNG:
        // a. Toán 10 TUYỆT ĐỐI KHÔNG gửi GET /attempts/att_physics_10
        assertEquals("Toán 10 không được fetch attempt của Vật lý", 0, fakeClient.countCalls("GET", "/attempts/att_physics_10"))
        // b. Request tải môn Toán phải gửi đúng subject = "math" và grade = 10
        val enrollReq = fakeClient.recordedRequests.firstOrNull { it.path == "/enrollments" }
        assertNotNull("Toán 10 phải gọi /enrollments để lấy path", enrollReq)
        assertEquals("math", enrollReq?.body?.optString("subjectId"))
        assertEquals(10, enrollReq?.body?.optInt("grade"))
        // c. State của model không chứa attempt của Vật lý
        assertNull("Model Toán 10 không được giữ attempt của Vật lý", viewModel.attempt)
        assertNotNull("Model Toán 10 phải có path", viewModel.path)
    }

    // 14. Yêu cầu 7B: Đổi lớp - Vật lý 10 có attempt A; Bind Vật lý 11 không dùng attempt/enrollKey lớp 10
    @Test
    fun test14_ChangeGrade_DoesNotFetchAttemptOrReuseEnrollmentKeyOfOldGrade() {
        val saved = SavedStateHandle()
        val fakeClient = FakeLessonApiClient()

        val attemptPhy10 = JSONObject().apply {
            put("id", "att_phy_grade10")
            put("subject", "physics")
            put("grade", 10)
            put("questions", JSONArray())
        }
        val phy11Path = JSONObject().apply {
            put("enrollment", JSONObject().put("id", "enr_phy_11"))
            put("curriculumVersion", "v1")
        }

        fakeClient.responseToReturn = attemptPhy10
        val viewModel = LessonViewModel(
            app,
            saved,
            fakeClient,
            directExecutor,
            directExecutor,
            { 1 }
        )

        saved.set("attemptId", "att_phy_grade10")
        viewModel.bind(1, "physics", 10)
        assertEquals(1, fakeClient.countCalls("GET", "/attempts/att_phy_grade10"))

        // Chuyển sang Vật lý 11
        fakeClient.recordedRequests.clear()
        fakeClient.responseToReturn = phy11Path
        viewModel.bind(1, "physics", 11)

        // KIỂM CHỨNG:
        // a. Không gọi attempt của lớp 10
        assertEquals(0, fakeClient.countCalls("GET", "/attempts/att_phy_grade10"))
        // b. Payload enrollment mang đúng grade = 11
        val enrollReq = fakeClient.recordedRequests.firstOrNull { it.path == "/enrollments" }
        assertNotNull(enrollReq)
        assertEquals(11, enrollReq?.body?.optInt("grade"))
        assertEquals("physics", enrollReq?.body?.optString("subjectId"))
        // c. Enrollment idempotency key không được dùng chung với lớp 10
        val enrollKeyPhy10 = saved.get<String>("1:physics:10:enrollKey")
        val enrollKeyPhy11 = enrollReq?.key
        assertNotEquals("Khóa enrollment của lớp 10 và 11 phải khác nhau", enrollKeyPhy10, enrollKeyPhy11)
    }

    // 15. Yêu cầu 7C: Tái hiện đúng lỗi độc lập - Vật lý 10 -> Toán 12
    @Test
    fun test15_ChangeBothSubjectAndGrade_ExactReproductionOfFailedIndependentTest() {
        val saved = SavedStateHandle()
        saved.set("savedOwner", 1)
        saved.set("attemptId", "physics10_attempt")

        val fakeClient = FakeLessonApiClient()
        fakeClient.responseToReturn = JSONObject().apply {
            put("id", "physics10_attempt")
            put("subject", "physics")
            put("grade", 10)
            put("questions", JSONArray())
        }

        val viewModel = LessonViewModel(
            app,
            saved,
            fakeClient,
            directExecutor,
            directExecutor,
            { 1 }
        )

        // 1. Bind Vật lý 10
        viewModel.bind(1, "physics", 10)
        assertEquals(1, fakeClient.countCalls("GET", "/attempts/physics10_attempt"))

        // 2. Bind Toán 12
        fakeClient.recordedRequests.clear()
        fakeClient.responseToReturn = JSONObject().apply {
            put("enrollment", JSONObject().put("id", "enr_math_12"))
            put("curriculumVersion", "v1")
        }
        viewModel.bind(1, "math", 12)

        // KIỂM CHỨNG: “Math 12 must not fetch Physics 10 attempt”
        assertEquals("Math 12 tuyệt đối không được fetch Physics 10 attempt", 0, fakeClient.countCalls("GET", "/attempts/physics10_attempt"))
        // Toán 12 phải gọi /enrollments với subjectId = math, grade = 12
        val mathEnroll = fakeClient.recordedRequests.firstOrNull { it.path == "/enrollments" }
        assertNotNull(mathEnroll)
        assertEquals("math", mathEnroll?.body?.optString("subjectId"))
        assertEquals(12, mathEnroll?.body?.optInt("grade"))
    }

    // 16. Yêu cầu 7D: Quay lại scope cũ - Vật lý 10 -> Toán 12 -> Vật lý 10 khôi phục đúng attempt Vật lý
    @Test
    fun test16_ReturnToOldScope_RestoresScopeAttemptWithoutCreatingNew() {
        val saved = SavedStateHandle()
        val fakeClient = FakeLessonApiClient()

        val phyAttempt = JSONObject().apply {
            put("id", "phy_10_attempt_persistent")
            put("subject", "physics")
            put("grade", 10)
            put("questions", JSONArray())
        }
        val mathPath = JSONObject().apply {
            put("enrollment", JSONObject().put("id", "enr_math_12"))
            put("curriculumVersion", "v1")
        }

        fakeClient.responseToReturn = phyAttempt
        val viewModel = LessonViewModel(
            app,
            saved,
            fakeClient,
            directExecutor,
            directExecutor,
            { 1 }
        )

        // 1. Vào Vật lý 10, bind rồi resume attempt
        viewModel.bind(1, "physics", 10)
        viewModel.resume("phy_10_attempt_persistent")
        assertEquals("phy_10_attempt_persistent", viewModel.attempt?.optString("id"))

        // 2. Chuyển sang Toán 12
        fakeClient.recordedRequests.clear()
        fakeClient.responseToReturn = mathPath
        viewModel.bind(1, "math", 12)
        assertNull("Ở Toán 12, attempt Vật lý không hiển thị", viewModel.attempt)

        // 3. Quay lại Vật lý 10
        fakeClient.recordedRequests.clear()
        fakeClient.responseToReturn = phyAttempt
        viewModel.bind(1, "physics", 10)

        // KIỂM CHỨNG:
        // a. Khôi phục đúng attempt Vật lý 10 qua GET /attempts/phy_10_attempt_persistent
        assertEquals(1, fakeClient.countCalls("GET", "/attempts/phy_10_attempt_persistent"))
        // b. Không tạo attempt mới (0 POST /attempts)
        assertEquals(0, fakeClient.countCalls("POST", "/attempts"))
        // c. Không gửi completion
        assertEquals(0, fakeClient.countCalls("POST", "/completions"))
        assertEquals("phy_10_attempt_persistent", viewModel.attempt?.optString("id"))
    }

    // 17. Yêu cầu 7E: Pending mutation của Vật lý 10 không được retry dưới Toán 12
    @Test
    fun test17_PendingMutation_ScopedPerSubjectGrade_NotSentUnderDifferentScope() {
        val saved = SavedStateHandle()
        val fakeClient = FakeLessonApiClient()

        val phyAttempt = JSONObject().apply {
            put("id", "phy_att_with_pending")
            put("subject", "physics")
            put("grade", 10)
            put("revision", 2)
            put("questions", JSONArray())
        }
        fakeClient.responseToReturn = phyAttempt

        val viewModel = LessonViewModel(
            app,
            saved,
            fakeClient,
            directExecutor,
            directExecutor,
            { 1 }
        )

        // Vào Vật lý 10
        viewModel.bind(1, "physics", 10)
        viewModel.resume("phy_att_with_pending")

        // Thực hiện lưu bước làm (tạo pending PATCH) nhưng mất mạng
        fakeClient.exceptionToThrow = IOException("Simulated network timeout during save")
        viewModel.save(2, "q101", "opt_C")
        assertTrue("Vật lý 10 phải có pending request khi lưu bị lỗi mạng", viewModel.hasPending())

        // Chuyển sang Toán 12
        fakeClient.exceptionToThrow = null
        fakeClient.recordedRequests.clear()
        fakeClient.responseToReturn = JSONObject().apply {
            put("enrollment", JSONObject().put("id", "enr_math_12"))
            put("curriculumVersion", "v1")
        }
        viewModel.bind(1, "math", 12)

        // KIỂM CHỨNG:
        // a. Tại Toán 12, hasPending() phải trả về false
        assertFalse("Toán 12 không được thừa kế pending của Vật lý 10", viewModel.hasPending())

        // b. Gọi retry() tại Toán 12: tuyệt đối không gửi PATCH /attempts/phy_att_with_pending
        viewModel.retry()
        assertEquals("Không được gửi PATCH của Vật lý trong scope Toán", 0, fakeClient.countCalls("PATCH", "/attempts/phy_att_with_pending"))

        // c. Quay lại Vật lý 10: pending của Vật lý 10 vẫn còn nguyên vẹn
        viewModel.bind(1, "physics", 10)
        assertTrue("Pending của Vật lý 10 phải được bảo toàn khi quay lại", viewModel.hasPending())
    }

    // 18. Yêu cầu 7H: Tái tạo SavedState qua vòng đời sản phẩm
    @Test
    fun test18_SavedStateRecreation_AcrossRebindAndProcessDeath() {
        val saved = SavedStateHandle()
        val fakeClient = FakeLessonApiClient()

        val phyAttempt = JSONObject().apply {
            put("id", "phy_lifecycle_att")
            put("subject", "physics")
            put("grade", 10)
            put("questions", JSONArray())
        }
        fakeClient.responseToReturn = phyAttempt

        val vm1 = LessonViewModel(
            app,
            saved,
            fakeClient,
            directExecutor,
            directExecutor,
            { 1 }
        )

        // Dùng action sản phẩm để thiết lập state
        vm1.bind(1, "physics", 10)
        vm1.resume("phy_lifecycle_att")

        // Tái tạo ViewModel mới từ cùng SavedStateHandle (mô phỏng Process Death)
        val vm2 = LessonViewModel(
            app,
            saved,
            fakeClient,
            directExecutor,
            directExecutor,
            { 1 }
        )

        // Nếu bind scope khác (Toán 12): không nhận nhầm
        fakeClient.recordedRequests.clear()
        fakeClient.responseToReturn = JSONObject().apply {
            put("enrollment", JSONObject().put("id", "enr_math_12"))
            put("curriculumVersion", "v1")
        }
        vm2.bind(1, "math", 12)
        assertEquals(0, fakeClient.countCalls("GET", "/attempts/phy_lifecycle_att"))
        assertNull(vm2.attempt)

        // Nếu bind đúng scope (Vật lý 10): khôi phục đúng attempt
        fakeClient.recordedRequests.clear()
        fakeClient.responseToReturn = phyAttempt
        vm2.bind(1, "physics", 10)
        assertEquals(1, fakeClient.countCalls("GET", "/attempts/phy_lifecycle_att"))
        assertEquals("phy_lifecycle_att", vm2.attempt?.optString("id"))
    }

    // 19. Yêu cầu 7: SavedState legacy có metadata môn khác không bị nhận nhầm
    @Test
    fun test19_LegacyUnscopedAttempt_WithSubjectMismatch_IsNotAdopted() {
        val saved = SavedStateHandle()
        // Trạng thái legacy đã lưu môn Vật lý 10
        saved.set("attemptId", "legacy_phy_att")
        saved.set("savedSubject", "physics")
        saved.set("savedGrade", 10)
        saved.set("savedOwner", 1)

        val fakeClient = FakeLessonApiClient()
        fakeClient.responseToReturn = JSONObject().apply {
            put("enrollment", JSONObject().put("id", "enr_chem_10"))
            put("curriculumVersion", "v1")
        }

        val viewModel = LessonViewModel(
            app,
            saved,
            fakeClient,
            directExecutor,
            directExecutor,
            { 1 }
        )

        // Bind môn Hóa 10: Do legacySubject là "physics" != "chemistry", attempt không được load
        viewModel.bind(1, "chemistry", 10)
        assertEquals(0, fakeClient.countCalls("GET", "/attempts/legacy_phy_att"))
        assertNull(viewModel.attempt)

        // Sau đó bind Vật lý 10: legacySubject khớp, attempt được load
        fakeClient.recordedRequests.clear()
        fakeClient.responseToReturn = JSONObject().apply {
            put("id", "legacy_phy_att")
            put("subject", "physics")
            put("grade", 10)
            put("questions", JSONArray())
        }
        viewModel.bind(1, "physics", 10)
        assertEquals(1, fakeClient.countCalls("GET", "/attempts/legacy_phy_att"))
        assertEquals("legacy_phy_att", viewModel.attempt?.optString("id"))
    }

    // 20. Tái hiện lỗi: SavedState legacy chưa có subject/grade, API backend thật không trả subject/grade
    // Model cũ nhận nhầm physics_attempt vào Toán 10 và xóa mất legacy pointer.
    @Test
    fun test20_ReproduceFailure_LegacyAttempt_AdoptedIntoWrongScope_DueToMissingMetadata() {
        val saved = SavedStateHandle()
        // SavedState legacy chỉ có savedOwner và attemptId, không có savedSubject/savedGrade
        saved.set("attemptId", "physics_attempt")
        saved.set("savedOwner", 1)

        val fakeClient = FakeLessonApiClient()
        fakeClient.customHandler = { method, path, _ ->
            when {
                method == "POST" && path == "/enrollments" -> {
                    // Scope Toán 10 có enrollmentId là "enr_math_10"
                    JSONObject().put("id", "enr_math_10").put("subjectId", "math").put("grade", 10)
                }
                method == "GET" && path == "/attempts/physics_attempt" -> {
                    // Hợp đồng backend thật: id, enrollmentId ("enr_phys_10"), questions; KHÔNG có subject hoặc grade
                    JSONObject().apply {
                        put("id", "physics_attempt")
                        put("enrollmentId", "enr_phys_10")
                        put("lessonId", "phys_optics_1")
                        put("curriculumVersion", "v1")
                        put("kind", "LESSON")
                        put("step", 1)
                        put("answers", JSONArray())
                        put("questions", JSONArray())
                        put("state", "IN_PROGRESS")
                        put("revision", 1)
                    }
                }
                method == "GET" && path.startsWith("/enrollments/enr_math_10/path") -> {
                    JSONObject().apply {
                        put("enrollment", JSONObject().put("id", "enr_math_10"))
                        put("curriculumVersion", "v1")
                        put("lessons", JSONArray())
                    }
                }
                else -> JSONObject()
            }
        }

        val viewModel = LessonViewModel(
            app,
            saved,
            fakeClient,
            directExecutor,
            directExecutor,
            { 1 }
        )

        // Bind vào Toán 10
        viewModel.bind(1, "math", 10)

        // KIỂM CHỨNG THEO YÊU CẦU:
        // 1. Attempt Vật lý 10 tuyệt đối KHÔNG được nhận vào UI scope Toán 10
        assertNull("Attempt Vật lý không được nhận vào UI của scope Toán 10", viewModel.attempt)

        // 2. Không được ghi đè physics_attempt vào scoped attemptId của Toán 10
        assertNull("Scoped attemptId của Toán 10 không được ghi nhận", saved.get("1:math:10:attemptId"))

        // 3. Phải bảo toàn legacy attemptId trong saved để khi vào Vật lý 10 khôi phục được
        assertEquals("Legacy attemptId phải được bảo toàn trong saved", "physics_attempt", saved.get("attemptId"))
    }

    // 21. Yêu cầu B: Legacy đúng scope (response backend thật không có subject/grade nhưng enrollmentId khớp)
    @Test
    fun test21_LegacyAttempt_WithMatchingScopeEnrollment_IsVerifiedAndAdopted() {
        val saved = SavedStateHandle()
        saved.set("attemptId", "math_legacy_att")
        saved.set("savedOwner", 1)

        val fakeClient = FakeLessonApiClient()
        fakeClient.customHandler = { method, path, _ ->
            when {
                method == "POST" && path == "/enrollments" -> {
                    JSONObject().put("id", "enr_math_10").put("subjectId", "math").put("grade", 10)
                }
                method == "GET" && path == "/attempts/math_legacy_att" -> {
                    // Backend thật: chỉ có enrollmentId khớp với scope Toán 10
                    JSONObject().apply {
                        put("id", "math_legacy_att")
                        put("enrollmentId", "enr_math_10")
                        put("lessonId", "math_u1")
                        put("curriculumVersion", "v1")
                        put("kind", "LESSON")
                        put("step", 3)
                        put("answers", JSONArray())
                        put("questions", JSONArray())
                        put("state", "IN_PROGRESS")
                        put("revision", 2)
                    }
                }
                else -> JSONObject()
            }
        }

        val viewModel = LessonViewModel(
            app,
            saved,
            fakeClient,
            directExecutor,
            directExecutor,
            { 1 }
        )

        viewModel.bind(1, "math", 10)

        // KIỂM CHỨNG:
        // 1. Attempt được nhận vào UI vì enrollmentId khớp
        assertNotNull("Attempt phải được nhận vào UI khi enrollment khớp", viewModel.attempt)
        assertEquals("math_legacy_att", viewModel.attempt?.optString("id"))
        assertEquals(3, viewModel.attempt?.optInt("step"))

        // 2. Scoped attemptId được ghi nhận
        assertEquals("math_legacy_att", saved.get("1:math:10:attemptId"))

        // 3. Sau khi xác minh thành công, attemptId được đồng bộ đúng với scope Toán 10
        assertEquals("math_legacy_att", saved.get("attemptId"))
        assertEquals("math", saved.get("savedSubject"))
        assertEquals(10, saved.get<Int>("savedGrade"))

        // 4. Không gọi tạo mới hay hoàn thành tự động
        assertEquals(0, fakeClient.countCalls("POST", "/attempts"))
        assertEquals(0, fakeClient.countCalls("POST", "/completions"))
    }

    // 22. Yêu cầu C: Khác lớp (cùng môn Vật lý, attempt lớp 10 nhưng bind lớp 11)
    @Test
    fun test22_LegacyAttempt_DifferentGrade_IsNotAdopted() {
        val saved = SavedStateHandle()
        saved.set("attemptId", "phy_10_att")
        saved.set("savedOwner", 1)

        val fakeClient = FakeLessonApiClient()
        fakeClient.customHandler = { method, path, _ ->
            when {
                method == "POST" && path == "/enrollments" -> {
                    // Scope Vật lý 11 có enrollmentId là "enr_phys_11"
                    JSONObject().put("id", "enr_phys_11").put("subjectId", "physics").put("grade", 11)
                }
                method == "GET" && path == "/attempts/phy_10_att" -> {
                    // Attempt thực tế thuộc lớp 10 (enrollmentId "enr_phys_10")
                    JSONObject().apply {
                        put("id", "phy_10_att")
                        put("enrollmentId", "enr_phys_10")
                        put("grade", 10)
                        put("questions", JSONArray())
                    }
                }
                method == "GET" && path.startsWith("/enrollments/enr_phys_11/path") -> {
                    JSONObject().apply {
                        put("enrollment", JSONObject().put("id", "enr_phys_11"))
                        put("curriculumVersion", "v1")
                        put("lessons", JSONArray())
                    }
                }
                else -> JSONObject()
            }
        }

        val viewModel = LessonViewModel(
            app,
            saved,
            fakeClient,
            directExecutor,
            directExecutor,
            { 1 }
        )

        // Bind vào Vật lý 11
        viewModel.bind(1, "physics", 11)

        // KIỂM CHỨNG:
        assertNull("Attempt lớp 10 không được nhận vào scope lớp 11", viewModel.attempt)
        assertNull("Scoped attemptId của Vật lý 11 không được ghi", saved.get("1:physics:11:attemptId"))
        assertEquals("Legacy attemptId phải được bảo toàn trong saved", "phy_10_att", saved.get("attemptId"))
        assertNotNull("Lộ trình của Vật lý 11 phải được nạp", viewModel.path)
    }

    // 23. Yêu cầu D: Không xác minh được (lỗi mạng hoặc response không hợp lệ)
    @Test
    fun test23_UnverifiedAttempt_OnNetworkFailure_PreservesLegacyState() {
        val saved = SavedStateHandle()
        saved.set("attemptId", "unverified_att")
        saved.set("savedOwner", 1)

        val fakeClient = FakeLessonApiClient()
        fakeClient.exceptionToThrow = IOException("Simulated network timeout during verification")

        val viewModel = LessonViewModel(
            app,
            saved,
            fakeClient,
            directExecutor,
            directExecutor,
            { 1 }
        )

        viewModel.bind(1, "math", 10)

        // KIỂM CHỨNG:
        assertEquals("NETWORK_ERROR", viewModel.error)
        assertNull("Không được nhận attempt mặc định khi mất mạng", viewModel.attempt)
        assertNull("Scoped state không được ghi nhận", saved.get("1:math:10:attemptId"))
        assertEquals("Legacy attemptId phải được bảo toàn để retry sau", "unverified_att", saved.get("attemptId"))
    }

    // 24. Yêu cầu E: Rebind trong lúc xác minh
    @Test
    fun test24_RebindDuringVerification_OldCallbackDoesNotCorruptNewScope() {
        val saved = SavedStateHandle()
        saved.set("attemptId", "slow_verify_att")
        saved.set("savedOwner", 1)

        val controllableWorker = ControllableExecutor()
        val controllableMain = ControllableExecutor()

        val fakeClient = FakeLessonApiClient()
        fakeClient.customHandler = { method, path, _ ->
            when {
                method == "POST" && path == "/enrollments" -> {
                    JSONObject().put("id", "enr_default")
                }
                method == "GET" && path == "/attempts/slow_verify_att" -> {
                    JSONObject().apply {
                        put("id", "slow_verify_att")
                        put("enrollmentId", "enr_phys_10")
                        put("subjectId", "physics")
                        put("grade", 10)
                        put("questions", JSONArray())
                    }
                }
                method == "GET" && path.contains("/path") -> {
                    JSONObject().apply {
                        put("enrollment", JSONObject().put("id", "enr_lit_12"))
                        put("curriculumVersion", "v1")
                        put("lessons", JSONArray())
                    }
                }
                else -> JSONObject()
            }
        }

        val viewModel = LessonViewModel(
            app,
            saved,
            fakeClient,
            controllableWorker,
            controllableMain,
            { 1 }
        )

        // Bắt đầu bind Toán 10 (generation 1)
        viewModel.bind(1, "math", 10)
        assertTrue(viewModel.busy)

        // Trong lúc tác vụ Toán 10 đang chờ executor, người dùng rebind sang Văn 12 (generation 2)
        viewModel.bind(1, "literature", 12)
        assertEquals("literature", viewModel.subject())
        assertEquals(12, viewModel.grade())

        // Chạy hết tất cả tác vụ nền và callback
        controllableWorker.runAll()
        controllableMain.runAll()

        // KIỂM CHỨNG: Callback cũ của Toán 10 không ghi đè state hay attempt của Văn 12
        assertNull("Văn 12 không nhận attempt của Vật lý", viewModel.attempt)
        assertNull("Scoped state của Văn 12 không bị corrupt", saved.get("1:literature:12:attemptId"))
    }

    // 25. Yêu cầu F: Quay lại và recreation sau khi mở nhầm scope
    @Test
    fun test25_OpenWrongScopeThenCorrectScope_RestoresAttemptSuccessfully() {
        val saved = SavedStateHandle()
        saved.set("attemptId", "bio_cross_att")
        saved.set("savedOwner", 1)

        val fakeClient = FakeLessonApiClient()
        fakeClient.customHandler = { method, path, _ ->
            when {
                method == "POST" && path == "/enrollments" -> {
                    JSONObject().put("id", "enr_bio_10").put("subjectId", "biology").put("grade", 10)
                }
                method == "GET" && path == "/attempts/bio_cross_att" -> {
                    JSONObject().apply {
                        put("id", "bio_cross_att")
                        put("enrollmentId", "enr_bio_10")
                        put("subjectId", "biology")
                        put("grade", 10)
                        put("step", 2)
                        put("questions", JSONArray())
                    }
                }
                method == "GET" && path.contains("/path") -> {
                    JSONObject().apply {
                        put("enrollment", JSONObject().put("id", "enr_scope"))
                        put("curriculumVersion", "v1")
                        put("lessons", JSONArray())
                    }
                }
                else -> JSONObject()
            }
        }

        val vm1 = LessonViewModel(
            app,
            saved,
            fakeClient,
            directExecutor,
            directExecutor,
            { 1 }
        )

        // 1. Mở Toán 10 (môn sai):
        vm1.bind(1, "math", 10)
        assertNull("Toán 10 không nhận attempt Sinh", vm1.attempt)
        assertEquals("Legacy attemptId phải được giữ nguyên", "bio_cross_att", saved.get("attemptId"))

        // 2. Chuyển sang Sinh học 10 (môn đúng):
        vm1.bind(1, "biology", 10)
        assertNotNull("Sinh học 10 phải khôi phục được attempt", vm1.attempt)
        assertEquals("bio_cross_att", vm1.attempt?.optString("id"))
        assertEquals("bio_cross_att", saved.get("1:biology:10:attemptId"))
        assertEquals("bio_cross_att", saved.get("attemptId"))
        assertEquals("biology", saved.get("savedSubject"))
        assertEquals(10, saved.get<Int>("savedGrade"))

        // 3. Tái tạo ViewModel từ SavedStateHandle (mô phỏng Process Death):
        val vm2 = LessonViewModel(
            app,
            saved,
            fakeClient,
            directExecutor,
            directExecutor,
            { 1 }
        )
        vm2.bind(1, "biology", 10)
        assertEquals("bio_cross_att", vm2.attempt?.optString("id"))
    }

    // 26. Yêu cầu G: Pending legacy không tự động gửi dưới scope mới
    @Test
    fun test26_PendingMutation_NeverSentUnderDifferentScope() {
        val saved = SavedStateHandle()
        // Thiết lập pending mutation trong scope Vật lý 10
        val phyScope = LessonViewModel.scopeKey(1, "physics", 10)
        saved.set("$phyScope:attemptId", "phy_pending_att")
        saved.set("$phyScope:pendingPath", "/attempts/phy_pending_att")
        saved.set("$phyScope:pendingMethod", "PATCH")
        saved.set("$phyScope:pendingBody", "{\"step\":2,\"answers\":[]}")
        saved.set("$phyScope:pendingRevision", 1)
        saved.set("$phyScope:pendingKey", "key_pending_phy")
        saved.set("savedOwner", 1)

        val fakeClient = FakeLessonApiClient()
        fakeClient.customHandler = { method, path, _ ->
            when {
                method == "POST" && path == "/enrollments" -> {
                    JSONObject().put("id", "enr_math_10")
                }
                method == "GET" && path.contains("/path") -> {
                    JSONObject().apply {
                        put("enrollment", JSONObject().put("id", "enr_math_10"))
                        put("curriculumVersion", "v1")
                        put("lessons", JSONArray())
                    }
                }
                else -> JSONObject()
            }
        }

        val viewModel = LessonViewModel(
            app,
            saved,
            fakeClient,
            directExecutor,
            directExecutor,
            { 1 }
        )

        // Bind sang Toán 10
        viewModel.bind(1, "math", 10)

        // KIỂM CHỨNG:
        // a. Tại Toán 10, hasPending() phải false
        assertFalse("Toán 10 không có pending", viewModel.hasPending())

        // b. Gọi retry() không được gửi PATCH /attempts/phy_pending_att
        viewModel.retry()
        assertEquals(0, fakeClient.countCalls("PATCH", "/attempts/phy_pending_att"))

        // c. Quay lại Vật lý 10: pending vẫn còn
        viewModel.bind(1, "physics", 10)
        assertTrue("Vật lý 10 vẫn còn pending", viewModel.hasPending())
    }

    // 27. Hợp đồng backend mới: Enriched DTO có subjectId và grade được xác minh trực tiếp
    @Test
    fun test27_EnrichedBackendDto_DirectScopeVerification() {
        val saved = SavedStateHandle()
        saved.set("attemptId", "enriched_chem_att")
        saved.set("savedOwner", 1)

        val fakeClient = FakeLessonApiClient()
        fakeClient.customHandler = { method, path, _ ->
            when {
                method == "GET" && path == "/attempts/enriched_chem_att" -> {
                    // DTO backend mới làm giàu với subjectId và grade
                    JSONObject().apply {
                        put("id", "enriched_chem_att")
                        put("enrollmentId", "enr_chem_10")
                        put("subjectId", "chemistry")
                        put("grade", 10)
                        put("questions", JSONArray())
                    }
                }
                else -> JSONObject()
            }
        }

        val viewModel = LessonViewModel(
            app,
            saved,
            fakeClient,
            directExecutor,
            directExecutor,
            { 1 }
        )

        viewModel.bind(1, "chemistry", 10)

        // KIỂM CHỨNG:
        assertNotNull("Attempt Hóa 10 được nhận trực tiếp qua enriched metadata", viewModel.attempt)
        assertEquals("enriched_chem_att", viewModel.attempt?.optString("id"))
        assertEquals("enriched_chem_att", saved.get("1:chemistry:10:attemptId"))
        assertEquals("enriched_chem_att", saved.get("attemptId"))
        assertEquals("chemistry", saved.get("savedSubject"))
        assertEquals(10, saved.get<Int>("savedGrade"))
        // Không cần gọi POST /enrollments phụ để xác minh vì metadata đã có sẵn
        assertEquals(0, fakeClient.countCalls("POST", "/enrollments"))
    }

    // 28. Tái hiện lỗi: Mở sai scope (Toán 10), từ chối nhận physics_legacy, sau đó mở phiên Toán (resume)
    // làm ghi đè flat attemptId; khi quay về Vật lý 10, con trỏ physics_legacy bị mất hoàn toàn.
    @Test
    fun test28_ReproduceFailure_LegacyCandidateWipedByOtherScopeAction() {
        val saved = SavedStateHandle()
        // SavedState ban đầu chỉ có savedOwner=1 và attemptId="physics_legacy", chưa có môn/lớp
        saved.set("attemptId", "physics_legacy")
        saved.set("savedOwner", 1)

        val fakeClient = FakeLessonApiClient()
        fakeClient.customHandler = { method, path, _ ->
            when {
                method == "POST" && path == "/enrollments" -> {
                    JSONObject().put("id", "enr_math_10").put("subjectId", "math").put("grade", 10)
                }
                method == "GET" && path == "/attempts/physics_legacy" -> {
                    // physics_legacy thuộc Vật lý 10
                    JSONObject().apply {
                        put("id", "physics_legacy")
                        put("enrollmentId", "enr_phys_10")
                        put("subjectId", "physics")
                        put("grade", 10)
                        put("step", 2)
                        put("questions", JSONArray())
                    }
                }
                method == "GET" && path.startsWith("/enrollments/enr_math_10/path") -> {
                    JSONObject().apply {
                        put("enrollment", JSONObject().put("id", "enr_math_10"))
                        put("curriculumVersion", "v1")
                        put("lessons", JSONArray())
                    }
                }
                method == "GET" && path == "/attempts/math_attempt" -> {
                    JSONObject().apply {
                        put("id", "math_attempt")
                        put("enrollmentId", "enr_math_10")
                        put("subjectId", "math")
                        put("grade", 10)
                        put("step", 1)
                        put("questions", JSONArray())
                    }
                }
                else -> JSONObject()
            }
        }

        val viewModel = LessonViewModel(
            app,
            saved,
            fakeClient,
            directExecutor,
            directExecutor,
            { 1 }
        )

        // 1. Mở Toán 10 (môn khác)
        viewModel.bind(1, "math", 10)
        assertNull("Toán 10 không được nhận physics_legacy", viewModel.attempt)

        // 2. Thao tác mở phiên ở Toán 10 qua action sản phẩm (resume)
        viewModel.resume("math_attempt")
        assertNotNull("Toán 10 đã nạp math_attempt", viewModel.attempt)
        assertEquals("math_attempt", viewModel.attempt?.optString("id"))

        // 3. Người dùng chuyển sang đúng môn Vật lý 10
        fakeClient.recordedRequests.clear()
        viewModel.bind(1, "physics", 10)

        // KIỂM CHỨNG:
        // Model phải gọi GET /attempts/physics_legacy và khôi phục đúng phiên
        assertEquals("Phải truy vấn đúng physics_legacy khi vào Vật lý 10", 1, fakeClient.countCalls("GET", "/attempts/physics_legacy"))
        assertNotNull("Vật lý 10 phải khôi phục được physics_legacy", viewModel.attempt)
        assertEquals("physics_legacy", viewModel.attempt?.optString("id"))
        assertEquals("1:physics:10:attemptId phải được scoped đúng", "physics_legacy", saved.get("1:physics:10:attemptId"))
    }

    // 29. Yêu cầu B: Action start() phiên mới ở môn khác không làm mất legacy candidate
    @Test
    fun test29_StartNewAttemptInOtherScope_PreservesLegacyCandidate() {
        val saved = SavedStateHandle()
        saved.set("attemptId", "physics_legacy_start")
        saved.set("savedOwner", 1)

        val fakeClient = FakeLessonApiClient()
        fakeClient.customHandler = { method, path, _ ->
            when {
                method == "POST" && path == "/enrollments" -> {
                    JSONObject().put("id", "enr_math_10").put("subjectId", "math").put("grade", 10)
                }
                method == "GET" && path == "/attempts/physics_legacy_start" -> {
                    JSONObject().apply {
                        put("id", "physics_legacy_start")
                        put("enrollmentId", "enr_phys_10")
                        put("subjectId", "physics")
                        put("grade", 10)
                        put("questions", JSONArray())
                    }
                }
                method == "GET" && path.startsWith("/enrollments/enr_math_10/path") -> {
                    JSONObject().apply {
                        put("enrollment", JSONObject().put("id", "enr_math_10"))
                        put("curriculumVersion", "v1")
                        put("lessons", JSONArray())
                    }
                }
                method == "POST" && path == "/attempts" -> {
                    JSONObject().apply {
                        put("id", "new_math_attempt")
                        put("enrollmentId", "enr_math_10")
                        put("subjectId", "math")
                        put("grade", 10)
                        put("questions", JSONArray())
                    }
                }
                else -> JSONObject()
            }
        }

        val viewModel = LessonViewModel(app, saved, fakeClient, directExecutor, directExecutor, { 1 })

        // 1. Mở Toán 10
        viewModel.bind(1, "math", 10)
        assertNull(viewModel.attempt)
        assertNotNull(viewModel.path)

        // 2. Khởi tạo phiên mới ở Toán 10 qua start()
        viewModel.start("math_unit_1", false)
        assertNotNull("Toán 10 đã tạo phiên mới", viewModel.attempt)
        assertEquals("new_math_attempt", viewModel.attempt?.optString("id"))

        // 3. Chuyển sang Vật lý 10
        fakeClient.recordedRequests.clear()
        viewModel.bind(1, "physics", 10)

        // KIỂM CHỨNG:
        assertEquals(1, fakeClient.countCalls("GET", "/attempts/physics_legacy_start"))
        assertNotNull(viewModel.attempt)
        assertEquals("physics_legacy_start", viewModel.attempt?.optString("id"))
        assertEquals("physics_legacy_start", saved.get("1:physics:10:attemptId"))
    }

    // 30. Yêu cầu C: backToPath() ở môn khác không làm mất legacy candidate
    @Test
    fun test30_BackToPathInOtherScope_DoesNotDeleteLegacyCandidate() {
        val saved = SavedStateHandle()
        saved.set("attemptId", "physics_legacy_b2p")
        saved.set("savedOwner", 1)

        val fakeClient = FakeLessonApiClient()
        fakeClient.customHandler = { method, path, _ ->
            when {
                method == "POST" && path == "/enrollments" -> {
                    JSONObject().put("id", "enr_math_10").put("subjectId", "math").put("grade", 10)
                }
                method == "GET" && path == "/attempts/physics_legacy_b2p" -> {
                    JSONObject().apply {
                        put("id", "physics_legacy_b2p")
                        put("enrollmentId", "enr_phys_10")
                        put("subjectId", "physics")
                        put("grade", 10)
                        put("questions", JSONArray())
                    }
                }
                method == "GET" && path.startsWith("/enrollments/enr_math_10/path") -> {
                    JSONObject().apply {
                        put("enrollment", JSONObject().put("id", "enr_math_10"))
                        put("curriculumVersion", "v1")
                        put("lessons", JSONArray())
                    }
                }
                method == "GET" && path == "/attempts/math_attempt_temp" -> {
                    JSONObject().apply {
                        put("id", "math_attempt_temp")
                        put("enrollmentId", "enr_math_10")
                        put("subjectId", "math")
                        put("grade", 10)
                        put("questions", JSONArray())
                    }
                }
                else -> JSONObject()
            }
        }

        val viewModel = LessonViewModel(app, saved, fakeClient, directExecutor, directExecutor, { 1 })

        viewModel.bind(1, "math", 10)
        viewModel.resume("math_attempt_temp")
        assertNotNull(viewModel.attempt)

        // Người dùng ấn backToPath() để thoát phiên Toán 10 về lộ trình
        viewModel.backToPath()
        assertNull("Toán 10 đã quay về lộ trình, attempt là null", viewModel.attempt)

        // Chuyển sang Vật lý 10
        fakeClient.recordedRequests.clear()
        viewModel.bind(1, "physics", 10)

        // KIỂM CHỨNG:
        assertEquals(1, fakeClient.countCalls("GET", "/attempts/physics_legacy_b2p"))
        assertNotNull("Vật lý 10 vẫn tìm thấy physics_legacy_b2p", viewModel.attempt)
        assertEquals("physics_legacy_b2p", viewModel.attempt?.optString("id"))
    }

    // 31. Yêu cầu D: Tái tạo ViewModel qua mô phỏng JVM (SavedStateHandle recreation)
    @Test
    fun test31_RecreationAcrossJvmSimulation_PreservesCandidateForCorrectScope() {
        val saved = SavedStateHandle()
        saved.set("attemptId", "physics_legacy_recreate")
        saved.set("savedOwner", 1)

        val fakeClient = FakeLessonApiClient()
        fakeClient.customHandler = { method, path, _ ->
            when {
                method == "POST" && path == "/enrollments" -> {
                    JSONObject().put("id", "enr_math_10").put("subjectId", "math").put("grade", 10)
                }
                method == "GET" && path == "/attempts/physics_legacy_recreate" -> {
                    JSONObject().apply {
                        put("id", "physics_legacy_recreate")
                        put("enrollmentId", "enr_phys_10")
                        put("subjectId", "physics")
                        put("grade", 10)
                        put("questions", JSONArray())
                    }
                }
                method == "GET" && path.startsWith("/enrollments/enr_math_10/path") -> {
                    JSONObject().apply {
                        put("enrollment", JSONObject().put("id", "enr_math_10"))
                        put("curriculumVersion", "v1")
                        put("lessons", JSONArray())
                    }
                }
                method == "GET" && path == "/attempts/math_active" -> {
                    JSONObject().apply {
                        put("id", "math_active")
                        put("enrollmentId", "enr_math_10")
                        put("subjectId", "math")
                        put("grade", 10)
                        put("questions", JSONArray())
                    }
                }
                else -> JSONObject()
            }
        }

        val vm1 = LessonViewModel(app, saved, fakeClient, directExecutor, directExecutor, { 1 })
        vm1.bind(1, "math", 10)
        vm1.resume("math_active")
        assertEquals("math_active", vm1.attempt?.optString("id"))

        // Mô phỏng tái tạo ViewModel trong JVM từ cùng SavedStateHandle
        val vm2 = LessonViewModel(app, saved, fakeClient, directExecutor, directExecutor, { 1 })

        // Chuyển sang Vật lý 10 trên ViewModel mới
        fakeClient.recordedRequests.clear()
        vm2.bind(1, "physics", 10)

        // KIỂM CHỨNG:
        assertEquals(1, fakeClient.countCalls("GET", "/attempts/physics_legacy_recreate"))
        assertNotNull(vm2.attempt)
        assertEquals("physics_legacy_recreate", vm2.attempt?.optString("id"))
    }

    // 32. Yêu cầu E: Lỗi mạng khi xác minh rồi sau đó quay lại thử lại khi có mạng
    @Test
    fun test32_NetworkFailureDuringVerification_PreservesCandidateForRetry() {
        val saved = SavedStateHandle()
        saved.set("attemptId", "physics_legacy_retry")
        saved.set("savedOwner", 1)

        val fakeClient = FakeLessonApiClient()

        val viewModel = LessonViewModel(app, saved, fakeClient, directExecutor, directExecutor, { 1 })

        // 1. Bind Toán 10 bị lỗi mạng khi xác minh legacy
        fakeClient.exceptionToThrow = IOException("Simulated network timeout")
        viewModel.bind(1, "math", 10)
        assertEquals("NETWORK_ERROR", viewModel.error)
        assertNull(viewModel.attempt)

        // 2. Mạng hồi phục, người dùng mở Hóa học 10
        fakeClient.exceptionToThrow = null
        fakeClient.customHandler = { method, path, _ ->
            when {
                method == "POST" && path == "/enrollments" -> {
                    JSONObject().put("id", "enr_chem_10").put("subjectId", "chemistry").put("grade", 10)
                }
                method == "GET" && path == "/attempts/physics_legacy_retry" -> {
                    JSONObject().apply {
                        put("id", "physics_legacy_retry")
                        put("enrollmentId", "enr_phys_10")
                        put("subjectId", "physics")
                        put("grade", 10)
                        put("questions", JSONArray())
                    }
                }
                method == "GET" && path.contains("/path") -> {
                    JSONObject().apply {
                        put("enrollment", JSONObject().put("id", "enr_chem_10"))
                        put("curriculumVersion", "v1")
                        put("lessons", JSONArray())
                    }
                }
                else -> JSONObject()
            }
        }
        viewModel.bind(1, "chemistry", 10)
        assertNull(viewModel.attempt)
        assertNotNull(viewModel.path)

        // 3. Người dùng chuyển sang đúng môn Vật lý 10
        fakeClient.recordedRequests.clear()
        viewModel.bind(1, "physics", 10)

        // KIỂM CHỨNG:
        assertEquals(1, fakeClient.countCalls("GET", "/attempts/physics_legacy_retry"))
        assertNotNull(viewModel.attempt)
        assertEquals("physics_legacy_retry", viewModel.attempt?.optString("id"))
        assertEquals("physics_legacy_retry", saved.get("1:physics:10:attemptId"))
    }

    // 33. Yêu cầu F: Cô lập candidate giữa Owner A và Owner B
    @Test
    fun test33_OwnerIsolation_CandidateNotAdoptedOrOverwrittenByDifferentOwner() {
        val saved = SavedStateHandle()
        // Legacy candidate thuộc về Owner 1
        saved.set("attemptId", "owner1_phys_legacy")
        saved.set("savedOwner", 1)

        val fakeClient = FakeLessonApiClient()
        fakeClient.customHandler = { method, path, _ ->
            when {
                method == "POST" && path == "/enrollments" -> {
                    JSONObject().put("id", "enr_math_10")
                }
                method == "GET" && path == "/attempts/owner1_phys_legacy" -> {
                    JSONObject().apply {
                        put("id", "owner1_phys_legacy")
                        put("enrollmentId", "enr_phys_10")
                        put("subjectId", "physics")
                        put("grade", 10)
                        put("questions", JSONArray())
                    }
                }
                method == "GET" && path == "/attempts/owner2_attempt" -> {
                    JSONObject().apply {
                        put("id", "owner2_attempt")
                        put("enrollmentId", "enr_math_10")
                        put("subjectId", "math")
                        put("grade", 10)
                        put("questions", JSONArray())
                    }
                }
                method == "GET" && path.contains("/path") -> {
                    JSONObject().apply {
                        put("enrollment", JSONObject().put("id", "enr_math_10"))
                        put("curriculumVersion", "v1")
                        put("lessons", JSONArray())
                    }
                }
                else -> JSONObject()
            }
        }

        var activeUser = 1
        val viewModel = LessonViewModel(app, saved, fakeClient, directExecutor, directExecutor, { activeUser })

        // Đổi sang Owner 2
        activeUser = 2
        viewModel.bind(2, "math", 10)
        // Owner 2 tuyệt đối không truy vấn attempt của Owner 1
        assertEquals(0, fakeClient.countCalls("GET", "/attempts/owner1_phys_legacy"))

        // Owner 2 mở phiên Toán
        viewModel.resume("owner2_attempt")
        assertEquals("owner2_attempt", viewModel.attempt?.optString("id"))

        // Đổi lại Owner 1 và vào Vật lý 10
        activeUser = 1
        fakeClient.recordedRequests.clear()
        viewModel.bind(1, "physics", 10)

        // KIỂM CHỨNG:
        assertEquals(1, fakeClient.countCalls("GET", "/attempts/owner1_phys_legacy"))
        assertNotNull(viewModel.attempt)
        assertEquals("owner1_phys_legacy", viewModel.attempt?.optString("id"))
    }

    // 34. Yêu cầu G: Callback cũ khi rebind không ghi đè scope mới
    @Test
    fun test34_StaleCallbackDuringRebind_DoesNotMutateNewScope() {
        val saved = SavedStateHandle()
        saved.set("attemptId", "slow_legacy_phy")
        saved.set("savedOwner", 1)

        val workerExec = ControllableExecutor()
        val mainExec = ControllableExecutor()

        val fakeClient = FakeLessonApiClient()
        fakeClient.customHandler = { method, path, _ ->
            when {
                method == "POST" && path == "/enrollments" -> {
                    JSONObject().put("id", "enr_bio_12")
                }
                method == "GET" && path == "/attempts/slow_legacy_phy" -> {
                    JSONObject().apply {
                        put("id", "slow_legacy_phy")
                        put("enrollmentId", "enr_phys_10")
                        put("subjectId", "physics")
                        put("grade", 10)
                        put("questions", JSONArray())
                    }
                }
                method == "GET" && path.contains("/path") -> {
                    JSONObject().apply {
                        put("enrollment", JSONObject().put("id", "enr_bio_12"))
                        put("curriculumVersion", "v1")
                        put("lessons", JSONArray())
                    }
                }
                else -> JSONObject()
            }
        }

        val viewModel = LessonViewModel(app, saved, fakeClient, workerExec, mainExec, { 1 })

        // Bắt đầu bind Vật lý 10 (generation 1)
        viewModel.bind(1, "physics", 10)
        assertTrue(viewModel.busy)

        // Trong lúc tác vụ đang xếp hàng, người dùng rebind sang Sinh học 12 (generation 2)
        viewModel.bind(1, "biology", 12)
        assertEquals("biology", viewModel.subject())

        // Chạy hết tất cả tác vụ nền
        workerExec.runAll()
        mainExec.runAll()

        // KIỂM CHỨNG: Callback cũ không ghi scoped attempt vào Sinh học 12
        assertNull("Sinh học 12 không nhận attempt của Vật lý", viewModel.attempt)
        assertNull(saved.get("1:biology:12:attemptId"))
    }

    // 35. Yêu cầu H: Hoàn tất migration - candidate được xử lý một lần, scoped pointer ổn định
    @Test
    fun test35_CompletedMigration_IsIdempotentAndUsesScopedPointer() {
        val saved = SavedStateHandle()
        saved.set("attemptId", "physics_done_migrated")
        saved.set("savedOwner", 1)

        val fakeClient = FakeLessonApiClient()
        fakeClient.customHandler = { method, path, _ ->
            when {
                method == "GET" && path == "/attempts/physics_done_migrated" -> {
                    JSONObject().apply {
                        put("id", "physics_done_migrated")
                        put("enrollmentId", "enr_phys_10")
                        put("subjectId", "physics")
                        put("grade", 10)
                        put("step", 3)
                        put("questions", JSONArray())
                    }
                }
                else -> JSONObject()
            }
        }

        val viewModel = LessonViewModel(app, saved, fakeClient, directExecutor, directExecutor, { 1 })

        // Lần 1: Bind Vật lý 10 -> xác minh và migrate thành công
        viewModel.bind(1, "physics", 10)
        assertEquals(1, fakeClient.countCalls("GET", "/attempts/physics_done_migrated"))
        assertEquals("physics_done_migrated", saved.get("1:physics:10:attemptId"))

        // Lần 2: Bind lại cùng scope -> idempotent, không gọi thêm GET hay POST
        fakeClient.recordedRequests.clear()
        viewModel.bind(1, "physics", 10)
        assertEquals(0, fakeClient.recordedRequests.size)

        // Lần 3: Tái tạo ViewModel mới từ SavedStateHandle đã migrate -> dùng thẳng scoped pointer
        val vm2 = LessonViewModel(app, saved, fakeClient, directExecutor, directExecutor, { 1 })
        fakeClient.recordedRequests.clear()
        vm2.bind(1, "physics", 10)
        assertEquals(1, fakeClient.countCalls("GET", "/attempts/physics_done_migrated"))
        assertEquals(0, fakeClient.countCalls("POST", "/attempts"))
        assertEquals(0, fakeClient.countCalls("POST", "/completions"))
        assertEquals("physics_done_migrated", vm2.attempt?.optString("id"))
    }
}
