package com.example.mentor.schedule.ui

import com.example.mentor.ui.design.mentorHeader
import com.example.mentor.ui.design.MentorSkeleton

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.mentor.schedule.R
import com.example.mentor.schedule.domain.ScheduleEntry
import com.example.mentor.schedule.domain.ScheduleRepository
import com.example.mentor.schedule.domain.TodoTask
import kotlinx.coroutines.launch

import android.app.Application
import androidx.compose.ui.platform.LocalContext

private val NullableScheduleEntrySaver: Saver<ScheduleEntry?, Any> = listSaver(
    save = { entry ->
        if (entry == null) {
            listOf()
        } else {
            listOf(
                entry.id,
                entry.accountId,
                entry.title,
                entry.dayOfWeek,
                entry.period,
                entry.teacher ?: "",
                entry.notes ?: ""
            )
        }
    },
    restore = { list ->
        if (list.isEmpty()) {
            null
        } else {
            val teacherStr = list[5] as? String
            val notesStr = list[6] as? String
            ScheduleEntry(
                id = (list[0] as? Number)?.toLong() ?: 0L,
                accountId = list[1] as? String ?: "",
                title = list[2] as? String ?: "",
                dayOfWeek = (list[3] as? Number)?.toInt() ?: 1,
                period = (list[4] as? Number)?.toInt() ?: 1,
                teacher = if (teacherStr.isNullOrEmpty()) null else teacherStr,
                notes = if (notesStr.isNullOrEmpty()) null else notesStr
            )
        }
    }
)

private val NullableTodoTaskSaver: Saver<TodoTask?, Any> = listSaver(
    save = { task ->
        if (task == null) {
            listOf()
        } else {
            listOf(
                task.id,
                task.accountId,
                task.title,
                task.isCompleted,
                task.dayOfWeek,
                task.priority,
                task.linkedSubject ?: "",
                task.createdAt,
                task.completedAt ?: -1L
            )
        }
    },
    restore = { list ->
        if (list.isEmpty()) {
            null
        } else {
            val subjectStr = list[6] as? String
            val completedAtVal = (list[8] as? Number)?.toLong()
            TodoTask(
                id = (list[0] as? Number)?.toLong() ?: 0L,
                accountId = list[1] as? String ?: "",
                title = list[2] as? String ?: "",
                isCompleted = list[3] as? Boolean ?: false,
                dayOfWeek = (list[4] as? Number)?.toInt() ?: 1,
                priority = (list[5] as? Number)?.toInt() ?: 1,
                linkedSubject = if (subjectStr.isNullOrEmpty()) null else subjectStr,
                createdAt = (list[7] as? Number)?.toLong() ?: 0L,
                completedAt = if (completedAtVal == null || completedAtVal < 0) null else completedAtVal
            )
        }
    }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    accountId: String,
    repository: ScheduleRepository,
    onBack: () -> Unit = {},
    onAiPhoto: () -> Unit = {},
    onAiPlan: () -> Unit = {}
) {
    val context = LocalContext.current
    val factory = remember { ScheduleViewModelFactory(context.applicationContext as Application, accountId, repository) }
    val viewModel: ScheduleViewModel = viewModel(factory = factory)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshOnResume()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    
    val daysArray = stringResource(R.string.schedule_days).split(",")
    val selectedDay by viewModel.selectedDay
    
    var showScheduleForm by rememberSaveable { mutableStateOf(false) }
    var showTodoForm by rememberSaveable { mutableStateOf(false) }
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var entryToEdit by rememberSaveable(stateSaver = NullableScheduleEntrySaver) { mutableStateOf<ScheduleEntry?>(null) }
    var taskToEdit by rememberSaveable(stateSaver = NullableTodoTaskSaver) { mutableStateOf<TodoTask?>(null) }
    var scheduleDialogSession by rememberSaveable { mutableStateOf(0) }
    var todoDialogSession by rememberSaveable { mutableStateOf(0) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    val undoMsg = stringResource(R.string.schedule_undo)
    val deletedMsgSchedule = stringResource(R.string.schedule_deleted)
    val deletedMsgTodo = stringResource(R.string.todo_deleted)

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.mentorHeader(com.example.mentor.ui.design.MentorArtwork.PLANNER),
                title = {
                    Text(
                        stringResource(R.string.schedule_page_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                actions = {
                    IconButton(onClick = { showHistory = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = stringResource(R.string.schedule_history))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (viewModel.error.value) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            stringResource(R.string.schedule_connection_error),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { viewModel.loadData() }) {
                            Text(stringResource(R.string.schedule_retry))
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .24f)),
                shadowElevation = 6.dp,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Column(
                    Modifier.background(Brush.linearGradient(listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = .8f)
                    ))).padding(top = 18.dp, bottom = 7.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(stringResource(R.string.schedule_week_title),
                        modifier = Modifier.padding(horizontal = 18.dp),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(stringResource(R.string.schedule_week_hint),
                        modifier = Modifier.padding(horizontal = 18.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .82f))
                    DaySelectorStrip(daysArray, selectedDay) { viewModel.selectDay(it) }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.schedule_ai_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ScheduleAiActionCard(Modifier.weight(1f), R.string.schedule_ai_photo,
                        R.string.schedule_ai_photo_hint, onAiPhoto)
                    ScheduleAiActionCard(Modifier.weight(1f), R.string.schedule_ai_plan,
                        R.string.schedule_ai_plan_hint, onAiPlan)
                }
            }

            // === KHUNG 1: THỜI KHÓA BIỂU ===
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                shadowElevation = 5.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
                    // Header: Tiêu đề + Nút Thêm lịch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.schedule_card_timetable),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        FilledTonalButton(
                            onClick = {
                                entryToEdit = null
                                scheduleDialogSession++
                                showScheduleForm = true
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.schedule_add), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.schedule_btn_add_class))
                        }
                    }

                    // Danh sách các buổi học hoặc trạng thái trống gọn
                    if (viewModel.loading.value) {
                        MentorSkeleton(stringResource(R.string.schedule_loading), Modifier.padding(horizontal = 16.dp))
                    } else if (viewModel.error.value) {
                        ScheduleUnavailable()
                    } else if (viewModel.entries.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp, horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.schedule_empty_day),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            viewModel.entries.sortedBy { it.period }.forEach { entry ->
                                ScheduleTimelineItemV2(entry) { clicked ->
                                    entryToEdit = clicked
                                    scheduleDialogSession++
                                    showScheduleForm = true
                                }
                            }
                        }
                    }
                }
            }

            // === KHUNG 2: VIỆC CẦN LÀM ===
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                shadowElevation = 5.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
                    // Header: Tiêu đề + Nút Thêm việc
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.todo_card_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        FilledTonalButton(
                            onClick = {
                                taskToEdit = null
                                todoDialogSession++
                                showTodoForm = true
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.todo_add_title), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.todo_btn_add_task))
                        }
                    }

                    if (viewModel.loading.value) {
                        MentorSkeleton(stringResource(R.string.schedule_loading), Modifier.padding(horizontal = 16.dp))
                    } else if (viewModel.error.value) {
                        ScheduleUnavailable()
                    } else if (viewModel.todos.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp, horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.todo_empty_day),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        // Progress bar
                        val completed = viewModel.todos.count { it.isCompleted }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    stringResource(R.string.todo_progress_short),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "$completed/${viewModel.todos.size}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            LinearProgressIndicator(
                                progress = { viewModel.todoProgress.value },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = MaterialTheme.colorScheme.secondary,
                                trackColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        // Danh sách việc (Chưa xong trước, Đã xong sau)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val incomplete = viewModel.todos.filter { !it.isCompleted }.sortedByDescending { it.priority }
                            val complete = viewModel.todos.filter { it.isCompleted }.sortedByDescending { it.completedAt }

                            incomplete.forEach { task ->
                                TodoCard(
                                    task = task,
                                    onToggle = { viewModel.toggleTodo(it) },
                                    onDelete = { taskToDelete ->
                                        viewModel.deleteTodo(taskToDelete)
                                        scope.launch {
                                            val res = snackbarHostState.showSnackbar(
                                                deletedMsgTodo,
                                                undoMsg,
                                                duration = SnackbarDuration.Short
                                            )
                                            if (res == SnackbarResult.ActionPerformed) viewModel.undoDeleteTodo()
                                        }
                                    },
                                    onClick = { clicked ->
                                        taskToEdit = clicked
                                        todoDialogSession++
                                        showTodoForm = true
                                    }
                                )
                            }

                            if (complete.isNotEmpty()) {
                                Text(
                                    stringResource(R.string.todo_completed_section),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                )
                                complete.forEach { task ->
                                    TodoCard(
                                        task = task,
                                        onToggle = { viewModel.toggleTodo(it) },
                                        onDelete = { taskToDelete ->
                                            viewModel.deleteTodo(taskToDelete)
                                            scope.launch {
                                                val res = snackbarHostState.showSnackbar(
                                                    deletedMsgTodo,
                                                    undoMsg,
                                                    duration = SnackbarDuration.Short
                                                )
                                                if (res == SnackbarResult.ActionPerformed) viewModel.undoDeleteTodo()
                                            }
                                        },
                                        onClick = { clicked ->
                                            taskToEdit = clicked
                                            todoDialogSession++
                                            showTodoForm = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
    
    if (showScheduleForm) {
        ScheduleFormDialog(
            initialEntry = entryToEdit,
            accountId = accountId,
            selectedDay = selectedDay,
            viewModel = viewModel,
            dialogSession = scheduleDialogSession,
            onDismiss = {
                showScheduleForm = false
                entryToEdit = null
            },
            onSave = { entries, isEdit ->
                viewModel.saveEntries(entries, isEdit)
                showScheduleForm = false
                entryToEdit = null
            },
            onDelete = { entry ->
                viewModel.deleteEntry(entry)
                showScheduleForm = false
                entryToEdit = null
                scope.launch {
                    val result = snackbarHostState.showSnackbar(deletedMsgSchedule, undoMsg, duration = SnackbarDuration.Short)
                    if (result == SnackbarResult.ActionPerformed) viewModel.undoDeleteEntry()
                }
            }
        )
    }
    
    if (showTodoForm) {
        TodoQuickAddDialog(
            initialTask = taskToEdit,
            dialogSession = todoDialogSession,
            onDismiss = {
                showTodoForm = false
                taskToEdit = null
            },
            onSave = { title, priority ->
                if (taskToEdit != null) {
                    viewModel.updateTodo(taskToEdit!!.copy(title = title, priority = priority))
                } else {
                    viewModel.saveTodo(title, priority, null)
                }
                showTodoForm = false
                taskToEdit = null
            }
        )
    }
    
    if (showHistory) {
        ActivityLogSheet(
            logs = viewModel.logs,
            onDismiss = { showHistory = false },
            onClearLogs = { viewModel.clearLogs() }
        )
    }
}

@Composable
private fun ScheduleAiActionCard(
    modifier: Modifier,
    title: Int,
    hint: Int,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 112.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .3f)),
        shadowElevation = 4.dp,
    ) {
        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(stringResource(hint), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ScheduleUnavailable() {
    Text(stringResource(R.string.schedule_unavailable),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun TodoQuickAddDialog(
    initialTask: TodoTask? = null,
    dialogSession: Int = 0,
    onDismiss: () -> Unit,
    onSave: (String, Int) -> Unit
) {
    val isEdit = initialTask != null
    var title by rememberSaveable(dialogSession) { mutableStateOf(initialTask?.title ?: "") }
    var priority by rememberSaveable(dialogSession) { mutableStateOf(initialTask?.priority ?: 1) } // 0=Low, 1=Normal, 2=High
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) stringResource(R.string.todo_edit_title) else stringResource(R.string.todo_add_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.todo_task_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Text(stringResource(R.string.todo_priority))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    FilterChip(selected = priority == 0, onClick = { priority = 0 }, label = { Text(stringResource(R.string.todo_priority_low)) })
                    FilterChip(selected = priority == 1, onClick = { priority = 1 }, label = { Text(stringResource(R.string.todo_priority_normal)) })
                    FilterChip(selected = priority == 2, onClick = { priority = 2 }, label = { Text(stringResource(R.string.todo_priority_high), color = MaterialTheme.colorScheme.error) })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (title.isNotBlank()) onSave(title.trim(), priority) },
                enabled = title.isNotBlank()
            ) { Text(stringResource(R.string.schedule_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.schedule_cancel)) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleFormDialog(
    initialEntry: ScheduleEntry?,
    accountId: String,
    selectedDay: Int,
    viewModel: ScheduleViewModel,
    dialogSession: Int = 0,
    onDismiss: () -> Unit,
    onSave: (List<ScheduleEntry>, Boolean) -> Unit,
    onDelete: (ScheduleEntry) -> Unit
) {
    val isEdit = initialEntry != null
    var title by rememberSaveable(dialogSession) { mutableStateOf(initialEntry?.title ?: "") }
    var teacher by rememberSaveable(dialogSession) { mutableStateOf(initialEntry?.teacher ?: "") }
    var notes by rememberSaveable(dialogSession) { mutableStateOf(initialEntry?.notes ?: "") }
    
    var period by rememberSaveable(dialogSession) { mutableStateOf(initialEntry?.period ?: 1) }
    var expanded by rememberSaveable(dialogSession) { mutableStateOf(false) }
    
    val daysArray = stringResource(R.string.schedule_days).split(",")
    val selectedDays = rememberSaveable(
        dialogSession,
        saver = listSaver<SnapshotStateList<Int>, Int>(
            save = { it.toList() },
            restore = { it.toMutableStateList() }
        )
    ) {
        val list = mutableStateListOf<Int>()
        if (!isEdit) {
            list.add(selectedDay)
        } else {
            list.add(initialEntry!!.dayOfWeek)
        }
        list
    }
    
    var titleError by rememberSaveable(dialogSession) { mutableStateOf<String?>(null) }
    var overlapWarning by rememberSaveable(dialogSession) { mutableStateOf<String?>(null) }
    
    fun validate(): Boolean {
        var valid = true
        if (title.isBlank()) {
            titleError = "Tên môn là bắt buộc"
            valid = false
        } else titleError = null
        return valid
    }

    LaunchedEffect(period, selectedDays.toList()) {
        val overlaps = selectedDays.any { day ->
            viewModel.checkOverlap(day, period, initialEntry?.id ?: 0L)
        }
        if (overlaps) overlapWarning = "Cảnh báo: Tiết này đã có môn học khác"
        else overlapWarning = null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) stringResource(R.string.schedule_edit) else stringResource(R.string.schedule_add)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; validate() },
                    label = { Text(stringResource(R.string.schedule_subject)) },
                    isError = titleError != null,
                    supportingText = { titleError?.let { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = stringResource(R.string.schedule_period, period),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.schedule_period_select)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        for (i in 1..10) {
                            val timeHint = if (i <= 5) stringResource(R.string.schedule_period_morning) else stringResource(R.string.schedule_period_afternoon)
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.schedule_period, i) + " " + timeHint) },
                                onClick = {
                                    period = i
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                if (overlapWarning != null) Text(overlapWarning!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                
                if (!isEdit) {
                    Text(stringResource(R.string.schedule_repeat_days), style = MaterialTheme.typography.bodySmall)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        daysArray.forEachIndexed { index, day ->
                            val d = index + 1
                            val sel = selectedDays.contains(d)
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable { 
                                    if (sel && selectedDays.size > 1) selectedDays.remove(d) 
                                    else if (!sel) selectedDays.add(d) 
                                }
                            ) {
                                Text(day, modifier = Modifier.padding(8.dp), color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                            }
                        }
                    }
                }

                OutlinedTextField(value = teacher, onValueChange = { teacher = it }, label = { Text(stringResource(R.string.schedule_teacher)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text(stringResource(R.string.schedule_notes)) }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = {
                if (validate()) {
                    val entries = selectedDays.map { d ->
                        ScheduleEntry(
                            id = initialEntry?.id ?: 0L,
                            accountId = accountId,
                            title = title.trim(),
                            dayOfWeek = d,
                            period = period,
                            teacher = teacher.trim().takeIf { it.isNotBlank() },
                            notes = notes.trim().takeIf { it.isNotBlank() }
                        )
                    }
                    onSave(entries, isEdit)
                }
            }) { Text(stringResource(R.string.schedule_save)) }
        },
        dismissButton = {
            Row {
                if (isEdit) TextButton(onClick = { onDelete(initialEntry!!) }) { Text(stringResource(R.string.schedule_delete), color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.schedule_cancel)) }
            }
        }
    )
}
