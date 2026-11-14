package com.example.asm_final.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.asm_final.AiMentorApplication
import com.example.asm_final.R
import com.example.asm_final.activities.HomeActivity
import com.example.asm_final.chat.TodoProposal
import com.example.mentor.data.ActiveUser
import com.example.mentor.schedule.domain.TodoTask
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class DraftTodo(
    val day: Int,
    val title: String,
    val subject: String,
    val priority: Int,
    val selected: Boolean = true,
) {
    fun valid() = day in 1..7 && title.isNotBlank() && title.length <= 160 &&
        title.none { it.isISOControl() } && subject.length <= 80 &&
        subject.none { it.isISOControl() } && priority in 0..2
}

@Composable
internal fun TodoImportAction(text: String, owner: Int) {
    val suggested = remember(text) { TodoProposal.parse(text) }
    if (suggested == null) {
        if (text.contains("```ai_todo", ignoreCase = true))
            Text(stringResource(R.string.todo_import_invalid), color = MaterialTheme.colorScheme.error)
        return
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val rows = remember(text) { suggested.map {
        DraftTodo(it.day, it.title, it.subject.orEmpty(), it.priority)
    }.toMutableStateList() }
    var preview by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf<Int?>(null) }
    var skipped by remember { mutableIntStateOf(0) }
    var failed by remember { mutableStateOf(false) }
    val dayLabels = stringResource(R.string.todo_import_days).split(",")
    val priorities = listOf(
        stringResource(R.string.todo_import_low),
        stringResource(R.string.todo_import_normal),
        stringResource(R.string.todo_import_high),
    )

    OutlinedButton(onClick = { preview = true }, enabled = !saving) {
        Text(stringResource(R.string.todo_import_preview))
    }
    saved?.let { count ->
        Text(stringResource(R.string.todo_import_result, count, skipped))
        TextButton(onClick = { (context as? HomeActivity)?.navigateTo(0, true) }) {
            Text(stringResource(R.string.home_schedule_title))
        }
    }

    if (preview) AlertDialog(
        onDismissRequest = { if (!saving) preview = false },
        title = { Text(stringResource(R.string.todo_import_preview)) },
        text = {
            Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.todo_import_note), style = MaterialTheme.typography.bodySmall)
                rows.forEachIndexed { index, row ->
                    Surface(shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                Checkbox(row.selected,
                                    onCheckedChange = { rows[index] = row.copy(selected = it) },
                                    enabled = !saving)
                                Text(stringResource(R.string.todo_import_task_index, index + 1),
                                    fontWeight = FontWeight.SemiBold)
                            }
                            OutlinedTextField(row.title,
                                onValueChange = { if (it.length <= 160) rows[index] = row.copy(title = it) },
                                label = { Text(stringResource(R.string.todo_import_task)) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !saving && row.selected,
                                singleLine = true)
                            OutlinedTextField(row.subject,
                                onValueChange = { if (it.length <= 80) rows[index] = row.copy(subject = it) },
                                label = { Text(stringResource(R.string.todo_import_subject)) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !saving && row.selected,
                                singleLine = true)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                var dayMenu by remember { mutableStateOf(false) }
                                Box {
                                    TextButton({ dayMenu = true }, enabled = !saving && row.selected) {
                                        Text(dayLabels[row.day - 1])
                                    }
                                    DropdownMenu(dayMenu, onDismissRequest = { dayMenu = false }) {
                                        dayLabels.forEachIndexed { day, label ->
                                            DropdownMenuItem(text = { Text(label) }, onClick = {
                                                rows[index] = row.copy(day = day + 1)
                                                dayMenu = false
                                            })
                                        }
                                    }
                                }
                                var priorityMenu by remember { mutableStateOf(false) }
                                Box {
                                    TextButton({ priorityMenu = true }, enabled = !saving && row.selected) {
                                        Text(priorities[row.priority])
                                    }
                                    DropdownMenu(priorityMenu, onDismissRequest = { priorityMenu = false }) {
                                        priorities.forEachIndexed { priority, label ->
                                            DropdownMenuItem(text = { Text(label) }, onClick = {
                                                rows[index] = row.copy(priority = priority)
                                                priorityMenu = false
                                            })
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (failed) Text(stringResource(R.string.todo_import_error), color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(
                enabled = !saving && rows.any { it.selected } && rows.filter { it.selected }.all { it.valid() },
                onClick = {
                    val selected = rows.filter { it.selected }.toList()
                    saving = true
                    failed = false
                    scope.launch {
                        try {
                            val count = withContext(Dispatchers.IO) {
                                check(owner > 0 && ActiveUser.get(context) == owner)
                                val repository = (context.applicationContext as AiMentorApplication).services().scheduleImport()
                                try {
                                    repository.importTodos(owner.toString(), selected.map { row ->
                                        TodoTask(accountId = owner.toString(), title = row.title.trim(),
                                            dayOfWeek = row.day, priority = row.priority,
                                            linkedSubject = row.subject.trim().ifEmpty { null })
                                    })
                                } finally { repository.close() }
                            }
                            runCatching {
                                val helper = Class.forName("com.example.asm_final.utils.ReminderHelper")
                                helper.getMethod("reschedule", android.content.Context::class.java)
                                    .invoke(null, context.applicationContext)
                            }
                            saved = count
                            skipped = selected.size - count
                            preview = false
                        } catch (cancelled: CancellationException) { throw cancelled }
                        catch (_: Exception) { failed = true }
                        finally { saving = false }
                    }
                }
            ) { Text(stringResource(if (saving) R.string.todo_import_saving else R.string.todo_import_save)) }
        },
        dismissButton = {
            TextButton(onClick = { preview = false }, enabled = !saving) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}
