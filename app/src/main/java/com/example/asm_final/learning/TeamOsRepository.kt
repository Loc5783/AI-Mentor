package com.example.asm_final.learning

import org.json.JSONObject
import java.util.UUID

/**
 * AI TeamOS Repository & Policy Engine
 * Manages private group projects, strict member authorization, role-based access control (OWNER/ADMIN/MEMBER/VIEWER),
 * task dependencies with cycle prevention, idempotency, and human review of AI suggestions.
 */
open class TeamOsRepository {

    enum class TeamRole {
        OWNER,
        ADMIN,
        MEMBER,
        VIEWER
    }

    enum class MemberStatus {
        INVITED,
        ACTIVE,
        REMOVED
    }

    enum class WarningType {
        MISSING_CONTENT,
        DUPLICATION,
        CONTRADICTION,
        UNGROUNDED_CLAIM
    }

    enum class WarningStatus {
        NEEDS_CHECK,
        RESOLVED,
        DISMISSED
    }

    enum class TaskStatus {
        TODO,
        IN_PROGRESS,
        DONE
    }

    enum class TaskPriority {
        LOW,
        MEDIUM,
        HIGH
    }

    enum class TaskSource {
        HUMAN,
        AI_SUGGESTED
    }

    enum class TaskConfirmation {
        PENDING_CONFIRMATION,
        CONFIRMED,
        DISMISSED
    }

    data class TeamMember(
        val userId: String,
        var role: TeamRole = TeamRole.MEMBER,
        var status: MemberStatus = MemberStatus.ACTIVE,
        val joinedAt: Long = System.currentTimeMillis(),
        var removedAt: Long? = null,
        val displayName: String? = null
    )

    data class TeamProject(
        val id: String = UUID.randomUUID().toString(),
        val title: String,
        val description: String?,
        val ownerId: String,
        val version: Int = 1,
        val members: MutableMap<String, TeamMember> = mutableMapOf()
    ) {
        // Compatibility accessor
        val memberIds: Set<Int>
            get() = members.values.filter { it.status == MemberStatus.ACTIVE }.mapNotNull { it.userId.toIntOrNull() }.toSet()
    }

    data class TeacherRequirement(
        val id: String = UUID.randomUUID().toString(),
        val projectId: String,
        val title: String,
        val description: String,
        val addedBy: String,
        val priority: TaskPriority = TaskPriority.MEDIUM,
        var status: String = "OPEN"
    )

    data class TeamDocument(
        val id: String = UUID.randomUUID().toString(),
        val projectId: String,
        val title: String,
        val content: String,
        val uploadedBy: String,
        var updatedBy: String? = null,
        var version: Int = 1,
        val filePath: String? = null,
        val fileName: String? = null,
        val fileMime: String? = null,
        val fileSize: Long? = null
    )

    data class TeamFileInput(val name: String, val mime: String, val bytes: ByteArray) {
        init {
            require(name.isNotBlank() && name.length <= 180 && name.none { Character.isISOControl(it) || it == '/' || it == '\\' })
            require(mime in setOf("application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "text/plain", "image/jpeg", "image/png", "image/webp"))
            require(bytes.isNotEmpty() && bytes.size <= 8 * 1024 * 1024)
        }
    }

    open fun uploadFileDocument(userId: String, projectId: String, title: String,
        content: String, file: TeamFileInput): TeamDocument {
        checkMembership(projectId, userId)
        require(title.isNotBlank())
        val doc = TeamDocument(projectId = projectId, title = title.trim(), content = content.trim(),
            uploadedBy = userId, filePath = "$projectId/${UUID.randomUUID()}",
            fileName = file.name, fileMime = file.mime, fileSize = file.bytes.size.toLong())
        documents.getOrPut(projectId) { mutableListOf() }.add(doc)
        return doc
    }

    open fun downloadFile(userId: String, document: TeamDocument): java.io.File =
        throw UnsupportedOperationException("TEAM_FILE_STORAGE_UNAVAILABLE")

    open fun deleteProject(userId: String, projectId: String, password: String) {
        require(password.isNotBlank())
        checkMembership(projectId, userId)
        val project = projects[projectId] ?: throw NoSuchElementException("PROJECT_NOT_FOUND")
        if (project.ownerId != userId) throw SecurityException("ONLY_PROJECT_OWNER_CAN_DELETE")
        projects.remove(projectId)
        requirements.remove(projectId)
        documents.remove(projectId)
        tasks.remove(projectId)?.forEach { taskDependencies.remove(it.id) }
        warnings.remove(projectId)
    }

    data class TeamTask(
        val id: String = UUID.randomUUID().toString(),
        val projectId: String,
        val title: String,
        val description: String?,
        val assigneeId: String?,
        val dependsOnTaskId: String? = null,
        val deadline: String? = null,
        var status: TaskStatus = TaskStatus.TODO,
        val priority: TaskPriority = TaskPriority.MEDIUM,
        val source: TaskSource = TaskSource.HUMAN,
        var confirmationStatus: TaskConfirmation = TaskConfirmation.CONFIRMED,
        var confirmedBy: String? = null,
        var confirmedAt: Long? = null,
        val clientRequestId: String? = null,
        var isConfirmed: Boolean = true
    )

    data class TeamWarning(
        val id: String = UUID.randomUUID().toString(),
        val projectId: String,
        val warningType: WarningType,
        val message: String,
        val docId: String?,
        val locationInfo: String,
        val excerpt: String? = null,
        val evidenceRef: String? = null,
        val explanation: String? = null,
        var status: WarningStatus = WarningStatus.NEEDS_CHECK,
        val createdByAi: Boolean = true,
        var reviewedBy: String? = null,
        var reviewedAt: Long? = null
    )

    private val projects = mutableMapOf<String, TeamProject>()
    private val requirements = mutableMapOf<String, MutableList<TeacherRequirement>>()
    private val documents = mutableMapOf<String, MutableList<TeamDocument>>()
    private val tasks = mutableMapOf<String, MutableList<TeamTask>>()
    private val taskDependencies = mutableMapOf<String, MutableSet<String>>() // taskId -> Set<dependsOnTaskId>
    private val warnings = mutableMapOf<String, MutableList<TeamWarning>>()

    // User session cache boundary
    private var cachedUserId: String? = null

    fun clearUserCache(userId: String) {
        if (cachedUserId == userId) {
            cachedUserId = null
        }
    }

    /**
     * Strict active membership check.
     * Throws SecurityException if user is not an active member.
     */
    fun checkMembership(projectId: String, userId: String) {
        val project = projects[projectId] ?: throw NoSuchElementException("PROJECT_NOT_FOUND")
        val member = project.members[userId]
        if (member == null || member.status != MemberStatus.ACTIVE) {
            throw SecurityException("ACCESS_DENIED: User $userId is not an active member of project $projectId")
        }
    }

    fun checkMembership(projectId: String, userId: Int) = checkMembership(projectId, userId.toString())

    /**
     * Role-based check: requires OWNER or ADMIN.
     */
    fun checkManager(projectId: String, userId: String) {
        checkMembership(projectId, userId)
        val project = projects[projectId]!!
        val role = project.members[userId]?.role
        if (role != TeamRole.OWNER && role != TeamRole.ADMIN) {
            throw SecurityException("PERMISSION_DENIED: Only OWNER or ADMIN can perform this action in project $projectId")
        }
    }

    fun checkManager(projectId: String, userId: Int) = checkManager(projectId, userId.toString())

    /**
     * VIEWER cannot edit project content; all active members may upload files separately.
     */
    fun checkNotViewer(projectId: String, userId: String) {
        checkMembership(projectId, userId)
        val project = projects[projectId]!!
        if (project.members[userId]?.role == TeamRole.VIEWER) {
            throw SecurityException("PERMISSION_DENIED: VIEWER role cannot modify project content")
        }
    }

    fun checkNotViewer(projectId: String, userId: Int) = checkNotViewer(projectId, userId.toString())

    open fun createProject(ownerId: String, title: String, description: String?): TeamProject {
        require(title.isNotBlank()) { "Project title cannot be blank" }
        val project = TeamProject(
            ownerId = ownerId,
            title = title.trim(),
            description = description?.trim()
        )
        // Project creator is automatically an active OWNER member
        project.members[ownerId] = TeamMember(ownerId, TeamRole.OWNER, MemberStatus.ACTIVE)
        projects[project.id] = project
        requirements[project.id] = mutableListOf()
        documents[project.id] = mutableListOf()
        tasks[project.id] = mutableListOf()
        warnings[project.id] = mutableListOf()
        return project
    }

    fun createProject(ownerId: Int, title: String, description: String?): TeamProject =
        createProject(ownerId.toString(), title, description)

    open fun getProjectsForUser(userId: String): List<TeamProject> {
        return projects.values.filter { it.members[userId]?.status == MemberStatus.ACTIVE }
    }

    open fun getMembers(userId: String, projectId: String): List<TeamMember> {
        checkMembership(projectId, userId)
        return projects[projectId]?.members?.values?.filter { it.status == MemberStatus.ACTIVE } ?: emptyList()
    }

    fun getMembers(userId: Int, projectId: String): List<TeamMember> =
        getMembers(userId.toString(), projectId)

    open fun getProject(userId: String, projectId: String): TeamProject {
        checkMembership(projectId, userId)
        return projects[projectId]!!
    }

    /**
     * Only OWNER or ADMIN can add or invite new members.
     * Prevents self-join and unauthorized invites.
     */
    open fun addMember(
        requesterId: String,
        projectId: String,
        newUserId: String,
        role: TeamRole = TeamRole.MEMBER
    ) {
        checkManager(projectId, requesterId)
        val project = projects[projectId]!!
        val existing = project.members[newUserId]
        if (existing != null) {
            existing.status = MemberStatus.ACTIVE
            existing.role = role
            existing.removedAt = null
        } else {
            project.members[newUserId] = TeamMember(newUserId, role, MemberStatus.ACTIVE)
        }
    }

    fun addMember(requesterId: Int, projectId: String, newUserId: Int) =
        addMember(requesterId.toString(), projectId, newUserId.toString(), TeamRole.MEMBER)

    /**
     * Remove member from project.
     * Cannot remove the last OWNER.
     */
    open fun removeMember(requesterId: String, projectId: String, targetUserId: String) {
        checkManager(projectId, requesterId)
        val project = projects[projectId]!!
        val target = project.members[targetUserId] ?: throw NoSuchElementException("MEMBER_NOT_FOUND")

        if (target.role == TeamRole.OWNER) {
            val ownerCount = project.members.values.count { it.role == TeamRole.OWNER && it.status == MemberStatus.ACTIVE }
            if (ownerCount <= 1) {
                throw IllegalStateException("CANNOT_REMOVE_LAST_OWNER: The project must have at least one active owner")
            }
        }

        target.status = MemberStatus.REMOVED
        target.removedAt = System.currentTimeMillis()
    }

    open fun addRequirement(
        userId: String,
        projectId: String,
        title: String,
        description: String,
        priority: TaskPriority = TaskPriority.MEDIUM
    ): TeacherRequirement {
        checkNotViewer(projectId, userId)
        require(title.isNotBlank() && description.isNotBlank())
        val req = TeacherRequirement(
            projectId = projectId,
            title = title.trim(),
            description = description.trim(),
            addedBy = userId,
            priority = priority
        )
        requirements.getOrPut(projectId) { mutableListOf() }.add(req)
        return req
    }

    fun addRequirement(userId: Int, projectId: String, title: String, description: String): TeacherRequirement =
        addRequirement(userId.toString(), projectId, title, description)

    open fun getRequirements(userId: String, projectId: String): List<TeacherRequirement> {
        checkMembership(projectId, userId)
        return requirements[projectId] ?: emptyList()
    }

    open fun uploadDocument(
        userId: String,
        projectId: String,
        title: String,
        content: String
    ): TeamDocument {
        checkNotViewer(projectId, userId)
        require(title.isNotBlank() && content.isNotBlank())
        val doc = TeamDocument(
            projectId = projectId,
            title = title.trim(),
            content = content.trim(),
            uploadedBy = userId
        )
        documents.getOrPut(projectId) { mutableListOf() }.add(doc)
        return doc
    }

    fun uploadDocument(userId: Int, projectId: String, title: String, content: String): TeamDocument =
        uploadDocument(userId.toString(), projectId, title, content)

    open fun getDocuments(userId: String, projectId: String): List<TeamDocument> {
        checkMembership(projectId, userId)
        return documents[projectId] ?: emptyList()
    }

    fun getDocuments(userId: Int, projectId: String): List<TeamDocument> =
        getDocuments(userId.toString(), projectId)

    open fun updateDocument(userId: String, projectId: String, docId: String, title: String, content: String): TeamDocument {
        checkNotViewer(projectId, userId)
        require(title.isNotBlank() && content.isNotBlank())
        val docList = documents[projectId] ?: throw NoSuchElementException("DOCUMENT_NOT_FOUND")
        val doc = docList.firstOrNull { it.id == docId } ?: throw NoSuchElementException("DOCUMENT_NOT_FOUND")
        val updated = TeamDocument(
            id = doc.id,
            projectId = doc.projectId,
            title = title.trim(),
            content = content.trim(),
            uploadedBy = doc.uploadedBy,
            updatedBy = userId,
            version = doc.version + 1,
            filePath = doc.filePath,
            fileName = doc.fileName,
            fileMime = doc.fileMime,
            fileSize = doc.fileSize
        )
        val index = docList.indexOf(doc)
        docList[index] = updated
        return updated
    }

    fun updateDocument(userId: Int, projectId: String, docId: String, title: String, content: String): TeamDocument =
        updateDocument(userId.toString(), projectId, docId, title, content)

    open fun getTasks(userId: String, projectId: String): List<TeamTask> {
        checkMembership(projectId, userId)
        return tasks[projectId] ?: emptyList()
    }

    fun getTasks(userId: Int, projectId: String): List<TeamTask> =
        getTasks(userId.toString(), projectId)

    open fun updateTaskStatus(userId: String, projectId: String, taskId: String, newStatus: TaskStatus): TeamTask {
        checkNotViewer(projectId, userId)
        val projectTasks = tasks[projectId] ?: throw NoSuchElementException("TASK_NOT_FOUND")
        val task = projectTasks.firstOrNull { it.id == taskId } ?: throw NoSuchElementException("TASK_NOT_FOUND")
        task.status = newStatus
        return task
    }

    fun updateTaskStatus(userId: Int, projectId: String, taskId: String, newStatus: TaskStatus): TeamTask =
        updateTaskStatus(userId.toString(), projectId, taskId, newStatus)

    open fun getWarnings(userId: String, projectId: String): List<TeamWarning> {
        checkMembership(projectId, userId)
        return warnings[projectId] ?: emptyList()
    }

    fun getWarnings(userId: Int, projectId: String): List<TeamWarning> =
        getWarnings(userId.toString(), projectId)

    /**
     * Add task dependency with strict validation:
     * - Both tasks must belong to the same project.
     * - Self-dependency is forbidden.
     * - Cycles of any length (A->B->A, A->B->C->A, etc.) are strictly forbidden.
     */
    open fun addTaskDependency(
        userId: String,
        projectId: String,
        taskId: String,
        dependsOnTaskId: String
    ) {
        checkNotViewer(projectId, userId)
        require(taskId != dependsOnTaskId) { "SELF_DEPENDENCY_FORBIDDEN" }

        val projectTasks = tasks[projectId] ?: emptyList()
        val taskExists = projectTasks.any { it.id == taskId }
        val depExists = projectTasks.any { it.id == dependsOnTaskId }
        require(taskExists && depExists) { "CROSS_PROJECT_DEPENDENCY_FORBIDDEN" }

        // Cycle check: verify if dependsOnTaskId already reaches taskId
        if (hasPath(dependsOnTaskId, taskId)) {
            throw IllegalArgumentException("CYCLIC_DEPENDENCY_DETECTED: Adding dependency would form a cycle")
        }

        taskDependencies.getOrPut(taskId) { mutableSetOf() }.add(dependsOnTaskId)
    }

    private fun hasPath(from: String, target: String, visited: MutableSet<String> = mutableSetOf()): Boolean {
        if (from == target) return true
        if (!visited.add(from)) return false
        val deps = taskDependencies[from] ?: return false
        for (next in deps) {
            if (hasPath(next, target, visited)) return true
        }
        return false
    }

    open fun getTaskDependencies(userId: String, projectId: String, taskId: String): Set<String> {
        checkMembership(projectId, userId)
        return taskDependencies[taskId] ?: emptySet()
    }

    /**
     * AI Analysis: Compares requirements vs documents and generates warnings with provenance.
     * Flags insufficient evidence as NEEDS_CHECK.
     */
    open fun analyzeGaps(userId: String, projectId: String, aiResponseJson: String? = null): List<TeamWarning> {
        checkMembership(projectId, userId)
        val reqs = requirements[projectId] ?: emptyList()
        val docs = documents[projectId] ?: emptyList()
        val list = warnings.getOrPut(projectId) { mutableListOf() }

        if (reqs.isEmpty() || docs.isEmpty()) {
            val emptyWarning = TeamWarning(
                projectId = projectId,
                warningType = WarningType.MISSING_CONTENT,
                message = "Chưa có tài liệu hoặc yêu cầu của giáo viên để so sánh đối chiếu.",
                docId = docs.firstOrNull()?.id,
                locationInfo = "Toàn bộ dự án",
                status = WarningStatus.NEEDS_CHECK
            )
            list.add(emptyWarning)
            return list
        }

        // Try parsing AI analysis JSON if available
        val parsedWarnings = runCatching {
            if (aiResponseJson.isNullOrBlank()) null else JSONObject(aiResponseJson).optJSONArray("warnings")
        }.getOrNull()

        if (parsedWarnings != null && parsedWarnings.length() > 0) {
            for (i in 0 until parsedWarnings.length()) {
                val item = parsedWarnings.getJSONObject(i)
                val typeStr = item.optString("warningType", "MISSING_CONTENT")
                val type = runCatching { WarningType.valueOf(typeStr) }.getOrDefault(WarningType.MISSING_CONTENT)
                val msg = item.optString("message", "Nội dung cần kiểm tra")
                val docId = item.optString("docId").takeIf { id -> docs.any { it.id == id } } ?: docs.first().id
                val loc = item.optString("locationInfo", "Trang 1 / Đoạn 1")

                list.add(TeamWarning(
                    projectId = projectId,
                    warningType = type,
                    message = msg,
                    docId = docId,
                    locationInfo = loc,
                    status = WarningStatus.NEEDS_CHECK,
                    excerpt = item.optString("excerpt").takeIf { it.isNotBlank() },
                    evidenceRef = item.optString("evidenceRef").takeIf { it.isNotBlank() },
                    explanation = item.optString("explanation").takeIf { it.isNotBlank() }
                ))
            }
        } else {
            // Rule-based fallback check: Compare teacher reqs keywords against documents
            for (req in reqs) {
                val keywords = req.title.lowercase().split(" ").filter { it.length > 3 }
                val matched = docs.any { doc -> keywords.any { kw -> doc.content.lowercase().contains(kw) } }
                if (!matched) {
                    list.add(TeamWarning(
                        projectId = projectId,
                        warningType = WarningType.MISSING_CONTENT,
                        message = "Yêu cầu '${req.title}' chưa thấy xuất hiện trong các tài liệu hiện có.",
                        docId = docs.first().id,
                        locationInfo = "Tài liệu '${docs.first().title}'",
                        status = WarningStatus.NEEDS_CHECK,
                        explanation = "Nội dung chưa phủ yêu cầu của giáo viên"
                    ))
                }
            }
        }

        return list
    }

    fun analyzeGaps(userId: Int, projectId: String, aiResponseJson: String? = null): List<TeamWarning> =
        analyzeGaps(userId.toString(), projectId, aiResponseJson)

    /**
     * Confirms and creates a task suggested by AI or team members.
     * Enforces idempotency via clientRequestId: repeating identical request returns existing task.
     */
    open fun confirmTask(
        userId: String,
        projectId: String,
        title: String,
        description: String?,
        assigneeId: String?,
        deadline: String? = null,
        clientRequestId: String? = null,
        priority: TaskPriority = TaskPriority.MEDIUM,
        source: TaskSource = TaskSource.HUMAN
    ): TeamTask {
        checkNotViewer(projectId, userId)
        require(title.isNotBlank())

        val projectTasks = tasks.getOrPut(projectId) { mutableListOf() }

        // Idempotency check: if clientRequestId already exists in project, return it
        if (!clientRequestId.isNullOrBlank()) {
            val existing = projectTasks.firstOrNull { it.clientRequestId == clientRequestId }
            if (existing != null) {
                return existing
            }
        }

        val task = TeamTask(
            projectId = projectId,
            title = title.trim(),
            description = description?.trim(),
            assigneeId = assigneeId,
            deadline = deadline,
            priority = priority,
            source = source,
            confirmationStatus = TaskConfirmation.CONFIRMED,
            confirmedBy = userId,
            confirmedAt = System.currentTimeMillis(),
            clientRequestId = clientRequestId,
            isConfirmed = true
        )
        projectTasks.add(task)
        return task
    }

    fun confirmTask(
        userId: Int,
        projectId: String,
        title: String,
        description: String?,
        assigneeId: Int?,
        deadline: String? = null
    ): TeamTask = confirmTask(
        userId.toString(),
        projectId,
        title,
        description,
        assigneeId?.toString(),
        deadline
    )

    /**
     * Human review of AI findings / warnings.
     * Status can only be changed to CONFIRMED or DISMISSED by human member.
     */
    open fun reviewWarning(
        userId: String,
        projectId: String,
        warningId: String,
        decision: WarningStatus
    ): TeamWarning {
        checkNotViewer(projectId, userId)
        require(decision == WarningStatus.RESOLVED || decision == WarningStatus.DISMISSED)
        val list = warnings[projectId] ?: throw NoSuchElementException("WARNING_NOT_FOUND")
        val warning = list.firstOrNull { it.id == warningId } ?: throw NoSuchElementException("WARNING_NOT_FOUND")
        warning.status = decision
        warning.reviewedBy = userId
        warning.reviewedAt = System.currentTimeMillis()
        return warning
    }
}
