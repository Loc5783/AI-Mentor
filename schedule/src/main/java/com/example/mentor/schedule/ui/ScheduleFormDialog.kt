package com.example.mentor.schedule.ui

import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.mentor.schedule.R
import com.example.mentor.schedule.domain.ScheduleEntry
import kotlinx.coroutines.launch

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
    
    var period by remember { mutableStateOf(initialEntry?.startTimeMin ?: 1) }
    
    // For repeating days when creating new
    val daysArray = stringResource(R.string.schedule_days).split(",")
    val selectedDays = remember { mutableStateListOf<Int>() }
    LaunchedEffect(Unit) {
        if (!isEdit && selectedDays.isEmpty()) {
            selectedDays.add(selectedDay)
        } else if (isEdit) {
            selectedDays.add(initialEntry!!.dayOfWeek)
        }
    }
    
    var titleError by remember { mutableStateOf<String?>(null) }
    var timeError by remember { mutableStateOf<String?>(null) }
    var overlapWarning by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    
    fun validate(): Boolean {
        var valid = true
        if (title.isBlank()) {
            titleError = "Tên môn là bắt buộc"
            valid = false
        } else { titleError = null }
        return valid
    }

    LaunchedEffect(period, selectedDays.toList()) {
        val overlaps = selectedDays.any { day ->
            viewModel.checkOverlap(day, period, period + 1, initialEntry?.id ?: 0L)
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
                    modifier = Modifier.fillMaxWidth()
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
                        modifier = Modifier.fillMaxWidth().menuAnchor()
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
                
                if (overlapWarning != null) {
                    Text(overlapWarning!!, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall)
                }
                
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
                                Text(day, modifier = Modifier.padding(8.dp), color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }

                OutlinedTextField(value = teacher, onValueChange = { teacher = it }, label = { Text(stringResource(R.string.schedule_teacher)) }, modifier = Modifier.fillMaxWidth())
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
                            startTimeMin = period,
                            endTimeMin = period + 1,
                            room = null,
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
                if (isEdit) {
                    TextButton(onClick = { onDelete(initialEntry!!) }) { Text(stringResource(R.string.schedule_delete), color = MaterialTheme.colorScheme.error) }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.schedule_cancel)) }
            }
        }
    )
}