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
    fun allActiveMembersCanUploadFileButOnlyOwnerCanDeleteProject() {
        val project = repository.createProject("owner", "Nhóm ôn tập", null)
        repository.addMember("owner", project.id, "member", TeamOsRepository.TeamRole.MEMBER)
        repository.addMember("owner", project.id, "viewer", TeamOsRepository.TeamRole.VIEWER)
        val input = TeamOsRepository.TeamFileInput("note.pdf", "application/pdf", byteArrayOf(1, 2))
        repository.uploadFileDocument("member", project.id, "Tài liệu", "", input)
        repository.uploadFileDocument("viewer", project.id, "Tài liệu khác", "", input)
        assertEquals(2, repository.getDocuments("owner", project.id).size)
        assertThrows(SecurityException::class.java) {
            repository.uploadFileDocument("outsider", project.id, "Lạ", "", input)
        }
        assertThrows(SecurityException::class.java) { repository.deleteProject("member", project.id, "test") }
        repository.deleteProject("owner", project.id, "test")
        assertTrue(repository.getProjectsForUser("member").isEmpty())
        assertThrows(NoSuchElementException::class.java) { repository.getProject("owner", project.id) }
    }

    @Test
    fun uploadRejectsUnsupportedOrOversizeFile() {
        assertThrows(IllegalArgumentException::class.java) {
            TeamOsRepository.TeamFileInput("bad.exe", "application/octet-stream", byteArrayOf(1))
        }
        assertThrows(IllegalArgumentException::class.java) {
            TeamOsRepository.TeamFileInput("huge.pdf", "application/pdf", ByteArray(8 * 1024 * 1024 + 1))
        }
    }

    @Test
    fun projectMembersCanAccessDocumentsAndNonMembersAreRejected() {
        val ownerId = "usr-101"
        val memberId = "usr-102"
        val outsiderId = "usr-999"

        val project = repository.createProject(
            ownerId = ownerId,
            title = "Dự án Năng lượng xanh",
            description = "Nghiên cứu về pin mặt trời"
        )

        repository.addMember(ownerId, project.id, memberId, TeamOsRepository.TeamRole.MEMBER)

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
    fun onlyOwnerAndAdminCanAddMembers() {
        val ownerId = "usr-owner"
        val adminId = "usr-admin"
        val memberId = "usr-member"
        val viewerId = "usr-viewer"
        val newUserId1 = "usr-new-1"
        val newUserId2 = "usr-new-2"

        val project = repository.createProject(ownerId, "Dự án phân quyền", "Kiểm thử vai trò")
        repository.addMember(ownerId, project.id, adminId, TeamOsRepository.TeamRole.ADMIN)
        repository.addMember(ownerId, project.id, memberId, TeamOsRepository.TeamRole.MEMBER)
        repository.addMember(ownerId, project.id, viewerId, TeamOsRepository.TeamRole.VIEWER)

        // Admin can add a member
        repository.addMember(adminId, project.id, newUserId1, TeamOsRepository.TeamRole.MEMBER)
        val membersAfterAdmin = repository.getMembers(ownerId, project.id)
        assertTrue(membersAfterAdmin.any { it.userId == newUserId1 })

        // Regular Member CANNOT add a member
        try {
            repository.addMember(memberId, project.id, newUserId2, TeamOsRepository.TeamRole.MEMBER)
            fail("Expected SecurityException when normal member tries to add member")
        } catch (e: SecurityException) {
            assertTrue(e.message!!.contains("PERMISSION_DENIED"))
        }

        // Viewer CANNOT add a member
        try {
            repository.addMember(viewerId, project.id, newUserId2, TeamOsRepository.TeamRole.MEMBER)
            fail("Expected SecurityException when viewer tries to add member")
        } catch (e: SecurityException) {
            assertTrue(e.message!!.contains("PERMISSION_DENIED"))
        }
    }

    @Test
    fun onlyOwnerAndAdminCanRemoveMembers() {
        val ownerId = "usr-owner"
        val adminId = "usr-admin"
        val memberId = "usr-member"

        val project = repository.createProject(ownerId, "Dự án xóa thành viên", null)
        repository.addMember(ownerId, project.id, adminId, TeamOsRepository.TeamRole.ADMIN)
        repository.addMember(ownerId, project.id, memberId, TeamOsRepository.TeamRole.MEMBER)

        // Regular member cannot remove another member
        try {
            repository.removeMember(memberId, project.id, adminId)
            fail("Expected SecurityException when normal member tries to remove someone")
        } catch (e: SecurityException) {
            assertTrue(e.message!!.contains("PERMISSION_DENIED"))
        }

        // Cannot remove the last OWNER
        try {
            repository.removeMember(ownerId, project.id, ownerId)
            fail("Expected IllegalStateException when trying to remove last owner")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("CANNOT_REMOVE_LAST_OWNER"))
        }

        // Admin can remove regular member
        repository.removeMember(adminId, project.id, memberId)
        val activeMembers = repository.getMembers(ownerId, project.id)
        assertFalse(activeMembers.any { it.userId == memberId })
    }

    @Test
    fun viewerRoleIsStrictlyReadOnly() {
        val ownerId = "usr-owner"
        val viewerId = "usr-viewer"

        val project = repository.createProject(ownerId, "Dự án xem", null)
        repository.addMember(ownerId, project.id, viewerId, TeamOsRepository.TeamRole.VIEWER)

        // Viewer can read project
        val readProj = repository.getProject(viewerId, project.id)
        assertEquals(project.id, readProj.id)

        // Viewer cannot upload document
        try {
            repository.uploadDocument(viewerId, project.id, "Báo cáo", "Nội dung")
            fail("Expected SecurityException for viewer upload")
        } catch (e: SecurityException) {
            assertTrue(e.message!!.contains("PERMISSION_DENIED"))
        }

        // Viewer cannot add requirement
        try {
            repository.addRequirement(viewerId, project.id, "Yêu cầu", "Mô tả")
            fail("Expected SecurityException for viewer addRequirement")
        } catch (e: SecurityException) {
            assertTrue(e.message!!.contains("PERMISSION_DENIED"))
        }

        // Viewer cannot create task
        try {
            repository.confirmTask(viewerId, project.id, "Làm slide", null, null)
            fail("Expected SecurityException for viewer confirmTask")
        } catch (e: SecurityException) {
            assertTrue(e.message!!.contains("PERMISSION_DENIED"))
        }
    }

    @Test
    fun selfDependencyIsForbidden() {
        val ownerId = "usr-owner"
        val project = repository.createProject(ownerId, "Dự án task dep", null)

        val task1 = repository.confirmTask(ownerId, project.id, "Nhiệm vụ 1", null, null)

        try {
            repository.addTaskDependency(ownerId, project.id, task1.id, task1.id)
            fail("Expected IllegalArgumentException for self-dependency")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("SELF_DEPENDENCY_FORBIDDEN"))
        }
    }

    @Test
    fun cyclicDependencyIsForbidden() {
        val ownerId = "usr-owner"
        val project = repository.createProject(ownerId, "Dự án task cyclic", null)

        val t1 = repository.confirmTask(ownerId, project.id, "Task 1", null, null)
        val t2 = repository.confirmTask(ownerId, project.id, "Task 2", null, null)
        val t3 = repository.confirmTask(ownerId, project.id, "Task 3", null, null)

        // t1 depends on t2 (t2 must finish before t1)
        repository.addTaskDependency(ownerId, project.id, t1.id, t2.id)

        // Direct cycle: t2 depends on t1 -> forbidden
        try {
            repository.addTaskDependency(ownerId, project.id, t2.id, t1.id)
            fail("Expected IllegalArgumentException for direct cycle")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("CYCLIC_DEPENDENCY_DETECTED"))
        }

        // Transitive chain: t2 depends on t3
        repository.addTaskDependency(ownerId, project.id, t2.id, t3.id)

        // Transitive cycle: t3 depends on t1 -> forbidden (t1 -> t2 -> t3 -> t1)
        try {
            repository.addTaskDependency(ownerId, project.id, t3.id, t1.id)
            fail("Expected IllegalArgumentException for transitive cycle")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("CYCLIC_DEPENDENCY_DETECTED"))
        }
    }

    @Test
    fun crossProjectDependencyIsForbidden() {
        val ownerId = "usr-owner"
        val p1 = repository.createProject(ownerId, "Project 1", null)
        val p2 = repository.createProject(ownerId, "Project 2", null)

        val t1 = repository.confirmTask(ownerId, p1.id, "Task in P1", null, null)
        val t2 = repository.confirmTask(ownerId, p2.id, "Task in P2", null, null)

        try {
            repository.addTaskDependency(ownerId, p1.id, t1.id, t2.id)
            fail("Expected IllegalArgumentException for cross-project dependency")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("CROSS_PROJECT_DEPENDENCY_FORBIDDEN"))
        }
    }

    @Test
    fun confirmTaskIsIdempotent() {
        val ownerId = "usr-owner"
        val project = repository.createProject(ownerId, "Dự án Idempotent", null)

        val clientReqId = "req-idempotent-uuid-001"

        val task1 = repository.confirmTask(
            userId = ownerId,
            projectId = project.id,
            title = "Viết dàn ý bài báo",
            description = "Nội dung chi tiết",
            assigneeId = ownerId,
            clientRequestId = clientReqId
        )

        // Repeating exact request with identical clientRequestId returns existing task without duplicating
        val task2 = repository.confirmTask(
            userId = ownerId,
            projectId = project.id,
            title = "Viết dàn ý bài báo",
            description = "Nội dung chi tiết",
            assigneeId = ownerId,
            clientRequestId = clientReqId
        )

        assertEquals(task1.id, task2.id)
        val tasks = repository.getTasks(ownerId, project.id)
        assertEquals(1, tasks.size)
    }

    @Test
    fun aiGapAnalysisGeneratesWarningsWithValidProvenance() {
        val ownerId = "usr-101"
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
    fun humanReviewWarningUpdatesProvenance() {
        val ownerId = "usr-101"
        val project = repository.createProject(ownerId, "Dự án thẩm định AI", null)
        repository.addRequirement(ownerId, project.id, "Báo cáo thực địa", "Chi tiết mẫu đất")
        repository.uploadDocument(ownerId, project.id, "Bản nháp", "Chỉ có lý thuyết")

        val warnings = repository.analyzeGaps(ownerId, project.id)
        assertEquals(1, warnings.size)
        val warning = warnings[0]
        assertEquals(TeamOsRepository.WarningStatus.NEEDS_CHECK, warning.status)
        assertNull(warning.reviewedBy)
        assertNull(warning.reviewedAt)

        // Member reviews warning and resolves it
        val reviewed = repository.reviewWarning(
            userId = ownerId,
            projectId = project.id,
            warningId = warning.id,
            decision = TeamOsRepository.WarningStatus.RESOLVED
        )

        assertEquals(TeamOsRepository.WarningStatus.RESOLVED, reviewed.status)
        assertEquals(ownerId, reviewed.reviewedBy)
        assertNotNull(reviewed.reviewedAt)
    }

    @Test
    fun removedMemberLosesAccessImmediately() {
        val ownerId = "usr-owner"
        val memberId = "usr-member"

        val project = repository.createProject(ownerId, "Dự án trục xuất", null)
        repository.addMember(ownerId, project.id, memberId, TeamOsRepository.TeamRole.MEMBER)

        // Member has access initially
        val docsInitial = repository.getDocuments(memberId, project.id)
        assertEquals(0, docsInitial.size)

        // Owner removes member
        repository.removeMember(ownerId, project.id, memberId)

        // Member loses access immediately
        try {
            repository.getDocuments(memberId, project.id)
            fail("Expected SecurityException after member was removed")
        } catch (e: SecurityException) {
            assertTrue(e.message!!.contains("ACCESS_DENIED"))
        }

        try {
            repository.uploadDocument(memberId, project.id, "Spam", "No access")
            fail("Expected SecurityException after member was removed")
        } catch (e: SecurityException) {
            assertTrue(e.message!!.contains("ACCESS_DENIED"))
        }
    }

    @Test
    fun userCacheIsolationOnAccountSwitch() {
        val userA = "usr-alice"
        val userB = "usr-bob"

        val pA = repository.createProject(userA, "Alice Project", null)
        val pB = repository.createProject(userB, "Bob Project", null)

        val aliceProjects = repository.getProjectsForUser(userA)
        assertEquals(1, aliceProjects.size)
        assertEquals(pA.id, aliceProjects[0].id)

        repository.clearUserCache(userA)

        val bobProjects = repository.getProjectsForUser(userB)
        assertEquals(1, bobProjects.size)
        assertEquals(pB.id, bobProjects[0].id)

        // Bob cannot access Alice's private project
        try {
            repository.getProject(userB, pA.id)
            fail("Expected SecurityException when Bob accesses Alice's project")
        } catch (e: SecurityException) {
            assertTrue(e.message!!.contains("ACCESS_DENIED"))
        }
    }
}
