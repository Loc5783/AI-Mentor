package com.example.mentor.schedule.ui

import com.example.mentor.ui.design.mentorHeader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mentor.schedule.R
import com.example.mentor.schedule.domain.ScheduleEntry
import com.example.mentor.schedule.domain.ScheduleRepository
import kotlinx.coroutines.launch

import android.app.Application
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    accountId: String,
    repository: ScheduleRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val factory = remember { ScheduleViewModelFactory(context.applicationContext as Application, accountId, repository) }
    val viewModel: ScheduleViewModel = viewModel(factory = factory)
    
    val daysArray = stringResource(R.string.schedule_days).split(",")
    val selectedDay by viewModel.selectedDay
    
    var selectedTabIndex by remember { mutableStateOf(0) }
    var showScheduleForm by remember { mutableStateOf(false) }
    var showTodoForm by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var entryToEdit by remember { mutableStateOf<ScheduleEntry?>(null) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    val undoMsg = stringResource(R.string.schedule_undo)
    val deletedMsgSchedule = stringResource(R.string.schedule_deleted)
    val deletedMsgTodo = stringResource(R.string.todo_deleted)

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.mentorHeader(com.example.mentor.ui.design.MentorArtwork.PLANNER),
                title = { Text(if (selectedTabIndex == 0) stringResource(R.string.schedule_tab_schedule) else stringResource(R.string.schedule_tab_todo)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showHistory = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = stringResource(R.string.schedule_history))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    if (selectedTabIndex == 0) {
                        entryToEdit = null
                        showScheduleForm = true
                    } else {
                        showTodoForm = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.schedule_add_button))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = androidx.compose.ui.graphics.Color.Transparent
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (viewModel.error.value) {
                Text(stringResource(R.string.schedule_connection_error), Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { viewModel.loadData() }) { Text(stringResource(R.string.schedule_retry)) }
            }
            // Tabs cho Lịch học vs Nhiệm vụ
            TabRow(selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .9f)) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text(stringResource(R.string.schedule_tab_schedule)) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text(stringResource(R.string.schedule_tab_todo)) }
                )
            }
            
            // Thanh chọn ngày dùng chung
            DaySelectorStrip(
                days = daysArray,
                selectedDay = selectedDay,
                onDaySelected = { viewModel.selectDay(it) }
            )
            
            if (selectedTabIndex == 0) {
                ScheduleTimelineList(
                    entries = viewModel.entries,
                    onItemClick = { entry ->
                        entryToEdit = entry
                        showScheduleForm = true
                    }
                )
            } else {
                DailyTodoList(
                    todos = viewModel.todos,
                    progress = viewModel.todoProgress.value,
                    onToggle = { viewModel.toggleTodo(it) },
                    onDelete = { task ->
                        viewModel.deleteTodo(task)
                        scope.launch {
                            val res = snackbarHostState.showSnackbar(
                                message = deletedMsgTodo, 
                                actionLabel = undoMsg, 
                                duration = SnackbarDuration.Short
                            )
                            if (res == SnackbarResult.ActionPerformed) viewModel.undoDeleteTodo()
                        }
                    }
                )
            }
        }
    }
    
    if (showScheduleForm) {
        ScheduleFormDialog(
            initialEntry = entryToEdit,
            accountId = accountId,
            selectedDay = selectedDay,
            viewModel = viewModel,
            onDismiss = { showScheduleForm = false },
            onSave = { entries, isEdit ->
                viewModel.saveEntries(entries, isEdit)
                showScheduleForm = false
            },
            onDelete = { entry ->
                viewModel.deleteEntry(entry)
                showScheduleForm = false
                scope.launch {
                    val result = snackbarHostState.showSnackbar(deletedMsgSchedule, undoMsg, duration = SnackbarDuration.Short)
                    if (result == SnackbarResult.ActionPerformed) viewModel.undoDeleteEntry()
                }
            }
        )
    }
    
    if (showTodoForm) {
        TodoQuickAddDialog(
            onDismiss = { showTodoForm = false },
            onSave = { title, priority ->
                viewModel.saveTodo(title, priority, null)
                showTodoForm = false
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
fun TodoQuickAddDialog(onDismiss: () -> Unit, onSave: (String, Int) -> Unit) {
    var title by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(1) } // 0=Low, 1=Normal, 2=High
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.todo_add_title)) },
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
    onDismiss: () -> Unit,
    onSave: (List<ScheduleEntry>, Boolean) -> Unit,
    onDelete: (ScheduleEntry) -> Unit
) {
    val isEdit = initialEntry != null
    var title by remember { mutableStateOf(initialEntry?.title ?: "") }
    var teacher by remember { mutableStateOf(initialEntry?.teacher ?: "") }
    var notes by remember { mutableStateOf(initialEntry?.notes ?: "") }
    
    var period by remember { mutableStateOf(initialEntry?.period ?: 1) }
    var expanded by remember { mutableStateOf(false) }
    
    val daysArray = stringResource(R.string.schedule_days).split(",")
    val selectedDays = remember { mutableStateListOf<Int>() }
    
    LaunchedEffect(Unit) {
        if (!isEdit && selectedDays.isEmpty()) selectedDays.add(selectedDay)
        else if (isEdit) selectedDays.add(initialEntry!!.dayOfWeek)
    }
    
    var titleError by remember { mutableStateOf<String?>(null) }
    var overlapWarning by remember { mutableStateOf<String?>(null) }
    
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
