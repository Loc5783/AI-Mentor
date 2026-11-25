package com.example.asm_final.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import android.provider.OpenableColumns
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.asm_final.AiMentorApplication
import com.example.asm_final.R
import com.example.asm_final.learning.*
import com.example.mentor.data.ActiveUser
import com.example.mentor.ui.design.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamOsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val ownerId = remember(context) { ActiveUser.get(context).toString() }
    val app = context.applicationContext as AiMentorApplication
    val repository = remember(app) { app.services().teamOs() }
    val scope = rememberCoroutineScope()
    val viewModel = remember(repository, scope) {
        TeamOsViewModel(repository, scope, learnerDirectory = app.services().learnerDirectory())
    }

    LaunchedEffect(ownerId) {
        viewModel.loadProjects(ownerId)
    }

    BackHandler {
        if (viewModel.screen == TeamOsScreen.PROJECT_DETAIL) {
            viewModel.backToProjectList()
        } else {
            onBack()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (viewModel.screen == TeamOsScreen.PROJECT_DETAIL)
                            viewModel.selectedProject?.title ?: "Chi tiết dự án"
                        else "Dự án nhóm (TeamOS)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (viewModel.screen == TeamOsScreen.PROJECT_DETAIL) {
                            viewModel.backToProjectList()
                        } else {
                            onBack()
                        }
                    }) {
                        Text(
                            text = "←",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    if (viewModel.screen == TeamOsScreen.PROJECT_DETAIL) {
                        viewModel.currentUserRole?.let { role ->
                            RoleBadge(role = role)
                            Spacer(Modifier.width(8.dp))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Error banner
            viewModel.errorMessage?.let { error ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { viewModel.dismissError() }) {
                            Text("Đóng", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // Success banner
            viewModel.actionSuccessMessage?.let { msg ->
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = msg,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { viewModel.dismissActionMessage() }) {
                            Text("Đóng", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            if (viewModel.isLoading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }

            when (viewModel.screen) {
                TeamOsScreen.PROJECT_LIST -> {
                    TeamOsProjectListScreen(viewModel = viewModel)
                }
                TeamOsScreen.PROJECT_DETAIL -> {
                    TeamOsProjectDetailScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun TeamOsProjectListScreen(viewModel: TeamOsViewModel) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var declineInvite by remember { mutableStateOf<TeamOsRepository.TeamInvitation?>(null) }

    Column(Modifier.fillMaxSize()) {
        if (viewModel.invitations.isNotEmpty()) {
            Text("Lời mời tham gia", Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            viewModel.invitations.forEach { invite ->
                Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text(invite.title, style = MaterialTheme.typography.titleMedium)
                        Text("Vai trò: ${invite.role.name}", style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { viewModel.respondToInvitation(invite.projectId, true) },
                                enabled = !viewModel.isLoading) { Text("Tham gia") }
                            TextButton(onClick = { declineInvite = invite }, enabled = !viewModel.isLoading) {
                                Text("Từ chối")
                            }
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Danh sách dự án của bạn",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Button(
                onClick = { showCreateDialog = true },
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("+ Dự án mới", style = MaterialTheme.typography.labelMedium)
            }
        }

        if (viewModel.projects.isEmpty() && !viewModel.isLoading) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Chưa tham gia dự án nhóm nào",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Bấm '+ Dự án mới' để bắt đầu làm việc nhóm cùng AI Mentor",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(viewModel.projects, key = { it.id }) { project ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectProject(project) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = project.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "v${project.version}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            if (!project.description.isNullOrBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = project.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${project.members.size} thành viên",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Xem chi tiết →",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateProjectDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { title, desc ->
                viewModel.createProject(title, desc) {
                    showCreateDialog = false
                }
            }
        )
    }
    declineInvite?.let { invite ->
        AlertDialog(onDismissRequest = { declineInvite = null }, title = { Text("Từ chối lời mời?") },
            text = { Text("Bạn sẽ không tham gia dự án ${invite.title}.") },
            confirmButton = { Button(onClick = {
                viewModel.respondToInvitation(invite.projectId, false)
                declineInvite = null
            }) { Text("Từ chối") } },
            dismissButton = { TextButton(onClick = { declineInvite = null }) { Text("Quay lại") } })
    }
}

@Composable
fun TeamOsProjectDetailScreen(viewModel: TeamOsViewModel) {
    Column(Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = viewModel.currentTab.ordinal,
            edgePadding = 12.dp,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            TeamOsTab.values().forEach { tab ->
                Tab(
                    selected = viewModel.currentTab == tab,
                    onClick = { viewModel.switchTab(tab) },
                    text = {
                        Text(
                            text = when (tab) {
                                TeamOsTab.OVERVIEW -> "Tổng quan"
                                TeamOsTab.MEMBERS -> "Thành viên (${viewModel.members.size})"
                                TeamOsTab.REQUIREMENTS -> "Yêu cầu (${viewModel.requirements.size})"
                                TeamOsTab.DOCUMENTS -> "Tài liệu (${viewModel.documents.size})"
                                TeamOsTab.TASKS -> "Nhiệm vụ (${viewModel.tasks.size})"
                                TeamOsTab.WARNINGS -> "Cảnh báo nội dung (${viewModel.warnings.size})"
                            },
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                )
            }
        }

        Box(
            Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when (viewModel.currentTab) {
                TeamOsTab.OVERVIEW -> TeamOsOverviewTab(viewModel)
                TeamOsTab.MEMBERS -> TeamOsMembersTab(viewModel)
                TeamOsTab.REQUIREMENTS -> TeamOsRequirementsTab(viewModel)
                TeamOsTab.DOCUMENTS -> TeamOsDocumentsTab(viewModel)
                TeamOsTab.TASKS -> TeamOsTasksTab(viewModel)
                TeamOsTab.WARNINGS -> TeamOsWarningsTab(viewModel)
            }
        }
    }
}

@Composable
fun TeamOsOverviewTab(viewModel: TeamOsViewModel) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val project = viewModel.selectedProject ?: return
    val totalTasks = viewModel.tasks.size
    val doneTasks = viewModel.tasks.count { it.status == TeamOsRepository.TaskStatus.DONE }
    val progressPercent = if (totalTasks > 0) (doneTasks * 100) / totalTasks else 0
    val activeWarnings = viewModel.warnings.count { it.status == TeamOsRepository.WarningStatus.NEEDS_CHECK }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(text = project.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (!project.description.isNullOrBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(text = project.description, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Tiến độ nhiệm vụ: $progressPercent%", style = MaterialTheme.typography.labelMedium)
                    Text(text = "$doneTasks / $totalTasks hoàn thành", style = MaterialTheme.typography.labelSmall)
                }
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { if (totalTasks > 0) doneTasks.toFloat() / totalTasks else 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            }
        }

        // Quick Stats Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(title = "Thành viên", value = viewModel.members.size.toString(), modifier = Modifier.weight(1f))
            StatCard(title = "Yêu cầu GV", value = viewModel.requirements.size.toString(), modifier = Modifier.weight(1f))
            StatCard(title = "Tài liệu", value = viewModel.documents.size.toString(), modifier = Modifier.weight(1f))
            StatCard(
                title = "Cảnh báo nội dung",
                value = activeWarnings.toString(),
                modifier = Modifier.weight(1f),
                isAlert = activeWarnings > 0
            )
        }

        // Action recommendations
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = "Hành động nhanh",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.runGapAnalysis() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Đối chiếu nội dung", style = MaterialTheme.typography.labelSmall)
                    }
                    if (viewModel.canEdit) {
                        Button(
                            onClick = { viewModel.switchTab(TeamOsTab.TASKS) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Xem nhiệm vụ", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
        if (viewModel.canDeleteProject) {
            TextButton(onClick = { showDeleteDialog = true }) {
                Text("Xóa dự án", color = MaterialTheme.colorScheme.error)
            }
        }
    }
    if (showDeleteDialog) {
        var confirmation by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Xóa vĩnh viễn dự án?") },
            text = {
                Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Tất cả thành viên sẽ mất quyền truy cập. Tài liệu, tệp đã tải, nhiệm vụ, yêu cầu và cảnh báo của dự án sẽ bị xóa và không thể khôi phục.")
                    Text("Nhập tên dự án để xác nhận: ${project.title}")
                    OutlinedTextField(value = confirmation, onValueChange = { confirmation = it },
                        label = { Text("Tên dự án") }, singleLine = true)
                    OutlinedTextField(value = password, onValueChange = { password = it },
                        label = { Text("Mật khẩu tài khoản của bạn") }, singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation())
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.deleteProject(password) { showDeleteDialog = false } },
                    enabled = confirmation == project.title && password.isNotBlank() && !viewModel.isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text("Xóa vĩnh viễn")
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Hủy") } }
        )
    }
}

@Composable
fun StatCard(title: String, value: String, modifier: Modifier = Modifier, isAlert: Boolean = false) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isAlert) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (isAlert) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = if (isAlert) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun TeamOsMembersTab(viewModel: TeamOsViewModel) {
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var memberToRemove by remember { mutableStateOf<TeamOsRepository.TeamMember?>(null) }
    var memberToChange by remember { mutableStateOf<TeamOsRepository.TeamMember?>(null) }
    var newRole by remember { mutableStateOf(TeamOsRepository.TeamRole.MEMBER) }

    Column(Modifier.fillMaxSize()) {
        if (viewModel.canManageMembers) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quản lý thành viên",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Button(
                    onClick = { showAddMemberDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("+ Thêm thành viên", style = MaterialTheme.typography.labelMedium)
                }
            }
        } else {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Chỉ Trưởng nhóm (OWNER) hoặc Quản trị viên (ADMIN) mới có quyền quản lý thành viên.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        if (viewModel.members.any { it.displayName == null && it.userId != viewModel.currentUserId }) {
            TextButton(onClick = { viewModel.refreshProjectDetails() },
                modifier = Modifier.align(Alignment.End)) { Text("Tải lại tên thành viên") }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(viewModel.members, key = { it.userId }) { member ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = member.displayName ?: if (member.userId == viewModel.currentUserId) "Bạn" else "Chưa tải được tên",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(4.dp))
                            RoleBadge(role = member.role)
                            if (member.status == TeamOsRepository.MemberStatus.INVITED) {
                                Text("Đang chờ chấp nhận", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        if (viewModel.canManageMembers && member.userId != viewModel.currentUserId && member.role != TeamOsRepository.TeamRole.OWNER) {
                            if (member.status == TeamOsRepository.MemberStatus.ACTIVE) {
                                TextButton(onClick = { memberToChange = member; newRole = member.role }) {
                                    Text("Đổi quyền")
                                }
                            }
                            OutlinedButton(
                                onClick = { memberToRemove = member },
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text(if (member.status == TeamOsRepository.MemberStatus.INVITED) "Hủy mời" else "Xóa",
                                    style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddMemberDialog) {
        AddMemberDialog(
            onDismiss = { showAddMemberDialog = false },
            viewModel = viewModel,
            onAdd = { targetId, role ->
                viewModel.addMember(targetId, role) {
                    showAddMemberDialog = false
                }
            }
        )
    }
    memberToRemove?.let { member ->
        AlertDialog(onDismissRequest = { memberToRemove = null }, title = { Text("Xóa khỏi nhóm?") },
            text = { Text(if (member.status == TeamOsRepository.MemberStatus.INVITED)
                "Hủy lời mời của ${member.displayName ?: "người này"}?"
                else "${member.displayName ?: "Thành viên này"} sẽ mất quyền truy cập dự án và tệp của nhóm.") },
            confirmButton = { Button(onClick = {
                viewModel.removeMember(member.userId) { memberToRemove = null }
            }, enabled = !viewModel.isLoading) { Text("Xóa thành viên") } },
            dismissButton = { TextButton(onClick = { memberToRemove = null }) { Text("Quay lại") } })
    }
    memberToChange?.let { member ->
        AlertDialog(onDismissRequest = { memberToChange = null }, title = { Text("Đổi quyền thành viên") },
            text = { Column {
                Text(member.displayName ?: "Thành viên")
                TeamOsRepository.TeamRole.values().filter { it != TeamOsRepository.TeamRole.OWNER }.forEach { role ->
                    FilterChip(selected = newRole == role, onClick = { newRole = role },
                        label = { Text(role.name) })
                }
                Text("Quyền mới có hiệu lực ngay sau khi xác nhận.")
            } },
            confirmButton = { Button(onClick = {
                viewModel.changeMemberRole(member.userId, newRole) { memberToChange = null }
            }, enabled = newRole != member.role && !viewModel.isLoading) { Text("Xác nhận đổi quyền") } },
            dismissButton = { TextButton(onClick = { memberToChange = null }) { Text("Quay lại") } })
    }
}

@Composable
fun TeamOsRequirementsTab(viewModel: TeamOsViewModel) {
    var showAddDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Yêu cầu của giáo viên",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            if (viewModel.canEdit) {
                Button(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("+ Thêm yêu cầu", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        if (viewModel.requirements.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Chưa có yêu cầu giáo viên nào được ghi nhận",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(viewModel.requirements, key = { it.id }) { req ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = req.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                PriorityBadge(priority = req.priority)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = req.description,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddRequirementDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { title, desc, prio ->
                viewModel.addRequirement(title, desc, prio) {
                    showAddDialog = false
                }
            }
        )
    }
}

@Composable
fun TeamOsDocumentsTab(viewModel: TeamOsViewModel) {
    val context = LocalContext.current
    var showUploadDialog by remember { mutableStateOf(false) }
    var editingDoc by remember { mutableStateOf<TeamOsRepository.TeamDocument?>(null) }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tài liệu nhóm",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            if (viewModel.canUploadFile) {
                Button(
                    onClick = { showUploadDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("+ Tải tài liệu", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        if (viewModel.documents.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Chưa có tài liệu nào trong dự án",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(viewModel.documents, key = { it.id }) { doc ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = doc.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = "v${doc.version}",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            if (doc.content.isNotBlank()) Text(doc.content,
                                style = MaterialTheme.typography.bodySmall, maxLines = 4,
                                overflow = TextOverflow.Ellipsis)
                            if (doc.fileName != null) {
                                Text("Tệp: ${doc.fileName}", style = MaterialTheme.typography.bodySmall)
                                TextButton(onClick = {
                                    viewModel.openFile(doc) { file ->
                                        val uri = FileProvider.getUriForFile(context,
                                            "${context.packageName}.fileprovider", file)
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, doc.fileMime)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        runCatching { context.startActivity(intent) }
                                            .onFailure { android.widget.Toast.makeText(context,
                                                "Máy chưa có ứng dụng mở loại tệp này", android.widget.Toast.LENGTH_SHORT).show() }
                                    }
                                }) { Text("Mở tệp") }
                            }
                            if (viewModel.canEdit) {
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(onClick = { editingDoc = doc }) {
                                        Text("Sửa tài liệu", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showUploadDialog) {
        UploadDocumentDialog(
            onDismiss = { showUploadDialog = false },
            isBusy = viewModel.isLoading,
            onUpload = { title, content, file ->
                viewModel.uploadFile(title, content, file) {
                    showUploadDialog = false
                }
            }
        )
    }

    editingDoc?.let { doc ->
        EditDocumentDialog(
            initialTitle = doc.title,
            initialContent = doc.content,
            onDismiss = { editingDoc = null },
            onSave = { newTitle, newContent ->
                viewModel.updateDocument(doc.id, newTitle, newContent) {
                    editingDoc = null
                }
            }
        )
    }
}

@Composable
fun TeamOsTasksTab(viewModel: TeamOsViewModel) {
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var taskForDependency by remember { mutableStateOf<TeamOsRepository.TeamTask?>(null) }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Nhiệm vụ (${viewModel.tasks.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            if (viewModel.canEdit) {
                Button(
                    onClick = { showAddTaskDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("+ Tạo nhiệm vụ", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        if (viewModel.tasks.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Chưa có nhiệm vụ nào được xác nhận trong dự án",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(viewModel.tasks, key = { it.id }) { task ->
                    val deps = viewModel.taskDependencies[task.id] ?: emptySet()
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = task.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                PriorityBadge(priority = task.priority)
                            }
                            if (!task.description.isNullOrBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(text = task.description, style = MaterialTheme.typography.bodySmall)
                            }

                            if (deps.isNotEmpty()) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "Phụ thuộc: ${deps.joinToString(", ") { depId ->
                                        viewModel.tasks.firstOrNull { it.id == depId }?.title ?: depId
                                    }}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TaskStatusBadge(status = task.status)
                                if (viewModel.canEdit) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        TextButton(
                                            onClick = { taskForDependency = task },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("+ Phụ thuộc", style = MaterialTheme.typography.labelSmall)
                                        }
                                        if (task.status != TeamOsRepository.TaskStatus.DONE) {
                                            FilledTonalButton(
                                                onClick = {
                                                    val next = if (task.status == TeamOsRepository.TaskStatus.TODO)
                                                        TeamOsRepository.TaskStatus.IN_PROGRESS
                                                    else TeamOsRepository.TaskStatus.DONE
                                                    viewModel.updateTaskStatus(task.id, next)
                                                },
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = if (task.status == TeamOsRepository.TaskStatus.TODO) "Bắt đầu" else "Xong",
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddTaskDialog) {
        AddTaskDialog(
            existingTasks = viewModel.tasks,
            onDismiss = { showAddTaskDialog = false },
            onAdd = { title, desc, assignee, deadline, prio, depId ->
                viewModel.createTask(title, desc, assignee, deadline, prio, depId) {
                    showAddTaskDialog = false
                }
            }
        )
    }

    taskForDependency?.let { currentTask ->
        AddDependencyDialog(
            currentTask = currentTask,
            availableTasks = viewModel.tasks.filter { it.id != currentTask.id },
            onDismiss = { taskForDependency = null },
            onAdd = { dependsOnId ->
                viewModel.addTaskDependency(currentTask.id, dependsOnId) {
                    taskForDependency = null
                }
            }
        )
    }
}

@Composable
fun TeamOsWarningsTab(viewModel: TeamOsViewModel) {
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Cảnh báo nội dung",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Button(
                onClick = { viewModel.runGapAnalysis() },
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("Kiểm tra nội dung", style = MaterialTheme.typography.labelMedium)
            }
        }

        if (viewModel.warnings.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Không có cảnh báo nào. Bấm 'Kiểm tra nội dung' để kiểm tra khoảng cách nội dung.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(viewModel.warnings, key = { it.id }) { warning ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when (warning.status) {
                                TeamOsRepository.WarningStatus.NEEDS_CHECK -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                TeamOsRepository.WarningStatus.RESOLVED -> MaterialTheme.colorScheme.surfaceVariant
                                TeamOsRepository.WarningStatus.DISMISSED -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            }
                        )
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                WarningTypeBadge(type = warning.warningType)
                                WarningStatusBadge(status = warning.status)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = warning.message,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Vị trí: ${warning.locationInfo}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            if (!warning.excerpt.isNullOrBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Trích đoạn: \"${warning.excerpt}\"",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }
                            if (!warning.explanation.isNullOrBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Giải thích: ${warning.explanation}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            if (!warning.evidenceRef.isNullOrBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Dẫn chứng: ${warning.evidenceRef}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            if (viewModel.canEdit && warning.status == TeamOsRepository.WarningStatus.NEEDS_CHECK) {
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { viewModel.reviewWarning(warning.id, TeamOsRepository.WarningStatus.DISMISSED) },
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("Bỏ qua", style = MaterialTheme.typography.labelSmall)
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Button(
                                        onClick = { viewModel.reviewWarning(warning.id, TeamOsRepository.WarningStatus.RESOLVED) },
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("Xác nhận khắc phục", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Dialogs

@Composable
fun CreateProjectDialog(
    onDismiss: () -> Unit,
    onCreate: (title: String, description: String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tạo dự án nhóm mới") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tên dự án *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Mô tả ngắn") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (title.isNotBlank()) onCreate(title, description) },
                enabled = title.isNotBlank()
            ) {
                Text("Tạo")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}

@Composable
fun AddMemberDialog(
    onDismiss: () -> Unit,
    viewModel: TeamOsViewModel,
    onAdd: (userId: String, role: TeamOsRepository.TeamRole) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<com.example.asm_final.competition.FriendsViewModel.SearchItem?>(null) }
    var selectedRole by remember { mutableStateOf(TeamOsRepository.TeamRole.MEMBER) }
    LaunchedEffect(Unit) { viewModel.searchMembers("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thêm thành viên vào nhóm") },
        text = {
            Column(Modifier.heightIn(max = 350.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it; selected = null; viewModel.searchMembers(it) },
                    label = { Text("Tìm tên người dùng (ít nhất 2 ký tự)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (viewModel.memberSearchLoading) CircularProgressIndicator(Modifier.size(20.dp))
                if (viewModel.memberSearchError != null) Text(viewModel.memberSearchError!!,
                    color = MaterialTheme.colorScheme.error)
                viewModel.memberCandidates.forEach { person ->
                    Surface(onClick = { selected = person }, shape = RoundedCornerShape(8.dp),
                        color = if (selected?.id == person.id) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant) {
                        Text(person.nickname, Modifier.fillMaxWidth().padding(12.dp))
                    }
                }
                if (query.trim().length >= 2 && !viewModel.memberSearchLoading &&
                    viewModel.memberCandidates.isEmpty() && viewModel.memberSearchError == null) {
                    Text("Không tìm thấy tài khoản phù hợp", style = MaterialTheme.typography.bodySmall)
                }
                Text("Chọn vai trò:", style = MaterialTheme.typography.labelMedium)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    TeamOsRepository.TeamRole.values().filter { it != TeamOsRepository.TeamRole.OWNER }.forEach { role ->
                        FilterChip(
                            selected = selectedRole == role,
                            onClick = { selectedRole = role },
                            label = { Text(when (role) {
                                TeamOsRepository.TeamRole.ADMIN -> "Quản trị"
                                TeamOsRepository.TeamRole.MEMBER -> "Thành viên"
                                TeamOsRepository.TeamRole.VIEWER -> "Xem và tải tệp"
                                TeamOsRepository.TeamRole.OWNER -> "Trưởng nhóm"
                            }, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selected?.let { onAdd(it.id, selectedRole) } },
                enabled = selected != null && !viewModel.isLoading
            ) {
                Text("Thêm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}

@Composable
fun AddRequirementDialog(
    onDismiss: () -> Unit,
    onAdd: (title: String, desc: String, priority: TeamOsRepository.TaskPriority) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(TeamOsRepository.TaskPriority.MEDIUM) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thêm yêu cầu giáo viên") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tiêu đề yêu cầu *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Chi tiết yêu cầu *") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )
                Text("Độ ưu tiên:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TeamOsRepository.TaskPriority.values().forEach { prio ->
                        FilterChip(
                            selected = priority == prio,
                            onClick = { priority = prio },
                            label = { Text(prio.name, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (title.isNotBlank() && desc.isNotBlank()) onAdd(title, desc, priority) },
                enabled = title.isNotBlank() && desc.isNotBlank()
            ) {
                Text("Thêm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}

@Composable
fun UploadDocumentDialog(
    onDismiss: () -> Unit,
    isBusy: Boolean = false,
    onUpload: (title: String, content: String, file: TeamOsRepository.TeamFileInput) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedName by remember { mutableStateOf("") }
    var reading by remember { mutableStateOf(false) }
    var fileError by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        selectedUri = uri
        if (uri != null) {
            val name = runCatching {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                    ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
            }.getOrNull() ?: "Tài liệu"
            selectedName = name
            if (title.isBlank()) title = name.substringBeforeLast('.')
            fileError = null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tải lên tài liệu mới") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { picker.launch(arrayOf("application/pdf",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "text/plain", "image/jpeg", "image/png", "image/webp")) }) {
                    Text(if (selectedUri == null) "Chọn tệp từ máy" else "Đổi tệp: $selectedName")
                }
                Text("PDF, Word, PowerPoint, Excel, TXT hoặc ảnh · tối đa 8 MB",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tên tài liệu *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Mô tả (không bắt buộc)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 6
                )
                Text("AI đối chiếu phần mô tả; các thành viên có thể mở tệp gốc.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (fileError != null) Text(fileError!!, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val uri = selectedUri ?: return@Button
                    reading = true
                    scope.launch {
                        try {
                            val file = withContext(Dispatchers.IO) {
                                val mime = context.contentResolver.getType(uri)
                                    ?.takeUnless { it == "application/octet-stream" }
                                    ?: when (selectedName.substringAfterLast('.', "").lowercase()) {
                                        "pdf" -> "application/pdf"
                                        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                                        "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                                        "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                        "txt" -> "text/plain"
                                        "jpg", "jpeg" -> "image/jpeg"
                                        "png" -> "image/png"
                                        "webp" -> "image/webp"
                                        else -> throw IllegalArgumentException("Chỉ hỗ trợ PDF, DOCX, PPTX, XLSX, TXT và ảnh")
                                    }
                                val bytes = context.contentResolver.openInputStream(uri)?.use { stream ->
                                    val out = java.io.ByteArrayOutputStream()
                                    val buffer = ByteArray(8192)
                                    while (true) {
                                        val count = stream.read(buffer)
                                        if (count < 0) break
                                        require(out.size() + count <= 8 * 1024 * 1024) { "Tệp vượt quá 8 MB" }
                                        out.write(buffer, 0, count)
                                    }
                                    out.toByteArray()
                                } ?: throw IllegalArgumentException("Không đọc được tệp")
                                TeamOsRepository.TeamFileInput(selectedName, mime, bytes)
                            }
                            onUpload(title.trim(), content.trim(), file)
                        } catch (e: Exception) {
                            fileError = e.message ?: "Không đọc được tệp"
                        } finally { reading = false }
                    }
                },
                enabled = title.isNotBlank() && selectedUri != null && !reading && !isBusy
            ) {
                Text("Tải lên")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}

@Composable
fun EditDocumentDialog(
    initialTitle: String,
    initialContent: String,
    onDismiss: () -> Unit,
    onSave: (title: String, content: String) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var content by remember { mutableStateOf(initialContent) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chỉnh sửa tài liệu (Tăng phiên bản)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tên tài liệu *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Nội dung cập nhật *") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 6
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (title.isNotBlank() && content.isNotBlank()) onSave(title, content) },
                enabled = title.isNotBlank() && content.isNotBlank()
            ) {
                Text("Lưu phiên bản mới")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}

@Composable
fun AddTaskDialog(
    existingTasks: List<TeamOsRepository.TeamTask>,
    onDismiss: () -> Unit,
    onAdd: (title: String, desc: String?, assignee: String?, deadline: String?, priority: TeamOsRepository.TaskPriority, depId: String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var assignee by remember { mutableStateOf("") }
    var deadline by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(TeamOsRepository.TaskPriority.MEDIUM) }
    var selectedDepId by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tạo nhiệm vụ nhóm") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tên nhiệm vụ *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Mô tả công việc") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                OutlinedTextField(
                    value = assignee,
                    onValueChange = { assignee = it },
                    label = { Text("ID người phụ trách (tùy chọn)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = deadline,
                    onValueChange = { deadline = it },
                    label = { Text("Hạn chót (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Độ ưu tiên:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TeamOsRepository.TaskPriority.values().forEach { prio ->
                        FilterChip(
                            selected = priority == prio,
                            onClick = { priority = prio },
                            label = { Text(prio.name, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                if (existingTasks.isNotEmpty()) {
                    Text("Phụ thuộc vào nhiệm vụ khác:", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(existingTasks) { t ->
                            FilterChip(
                                selected = selectedDepId == t.id,
                                onClick = { selectedDepId = if (selectedDepId == t.id) null else t.id },
                                label = { Text(t.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onAdd(
                            title,
                            desc.takeIf { it.isNotBlank() },
                            assignee.takeIf { it.isNotBlank() },
                            deadline.takeIf { it.isNotBlank() },
                            priority,
                            selectedDepId
                        )
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("Tạo")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}

@Composable
fun AddDependencyDialog(
    currentTask: TeamOsRepository.TeamTask,
    availableTasks: List<TeamOsRepository.TeamTask>,
    onDismiss: () -> Unit,
    onAdd: (dependsOnId: String) -> Unit
) {
    var selectedTaskId by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thêm phụ thuộc cho nhiệm vụ") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Nhiệm vụ: ${currentTask.title}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Chọn nhiệm vụ phải hoàn thành trước:",
                    style = MaterialTheme.typography.labelMedium
                )
                if (availableTasks.isEmpty()) {
                    Text("Không có nhiệm vụ nào khác trong dự án", style = MaterialTheme.typography.bodySmall)
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(availableTasks) { task ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (selectedTaskId == task.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedTaskId = task.id }
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedTaskId == task.id,
                                        onClick = { selectedTaskId = task.id }
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(text = task.title, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selectedTaskId?.let { onAdd(it) } },
                enabled = selectedTaskId != null
            ) {
                Text("Xác nhận")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}

// Badges

@Composable
fun RoleBadge(role: TeamOsRepository.TeamRole) {
    val (label, bg, fg) = when (role) {
        TeamOsRepository.TeamRole.OWNER -> Triple("Trưởng nhóm", MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)
        TeamOsRepository.TeamRole.ADMIN -> Triple("Quản trị", MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.onSecondary)
        TeamOsRepository.TeamRole.MEMBER -> Triple("Thành viên", MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.onTertiary)
        TeamOsRepository.TeamRole.VIEWER -> Triple("Xem và tải tệp", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = bg
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun PriorityBadge(priority: TeamOsRepository.TaskPriority) {
    val (label, color) = when (priority) {
        TeamOsRepository.TaskPriority.HIGH -> "Cao" to MaterialTheme.colorScheme.error
        TeamOsRepository.TaskPriority.MEDIUM -> "Vừa" to MaterialTheme.colorScheme.primary
        TeamOsRepository.TaskPriority.LOW -> "Thấp" to MaterialTheme.colorScheme.outline
    }
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun TaskStatusBadge(status: TeamOsRepository.TaskStatus) {
    val (label, color) = when (status) {
        TeamOsRepository.TaskStatus.TODO -> "Cần làm" to MaterialTheme.colorScheme.outline
        TeamOsRepository.TaskStatus.IN_PROGRESS -> "Đang làm" to MaterialTheme.colorScheme.primary
        TeamOsRepository.TaskStatus.DONE -> "Hoàn thành" to MaterialTheme.colorScheme.tertiary
    }
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun WarningTypeBadge(type: TeamOsRepository.WarningType) {
    val label = when (type) {
        TeamOsRepository.WarningType.MISSING_CONTENT -> "Thiếu nội dung"
        TeamOsRepository.WarningType.DUPLICATION -> "Trùng lặp"
        TeamOsRepository.WarningType.CONTRADICTION -> "Mâu thuẫn"
        TeamOsRepository.WarningType.UNGROUNDED_CLAIM -> "Thiếu căn cứ"
    }
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun WarningStatusBadge(status: TeamOsRepository.WarningStatus) {
    val (label, color) = when (status) {
        TeamOsRepository.WarningStatus.NEEDS_CHECK -> "Cần kiểm tra" to MaterialTheme.colorScheme.error
        TeamOsRepository.WarningStatus.RESOLVED -> "Đã khắc phục" to MaterialTheme.colorScheme.primary
        TeamOsRepository.WarningStatus.DISMISSED -> "Đã bỏ qua" to MaterialTheme.colorScheme.outline
    }
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
