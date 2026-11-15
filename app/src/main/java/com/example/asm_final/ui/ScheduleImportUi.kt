package com.example.asm_final.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.asm_final.R
import com.example.asm_final.chat.ScheduleProposal
import com.example.mentor.data.ActiveUser
import com.example.asm_final.AiMentorApplication
import com.example.mentor.schedule.domain.ScheduleEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class ReviewSlot(val day: Int, val period: Int, val subject: String, val selected: Boolean = true)

@Composable
internal fun ScheduleImportAction(text: String, owner: Int, onDone: () -> Unit) {
    val slots = remember(text) { ScheduleProposal.parse(text) }
    if (slots == null) {
        if (text.contains("```ai_schedule", ignoreCase = true)) Text(stringResource(R.string.schedule_import_invalid), color = MaterialTheme.colorScheme.error)
        return
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var preview by remember { mutableStateOf(false) }
    var edits by remember(text) { mutableStateOf(slots.map { ReviewSlot(it.day, it.period, it.subject) }) }
    var saving by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf<Int?>(null) }
    var skipped by remember { mutableStateOf(0) }
    var failed by remember { mutableStateOf(false) }
    var invalid by remember { mutableStateOf(false) }
    var selectedDay by remember(text) { mutableIntStateOf(1) }
    val dayNames = stringResource(R.string.schedule_import_day_names).split(",")
    fun update(index: Int, change: (ReviewSlot) -> ReviewSlot) {
        edits = edits.mapIndexed { position, slot -> if (position == index) change(slot) else slot }
        invalid = false
    }
    OutlinedButton(onClick = { preview = true }, enabled = !saving) { Text(stringResource(R.string.schedule_import_preview)) }
    saved?.let { Text(stringResource(R.string.schedule_import_result, it, skipped))
        TextButton(onClick = onDone) { Text(stringResource(R.string.home_schedule_title)) }
    }
    if (preview) AlertDialog(
        onDismissRequest = { if (!saving) preview = false },
        title = { Text(stringResource(R.string.schedule_import_preview)) },
        text = { Column(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.schedule_import_note), style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (1..7).forEach { day ->
                    FilterChip(selected = selectedDay == day, onClick = { selectedDay = day },
                        label = { Text(dayNames[day - 1]) }, enabled = !saving)
                }
            }
            Text(dayNames[selectedDay - 1], style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(stringResource(R.string.schedule_import_period_header), modifier = Modifier.width(64.dp))
                Text(stringResource(R.string.schedule_import_subject_label))
            }
            HorizontalDivider()
            Column(Modifier.heightIn(max = 340.dp).verticalScroll(rememberScrollState())) {
                (1..10).forEach { period ->
                    val index = edits.indexOfFirst { it.day == selectedDay && it.period == period }
                    val slot = edits.getOrNull(index)
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text("$period", modifier = Modifier.width(22.dp))
                        if (slot != null) {
                            Checkbox(slot.selected, onCheckedChange = { checked -> update(index) { it.copy(selected = checked) } },
                                enabled = !saving, modifier = Modifier.width(42.dp))
                        } else Spacer(Modifier.width(42.dp))
                        OutlinedTextField(slot?.subject.orEmpty(), onValueChange = { value ->
                            if (index >= 0) update(index) { it.copy(subject = value.take(120)) }
                            else if (value.isNotBlank()) {
                                edits = edits + ReviewSlot(selectedDay, period, value.take(120))
                                invalid = false
                            }
                        }, placeholder = { Text(stringResource(R.string.schedule_import_empty_slot)) },
                            singleLine = true, enabled = !saving && (slot?.selected != false),
                            modifier = Modifier.weight(1f))
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
            if (invalid) Text(stringResource(R.string.schedule_import_check_entries), color = MaterialTheme.colorScheme.error)
            if (failed) Text(stringResource(R.string.schedule_import_error), color = MaterialTheme.colorScheme.error)
        } },
        confirmButton = { TextButton(enabled = !saving && edits.any { it.selected }, onClick = {
            val chosen = edits.filter { it.selected }
            if (chosen.any { it.subject.isBlank() || it.subject.any(Char::isISOControl) } ||
                chosen.distinctBy { it.day to it.period }.size != chosen.size) {
                invalid = true
                return@TextButton
            }
            saving = true; failed = false
            scope.launch {
                try {
                    val count = withContext(Dispatchers.IO) {
                        check(owner > 0 && ActiveUser.get(context) == owner)
                        val repository = (context.applicationContext as AiMentorApplication).services().scheduleImport()
                        try { repository.importVacantEntries(owner.toString(), chosen.map { ScheduleEntry(accountId = owner.toString(), title = it.subject.trim(), dayOfWeek = it.day, period = it.period) }) }
                        finally { repository.close() }
                    }
                    
                    try {
                        val helperClass = Class.forName("com.example.asm_final.utils.ReminderHelper")
                        val method = helperClass.getMethod("reschedule", android.content.Context::class.java)
                        method.invoke(null, context.applicationContext)
                    } catch (e: Exception) {}
                    
                    saved = count; skipped = chosen.size - count; preview = false
                } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                catch (_: Exception) { failed = true }
                finally { saving = false }
            }
        }) { Text(stringResource(if (saving) R.string.schedule_import_saving else R.string.schedule_import_save)) } },
        dismissButton = { TextButton(onClick = { preview = false }, enabled = !saving) { Text(stringResource(android.R.string.cancel)) } }
    )
}
