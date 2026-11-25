package com.example.asm_final.supabase

import com.example.asm_final.backend.BackendClient
import com.example.asm_final.learning.TeamOsRepository
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class SupabaseTeamOsRepositoryTest {

    @Test
    fun cloudReturnsEmptyListDoesNotFallbackToOldAccountData() {
        var requestedPath: String? = null
        val inMemory = TeamOsRepository()
        inMemory.createProject(101, "Old Stale Project in RAM", "Should not be returned")

        val repository = SupabaseTeamOsRepository(
            inMemoryFallback = inMemory,
            cloudEnabledOverride = true,
            remoteIdResolver = { "00000000-0000-0000-0000-000000000101" },
            remoteCaller = { _, _, path, _ ->
                requestedPath = path
                "[]" // Cloud returns empty list
            }
        )

        // When cloud is enabled and returns empty, result must be empty, NEVER falling back to stale RAM
        val result = repository.getProjectsForUser("101")
        assertNotNull(requestedPath)
        assertTrue(result.isEmpty())
    }

    @Test
    fun cloud401ThrowsAuthenticationRequired() {
        val repository = SupabaseTeamOsRepository(
            cloudEnabledOverride = true,
            remoteIdResolver = { "00000000-0000-0000-0000-000000000101" },
            remoteCaller = { _, _, _, _ ->
                throw BackendClient.Failure("UNAUTHORIZED", 401)
            }
        )

        val ex = assertThrows(SecurityException::class.java) {
            repository.getProjectsForUser("101")
        }
        assertTrue(ex.message!!.contains("AUTHENTICATION_REQUIRED"))
    }

    @Test
    fun cloud403ThrowsPermissionDenied() {
        val repository = SupabaseTeamOsRepository(
            cloudEnabledOverride = true,
            remoteIdResolver = { "00000000-0000-0000-0000-000000000101" },
            remoteCaller = { _, _, _, _ ->
                throw BackendClient.Failure("FORBIDDEN", 403)
            }
        )

        val ex = assertThrows(SecurityException::class.java) {
            repository.createProject("101", "Dự án mới", null)
        }
        assertTrue(ex.message!!.contains("PERMISSION_DENIED"))
    }

    @Test
    fun cloud409ThrowsConflict() {
        val repository = SupabaseTeamOsRepository(
            cloudEnabledOverride = true,
            remoteIdResolver = { "00000000-0000-0000-0000-000000000101" },
            remoteCaller = { _, _, _, _ ->
                throw BackendClient.Failure("CONFLICT", 409)
            }
        )

        val ex = assertThrows(IllegalStateException::class.java) {
            repository.addMember("101", "proj-1", "user-2", TeamOsRepository.TeamRole.MEMBER)
        }
        assertTrue(ex.message!!.contains("CONFLICT"))
    }

    @Test
    fun networkLossThrowsNetworkErrorWithoutFallback() {
        val inMemory = TeamOsRepository()
        inMemory.createProject(101, "Stale in RAM", null)

        val repository = SupabaseTeamOsRepository(
            inMemoryFallback = inMemory,
            cloudEnabledOverride = true,
            remoteIdResolver = { "00000000-0000-0000-0000-000000000101" },
            remoteCaller = { _, _, _, _ ->
                throw IOException("Connection timed out")
            }
        )

        val ex = assertThrows(IOException::class.java) {
            repository.getProjectsForUser("101")
        }
        assertTrue(ex.message!!.contains("NETWORK_ERROR"))
    }

    @Test
    fun cloudDisabledUsesInMemory() {
        val inMemory = TeamOsRepository()
        val repository = SupabaseTeamOsRepository(
            inMemoryFallback = inMemory,
            cloudEnabledOverride = false
        )

        val project = repository.createProject("user-local", "Dự án Offline", "Mô tả offline")
        assertNotNull(project)
        assertEquals("Dự án Offline", project.title)

        val list = repository.getProjectsForUser("user-local")
        assertEquals(1, list.size)
        assertEquals(project.id, list[0].id)
    }

    @Test
    fun switchingUserIsolatesCache() {
        val repository = SupabaseTeamOsRepository(
            cloudEnabledOverride = false
        )

        val p1 = repository.createProject("user-A", "Dự án User A", null)
        val p2 = repository.createProject("user-B", "Dự án User B", null)

        val listA = repository.getProjectsForUser("user-A")
        val listB = repository.getProjectsForUser("user-B")

        // User A only sees p1, never p2
        assertEquals(1, listA.size)
        assertEquals(p1.id, listA[0].id)

        // User B only sees p2, never p1
        assertEquals(1, listB.size)
        assertEquals(p2.id, listB[0].id)

        // User A adds User B to p1 as MEMBER
        repository.addMember("user-A", p1.id, "user-B", TeamOsRepository.TeamRole.MEMBER)
        assertEquals(2, repository.getProjectsForUser("user-B").size)

        // User A removes User B from p1 -> User B immediately loses access to p1
        repository.removeMember("user-A", p1.id, "user-B")
        val updatedListB = repository.getProjectsForUser("user-B")
        assertEquals(1, updatedListB.size)
        assertEquals(p2.id, updatedListB[0].id)
    }
}
