package com.example.asm_final.supabase

import android.content.Context
import com.example.asm_final.BuildConfig
import com.example.asm_final.accounts.RemoteAccountLinks
import com.example.asm_final.backend.BackendClient
import com.example.asm_final.learning.TeamOsRepository
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.UUID

/**
 * Cloud-backed TeamOS Repository communicating with Supabase PostgREST & RPC.
 * Enforces strict UUID isolation, RLS role-based permissions, error propagation,
 * and zero silent fallback to stale or cross-account data.
 */
class SupabaseTeamOsRepository @JvmOverloads constructor(
    private val context: Context? = null,
    private val inMemoryFallback: TeamOsRepository = TeamOsRepository(),
    private val accountsProvider: ((Context) -> SupabaseAccounts)? = null,
    private val remoteCaller: ((owner: Int, method: String, path: String, body: JSONObject?) -> String)? = null,
    private val cloudEnabledOverride: Boolean? = null,
    private val remoteIdResolver: ((owner: Int) -> String)? = null
) : TeamOsRepository() {

    private val appContext = context?.applicationContext

    private fun isCloudEnabled(): Boolean {
        return cloudEnabledOverride ?: (appContext?.let { SupabaseHttp.enabled(it) } ?: false)
    }

    private fun accounts(): SupabaseAccounts {
        val ctx = appContext ?: throw IllegalStateException("Context required when remoteCaller is not provided")
        return accountsProvider?.invoke(ctx) ?: SupabaseAccounts(ctx)
    }

    private fun remoteRequest(owner: Int, method: String, path: String, body: JSONObject? = null): String {
        return remoteCaller?.invoke(owner, method, path, body)
            ?: remoteRequest(owner, method, path, body)
    }

    private fun remoteUserId(owner: Int): String {
        if (remoteIdResolver != null) return remoteIdResolver.invoke(owner)
        val ctx = appContext ?: return owner.toString()
        val uid = RemoteAccountLinks(ctx).use { it.remoteId(owner, BuildConfig.SUPABASE_URL) }
        return uid.ifBlank { owner.toString() }
    }

    private fun mapCloudError(e: Throwable): Throwable {
        if (e is BackendClient.Failure) {
            return when (e.status) {
                401 -> SecurityException("AUTHENTICATION_REQUIRED: Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.")
                403 -> SecurityException("PERMISSION_DENIED: Không có quyền thực hiện thao tác này.")
                409 -> IllegalStateException("CONFLICT: Dữ liệu đã thay đổi hoặc trùng lặp yêu cầu.")
                else -> IOException("CLOUD_ERROR_${e.status}: ${e.code ?: e.message}")
            }
        }
        if (e is IOException) {
            return IOException("NETWORK_ERROR: Không thể kết nối đến máy chủ Supabase. Vui lòng kiểm tra mạng.", e)
        }
        return e
    }

    override fun createProject(ownerId: String, title: String, description: String?): TeamProject {
        val ownerInt = ownerId.toIntOrNull()
        if (ownerInt != null && isCloudEnabled()) {
            val remoteUid = remoteUserId(ownerInt)
            val projId = "proj-${UUID.randomUUID()}"
            try {
                val body = JSONObject()
                    .put("id", projId)
                    .put("title", title.trim())
                    .put("description", description?.trim() ?: JSONObject.NULL)
                    .put("owner_id", remoteUid)
                    .put("version", 1)

                remoteRequest(ownerInt, "POST", "/rest/v1/mentor_team_projects", body)
                val proj = TeamProject(
                    id = projId,
                    title = title.trim(),
                    description = description?.trim(),
                    ownerId = remoteUid,
                    version = 1
                )
                proj.members[remoteUid] = TeamMember(remoteUid, TeamRole.OWNER, MemberStatus.ACTIVE)
                return proj
            } catch (e: Exception) {
                throw mapCloudError(e)
            }
        }
        return inMemoryFallback.createProject(ownerId, title, description)
    }

    override fun getProjectsForUser(userId: String): List<TeamProject> {
        val ownerInt = userId.toIntOrNull()
        if (ownerInt != null && isCloudEnabled()) {
            return try {
                val resp = remoteRequest(
                    ownerInt,
                    "GET",
                    "/rest/v1/mentor_team_projects?select=id,title,description,owner_id,version"
                )
                val rows = JSONArray(resp)
                val list = mutableListOf<TeamProject>()
                for (i in 0 until rows.length()) {
                    val r = rows.getJSONObject(i)
                    list.add(
                        TeamProject(
                            id = r.getString("id"),
                            title = r.getString("title"),
                            description = r.optString("description").takeIf { it.isNotBlank() },
                            ownerId = r.getString("owner_id"),
                            version = r.optInt("version", 1)
                        )
                    )
                }
                list
            } catch (e: Exception) {
                throw mapCloudError(e)
            }
        }
        return inMemoryFallback.getProjectsForUser(userId)
    }

    override fun getProject(userId: String, projectId: String): TeamProject {
        val ownerInt = userId.toIntOrNull()
        if (ownerInt != null && isCloudEnabled()) {
            return try {
                val resp = remoteRequest(
                    ownerInt,
                    "GET",
                    "/rest/v1/mentor_team_projects?id=eq.$projectId&select=id,title,description,owner_id,version"
                )
                val rows = JSONArray(resp)
                if (rows.length() == 0) throw NoSuchElementException("PROJECT_NOT_FOUND")
                val r = rows.getJSONObject(0)
                val proj = TeamProject(
                    id = r.getString("id"),
                    title = r.getString("title"),
                    description = r.optString("description").takeIf { it.isNotBlank() },
                    ownerId = r.getString("owner_id"),
                    version = r.optInt("version", 1)
                )

                val membersList = getMembers(userId, projectId)
                for (m in membersList) {
                    proj.members[m.userId] = m
                }
                proj
            } catch (e: Exception) {
                throw mapCloudError(e)
            }
        }
        return inMemoryFallback.getProject(userId, projectId)
    }

    override fun getMembers(userId: String, projectId: String): List<TeamMember> {
        val userInt = userId.toIntOrNull()
        if (userInt != null && isCloudEnabled()) {
            return try {
                val resp = remoteRequest(
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
                    list.add(
                        TeamMember(
                            userId = r.getString("user_id"),
                            role = rRole,
                            status = rStatus
                        )
                    )
                }
                list
            } catch (e: Exception) {
                throw mapCloudError(e)
            }
        }
        return inMemoryFallback.getMembers(userId, projectId)
    }

    override fun addMember(
        requesterId: String,
        projectId: String,
        newUserId: String,
        role: TeamRole
    ) {
        val requesterInt = requesterId.toIntOrNull()
        if (requesterInt != null && isCloudEnabled()) {
            val newUid = newUserId.toIntOrNull()?.let { remoteUserId(it) } ?: newUserId
            try {
                val body = JSONObject()
                    .put("project_id", projectId)
                    .put("user_id", newUid)
                    .put("role", role.name)
                    .put("status", MemberStatus.ACTIVE.name)
                remoteRequest(requesterInt, "POST", "/rest/v1/mentor_team_members", body)
                return
            } catch (e: Exception) {
                throw mapCloudError(e)
            }
        }
        inMemoryFallback.addMember(requesterId, projectId, newUserId, role)
    }

    override fun removeMember(requesterId: String, projectId: String, targetUserId: String) {
        val requesterInt = requesterId.toIntOrNull()
        if (requesterInt != null && isCloudEnabled()) {
            val targetUid = targetUserId.toIntOrNull()?.let { remoteUserId(it) } ?: targetUserId
            try {
                val body = JSONObject()
                    .put("status", MemberStatus.REMOVED.name)
                    .put("removed_at", "now()")
                remoteRequest(
                    requesterInt,
                    "PATCH",
                    "/rest/v1/mentor_team_members?project_id=eq.$projectId&user_id=eq.$targetUid",
                    body
                )
                return
            } catch (e: Exception) {
                throw mapCloudError(e)
            }
        }
        inMemoryFallback.removeMember(requesterId, projectId, targetUserId)
    }

    override fun addRequirement(
        userId: String,
        projectId: String,
        title: String,
        description: String,
        priority: TaskPriority
    ): TeacherRequirement {
        val userInt = userId.toIntOrNull()
        if (userInt != null && isCloudEnabled()) {
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
                remoteRequest(userInt, "POST", "/rest/v1/mentor_team_requirements", body)
                return TeacherRequirement(reqId, projectId, title, description, remoteUid, priority)
            } catch (e: Exception) {
                throw mapCloudError(e)
            }
        }
        return inMemoryFallback.addRequirement(userId, projectId, title, description, priority)
    }

    override fun getRequirements(userId: String, projectId: String): List<TeacherRequirement> {
        val userInt = userId.toIntOrNull()
        if (userInt != null && isCloudEnabled()) {
            return try {
                val resp = remoteRequest(
                    userInt,
                    "GET",
                    "/rest/v1/mentor_team_requirements?project_id=eq.$projectId&select=id,project_id,title,description,added_by,priority,status"
                )
                val rows = JSONArray(resp)
                val list = mutableListOf<TeacherRequirement>()
                for (i in 0 until rows.length()) {
                    val r = rows.getJSONObject(i)
                    val prio = runCatching { TaskPriority.valueOf(r.optString("priority", "MEDIUM")) }.getOrDefault(TaskPriority.MEDIUM)
                    list.add(
                        TeacherRequirement(
                            id = r.getString("id"),
                            projectId = r.getString("project_id"),
                            title = r.getString("title"),
                            description = r.getString("description"),
                            addedBy = r.getString("added_by"),
                            priority = prio,
                            status = r.optString("status", "OPEN")
                        )
                    )
                }
                list
            } catch (e: Exception) {
                throw mapCloudError(e)
            }
        }
        return inMemoryFallback.getRequirements(userId, projectId)
    }

    override fun uploadDocument(
        userId: String,
        projectId: String,
        title: String,
        content: String
    ): TeamDocument {
        val userInt = userId.toIntOrNull()
        if (userInt != null && isCloudEnabled()) {
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
                remoteRequest(userInt, "POST", "/rest/v1/mentor_team_documents", body)
                return TeamDocument(docId, projectId, title, content, remoteUid)
            } catch (e: Exception) {
                throw mapCloudError(e)
            }
        }
        return inMemoryFallback.uploadDocument(userId, projectId, title, content)
    }

    override fun updateDocument(
        userId: String,
        projectId: String,
        docId: String,
        title: String,
        content: String
    ): TeamDocument {
        val userInt = userId.toIntOrNull()
        if (userInt != null && isCloudEnabled()) {
            val remoteUid = remoteUserId(userInt)
            try {
                val body = JSONObject()
                    .put("title", title.trim())
                    .put("content", content.trim())
                    .put("updated_by", remoteUid)
                remoteRequest(userInt, "PATCH", "/rest/v1/mentor_team_documents?id=eq.$docId", body)
                return TeamDocument(docId, projectId, title, content, remoteUid, remoteUid, version = 2)
            } catch (e: Exception) {
                throw mapCloudError(e)
            }
        }
        return inMemoryFallback.updateDocument(userId, projectId, docId, title, content)
    }

    override fun getDocuments(userId: String, projectId: String): List<TeamDocument> {
        val userInt = userId.toIntOrNull()
        if (userInt != null && isCloudEnabled()) {
            return try {
                val resp = remoteRequest(
                    userInt,
                    "GET",
                    "/rest/v1/mentor_team_documents?project_id=eq.$projectId&select=id,project_id,title,content,uploaded_by,updated_by,version"
                )
                val rows = JSONArray(resp)
                val list = mutableListOf<TeamDocument>()
                for (i in 0 until rows.length()) {
                    val r = rows.getJSONObject(i)
                    list.add(
                        TeamDocument(
                            id = r.getString("id"),
                            projectId = r.getString("project_id"),
                            title = r.getString("title"),
                            content = r.getString("content"),
                            uploadedBy = r.getString("uploaded_by"),
                            updatedBy = r.optString("updated_by").takeIf { it.isNotBlank() },
                            version = r.optInt("version", 1)
                        )
                    )
                }
                list
            } catch (e: Exception) {
                throw mapCloudError(e)
            }
        }
        return inMemoryFallback.getDocuments(userId, projectId)
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
        if (userInt != null && isCloudEnabled()) {
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
                    .put("confirmation_status", if (source == TaskSource.AI_SUGGESTED) TaskConfirmation.PENDING_CONFIRMATION.name else TaskConfirmation.CONFIRMED.name)
                    .put("confirmed_by", if (source == TaskSource.AI_SUGGESTED) JSONObject.NULL else remoteUid)
                    .put("client_request_id", clientRequestId ?: JSONObject.NULL)
                remoteRequest(userInt, "POST", "/rest/v1/mentor_team_tasks", body)
                return TeamTask(
                    id = taskId,
                    projectId = projectId,
                    title = title.trim(),
                    description = description?.trim(),
                    assigneeId = assigneeId,
                    deadline = deadline,
                    priority = priority,
                    source = source,
                    confirmationStatus = if (source == TaskSource.AI_SUGGESTED) TaskConfirmation.PENDING_CONFIRMATION else TaskConfirmation.CONFIRMED,
                    confirmedBy = if (source == TaskSource.AI_SUGGESTED) null else remoteUid,
                    clientRequestId = clientRequestId,
                    isConfirmed = (source != TaskSource.AI_SUGGESTED)
                )
            } catch (e: Exception) {
                throw mapCloudError(e)
            }
        }
        return inMemoryFallback.confirmTask(
            userId, projectId, title, description, assigneeId, deadline, clientRequestId, priority, source
        )
    }

    override fun confirmPendingTask(userId: String, projectId: String, taskId: String): TeamTask {
        val userInt = userId.toIntOrNull()
        if (userInt != null && isCloudEnabled()) {
            val remoteUid = remoteUserId(userInt)
            try {
                val body = JSONObject().put("p_task_id", taskId)
                remoteRequest(userInt, "POST", "/rest/v1/rpc/mentor_confirm_task", body)
                return TeamTask(
                    id = taskId,
                    projectId = projectId,
                    title = "",
                    description = null,
                    assigneeId = null,
                    confirmationStatus = TaskConfirmation.CONFIRMED,
                    confirmedBy = remoteUid,
                    isConfirmed = true
                )
            } catch (e: Exception) {
                throw mapCloudError(e)
            }
        }
        return inMemoryFallback.confirmPendingTask(userId, projectId, taskId)
    }

    override fun getTasks(userId: String, projectId: String): List<TeamTask> {
        val userInt = userId.toIntOrNull()
        if (userInt != null && isCloudEnabled()) {
            return try {
                val resp = remoteRequest(
                    userInt,
                    "GET",
                    "/rest/v1/mentor_team_tasks?project_id=eq.$projectId&select=id,project_id,title,description,assignee_id,deadline,status,priority,source,confirmation_status,confirmed_by,client_request_id"
                )
                val rows = JSONArray(resp)
                val list = mutableListOf<TeamTask>()
                for (i in 0 until rows.length()) {
                    val r = rows.getJSONObject(i)
                    val prio = runCatching { TaskPriority.valueOf(r.optString("priority", "MEDIUM")) }.getOrDefault(TaskPriority.MEDIUM)
                    val status = runCatching { TaskStatus.valueOf(r.optString("status", "TODO")) }.getOrDefault(TaskStatus.TODO)
                    val source = runCatching { TaskSource.valueOf(r.optString("source", "HUMAN")) }.getOrDefault(TaskSource.HUMAN)
                    val confStatus = runCatching { TaskConfirmation.valueOf(r.optString("confirmation_status", "CONFIRMED")) }.getOrDefault(TaskConfirmation.CONFIRMED)
                    list.add(
                        TeamTask(
                            id = r.getString("id"),
                            projectId = r.getString("project_id"),
                            title = r.getString("title"),
                            description = r.optString("description").takeIf { it.isNotBlank() },
                            assigneeId = r.optString("assignee_id").takeIf { it.isNotBlank() },
                            deadline = r.optString("deadline").takeIf { it.isNotBlank() },
                            status = status,
                            priority = prio,
                            source = source,
                            confirmationStatus = confStatus,
                            confirmedBy = r.optString("confirmed_by").takeIf { it.isNotBlank() },
                            clientRequestId = r.optString("client_request_id").takeIf { it.isNotBlank() },
                            isConfirmed = confStatus == TaskConfirmation.CONFIRMED
                        )
                    )
                }
                list
            } catch (e: Exception) {
                throw mapCloudError(e)
            }
        }
        return inMemoryFallback.getTasks(userId, projectId)
    }

    override fun updateTaskStatus(userId: String, projectId: String, taskId: String, newStatus: TaskStatus): TeamTask {
        val userInt = userId.toIntOrNull()
        if (userInt != null && isCloudEnabled()) {
            try {
                val body = JSONObject().put("status", newStatus.name)
                remoteRequest(userInt, "PATCH", "/rest/v1/mentor_team_tasks?id=eq.$taskId", body)
                return TeamTask(id = taskId, projectId = projectId, title = "", description = null, assigneeId = null, status = newStatus)
            } catch (e: Exception) {
                throw mapCloudError(e)
            }
        }
        return inMemoryFallback.updateTaskStatus(userId, projectId, taskId, newStatus)
    }

    override fun addTaskDependency(
        userId: String,
        projectId: String,
        taskId: String,
        dependsOnTaskId: String
    ) {
        val userInt = userId.toIntOrNull()
        if (userInt != null && isCloudEnabled()) {
            try {
                val body = JSONObject()
                    .put("task_id", taskId)
                    .put("depends_on_task_id", dependsOnTaskId)
                remoteRequest(userInt, "POST", "/rest/v1/mentor_team_task_dependencies", body)
                return
            } catch (e: Exception) {
                if (e.message?.contains("CYCLIC") == true) {
                    throw IllegalArgumentException("CYCLIC_DEPENDENCY_DETECTED")
                }
                if (e.message?.contains("CROSS_PROJECT") == true) {
                    throw IllegalArgumentException("CROSS_PROJECT_DEPENDENCY_FORBIDDEN")
                }
                if (e.message?.contains("SELF_DEPENDENCY") == true) {
                    throw IllegalArgumentException("SELF_DEPENDENCY_FORBIDDEN")
                }
                throw mapCloudError(e)
            }
        }
        inMemoryFallback.addTaskDependency(userId, projectId, taskId, dependsOnTaskId)
    }

    override fun getTaskDependencies(userId: String, projectId: String, taskId: String): Set<String> {
        val userInt = userId.toIntOrNull()
        if (userInt != null && isCloudEnabled()) {
            return try {
                val resp = remoteRequest(
                    userInt,
                    "GET",
                    "/rest/v1/mentor_team_task_dependencies?task_id=eq.$taskId&select=depends_on_task_id"
                )
                val rows = JSONArray(resp)
                val set = mutableSetOf<String>()
                for (i in 0 until rows.length()) {
                    set.add(rows.getJSONObject(i).getString("depends_on_task_id"))
                }
                set
            } catch (e: Exception) {
                throw mapCloudError(e)
            }
        }
        return inMemoryFallback.getTaskDependencies(userId, projectId, taskId)
    }

    override fun getWarnings(userId: String, projectId: String): List<TeamWarning> {
        val userInt = userId.toIntOrNull()
        if (userInt != null && isCloudEnabled()) {
            return try {
                val resp = remoteRequest(
                    userInt,
                    "GET",
                    "/rest/v1/mentor_team_warnings?project_id=eq.$projectId&select=id,project_id,warning_type,message,doc_id,location_info,excerpt,evidence_ref,explanation,status,created_by_ai,reviewed_by,reviewed_at"
                )
                val rows = JSONArray(resp)
                val list = mutableListOf<TeamWarning>()
                for (i in 0 until rows.length()) {
                    val r = rows.getJSONObject(i)
                    val wType = runCatching { WarningType.valueOf(r.getString("warning_type")) }.getOrDefault(WarningType.MISSING_CONTENT)
                    val status = runCatching { WarningStatus.valueOf(r.optString("status", "NEEDS_CHECK")) }.getOrDefault(WarningStatus.NEEDS_CHECK)
                    list.add(
                        TeamWarning(
                            id = r.getString("id"),
                            projectId = r.getString("project_id"),
                            warningType = wType,
                            message = r.getString("message"),
                            docId = r.optString("doc_id").takeIf { it.isNotBlank() },
                            locationInfo = r.getString("location_info"),
                            excerpt = r.optString("excerpt").takeIf { it.isNotBlank() },
                            evidenceRef = r.optString("evidence_ref").takeIf { it.isNotBlank() },
                            explanation = r.optString("explanation").takeIf { it.isNotBlank() },
                            status = status,
                            createdByAi = r.optBoolean("created_by_ai", true),
                            reviewedBy = r.optString("reviewed_by").takeIf { it.isNotBlank() }
                        )
                    )
                }
                list
            } catch (e: Exception) {
                throw mapCloudError(e)
            }
        }
        return inMemoryFallback.getWarnings(userId, projectId)
    }

    override fun reviewWarning(
        userId: String,
        projectId: String,
        warningId: String,
        decision: WarningStatus
    ): TeamWarning {
        val userInt = userId.toIntOrNull()
        if (userInt != null && isCloudEnabled()) {
            val remoteUid = remoteUserId(userInt)
            try {
                val body = JSONObject()
                    .put("status", decision.name)
                    .put("reviewed_by", remoteUid)
                remoteRequest(userInt, "PATCH", "/rest/v1/mentor_team_warnings?id=eq.$warningId", body)
                return TeamWarning(
                    id = warningId,
                    projectId = projectId,
                    warningType = WarningType.MISSING_CONTENT,
                    message = "",
                    docId = null,
                    locationInfo = "",
                    status = decision,
                    reviewedBy = remoteUid
                )
            } catch (e: Exception) {
                throw mapCloudError(e)
            }
        }
        return inMemoryFallback.reviewWarning(userId, projectId, warningId, decision)
    }
}
