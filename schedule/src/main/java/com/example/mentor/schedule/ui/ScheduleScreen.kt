package com.example.mentor.schedule.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mentor.schedule.R
import com.example.mentor.schedule.domain.ScheduleEntry
import com.example.mentor.schedule.domain.ScheduleRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    accountId: String,
    repository: ScheduleRepository,
    onBack: () -> Unit
) {
    val factory = remember { ScheduleViewModelFactory(accountId, repository) }
    val viewModel: ScheduleViewModel = viewModel(factory = factory)
    val daysArray = stringResource(R.string.schedule_days).split(",")
    val selectedDay by viewModel.selectedDay
    
    var showForm by remember { mutableStateOf(false) }
    var entryToEdit by remember { mutableStateOf<ScheduleEntry?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    val undoMsg = stringResource(R.string.schedule_undo)
    val deletedMsg = stringResource(R.string.schedule_deleted)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.schedule_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { entryToEdit = null; showForm = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.schedule_add))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            ScrollableTabRow(
                selectedTabIndex = selectedDay - 1,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                daysArray.forEachIndexed { index, dayName ->
                    val isSelected = (index + 1) == selectedDay
                    Tab(
                        selected = isSelected,
                        onClick = { viewModel.selectDay(index + 1) },
                        text = { 
                            Text(
                                dayName, 
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            ) 
                        }
                    )
                }
            }
            
            if (viewModel.entries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.schedule_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(viewModel.entries.sortedBy { it.period }, key = { it.id }) { entry ->
                        ScheduleTimelineItem(entry) {
                            entryToEdit = entry
                            showForm = true
                        }
                    }
                }
            }
        }
    }
    
    if (showForm) {
        ScheduleFormDialog(
            initialEntry = entryToEdit,
            accountId = accountId,
            selectedDay = selectedDay,
            viewModel = viewModel,
            onDismiss = { showForm = false },
            onSave = { entries, isEdit ->
                viewModel.saveEntries(entries, isEdit)
                showForm = false
            },
            onDelete = { entry ->
                viewModel.deleteEntry(entry)
                showForm = false
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = deletedMsg,
                        actionLabel = undoMsg,
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.undoDelete()
                    }
                }
            }
        )
    }
}

@Composable
fun ScheduleTimelineItem(entry: ScheduleEntry, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.width(60.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                entry.getFormattedStartTime(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(2.dp))
            Text(
                entry.getFormattedEndTime(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(Modifier.width(16.dp))
        
        Box(
            Modifier
                .width(4.dp)
                .height(48.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
        )
        
        Spacer(Modifier.width(16.dp))
        
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            shadowElevation = 0.dp
        ) {
            Column(Modifier.padding(12.dp)) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!entry.teacher.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "GV: ${entry.teacher}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
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
                        value = "Tiết $period",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Chọn tiết") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        for (i in 1..10) {
                            val timeHint = if (i <= 5) "(Sáng)" else "(Chiều)"
                            DropdownMenuItem(
                                text = { Text("Tiết $i $timeHint") },
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
                    Text("Lặp lại các ngày:", style = MaterialTheme.typography.bodySmall)
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