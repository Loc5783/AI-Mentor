package com.example.asm_final.supabase

import android.content.Context
import com.example.asm_final.BuildConfig
import com.example.asm_final.accounts.RemoteAccountLinks
import com.example.asm_final.learning.TeamOsRepository
import com.example.asm_final.backend.BackendClient
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Cloud-backed TeamOS Repository communicating with Supabase PostgREST & RPC.
 * Enforces strict UUID isolation, RLS role-based permissions, idempotency, and concurrency checks.
 */
class SupabaseTeamOsRepository(private val context: Context) : TeamOsRepository() {

    private val appContext = context.applicationContext

    private fun remoteUserId(owner: Int): String {
        val uid = RemoteAccountLinks(appContext).use { it.remoteId(owner, BuildConfig.SUPABASE_URL) }
        return uid.ifBlank { owner.toString() }
    }

    private fun localUserId(remoteId: String): String {
        val owner = RemoteAccountLinks(appContext).use { it.owner(BuildConfig.SUPABASE_URL, remoteId) }
        return if (owner > 0) owner.toString() else remoteId
    }

    private fun accounts() = SupabaseAccounts(appContext)

    private fun onlineUser(userId: String): Int {
        check(SupabaseHttp.enabled(appContext)) { "TEAMOS_REQUIRES_CONNECTION" }
        return userId.toIntOrNull() ?: throw IllegalArgumentException("INVALID_USER_ID")
    }

    private fun rows(userId: String, path: String): JSONArray =
        JSONArray(accounts().requestFor(onlineUser(userId), "GET", path))

    private fun projectRow(row: JSONObject): TeamProject = TeamProject(
        id = row.getString("id"), title = row.getString("title"),
        description = row.optString("description").takeUnless { it.isBlank() || it == "null" },
        ownerId = localUserId(row.getString("owner_id")), version = row.optInt("version", 1)
    )

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
                    ownerId = ownerId,
                    version = 1
                )
                proj.members[ownerId] = TeamMember(ownerId, TeamRole.OWNER, MemberStatus.ACTIVE)
                proj
            } catch (e: Exception) {
                throw e
            }
        }
        throw IllegalStateException("TEAMOS_REQUIRES_CONNECTION")
    }

    override fun getProjectsForUser(userId: String): List<TeamProject> {
        val ownerInt = userId.toIntOrNull()
        if (ownerInt != null && SupabaseHttp.enabled(appContext)) {
            runCatching { cleanDeletedProjects(ownerInt) }
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
                        ownerId = localUserId(r.getString("owner_id")),
                        version = r.optInt("version", 1)
                    )
                    list.add(proj)
                }
                list
            } catch (e: Exception) {
                throw e
            }
        }
        throw IllegalStateException("TEAMOS_REQUIRES_CONNECTION")
    }

    override fun getInvitations(userId: String): List<TeamInvitation> {
        val result = JSONArray(accounts().requestFor(onlineUser(userId), "POST",
            "/rest/v1/rpc/mentor_team_my_invites", JSONObject()))
        return (0 until result.length()).map { index ->
            val row = result.getJSONObject(index)
            TeamInvitation(row.getString("project_id"), row.getString("title"), TeamRole.valueOf(row.getString("role")))
        }
    }

    override fun respondToInvitation(userId: String, projectId: String, accept: Boolean) {
        accounts().requestFor(onlineUser(userId), "POST", "/rest/v1/rpc/mentor_team_respond_invite",
            JSONObject().put("p_project_id", projectId).put("p_accept", accept))
    }

    override fun getProject(userId: String, projectId: String): TeamProject {
        val result = rows(userId, "/rest/v1/mentor_team_projects?id=eq.$projectId&select=id,title,description,owner_id,version")
        if (result.length() == 0) throw NoSuchElementException("PROJECT_NOT_FOUND")
        return projectRow(result.getJSONObject(0))
    }

    override fun getRequirements(userId: String, projectId: String): List<TeacherRequirement> {
        val result = rows(userId, "/rest/v1/mentor_team_requirements?project_id=eq.$projectId&select=id,project_id,title,description,added_by,priority,status")
        return (0 until result.length()).map { i ->
            val r = result.getJSONObject(i)
            TeacherRequirement(r.getString("id"), projectId, r.getString("title"),
                r.getString("description"), r.getString("added_by"),
                TaskPriority.valueOf(r.optString("priority", "MEDIUM")), r.optString("status", "OPEN"))
        }
    }

    override fun getDocuments(userId: String, projectId: String): List<TeamDocument> {
        val result = rows(userId, "/rest/v1/mentor_team_documents?project_id=eq.$projectId&select=id,project_id,title,content,uploaded_by,updated_by,version,file_path,file_name,file_mime,file_size")
        return (0 until result.length()).map { i ->
            val r = result.getJSONObject(i)
            TeamDocument(r.getString("id"), projectId, r.getString("title"), r.getString("content"),
                r.getString("uploaded_by"), r.optString("updated_by").takeIf { it.isNotBlank() && it != "null" },
                r.optInt("version", 1),
                r.optString("file_path").takeUnless { it.isBlank() || it == "null" },
                r.optString("file_name").takeUnless { it.isBlank() || it == "null" },
                r.optString("file_mime").takeUnless { it.isBlank() || it == "null" },
                if (r.isNull("file_size")) null else r.getLong("file_size"))
        }
    }

    override fun getTasks(userId: String, projectId: String): List<TeamTask> {
        val result = rows(userId, "/rest/v1/mentor_team_tasks?project_id=eq.$projectId&select=id,project_id,title,description,assignee_id,deadline,status,priority,source,confirmation_status,confirmed_by,client_request_id,is_confirmed")
        return (0 until result.length()).map { i ->
            val r = result.getJSONObject(i)
            TeamTask(id = r.getString("id"), projectId = projectId, title = r.getString("title"),
                description = r.optString("description").takeIf { it.isNotBlank() && it != "null" },
                assigneeId = r.optString("assignee_id").takeIf { it.isNotBlank() && it != "null" }?.let(::localUserId),
                deadline = r.optString("deadline").takeIf { it.isNotBlank() && it != "null" },
                status = TaskStatus.valueOf(r.optString("status", "TODO")),
                priority = TaskPriority.valueOf(r.optString("priority", "MEDIUM")),
                source = TaskSource.valueOf(r.optString("source", "HUMAN")),
                confirmationStatus = TaskConfirmation.valueOf(r.optString("confirmation_status", "CONFIRMED")),
                confirmedBy = r.optString("confirmed_by").takeIf { it.isNotBlank() && it != "null" },
                clientRequestId = r.optString("client_request_id").takeIf { it.isNotBlank() && it != "null" },
                isConfirmed = r.optBoolean("is_confirmed", true))
        }
    }

    override fun getWarnings(userId: String, projectId: String): List<TeamWarning> {
        val result = rows(userId, "/rest/v1/mentor_team_warnings?project_id=eq.$projectId&select=id,project_id,warning_type,message,doc_id,location_info,excerpt,evidence_ref,explanation,status,created_by_ai,reviewed_by")
        return (0 until result.length()).map { i ->
            val r = result.getJSONObject(i)
            TeamWarning(id = r.getString("id"), projectId = projectId,
                warningType = WarningType.valueOf(r.getString("warning_type")), message = r.getString("message"),
                docId = r.optString("doc_id").takeIf { it.isNotBlank() && it != "null" },
                locationInfo = r.getString("location_info"),
                excerpt = r.optString("excerpt").takeIf { it.isNotBlank() && it != "null" },
                evidenceRef = r.optString("evidence_ref").takeIf { it.isNotBlank() && it != "null" },
                explanation = r.optString("explanation").takeIf { it.isNotBlank() && it != "null" },
                status = WarningStatus.valueOf(r.optString("status", "NEEDS_CHECK")),
                createdByAi = r.optBoolean("created_by_ai", true),
                reviewedBy = r.optString("reviewed_by").takeIf { it.isNotBlank() && it != "null" })
        }
    }

    override fun getTaskDependencies(userId: String, projectId: String, taskId: String): Set<String> {
        val result = rows(userId, "/rest/v1/mentor_team_task_dependencies?task_id=eq.$taskId&select=depends_on_task_id")
        return (0 until result.length()).map { result.getJSONObject(it).getString("depends_on_task_id") }.toSet()
    }

    override fun removeMember(requesterId: String, projectId: String, targetUserId: String) {
        val owner = onlineUser(requesterId)
        val uid = targetUserId.toIntOrNull()?.let { remoteUserId(it) } ?: targetUserId
        accounts().requestFor(owner, "POST", "/rest/v1/rpc/mentor_team_remove_member",
            JSONObject().put("p_project_id", projectId).put("p_user_id", uid))
    }

    override fun changeMemberRole(requesterId: String, projectId: String, targetUserId: String, role: TeamRole) {
        val owner = onlineUser(requesterId)
        val uid = targetUserId.toIntOrNull()?.let { remoteUserId(it) } ?: targetUserId
        accounts().requestFor(owner, "POST", "/rest/v1/rpc/mentor_team_change_role",
            JSONObject().put("p_project_id", projectId).put("p_user_id", uid).put("p_role", role.name))
    }

    override fun analyzeGaps(userId: String, projectId: String, aiResponseJson: String?): List<TeamWarning> {
        val reqs = getRequirements(userId, projectId)
        val docs = getDocuments(userId, projectId)
        val existing = getWarnings(userId, projectId)
        val missing = if (reqs.isEmpty() || docs.isEmpty()) {
            listOf("Chưa đủ yêu cầu hoặc tài liệu để đối chiếu.")
        } else reqs.filter { req ->
            val words = req.title.lowercase().split(" ").filter { it.length > 3 }
            docs.none { doc -> words.any { doc.content.lowercase().contains(it) } }
        }.map { "Yêu cầu '${it.title}' chưa thấy trong tài liệu." }
        val owner = onlineUser(userId)
        for (message in missing) {
            if (existing.any { it.message == message && it.status == WarningStatus.NEEDS_CHECK }) continue
            val body = JSONObject().put("id", "warning-${UUID.randomUUID()}")
                .put("project_id", projectId).put("warning_type", WarningType.MISSING_CONTENT.name)
                .put("message", message).put("location_info", "Toàn bộ dự án")
                .put("doc_id", docs.firstOrNull()?.id ?: JSONObject.NULL)
                .put("created_by_ai", false)
            accounts().requestFor(owner, "POST", "/rest/v1/mentor_team_warnings", body)
        }
        return getWarnings(userId, projectId)
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
                val body = JSONObject().put("p_project_id", projectId)
                    .put("p_user_id", newUid).put("p_role", role.name)
                accounts().requestFor(requesterInt, "POST", "/rest/v1/rpc/mentor_team_invite", body)
                return
            } catch (e: Exception) {
                throw e
            }
        }
        throw IllegalStateException("TEAMOS_REQUIRES_CONNECTION")
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
            } catch (e: Exception) { throw e }
        }
        throw IllegalStateException("TEAMOS_REQUIRES_CONNECTION")
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
            } catch (e: Exception) { throw e }
        }
        throw IllegalStateException("TEAMOS_REQUIRES_CONNECTION")
    }

    override fun uploadFileDocument(userId: String, projectId: String, title: String,
        content: String, file: TeamFileInput): TeamDocument {
        require(title.isNotBlank() && title.length <= 180)
        val owner = onlineUser(userId)
        val docId = "doc-${UUID.randomUUID()}"
        val path = SupabaseStorage(appContext).uploadTeamFile(owner, projectId, docId, file.bytes, file.mime)
        try {
            val body = JSONObject().put("id", docId).put("project_id", projectId)
                .put("title", title.trim()).put("content", content.trim())
                .put("uploaded_by", remoteUserId(owner)).put("version", 1)
                .put("file_path", path).put("file_name", file.name)
                .put("file_mime", file.mime).put("file_size", file.bytes.size)
            accounts().requestFor(owner, "POST", "/rest/v1/mentor_team_documents", body)
        } catch (error: Exception) {
            // A transport failure may happen after the server committed the row.
            // Only remove the object when the server definitively rejected metadata.
            if (error is BackendClient.Failure && error.status in 400..499) {
                runCatching { SupabaseStorage(appContext).deleteTeamFile(owner, path) }
            }
            throw error
        }
        return TeamDocument(docId, projectId, title.trim(), content.trim(), remoteUserId(owner),
            filePath = path, fileName = file.name, fileMime = file.mime, fileSize = file.bytes.size.toLong())
    }

    override fun downloadFile(userId: String, document: TeamDocument): java.io.File {
        val path = document.filePath ?: throw IllegalArgumentException("DOCUMENT_HAS_NO_FILE")
        val mime = document.fileMime ?: throw IllegalArgumentException("DOCUMENT_HAS_NO_FILE")
        val size = document.fileSize ?: throw IllegalArgumentException("DOCUMENT_HAS_NO_FILE")
        return SupabaseStorage(appContext).cacheTeamFile(onlineUser(userId), document.projectId,
            document.id, path, mime, size)
    }

    override fun deleteProject(userId: String, projectId: String, password: String) {
        val owner = onlineUser(userId)
        val project = getProject(userId, projectId)
        if (project.ownerId != userId) throw SecurityException("ONLY_PROJECT_OWNER_CAN_DELETE")
        val verifiedToken = accounts().reauthenticate(owner, password)
        val files = getDocuments(userId, projectId).filter { it.filePath != null }
        val paths = JSONArray(files.mapNotNull { it.filePath })
        val cleanup = JSONObject().put("project_id", projectId)
            .put("owner_id", remoteUserId(owner)).put("file_paths", paths)
        try {
            accounts().requestFor(owner, "POST", "/rest/v1/mentor_team_file_cleanup", cleanup)
        } catch (error: BackendClient.Failure) {
            if (error.status != 409) throw error
            accounts().requestFor(owner, "PATCH",
                "/rest/v1/mentor_team_file_cleanup?project_id=eq.$projectId",
                JSONObject().put("file_paths", paths))
        }

        // 1. Delete project database record first.
        // Once this succeeds, the project is removed from the database and UI instantly,
        // preventing any surviving project with broken file links.
        check(com.example.mentor.data.ActiveUser.get(appContext) == owner)
        SupabaseHttp().request("DELETE",
            "/rest/v1/mentor_team_projects?id=eq.$projectId&owner_id=eq.${remoteUserId(owner)}",
            token = verifiedToken)

        // Cleanup is durable and retried on the next project-list load.
        runCatching { cleanDeletedProjects(owner) }
    }

    private fun cleanDeletedProjects(owner: Int) {
        val jobs = JSONArray(accounts().requestFor(owner, "GET",
            "/rest/v1/mentor_team_file_cleanup?select=project_id,file_paths"))
        for (i in 0 until jobs.length()) {
            val job = jobs.getJSONObject(i)
            val projectId = job.getString("project_id")
            val existing = JSONArray(accounts().requestFor(owner, "GET",
                "/rest/v1/mentor_team_projects?id=eq.$projectId&select=id"))
            if (existing.length() != 0) continue
            val paths = job.getJSONArray("file_paths")
            for (j in 0 until paths.length()) {
                val path = paths.getString(j)
                try {
                    SupabaseStorage(appContext).deleteTeamFile(owner, path)
                } catch (error: BackendClient.Failure) {
                    if (error.status != 404) throw error
                }
                path.split('/').getOrNull(1)?.let { docId ->
                    SupabaseStorage(appContext).clearCachedTeamFile(owner, docId)
                }
            }
            accounts().requestFor(owner, "DELETE",
                "/rest/v1/mentor_team_file_cleanup?project_id=eq.$projectId")
        }
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
                    .put("assignee_id", assigneeId?.let { id ->
                        id.toIntOrNull()?.let(::remoteUserId) ?: id
                    } ?: JSONObject.NULL)
                    .put("status", TaskStatus.TODO.name)
                    .put("priority", priority.name)
                    .put("source", source.name)
                    .put("deadline", deadline ?: JSONObject.NULL)
                    .put("is_confirmed", true)
                    .put("confirmation_status", TaskConfirmation.CONFIRMED.name)
                    .put("confirmed_by", remoteUid)
                    .put("client_request_id", clientRequestId ?: JSONObject.NULL)
                accounts().requestFor(userInt, "POST", "/rest/v1/mentor_team_tasks", body)
                return TeamTask(id = taskId, projectId = projectId, title = title.trim(),
                    description = description?.trim(), assigneeId = assigneeId,
                    deadline = deadline, priority = priority, source = source,
                    confirmationStatus = TaskConfirmation.CONFIRMED, confirmedBy = remoteUid,
                    clientRequestId = clientRequestId, isConfirmed = true)
            } catch (e: Exception) { throw e }
        }
        throw IllegalStateException("TEAMOS_REQUIRES_CONNECTION")
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
                throw e
            }
            return
        }
        throw IllegalStateException("TEAMOS_REQUIRES_CONNECTION")
    }

    override fun getMembers(userId: String, projectId: String): List<TeamMember> {
        val userInt = userId.toIntOrNull()
        if (userInt != null && SupabaseHttp.enabled(appContext)) {
            try {
                val resp = accounts().requestFor(
                    userInt,
                    "GET",
                    "/rest/v1/mentor_team_members?project_id=eq.$projectId&status=in.(ACTIVE,INVITED)&select=user_id,role,status,joined_at"
                )
                val rows = JSONArray(resp)
                val ids = (0 until rows.length()).map { rows.getJSONObject(it).getString("user_id") }
                val names = if (ids.isEmpty()) emptyMap() else runCatching {
                    val profiles = JSONArray(accounts().requestFor(userInt, "GET",
                        "/rest/v1/mentor_profiles?id=in.(${ids.joinToString(",")})&select=id,nickname"))
                    (0 until profiles.length()).associate { index ->
                        val profile = profiles.getJSONObject(index)
                        profile.getString("id") to profile.getString("nickname")
                    }
                }.getOrDefault(emptyMap())
                val list = mutableListOf<TeamMember>()
                for (i in 0 until rows.length()) {
                    val r = rows.getJSONObject(i)
                    val rRole = runCatching { TeamRole.valueOf(r.getString("role")) }.getOrDefault(TeamRole.MEMBER)
                    val rStatus = runCatching { MemberStatus.valueOf(r.getString("status")) }.getOrDefault(MemberStatus.ACTIVE)
                    list.add(TeamMember(
                        userId = localUserId(r.getString("user_id")),
                        role = rRole,
                        status = rStatus,
                        displayName = names[r.getString("user_id")]
                    ))
                }
                return list
            } catch (e: Exception) { throw e }
        }
        throw IllegalStateException("TEAMOS_REQUIRES_CONNECTION")
    }

    override fun updateTaskStatus(userId: String, projectId: String, taskId: String, newStatus: TaskStatus): TeamTask {
        val userInt = userId.toIntOrNull()
        if (userInt != null && SupabaseHttp.enabled(appContext)) {
            try {
                val body = JSONObject().put("status", newStatus.name)
                accounts().requestFor(userInt, "PATCH", "/rest/v1/mentor_team_tasks?id=eq.$taskId&project_id=eq.$projectId", body)
                return getTasks(userId, projectId).firstOrNull { it.id == taskId }
                    ?: throw NoSuchElementException("TASK_NOT_FOUND")
            } catch (e: Exception) { throw e }
        }
        throw IllegalStateException("TEAMOS_REQUIRES_CONNECTION")
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
                accounts().requestFor(userInt, "PATCH", "/rest/v1/mentor_team_documents?id=eq.$docId&project_id=eq.$projectId", body)
                return getDocuments(userId, projectId).firstOrNull { it.id == docId }
                    ?: throw NoSuchElementException("DOCUMENT_NOT_FOUND")
            } catch (e: Exception) { throw e }
        }
        throw IllegalStateException("TEAMOS_REQUIRES_CONNECTION")
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
                accounts().requestFor(userInt, "PATCH", "/rest/v1/mentor_team_warnings?id=eq.$warningId&project_id=eq.$projectId", body)
                return getWarnings(userId, projectId).firstOrNull { it.id == warningId }
                    ?: throw NoSuchElementException("WARNING_NOT_FOUND")
            } catch (e: Exception) { throw e }
        }
        throw IllegalStateException("TEAMOS_REQUIRES_CONNECTION")
    }
}
