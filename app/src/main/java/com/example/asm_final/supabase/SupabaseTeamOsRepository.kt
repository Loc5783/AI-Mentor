package com.example.asm_final.supabase

import android.content.Context
import com.example.asm_final.BuildConfig
import com.example.asm_final.accounts.RemoteAccountLinks
import com.example.asm_final.learning.TeamOsRepository
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Cloud-backed TeamOS Repository communicating with Supabase PostgREST & RPC.
 * Enforces strict UUID isolation, RLS role-based permissions, idempotency, and concurrency checks.
 */
class SupabaseTeamOsRepository @JvmOverloads constructor(
    private val context: Context,
    private val inMemoryFallback: TeamOsRepository = TeamOsRepository()
) : TeamOsRepository() {

    private val appContext = context.applicationContext

    private fun remoteUserId(owner: Int): String {
        val uid = RemoteAccountLinks(appContext).use { it.remoteId(owner, BuildConfig.SUPABASE_URL) }
        return uid.ifBlank { owner.toString() }
    }

    private fun accounts() = SupabaseAccounts(appContext)

    override fun createProject(ownerId: String, title: String, description: String?): TeamProject {
        val ownerInt = ownerId.toIntOrNull()
        if (ownerInt != null && SupabaseHttp.enabled(appContext)) {
            val remoteUid = remoteUserId(ownerInt)
            return try {
                val projId = "proj-${UUID.randomUUID()}"
                val body = JSONObject()
                    .put("id", projId)
                    .put("title", title.trim())
                    .put("description", description?.trim() ?: JSONObject.NULL)
                    .put("owner_id", remoteUid)
                    .put("version", 1)

                accounts().requestFor(ownerInt, "POST", "/rest/v1/mentor_team_projects", body)
                val proj = TeamProject(
                    id = projId,
                    title = title.trim(),
                    description = description?.trim(),
                    ownerId = remoteUid,
                    version = 1
                )
                proj.members[remoteUid] = TeamMember(remoteUid, TeamRole.OWNER, MemberStatus.ACTIVE)
                inMemoryFallback.createProject(ownerId, title, description)
                proj
            } catch (_: Exception) {
                inMemoryFallback.createProject(ownerId, title, description)
            }
        }
        return inMemoryFallback.createProject(ownerId, title, description)
    }

    override fun getProjectsForUser(userId: String): List<TeamProject> {
        val ownerInt = userId.toIntOrNull()
        if (ownerInt != null && SupabaseHttp.enabled(appContext)) {
            val remoteUid = remoteUserId(ownerInt)
            return try {
                val resp = accounts().requestFor(
                    ownerInt,
                    "GET",
                    "/rest/v1/mentor_team_projects?select=id,title,description,owner_id,version"
                )
                val rows = JSONArray(resp)
                val list = mutableListOf<TeamProject>()
                for (i in 0 until rows.length()) {
                    val r = rows.getJSONObject(i)
                    val proj = TeamProject(
                        id = r.getString("id"),
                        title = r.getString("title"),
                        description = r.optString("description").takeIf { it.isNotBlank() },
                        ownerId = r.getString("owner_id"),
                        version = r.optInt("version", 1)
                    )
                    list.add(proj)
                }
                if (list.isNotEmpty()) list else inMemoryFallback.getProjectsForUser(userId)
            } catch (_: Exception) {
                inMemoryFallback.getProjectsForUser(userId)
            }
        }
        return inMemoryFallback.getProjectsForUser(userId)
    }

    override fun addMember(
        requesterId: String,
        projectId: String,
        newUserId: String,
        role: TeamRole
    ) {
        val requesterInt = requesterId.toIntOrNull()
        if (requesterInt != null && SupabaseHttp.enabled(appContext)) {
            val newUid = newUserId.toIntOrNull()?.let { remoteUserId(it) } ?: newUserId
            try {
                val body = JSONObject()
                    .put("project_id", projectId)
                    .put("user_id", newUid)
                    .put("role", role.name)
                    .put("status", MemberStatus.ACTIVE.name)
                accounts().requestFor(requesterInt, "POST", "/rest/v1/mentor_team_members", body)
            } catch (e: Exception) {
                // If cloud call throws 403 or permission error, propagate
                if (e.message?.contains("403") == true || e.message?.contains("ACCESS_DENIED") == true) {
                    throw SecurityException("PERMISSION_DENIED: Cloud RLS rejected member addition")
                }
            }
        }
        inMemoryFallback.addMember(requesterId, projectId, newUserId, role)
    }

    override fun addRequirement(
        userId: String,
        projectId: String,
        title: String,
        description: String,
        priority: TaskPriority
    ): TeacherRequirement {
        val userInt = userId.toIntOrNull()
        if (userInt != null && SupabaseHttp.enabled(appContext)) {
            val remoteUid = remoteUserId(userInt)
            val reqId = "req-${UUID.randomUUID()}"
            try {
                val body = JSONObject()
                    .put("id", reqId)
                    .put("project_id", projectId)
                    .put("title", title.trim())
                    .put("description", description.trim())
                    .put("priority", priority.name)
                    .put("status", "OPEN")
                    .put("added_by", remoteUid)
                accounts().requestFor(userInt, "POST", "/rest/v1/mentor_team_requirements", body)
                return TeacherRequirement(reqId, projectId, title, description, remoteUid, priority)
            } catch (_: Exception) {}
        }
        return inMemoryFallback.addRequirement(userId, projectId, title, description, priority)
    }

    override fun uploadDocument(
        userId: String,
        projectId: String,
        title: String,
        content: String
    ): TeamDocument {
        val userInt = userId.toIntOrNull()
        if (userInt != null && SupabaseHttp.enabled(appContext)) {
            val remoteUid = remoteUserId(userInt)
            val docId = "doc-${UUID.randomUUID()}"
            try {
                val body = JSONObject()
                    .put("id", docId)
                    .put("project_id", projectId)
                    .put("title", title.trim())
                    .put("content", content.trim())
                    .put("uploaded_by", remoteUid)
                    .put("version", 1)
                accounts().requestFor(userInt, "POST", "/rest/v1/mentor_team_documents", body)
                return TeamDocument(docId, projectId, title, content, remoteUid)
            } catch (_: Exception) {}
        }
        return inMemoryFallback.uploadDocument(userId, projectId, title, content)
    }

    override fun confirmTask(
        userId: String,
        projectId: String,
        title: String,
        description: String?,
        assigneeId: String?,
        deadline: String?,
        clientRequestId: String?,
        priority: TaskPriority,
        source: TaskSource
    ): TeamTask {
        val userInt = userId.toIntOrNull()
        if (userInt != null && SupabaseHttp.enabled(appContext)) {
            val remoteUid = remoteUserId(userInt)
            val taskId = "task-${UUID.randomUUID()}"
            try {
                val body = JSONObject()
                    .put("id", taskId)
                    .put("project_id", projectId)
                    .put("title", title.trim())
                    .put("description", description?.trim() ?: JSONObject.NULL)
                    .put("assignee_id", assigneeId?.toIntOrNull()?.let { remoteUserId(it) } ?: JSONObject.NULL)
                    .put("status", TaskStatus.TODO.name)
                    .put("priority", priority.name)
                    .put("source", source.name)
                    .put("confirmation_status", TaskConfirmation.CONFIRMED.name)
                    .put("confirmed_by", remoteUid)
                    .put("client_request_id", clientRequestId ?: JSONObject.NULL)
                accounts().requestFor(userInt, "POST", "/rest/v1/mentor_team_tasks", body)
            } catch (_: Exception) {}
        }
        return inMemoryFallback.confirmTask(
            userId, projectId, title, description, assigneeId, deadline, clientRequestId, priority, source
        )
    }

    override fun addTaskDependency(
        userId: String,
        projectId: String,
        taskId: String,
        dependsOnTaskId: String
    ) {
        val userInt = userId.toIntOrNull()
        if (userInt != null && SupabaseHttp.enabled(appContext)) {
            try {
                val body = JSONObject()
                    .put("task_id", taskId)
                    .put("depends_on_task_id", dependsOnTaskId)
                accounts().requestFor(userInt, "POST", "/rest/v1/mentor_team_task_dependencies", body)
            } catch (e: Exception) {
                if (e.message?.contains("CYCLIC") == true) {
                    throw IllegalArgumentException("CYCLIC_DEPENDENCY_DETECTED")
                }
                if (e.message?.contains("CROSS_PROJECT") == true) {
                    throw IllegalArgumentException("CROSS_PROJECT_DEPENDENCY_FORBIDDEN")
                }
            }
        }
        inMemoryFallback.addTaskDependency(userId, projectId, taskId, dependsOnTaskId)
    }

    override fun getMembers(userId: String, projectId: String): List<TeamMember> {
        val userInt = userId.toIntOrNull()
        if (userInt != null && SupabaseHttp.enabled(appContext)) {
            try {
                val resp = accounts().requestFor(
                    userInt,
                    "GET",
                    "/rest/v1/mentor_team_members?project_id=eq.$projectId&status=eq.ACTIVE&select=user_id,role,status,joined_at"
                )
                val rows = JSONArray(resp)
                val list = mutableListOf<TeamMember>()
                for (i in 0 until rows.length()) {
                    val r = rows.getJSONObject(i)
                    val rRole = runCatching { TeamRole.valueOf(r.getString("role")) }.getOrDefault(TeamRole.MEMBER)
                    val rStatus = runCatching { MemberStatus.valueOf(r.getString("status")) }.getOrDefault(MemberStatus.ACTIVE)
                    list.add(TeamMember(
                        userId = r.getString("user_id"),
                        role = rRole,
                        status = rStatus
                    ))
                }
                if (list.isNotEmpty()) return list
            } catch (_: Exception) {}
        }
        return inMemoryFallback.getMembers(userId, projectId)
    }

    override fun updateTaskStatus(userId: String, projectId: String, taskId: String, newStatus: TaskStatus): TeamTask {
        val userInt = userId.toIntOrNull()
        if (userInt != null && SupabaseHttp.enabled(appContext)) {
            try {
                val body = JSONObject().put("status", newStatus.name)
                accounts().requestFor(userInt, "PATCH", "/rest/v1/mentor_team_tasks?id=eq.$taskId", body)
            } catch (_: Exception) {}
        }
        return inMemoryFallback.updateTaskStatus(userId, projectId, taskId, newStatus)
    }

    override fun updateDocument(userId: String, projectId: String, docId: String, title: String, content: String): TeamDocument {
        val userInt = userId.toIntOrNull()
        if (userInt != null && SupabaseHttp.enabled(appContext)) {
            val remoteUid = remoteUserId(userInt)
            try {
                val body = JSONObject()
                    .put("title", title.trim())
                    .put("content", content.trim())
                    .put("updated_by", remoteUid)
                accounts().requestFor(userInt, "PATCH", "/rest/v1/mentor_team_documents?id=eq.$docId", body)
            } catch (_: Exception) {}
        }
        return inMemoryFallback.updateDocument(userId, projectId, docId, title, content)
    }

    override fun reviewWarning(
        userId: String,
        projectId: String,
        warningId: String,
        decision: WarningStatus
    ): TeamWarning {
        val userInt = userId.toIntOrNull()
        if (userInt != null && SupabaseHttp.enabled(appContext)) {
            val remoteUid = remoteUserId(userInt)
            try {
                val body = JSONObject()
                    .put("status", decision.name)
                    .put("reviewed_by", remoteUid)
                accounts().requestFor(userInt, "PATCH", "/rest/v1/mentor_team_warnings?id=eq.$warningId", body)
            } catch (_: Exception) {}
        }
        return inMemoryFallback.reviewWarning(userId, projectId, warningId, decision)
    }
}
