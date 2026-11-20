package com.example.asm_final.learning

import org.json.JSONObject
import java.util.UUID

/**
 * AI TeamOS Repository & Policy Engine
 * Manages private group projects, strict member authorization, document gap analysis, and task confirmation.
 */
class TeamOsRepository {

    enum class WarningType {
        MISSING_CONTENT,
        DUPLICATION,
        CONTRADICTION,
        UNGROUNDED_CLAIM
    }

    enum class WarningStatus {
        NEEDS_CHECK,
        RESOLVED
    }

    enum class TaskStatus {
        TODO,
        IN_PROGRESS,
        DONE
    }

    data class TeamProject(
        val id: String = UUID.randomUUID().toString(),
        val title: String,
        val description: String?,
        val ownerId: Int,
        val memberIds: MutableSet<Int> = mutableSetOf(ownerId)
    )

    data class TeacherRequirement(
        val id: String = UUID.randomUUID().toString(),
        val projectId: String,
        val title: String,
        val description: String,
        val addedBy: Int
    )

    data class TeamDocument(
        val id: String = UUID.randomUUID().toString(),
        val projectId: String,
        val title: String,
        val content: String,
        val uploadedBy: Int
    )

    data class TeamTask(
        val id: String = UUID.randomUUID().toString(),
        val projectId: String,
        val title: String,
        val description: String?,
        val assigneeId: Int?,
        val dependsOnTaskId: String? = null,
        val deadline: String? = null,
        var status: TaskStatus = TaskStatus.TODO,
        var isConfirmed: Boolean = false // Task is created ONLY AFTER team confirmation
    )

    data class TeamWarning(
        val id: String = UUID.randomUUID().toString(),
        val projectId: String,
        val warningType: WarningType,
        val message: String,
        val docId: String?,
        val locationInfo: String,
        var status: WarningStatus = WarningStatus.NEEDS_CHECK
    )

    private val projects = mutableMapOf<String, TeamProject>()
    private val requirements = mutableMapOf<String, MutableList<TeacherRequirement>>()
    private val documents = mutableMapOf<String, MutableList<TeamDocument>>()
    private val tasks = mutableMapOf<String, MutableList<TeamTask>>()
    private val warnings = mutableMapOf<String, MutableList<TeamWarning>>()

    /**
     * Strict membership authorization check.
     * Throws SecurityException if userId is NOT an active member of project.
     */
    fun checkMembership(projectId: String, userId: Int) {
        val project = projects[projectId] ?: throw NoSuchElementException("PROJECT_NOT_FOUND")
        if (!project.memberIds.contains(userId)) {
            throw SecurityException("ACCESS_DENIED: User $userId is not a member of project $projectId")
        }
    }

    fun createProject(ownerId: Int, title: String, description: String?): TeamProject {
        require(title.isNotBlank()) { "Project title cannot be blank" }
        val project = TeamProject(
            ownerId = ownerId,
            title = title.trim(),
            description = description?.trim()
        )
        projects[project.id] = project
        requirements[project.id] = mutableListOf()
        documents[project.id] = mutableListOf()
        tasks[project.id] = mutableListOf()
        warnings[project.id] = mutableListOf()
        return project
    }

    fun addMember(requesterId: Int, projectId: String, newUserId: Int) {
        checkMembership(projectId, requesterId)
        val project = projects[projectId]!!
        project.memberIds.add(newUserId)
    }

    fun addRequirement(userId: Int, projectId: String, title: String, description: String): TeacherRequirement {
        checkMembership(projectId, userId)
        require(title.isNotBlank() && description.isNotBlank())
        val req = TeacherRequirement(
            projectId = projectId,
            title = title.trim(),
            description = description.trim(),
            addedBy = userId
        )
        requirements.getOrPut(projectId) { mutableListOf() }.add(req)
        return req
    }

    fun uploadDocument(userId: Int, projectId: String, title: String, content: String): TeamDocument {
        checkMembership(projectId, userId)
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

    fun getDocuments(userId: Int, projectId: String): List<TeamDocument> {
        checkMembership(projectId, userId)
        return documents[projectId] ?: emptyList()
    }

    fun getTasks(userId: Int, projectId: String): List<TeamTask> {
        checkMembership(projectId, userId)
        return tasks[projectId] ?: emptyList()
    }

    fun getWarnings(userId: Int, projectId: String): List<TeamWarning> {
        checkMembership(projectId, userId)
        return warnings[projectId] ?: emptyList()
    }

    /**
     * AI Analysis: Compares requirements vs documents and generates warnings with provenance.
     * Flags insufficient evidence as NEEDS_CHECK.
     */
    fun analyzeGaps(userId: Int, projectId: String, aiResponseJson: String? = null): List<TeamWarning> {
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
                    status = WarningStatus.NEEDS_CHECK
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
                        status = WarningStatus.NEEDS_CHECK
                    ))
                }
            }
        }

        return list
    }

    /**
     * Confirms and creates a task suggested by AI or team members.
     * Task is created ONLY AFTER explicit team confirmation (isConfirmed = true).
     */
    fun confirmTask(
        userId: Int,
        projectId: String,
        title: String,
        description: String?,
        assigneeId: Int?,
        deadline: String? = null
    ): TeamTask {
        checkMembership(projectId, userId)
        require(title.isNotBlank())

        val task = TeamTask(
            projectId = projectId,
            title = title.trim(),
            description = description?.trim(),
            assigneeId = assigneeId,
            deadline = deadline,
            isConfirmed = true // Confirmed by team
        )
        tasks.getOrPut(projectId) { mutableListOf() }.add(task)
        return task
    }
}
