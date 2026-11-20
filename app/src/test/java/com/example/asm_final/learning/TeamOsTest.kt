package com.example.asm_final.learning

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TeamOsTest {

    private lateinit var repository: TeamOsRepository

    @Before
    fun setUp() {
        repository = TeamOsRepository()
    }

    @Test
    fun projectMembersCanAccessDocumentsAndNonMembersAreRejected() {
        val ownerId = 101
        val memberId = 102
        val outsiderId = 999

        val project = repository.createProject(
            ownerId = ownerId,
            title = "Dự án Năng lượng xanh",
            description = "Nghiên cứu về pin mặt trời"
        )

        repository.addMember(ownerId, project.id, memberId)

        // Member uploads document
        val doc = repository.uploadDocument(
            userId = memberId,
            projectId = project.id,
            title = "Báo cáo khảo sát",
            content = "Phần 1: Khảo sát thực địa tại mỏ pin mặt trời"
        )
        assertNotNull(doc.id)

        // Owner can access documents
        val ownerDocs = repository.getDocuments(ownerId, project.id)
        assertEquals(1, ownerDocs.size)

        // Member can access documents
        val memberDocs = repository.getDocuments(memberId, project.id)
        assertEquals(1, memberDocs.size)

        // Non-member is REJECTED at repository/server authorization check
        try {
            repository.getDocuments(outsiderId, project.id)
            fail("Expected SecurityException for outsider access attempt")
        } catch (e: SecurityException) {
            assertTrue(e.message!!.contains("ACCESS_DENIED"))
        }

        // Non-member cannot upload documents to private project
        try {
            repository.uploadDocument(outsiderId, project.id, "Tài liệu rác", "Nội dung hack")
            fail("Expected SecurityException for outsider upload attempt")
        } catch (e: SecurityException) {
            assertTrue(e.message!!.contains("ACCESS_DENIED"))
        }
    }

    @Test
    fun aiGapAnalysisGeneratesWarningsWithValidProvenance() {
        val ownerId = 101
        val project = repository.createProject(
            ownerId = ownerId,
            title = "Dự án Lịch sử 11",
            description = "Tìm hiểu Cách mạng Tháng Mười Nga"
        )

        repository.addRequirement(
            userId = ownerId,
            projectId = project.id,
            title = "Chuyên đề Đạo hàm",
            description = "Cần có bài giải mẫu các dạng đạo hàm lượng giác"
        )

        val doc = repository.uploadDocument(
            userId = ownerId,
            projectId = project.id,
            title = "Bản thảo 1",
            content = "Bài viết tổng quan về khởi nghĩa Petrograd tháng 10 năm 1917"
        )

        val warnings = repository.analyzeGaps(ownerId, project.id)
        assertEquals(1, warnings.size)

        val warning = warnings[0]
        assertEquals(TeamOsRepository.WarningType.MISSING_CONTENT, warning.warningType)
        assertEquals(TeamOsRepository.WarningStatus.NEEDS_CHECK, warning.status) // Flagged as NEEDS_CHECK
        assertEquals(doc.id, warning.docId) // Valid provenance: docId matches
        assertFalse(warning.locationInfo.isBlank()) // Valid provenance: location specified
    }

    @Test
    fun tasksAreCreatedOnlyAfterExplicitTeamConfirmation() {
        val ownerId = 101
        val project = repository.createProject(
            ownerId = ownerId,
            title = "Dự án Tiếng Anh 11",
            description = "Group Presentation on Global Warming"
        )

        // Confirm task creation
        val confirmedTask = repository.confirmTask(
            userId = ownerId,
            projectId = project.id,
            title = "Sửa lại slide phần giải pháp",
            description = "Bổ sung biểu đồ khí thải CO2",
            assigneeId = ownerId,
            deadline = "2026-12-01"
        )

        assertTrue(confirmedTask.isConfirmed)
        assertEquals(TeamOsRepository.TaskStatus.TODO, confirmedTask.status)

        val tasks = repository.getTasks(ownerId, project.id)
        assertEquals(1, tasks.size)
        assertTrue(tasks[0].isConfirmed)
    }
}
