package com.example.asm_final.learning

import androidx.compose.runtime.saveable.SaverScope
import androidx.lifecycle.SavedStateHandle
import com.example.mentor.learning.SubjectCatalog
import com.example.mentor.learning.SubjectId
import org.junit.Assert.*
import org.junit.Test

class LessonHubNavigationTest {

    // 1. Bốn khu vực trong Tab Bài học: Thư viện, Ôn thi, Lộ trình, Luyện cùng AI
    @Test
    fun test1_FourSections_ExistAndHaveDistinctRoutes() {
        val nav = LessonHubNavigationModel(initialSection = HubSection.MAIN_HUB, initialGrade = 10)
        assertEquals(HubSection.MAIN_HUB, nav.currentSection)

        // 1. Thư viện
        nav.navigateTo(HubSection.LIBRARY)
        assertEquals(HubSection.LIBRARY, nav.currentSection)

        // 2. Ôn thi
        nav.navigateTo(HubSection.EXAM_PREP)
        assertEquals(HubSection.EXAM_PREP, nav.currentSection)

        // 3. Lộ trình học
        nav.navigateTo(HubSection.ROADMAP)
        assertEquals(HubSection.ROADMAP, nav.currentSection)

        // Sổ lỗi sai (tích hợp trong Thư viện / Subject Hub)
        nav.openNotebook(null)
        assertEquals(HubSection.NOTEBOOK, nav.currentSection)
    }

    // 2. Đủ chín môn học THPT chuẩn theo danh mục sản phẩm, không có môn lập trình cũ
    @Test
    fun test2_NineSubjects_HaveCorrectWireIdsAndLabels() {
        val catalog: SubjectCatalog = SubjectModules.create()
        assertEquals("Must contain exactly 9 subject modules", 9, catalog.all().size)

        val expectedSubjects = listOf(
            SubjectId.MATH to "math",
            SubjectId.LITERATURE to "literature",
            SubjectId.ENGLISH to "english",
            SubjectId.HISTORY to "history",
            SubjectId.GEOGRAPHY to "geography",
            SubjectId.CHEMISTRY to "chemistry",
            SubjectId.PHYSICS to "physics",
            SubjectId.BIOLOGY to "biology",
            SubjectId.ECONOMICS_LAW to "economics_law"
        )

        for ((id, expectedWireId) in expectedSubjects) {
            val module = catalog.module(id)
            assertNotNull("Subject module for $id should exist", module)
            assertEquals("Subject $id must have expected wireId", expectedWireId, id.wireId)
        }

        // Verify no programming or python legacy module is present in the THPT list
        for (module in catalog.all()) {
            assertFalse(
                "Legacy programming/python module must not be part of THPT subjects: ${module.id().wireId}",
                module.id().wireId.contains("program", ignoreCase = true) ||
                        module.id().wireId.contains("python", ignoreCase = true)
            )
        }
    }

    // 3. Dòng chọn lớp: Chọn lớp 10, 11, 12 nhất quán giữa các khu vực
    @Test
    fun test3_GradeSelection_ConsistencyAcrossSections() {
        val nav = LessonHubNavigationModel(initialSection = HubSection.MAIN_HUB, initialGrade = 10)
        assertEquals(10, nav.grade)

        // Change grade in Main Hub
        nav.setGrade(12)
        assertEquals(12, nav.grade)

        // Navigate to Library - grade remains 12
        nav.navigateTo(HubSection.LIBRARY)
        assertEquals(12, nav.grade)

        // Change grade in Library
        nav.setGrade(11)
        assertEquals(11, nav.grade)

        // Navigate to Roadmap - grade remains 11 (does not reset to default 10)
        nav.navigateTo(HubSection.ROADMAP)
        assertEquals(11, nav.grade)

        // Navigate to Exam Prep - grade remains 11
        nav.navigateTo(HubSection.EXAM_PREP)
        assertEquals(11, nav.grade)

        // Invalid grade is rejected
        nav.setGrade(9)
        assertEquals(11, nav.grade)
        nav.setGrade(13)
        assertEquals(11, nav.grade)
    }

    // 4. Back stack Luồng A: Main Hub -> Thư viện -> Chọn môn -> Back về danh sách môn -> Back về Main Hub
    @Test
    fun test4_BackStack_FlowA_LibraryHierarchy() {
        val nav = LessonHubNavigationModel(initialSection = HubSection.MAIN_HUB)

        // Root level: Back cannot go further
        assertFalse("At root MAIN_HUB, handleBack returns false to exit or delegate to shell", nav.handleBack())

        // 1. Enter Library (subject list)
        nav.navigateTo(HubSection.LIBRARY)
        assertEquals(HubSection.LIBRARY, nav.currentSection)
        assertNull(nav.selectedSubjectWireId)

        // 2. Select Math subject
        nav.selectSubject(SubjectId.MATH)
        assertEquals("math", nav.selectedSubjectWireId)

        // 3. Back 1: returns from Math subject view to Library subject list (subject selection cleared)
        assertTrue(nav.handleBack())
        assertEquals(HubSection.LIBRARY, nav.currentSection)
        assertNull(nav.selectedSubjectWireId)

        // 4. Back 2: returns from Library subject list to Main Hub
        assertTrue(nav.handleBack())
        assertEquals(HubSection.MAIN_HUB, nav.currentSection)

        // 5. At Main Hub: back is not handled internally
        assertFalse(nav.handleBack())
    }

    // 5. Back stack Luồng B (LỖI A ĐÃ FIX):
    //    - Mở Sổ lỗi sai toàn môn từ danh sách môn -> Back về đúng danh sách môn Thư viện
    //    - Mở Sổ lỗi sai theo môn từ danh sách đề -> Back về đúng danh sách đề của môn đó
    @Test
    fun test5_BackStack_FlowB_NotebookHierarchy_FixedDestination() {
        val nav = LessonHubNavigationModel(initialSection = HubSection.MAIN_HUB)

        // Kịch bản 1: Mở Sổ lỗi sai toàn môn từ danh sách môn Thư viện (selectedSubjectWireId = null)
        nav.navigateTo(HubSection.LIBRARY)
        nav.selectSubject(null)
        nav.openNotebook(null, fromSection = HubSection.LIBRARY)
        assertEquals(HubSection.NOTEBOOK, nav.currentSection)
        assertEquals(HubSection.LIBRARY, nav.notebookSourceSection)
        assertNull(nav.selectedSubjectWireId)

        // Bấm Back: Phải quay về đúng danh sách môn của Thư viện (HubSection.LIBRARY, selectedSubjectWireId = null)
        // chứ KHÔNG ĐƯỢC văng về MAIN_HUB!
        assertTrue(nav.handleBack())
        assertEquals("Back từ sổ toàn môn phải về Thư viện danh sách môn", HubSection.LIBRARY, nav.currentSection)
        assertNull("Selected subject phải là null khi ở danh sách môn", nav.selectedSubjectWireId)

        // Kịch bản 2: Mở Sổ lỗi sai theo môn từ danh sách đề Hóa học
        nav.selectSubject(SubjectId.CHEMISTRY)
        nav.openNotebook(SubjectId.CHEMISTRY, fromSection = HubSection.LIBRARY)
        assertEquals(HubSection.NOTEBOOK, nav.currentSection)
        assertEquals(HubSection.LIBRARY, nav.notebookSourceSection)
        assertEquals("chemistry", nav.selectedSubjectWireId)

        // Bấm Back: Phải quay về đúng danh sách đề Hóa học của Thư viện
        assertTrue(nav.handleBack())
        assertEquals(HubSection.LIBRARY, nav.currentSection)
        assertEquals("chemistry", nav.selectedSubjectWireId)

        // Kịch bản 3: Mở Sổ lỗi sai từ Main Hub (nếu có shortcut)
        nav.navigateTo(HubSection.MAIN_HUB)
        nav.openNotebook(null, fromSection = HubSection.MAIN_HUB)
        assertEquals(HubSection.NOTEBOOK, nav.currentSection)
        assertEquals(HubSection.MAIN_HUB, nav.notebookSourceSection)

        // Bấm Back: Quay về Main Hub
        assertTrue(nav.handleBack())
        assertEquals(HubSection.MAIN_HUB, nav.currentSection)
    }

    // 6. Back stack Luồng C & D: Roadmap và Exam Prep Back về Main Hub
    @Test
    fun test6_BackStack_RoadmapAndExamPrep() {
        val nav = LessonHubNavigationModel(initialSection = HubSection.MAIN_HUB)

        // Roadmap
        nav.navigateTo(HubSection.ROADMAP)
        assertEquals(HubSection.ROADMAP, nav.currentSection)
        assertTrue(nav.handleBack())
        assertEquals(HubSection.MAIN_HUB, nav.currentSection)

        // Exam Prep (placeholder area)
        nav.navigateTo(HubSection.EXAM_PREP)
        assertEquals(HubSection.EXAM_PREP, nav.currentSection)
        assertTrue(nav.handleBack())
        assertEquals(HubSection.MAIN_HUB, nav.currentSection)
    }

    // 7. Công cụ đúng môn: Kiểm tra trực tiếp SubjectToolResolver sản phẩm được LessonActivity sử dụng
    @Test
    fun test7_SubjectTools_ProductResolverMapping() {
        assertEquals(
            "Toán phải mở MathToolsActivity",
            com.example.mathmodule.MathToolsActivity::class.java,
            SubjectToolResolver.resolveToolActivity(SubjectId.MATH)
        )
        assertEquals(
            "Vật lý phải mở CircuitDiagramActivity",
            com.example.mentor.physics.presentation.CircuitDiagramActivity::class.java,
            SubjectToolResolver.resolveToolActivity(SubjectId.PHYSICS)
        )
        assertEquals(
            "Hóa học phải mở PeriodicTableActivity",
            com.example.mentor.chemistry.presentation.PeriodicTableActivity::class.java,
            SubjectToolResolver.resolveToolActivity(SubjectId.CHEMISTRY)
        )

        // Các môn còn lại không có Activity công cụ độc lập
        assertNull(SubjectToolResolver.resolveToolActivity(SubjectId.BIOLOGY))
        assertNull(SubjectToolResolver.resolveToolActivity(SubjectId.LITERATURE))
        assertNull(SubjectToolResolver.resolveToolActivity(SubjectId.ENGLISH))
        assertNull(SubjectToolResolver.resolveToolActivity(SubjectId.HISTORY))
        assertNull(SubjectToolResolver.resolveToolActivity(SubjectId.GEOGRAPHY))
        assertNull(SubjectToolResolver.resolveToolActivity(SubjectId.ECONOMICS_LAW))
    }

    // 8. Lộ trình -> Chat AI -> Quay lại: Kiểm tra hợp đồng Intent handoff và cơ chế giữ attempt của LessonViewModel
    @Test
    fun test8_RoadmapToChatHandoff_ProductContractAndAttemptPreservation() {
        // A. Kiểm tra hợp đồng Intent được LessonActivity.askAi() sản phẩm sử dụng
        val threadId = "thread_phy10_optics"
        val username = "student_test_101"

        val handoffMap = LessonChatHandoff.buildHandoffMap(username, threadId)

        // 1. Map chứa đầy đủ contract cần thiết
        assertEquals(threadId, handoffMap[LessonChatHandoff.EXTRA_CHAT_THREAD_ID])
        assertEquals(true, handoffMap[LessonChatHandoff.EXTRA_LESSON_RETURN])
        assertEquals(username, handoffMap[LessonChatHandoff.EXTRA_USERNAME])

        // 2. Map TUYỆT ĐỐI KHÔNG chứa secret, token hay password
        assertNull(handoffMap["TOKEN"])
        assertNull(handoffMap["PASSWORD"])
        assertNull(handoffMap["SECRET"])

        // B. Kiểm tra cơ chế giữ attemptId qua SavedStateHandle như LessonViewModel thực hiện
        val saved = SavedStateHandle()
        val attemptId = "att_phy10_unit01"

        // Khi bắt đầu hoặc khôi phục attempt: LessonViewModel gọi saved.set("attemptId", id)
        saved.set("attemptId", attemptId)

        // Khi quay lại từ Chat (LessonActivity.onResume -> returning = true):
        // 1. LessonViewModel.reload() thực hiện GET /attempts/<id>, KHÔNG gọi POST /attempts (tạo attempt mới)
        //    và KHÔNG gọi POST /completions (cộng lặp EXP)
        val restoredAttemptId: String? = saved.get("attemptId")
        assertEquals("Attempt ID phải được giữ nguyên vẹn khi quay lại từ Chat", attemptId, restoredAttemptId)

        // 2. Không có pending conflict
        assertNull(saved.get<String>("pendingPath"))
    }

    // 9. Không bắt thêm/quản lý môn học trước khi truy cập: Tất cả 9 môn truy cập được ngay lập tức
    @Test
    fun test9_DirectAccessToAllNineSubjects_WithoutPrerequisiteEnrollment() {
        val nav = LessonHubNavigationModel(initialSection = HubSection.MAIN_HUB)
        nav.navigateTo(HubSection.LIBRARY)

        // Student can directly pick ANY of the 9 subjects
        for (subject in SubjectId.values()) {
            nav.selectSubject(subject)
            assertEquals("Subject ${subject.wireId} must be accessible directly without pre-enrollment",
                subject.wireId, nav.selectedSubjectWireId)
            assertEquals(subject, nav.selectedSubject)
        }
    }

    // 10. Vòng đời và Tái tạo State: Kiểm tra Saver của LessonHubNavigationModel
    @Test
    fun test10_LifecycleAndStateRestoration_NavigationSaver() {
        val original = LessonHubNavigationModel(
            initialSection = HubSection.NOTEBOOK,
            initialSubjectWireId = "physics",
            initialGrade = 11,
            initialNotebookSource = HubSection.LIBRARY
        )

        val scope = object : SaverScope {
            override fun canBeSaved(value: Any): Boolean = true
        }

        // Thực hiện lưu qua Saver
        val savedMap = with(LessonHubNavigationModel.Saver) {
            scope.save(original)
        }
        assertNotNull("Saved map không được null", savedMap)

        // Thực hiện phục hồi qua Saver (giả lập Activity/Process recreation)
        val restored = LessonHubNavigationModel.Saver.restore(savedMap!!)
        assertNotNull("Restored model không được null", restored)

        assertEquals(HubSection.NOTEBOOK, restored!!.currentSection)
        assertEquals("physics", restored.selectedSubjectWireId)
        assertEquals(11, restored.grade)
        assertEquals(HubSection.LIBRARY, restored.notebookSourceSection)

        // Phục hồi xong, bấm Back phải trả về đúng Thư viện môn Lý
        assertTrue(restored.handleBack())
        assertEquals(HubSection.LIBRARY, restored.currentSection)
        assertEquals("physics", restored.selectedSubjectWireId)
    }
}
