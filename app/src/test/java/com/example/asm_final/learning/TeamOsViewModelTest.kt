package com.example.asm_final.learning

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TeamOsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: TeamOsRepository
    private lateinit var viewModel: TeamOsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = TeamOsRepository()
        viewModel = TeamOsViewModel(repository, TestScope(testDispatcher), testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialStateIsProjectList() {
        assertEquals(TeamOsScreen.PROJECT_LIST, viewModel.screen)
        assertEquals(TeamOsTab.OVERVIEW, viewModel.currentTab)
        assertTrue(viewModel.projects.isEmpty())
        assertNull(viewModel.selectedProject)
    }

    @Test
    fun loadProjectsPopulatesList() {
        val ownerId = "usr-owner"
        repository.createProject(ownerId, "Dự án A", "Mô tả A")
        repository.createProject(ownerId, "Dự án B", "Mô tả B")

        viewModel.loadProjects(ownerId)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.projects.size)
        assertEquals("Dự án A", viewModel.projects[0].title)
    }

    @Test
    fun switchingUserResetsStateAndIsolatesCache() {
        val user1 = "usr-1"
        val user2 = "usr-2"

        val p1 = repository.createProject(user1, "Dự án User 1", null)
        repository.createProject(user2, "Dự án User 2", null)

        viewModel.loadProjects(user1)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.selectProject(p1)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(TeamOsScreen.PROJECT_DETAIL, viewModel.screen)
        assertEquals(p1.id, viewModel.selectedProject?.id)

        // Switch to user 2
        viewModel.loadProjects(user2)
        testDispatcher.scheduler.advanceUntilIdle()

        // Screen must be reset to PROJECT_LIST and selectedProject cleared
        assertEquals(TeamOsScreen.PROJECT_LIST, viewModel.screen)
        assertNull(viewModel.selectedProject)
        assertEquals(1, viewModel.projects.size)
        assertEquals("Dự án User 2", viewModel.projects[0].title)
    }

    @Test
    fun nonManagerCannotAddMember() {
        val ownerId = "usr-owner"
        val memberId = "usr-member"
        val p = repository.createProject(ownerId, "Dự án phân quyền", null)
        repository.addMember(ownerId, p.id, memberId, TeamOsRepository.TeamRole.MEMBER)

        // Login as regular member
        viewModel.loadProjects(memberId)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.selectProject(p)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.canManageMembers)

        viewModel.addMember("usr-newbie")
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.errorMessage)
        assertTrue(viewModel.errorMessage!!.contains("Chỉ Trưởng nhóm"))
    }

    @Test
    fun viewerCannotUploadDocument() {
        val ownerId = "usr-owner"
        val viewerId = "usr-viewer"
        val p = repository.createProject(ownerId, "Dự án chỉ xem", null)
        repository.addMember(ownerId, p.id, viewerId, TeamOsRepository.TeamRole.VIEWER)

        // Login as viewer
        viewModel.loadProjects(viewerId)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.selectProject(p)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.isViewer)
        assertFalse(viewModel.canEdit)

        viewModel.uploadDocument("Tài liệu mới", "Nội dung")
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.errorMessage)
        assertTrue(viewModel.errorMessage!!.contains("Người xem (VIEWER) không có quyền"))
    }

    @Test
    fun cyclicDependencySetsFriendlyErrorMessage() {
        val ownerId = "usr-owner"
        val p = repository.createProject(ownerId, "Dự án phụ thuộc", null)

        viewModel.loadProjects(ownerId)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.selectProject(p)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.createTask("Task 1", null, null, null)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.createTask("Task 2", null, null, null)
        testDispatcher.scheduler.advanceUntilIdle()

        val t1 = viewModel.tasks.first { it.title == "Task 1" }
        val t2 = viewModel.tasks.first { it.title == "Task 2" }

        viewModel.addTaskDependency(t1.id, t2.id)
        testDispatcher.scheduler.advanceUntilIdle()

        // Forming cycle: t2 depends on t1
        viewModel.addTaskDependency(t2.id, t1.id)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.errorMessage)
        assertTrue(viewModel.errorMessage!!.contains("Phát hiện phụ thuộc vòng"))
    }

    @Test
    fun gapAnalysisLoadsWarnings() {
        val ownerId = "usr-owner"
        val p = repository.createProject(ownerId, "Dự án AI", null)
        repository.addRequirement(ownerId, p.id, "Yêu cầu 1", "Phải có nội dung A")
        repository.uploadDocument(ownerId, p.id, "Tài liệu 1", "Nội dung hoàn toàn khác B")

        viewModel.loadProjects(ownerId)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.selectProject(p)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.runGapAnalysis()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.warnings.size)
        assertEquals(TeamOsRepository.WarningStatus.NEEDS_CHECK, viewModel.warnings[0].status)
    }

    @Test
    fun reviewWarningUpdatesStatus() {
        val ownerId = "usr-owner"
        val p = repository.createProject(ownerId, "Dự án AI Review", null)
        repository.addRequirement(ownerId, p.id, "Yêu cầu 1", "Nội dung A")
        repository.uploadDocument(ownerId, p.id, "Tài liệu 1", "Nội dung B")

        viewModel.loadProjects(ownerId)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.selectProject(p)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.runGapAnalysis()
        testDispatcher.scheduler.advanceUntilIdle()

        val warning = viewModel.warnings[0]
        viewModel.reviewWarning(warning.id, TeamOsRepository.WarningStatus.RESOLVED)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Đã xác nhận khắc phục cảnh báo", viewModel.actionSuccessMessage)
        val refreshed = viewModel.warnings.first { it.id == warning.id }
        assertEquals(TeamOsRepository.WarningStatus.RESOLVED, refreshed.status)
    }
}
