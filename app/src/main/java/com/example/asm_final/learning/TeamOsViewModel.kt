package com.example.asm_final.learning

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import com.example.asm_final.competition.LearnerDirectory
import com.example.asm_final.competition.FriendsViewModel

enum class TeamOsScreen {
    PROJECT_LIST,
    PROJECT_DETAIL
}

enum class TeamOsTab {
    OVERVIEW,
    MEMBERS,
    REQUIREMENTS,
    DOCUMENTS,
    TASKS,
    WARNINGS
}

/**
 * Presentation ViewModel for TeamOS collaborative projects.
 * Enforces role capabilities (OWNER/ADMIN/MEMBER/VIEWER, with file upload for every active member),
 * multi-tab navigation, state isolation across user switches,
 * task dependency validations, and human review of AI findings.
 */
class TeamOsViewModel(
    private val repository: TeamOsRepository,
    private val scope: CoroutineScope? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val learnerDirectory: LearnerDirectory? = null
) : ViewModel() {

    private val ownedJob = SupervisorJob((scope ?: viewModelScope).coroutineContext[Job])
    private val vmScope = CoroutineScope((scope ?: viewModelScope).coroutineContext + ownedJob)

    var currentUserId by mutableStateOf<String?>(null)
        private set

    var screen by mutableStateOf(TeamOsScreen.PROJECT_LIST)
        private set

    var currentTab by mutableStateOf(TeamOsTab.OVERVIEW)
        private set

    var projects by mutableStateOf<List<TeamOsRepository.TeamProject>>(emptyList())
        private set

    var invitations by mutableStateOf<List<TeamOsRepository.TeamInvitation>>(emptyList())
        private set

    var selectedProject by mutableStateOf<TeamOsRepository.TeamProject?>(null)
        private set

    var members by mutableStateOf<List<TeamOsRepository.TeamMember>>(emptyList())
        private set

    var requirements by mutableStateOf<List<TeamOsRepository.TeacherRequirement>>(emptyList())
        private set

    var documents by mutableStateOf<List<TeamOsRepository.TeamDocument>>(emptyList())
        private set

    var tasks by mutableStateOf<List<TeamOsRepository.TeamTask>>(emptyList())
        private set

    var warnings by mutableStateOf<List<TeamOsRepository.TeamWarning>>(emptyList())
        private set

    var taskDependencies by mutableStateOf<Map<String, Set<String>>>(emptyMap())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var actionSuccessMessage by mutableStateOf<String?>(null)
        private set

    var memberCandidates by mutableStateOf<List<FriendsViewModel.SearchItem>>(emptyList())
        private set
    var memberSearchLoading by mutableStateOf(false)
        private set
    var memberSearchError by mutableStateOf<String?>(null)
        private set
    private var memberSearchGeneration = 0

    fun searchMembers(query: String) {
        val generation = ++memberSearchGeneration
        val owner = currentUserId?.toIntOrNull()
        memberCandidates = emptyList()
        memberSearchError = null
        if (query.trim().length < 2 || owner == null || learnerDirectory == null) {
            memberSearchLoading = false
            return
        }
        memberSearchLoading = true
        vmScope.launch {
            delay(250)
            try {
                val results = withContext(ioDispatcher) {
                    val self = learnerDirectory.currentProfileId(owner)
                    learnerDirectory.search(owner, query).filterNot { it.id == self }
                }
                if (generation == memberSearchGeneration && currentUserId == owner.toString()) {
                    memberCandidates = results
                }
            } catch (e: Exception) {
                if (generation == memberSearchGeneration) memberSearchError = "Không tìm được tài khoản. Thử lại khi có mạng."
            } finally {
                if (generation == memberSearchGeneration) memberSearchLoading = false
            }
        }
    }

    // Role-based capability properties
    val currentUserRole: TeamOsRepository.TeamRole?
        get() {
            val uid = currentUserId ?: return null
            val proj = selectedProject ?: return null
            return proj.members[uid]?.role
                ?: members.firstOrNull { it.userId == uid }?.role
        }

    val canManageMembers: Boolean
        get() = currentUserRole == TeamOsRepository.TeamRole.OWNER ||
                currentUserRole == TeamOsRepository.TeamRole.ADMIN

    val isViewer: Boolean
        get() = currentUserRole == TeamOsRepository.TeamRole.VIEWER

    val canEdit: Boolean
        get() = selectedProject != null && !isViewer

    val canUploadFile: Boolean
        get() = selectedProject != null && currentUserRole != null

    val canDeleteProject: Boolean
        get() = selectedProject?.ownerId == currentUserId && currentUserRole == TeamOsRepository.TeamRole.OWNER

    /**
     * Loads projects for given user.
     * Enforces user isolation: resets all project/detail state if switching accounts.
     */
    fun loadProjects(userId: String) {
        if (currentUserId != null && currentUserId != userId) {
            // Identity switch: isolate state
            repository.clearUserCache(currentUserId!!)
            selectedProject = null
            screen = TeamOsScreen.PROJECT_LIST
            currentTab = TeamOsTab.OVERVIEW
            projects = emptyList()
            invitations = emptyList()
            members = emptyList()
            requirements = emptyList()
            documents = emptyList()
            tasks = emptyList()
            warnings = emptyList()
            taskDependencies = emptyMap()
            memberSearchGeneration++
            memberCandidates = emptyList()
            errorMessage = null
            actionSuccessMessage = null
        }
        currentUserId = userId

        vmScope.launch {
            isLoading = true
            try {
                val (list, invites) = withContext(ioDispatcher) {
                    repository.getProjectsForUser(userId) to repository.getInvitations(userId)
                }
                if (currentUserId == userId) {
                    projects = list
                    invitations = invites
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Không tải được danh sách dự án"
            } finally {
                isLoading = false
            }
        }
    }

    fun respondToInvitation(projectId: String, accept: Boolean) {
        val uid = currentUserId ?: return
        if (isLoading) return
        vmScope.launch {
            isLoading = true
            try {
                withContext(ioDispatcher) { repository.respondToInvitation(uid, projectId, accept) }
                if (currentUserId == uid) {
                    projects = withContext(ioDispatcher) { repository.getProjectsForUser(uid) }
                    invitations = withContext(ioDispatcher) { repository.getInvitations(uid) }
                    actionSuccessMessage = if (accept) "Đã tham gia dự án" else "Đã từ chối lời mời"
                }
            } catch (e: Exception) { if (currentUserId == uid) errorMessage = e.message ?: "Không xử lý được lời mời" }
            finally { isLoading = false }
        }
    }

    fun selectProject(project: TeamOsRepository.TeamProject) {
        selectedProject = project
        screen = TeamOsScreen.PROJECT_DETAIL
        currentTab = TeamOsTab.OVERVIEW
        refreshProjectDetails()
    }

    fun backToProjectList() {
        screen = TeamOsScreen.PROJECT_LIST
        selectedProject = null
        errorMessage = null
        actionSuccessMessage = null
    }

    fun switchTab(tab: TeamOsTab) {
        currentTab = tab
    }

    fun refreshProjectDetails() {
        val uid = currentUserId ?: return
        val proj = selectedProject ?: return

        vmScope.launch {
            isLoading = true
            try {
                withContext(ioDispatcher) {
                    val m = repository.getMembers(uid, proj.id)
                    val r = repository.getRequirements(uid, proj.id)
                    val d = repository.getDocuments(uid, proj.id)
                    val t = repository.getTasks(uid, proj.id)
                    val w = repository.getWarnings(uid, proj.id)
                    val deps = t.associate { it.id to repository.getTaskDependencies(uid, proj.id, it.id) }

                    withContext(Dispatchers.Main) {
                        members = m
                        requirements = r
                        documents = d
                        tasks = t
                        warnings = w
                        taskDependencies = deps
                    }
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Không tải được chi tiết dự án"
            } finally {
                isLoading = false
            }
        }
    }

    fun createProject(title: String, description: String?, onCreated: (() -> Unit)? = null) {
        val uid = currentUserId ?: return
        if (title.isBlank()) {
            errorMessage = "Tên dự án không được để trống"
            return
        }

        vmScope.launch {
            isLoading = true
            try {
                val project = withContext(ioDispatcher) {
                    repository.createProject(uid, title.trim(), description?.trim())
                }
                val list = withContext(ioDispatcher) {
                    repository.getProjectsForUser(uid)
                }
                projects = list
                selectProject(project)
                actionSuccessMessage = "Tạo dự án thành công"
                onCreated?.invoke()
            } catch (e: Exception) {
                errorMessage = e.message ?: "Lỗi tạo dự án"
            } finally {
                isLoading = false
            }
        }
    }

    fun addMember(
        targetUserId: String,
        role: TeamOsRepository.TeamRole = TeamOsRepository.TeamRole.MEMBER,
        onSuccess: (() -> Unit)? = null
    ) {
        val uid = currentUserId ?: return
        val proj = selectedProject ?: return

        if (!canManageMembers) {
            errorMessage = "Chỉ Trưởng nhóm (OWNER) hoặc Quản trị viên (ADMIN) mới có quyền thêm thành viên"
            return
        }

        vmScope.launch {
            isLoading = true
            try {
                withContext(ioDispatcher) {
                    repository.addMember(uid, proj.id, targetUserId.trim(), role)
                }
                actionSuccessMessage = "Đã gửi lời mời tham gia"
                refreshProjectDetails()
                onSuccess?.invoke()
            } catch (e: Exception) {
                errorMessage = e.message ?: "Lỗi thêm thành viên"
            } finally {
                isLoading = false
            }
        }
    }

    fun removeMember(targetUserId: String, onSuccess: (() -> Unit)? = null) {
        val uid = currentUserId ?: return
        val proj = selectedProject ?: return

        if (!canManageMembers) {
            errorMessage = "Chỉ Trưởng nhóm (OWNER) hoặc Quản trị viên (ADMIN) mới có quyền xóa thành viên"
            return
        }

        vmScope.launch {
            isLoading = true
            try {
                withContext(ioDispatcher) {
                    repository.removeMember(uid, proj.id, targetUserId)
                }
                actionSuccessMessage = "Xóa thành viên thành công"
                refreshProjectDetails()
                onSuccess?.invoke()
            } catch (e: Exception) {
                errorMessage = e.message ?: "Lỗi xóa thành viên"
            } finally {
                isLoading = false
            }
        }
    }

    fun changeMemberRole(targetUserId: String, role: TeamOsRepository.TeamRole, onSuccess: (() -> Unit)? = null) {
        val uid = currentUserId ?: return
        val proj = selectedProject ?: return
        if (!canManageMembers || role == TeamOsRepository.TeamRole.OWNER || targetUserId == uid) return
        vmScope.launch {
            isLoading = true
            try {
                withContext(ioDispatcher) { repository.changeMemberRole(uid, proj.id, targetUserId, role) }
                actionSuccessMessage = "Đã cập nhật quyền thành viên"
                refreshProjectDetails()
                onSuccess?.invoke()
            } catch (e: Exception) { errorMessage = e.message ?: "Không đổi được quyền" }
            finally { isLoading = false }
        }
    }

    fun addRequirement(
        title: String,
        description: String,
        priority: TeamOsRepository.TaskPriority = TeamOsRepository.TaskPriority.MEDIUM,
        file: TeamOsRepository.TeamFileInput? = null,
        onSuccess: (() -> Unit)? = null
    ) {
        val uid = currentUserId ?: return
        val proj = selectedProject ?: return

        if (!canEdit) {
            errorMessage = "Vai trò Người xem (VIEWER) không có quyền thêm yêu cầu"
            return
        }

        vmScope.launch {
            isLoading = true
            try {
                withContext(ioDispatcher) {
                    repository.addRequirement(uid, proj.id, title.trim(), description.trim(), priority)
                    if (file != null) {
                        repository.uploadFileDocument(uid, proj.id, title.trim(), description.trim(), file)
                    }
                }
                actionSuccessMessage = "Thêm yêu cầu và tài liệu đính kèm thành công"
                refreshProjectDetails()
                onSuccess?.invoke()
            } catch (e: Exception) {
                errorMessage = e.message ?: "Lỗi thêm yêu cầu"
            } finally {
                isLoading = false
            }
        }
    }

    fun uploadDocument(
        title: String,
        content: String,
        onSuccess: (() -> Unit)? = null
    ) {
        val uid = currentUserId ?: return
        val proj = selectedProject ?: return

        if (!canEdit) {
            errorMessage = "Vai trò Người xem (VIEWER) không có quyền tải tài liệu"
            return
        }

        vmScope.launch {
            isLoading = true
            try {
                withContext(ioDispatcher) {
                    repository.uploadDocument(uid, proj.id, title.trim(), content.trim())
                }
                actionSuccessMessage = "Tải tài liệu thành công"
                refreshProjectDetails()
                onSuccess?.invoke()
            } catch (e: Exception) {
                errorMessage = e.message ?: "Lỗi tải tài liệu"
            } finally {
                isLoading = false
            }
        }
    }

    fun uploadFile(title: String, content: String, file: TeamOsRepository.TeamFileInput,
        onSuccess: (() -> Unit)? = null) {
        val uid = currentUserId ?: return
        val projectId = selectedProject?.id ?: return
        if (!canUploadFile || isLoading) return
        isLoading = true
        vmScope.launch {
            errorMessage = null
            try {
                withContext(ioDispatcher) { repository.uploadFileDocument(uid, projectId, title, content, file) }
                if (currentUserId == uid && selectedProject?.id == projectId) {
                    actionSuccessMessage = "Đã tải tệp lên nhóm"
                    refreshProjectDetails()
                    onSuccess?.invoke()
                }
            } catch (e: Exception) {
                if (currentUserId == uid) errorMessage = e.message ?: "Không tải được tệp"
            } finally { isLoading = false }
        }
    }

    fun openFile(document: TeamOsRepository.TeamDocument, onReady: (java.io.File) -> Unit) {
        val uid = currentUserId ?: return
        val projectId = selectedProject?.id ?: return
        if (document.projectId != projectId) return
        vmScope.launch {
            try {
                val file = withContext(ioDispatcher) { repository.downloadFile(uid, document) }
                if (currentUserId == uid && selectedProject?.id == projectId) onReady(file)
            } catch (e: Exception) {
                if (currentUserId == uid) errorMessage = e.message ?: "Không mở được tệp"
            }
        }
    }

    fun deleteProject(password: String, onSuccess: (() -> Unit)? = null) {
        val uid = currentUserId ?: return
        val projectId = selectedProject?.id ?: return
        if (!canDeleteProject || isLoading) return
        isLoading = true
        vmScope.launch {
            errorMessage = null
            try {
                withContext(ioDispatcher) { repository.deleteProject(uid, projectId, password) }
                if (currentUserId == uid && selectedProject?.id == projectId) {
                    backToProjectList()
                    projects = projects.filterNot { it.id == projectId }
                    actionSuccessMessage = "Đã xóa dự án"
                    onSuccess?.invoke()
                    try {
                        projects = withContext(ioDispatcher) { repository.getProjectsForUser(uid) }
                    } catch (_: Exception) {
                        // The delete has already succeeded; keep the confirmed local state.
                    }
                }
            } catch (e: Exception) {
                if (currentUserId == uid) errorMessage = when (e.message) {
                    "INVALID_CREDENTIALS" -> "Mật khẩu không đúng. Dự án vẫn được giữ nguyên."
                    else -> e.message ?: "Không xóa được dự án"
                }
            } finally { isLoading = false }
        }
    }

    fun updateDocument(
        docId: String,
        title: String,
        content: String,
        onSuccess: (() -> Unit)? = null
    ) {
        val uid = currentUserId ?: return
        val proj = selectedProject ?: return

        if (!canEdit) {
            errorMessage = "Vai trò Người xem (VIEWER) không có quyền chỉnh sửa tài liệu"
            return
        }

        vmScope.launch {
            isLoading = true
            try {
                withContext(ioDispatcher) {
                    repository.updateDocument(uid, proj.id, docId, title.trim(), content.trim())
                }
                actionSuccessMessage = "Cập nhật tài liệu thành công (phiên bản mới)"
                refreshProjectDetails()
                onSuccess?.invoke()
            } catch (e: Exception) {
                errorMessage = e.message ?: "Lỗi cập nhật tài liệu"
            } finally {
                isLoading = false
            }
        }
    }

    fun createTask(
        title: String,
        description: String?,
        assigneeId: String?,
        deadline: String?,
        priority: TeamOsRepository.TaskPriority = TeamOsRepository.TaskPriority.MEDIUM,
        dependsOnTaskId: String? = null,
        clientRequestId: String? = null,
        file: TeamOsRepository.TeamFileInput? = null,
        onSuccess: (() -> Unit)? = null
    ) {
        val uid = currentUserId ?: return
        val proj = selectedProject ?: return

        if (!canEdit) {
            errorMessage = "Vai trò Người xem (VIEWER) không có quyền tạo nhiệm vụ"
            return
        }

        vmScope.launch {
            isLoading = true
            try {
                val created = withContext(ioDispatcher) {
                    val t = repository.confirmTask(
                        userId = uid,
                        projectId = proj.id,
                        title = title.trim(),
                        description = description?.trim(),
                        assigneeId = assigneeId?.trim(),
                        deadline = deadline?.trim(),
                        clientRequestId = clientRequestId,
                        priority = priority
                    )
                    if (!dependsOnTaskId.isNullOrBlank()) {
                        repository.addTaskDependency(uid, proj.id, t.id, dependsOnTaskId)
                    }
                    if (file != null) {
                        repository.uploadFileDocument(uid, proj.id, title.trim(), description?.trim() ?: title.trim(), file)
                    }
                    t
                }
                actionSuccessMessage = "Xác nhận tạo nhiệm vụ và tài liệu thành công"
                refreshProjectDetails()
                onSuccess?.invoke()
            } catch (e: Exception) {
                errorMessage = when {
                    e.message?.contains("CYCLIC") == true -> "Phát hiện phụ thuộc vòng! Không thể thêm phụ thuộc này."
                    e.message?.contains("CROSS_PROJECT") == true -> "Hai nhiệm vụ không cùng dự án!"
                    e.message?.contains("SELF_DEPENDENCY") == true -> "Nhiệm vụ không thể tự phụ thuộc chính nó!"
                    else -> e.message ?: "Lỗi tạo nhiệm vụ"
                }
            } finally {
                isLoading = false
            }
        }
    }

    fun addTaskDependency(
        taskId: String,
        dependsOnTaskId: String,
        onSuccess: (() -> Unit)? = null
    ) {
        val uid = currentUserId ?: return
        val proj = selectedProject ?: return

        if (!canEdit) {
            errorMessage = "Vai trò Người xem (VIEWER) không có quyền sửa phụ thuộc"
            return
        }

        vmScope.launch {
            isLoading = true
            try {
                withContext(ioDispatcher) {
                    repository.addTaskDependency(uid, proj.id, taskId, dependsOnTaskId)
                }
                actionSuccessMessage = "Đã thêm phụ thuộc nhiệm vụ"
                refreshProjectDetails()
                onSuccess?.invoke()
            } catch (e: Exception) {
                errorMessage = when {
                    e.message?.contains("CYCLIC") == true -> "Phát hiện phụ thuộc vòng! Không thể thêm phụ thuộc này."
                    e.message?.contains("CROSS_PROJECT") == true -> "Hai nhiệm vụ không cùng dự án!"
                    e.message?.contains("SELF_DEPENDENCY") == true -> "Nhiệm vụ không thể tự phụ thuộc chính nó!"
                    else -> e.message ?: "Lỗi thêm phụ thuộc"
                }
            } finally {
                isLoading = false
            }
        }
    }

    fun updateTaskStatus(
        taskId: String,
        newStatus: TeamOsRepository.TaskStatus,
        onSuccess: (() -> Unit)? = null
    ) {
        val uid = currentUserId ?: return
        val proj = selectedProject ?: return

        if (!canEdit) {
            errorMessage = "Vai trò Người xem (VIEWER) không có quyền đổi trạng thái nhiệm vụ"
            return
        }

        vmScope.launch {
            isLoading = true
            try {
                withContext(ioDispatcher) {
                    repository.updateTaskStatus(uid, proj.id, taskId, newStatus)
                }
                refreshProjectDetails()
                onSuccess?.invoke()
            } catch (e: Exception) {
                errorMessage = e.message ?: "Lỗi cập nhật trạng thái nhiệm vụ"
            } finally {
                isLoading = false
            }
        }
    }

    fun runGapAnalysis() {
        val uid = currentUserId ?: return
        val proj = selectedProject ?: return

        vmScope.launch {
            isLoading = true
            try {
                val list = withContext(ioDispatcher) {
                    repository.analyzeGaps(uid, proj.id)
                }
                warnings = list
                actionSuccessMessage = "Đã kiểm tra yêu cầu và tài liệu"
            } catch (e: Exception) {
                errorMessage = e.message ?: "Lỗi kiểm tra nội dung"
            } finally {
                isLoading = false
            }
        }
    }

    fun reviewWarning(
        warningId: String,
        decision: TeamOsRepository.WarningStatus,
        onSuccess: (() -> Unit)? = null
    ) {
        val uid = currentUserId ?: return
        val proj = selectedProject ?: return

        if (!canEdit) {
            errorMessage = "Vai trò Người xem (VIEWER) không có quyền thẩm định cảnh báo"
            return
        }

        vmScope.launch {
            isLoading = true
            try {
                withContext(ioDispatcher) {
                    repository.reviewWarning(uid, proj.id, warningId, decision)
                }
                actionSuccessMessage = if (decision == TeamOsRepository.WarningStatus.RESOLVED)
                    "Đã xác nhận khắc phục cảnh báo" else "Đã bỏ qua cảnh báo"
                refreshProjectDetails()
                onSuccess?.invoke()
            } catch (e: Exception) {
                errorMessage = e.message ?: "Lỗi thẩm định cảnh báo"
            } finally {
                isLoading = false
            }
        }
    }

    fun dismissError() {
        errorMessage = null
    }

    fun dismissActionMessage() {
        actionSuccessMessage = null
    }
}
