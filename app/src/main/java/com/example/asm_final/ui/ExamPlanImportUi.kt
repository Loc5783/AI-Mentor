package com.example.asm_final.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.asm_final.AiMentorApplication
import com.example.asm_final.R
import com.example.asm_final.chat.ExamPlanProposal
import com.example.asm_final.utils.ReminderHelper
import com.example.mentor.data.ActiveUser
import com.example.mentor.schedule.domain.TodoTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@Composable
internal fun ExamPlanImportAction(text: String, owner: Int, today: LocalDate,
    examDate: LocalDate, subjects: Set<String>, dailyMinutes: Int, reminderHour: Int,
    onDone: () -> Unit) {
    val tasks = remember(text) { ExamPlanProposal.parse(text, today, examDate, subjects, dailyMinutes) }
    if (tasks == null) { MentorErrorNotice(stringResource(R.string.planner_invalid_result)); return }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val selected = remember(text) { mutableStateListOf<Boolean>().apply { repeat(tasks.size) { add(true) } } }
    var preview by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }

    Button(onClick = { preview = true }, enabled = !saving && !saved) {
        Text(stringResource(R.string.exam_plan_review))
    }
    if (saved) {
        Text(stringResource(R.string.exam_plan_saved))
        TextButton(onClick = onDone) { Text(stringResource(R.string.home_schedule_title)) }
    }
    if (preview) AlertDialog(onDismissRequest = { if (!saving) preview = false },
        title = { Text(stringResource(R.string.exam_plan_review)) },
        text = { Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.exam_plan_review_hint), style = MaterialTheme.typography.bodySmall)
            tasks.forEachIndexed { index, task ->
                Row(Modifier.fillMaxWidth()) {
                    Checkbox(selected[index], onCheckedChange = { selected[index] = it }, enabled = !saving)
                    Column {
                        Text("${task.dueDate} · ${task.startTime} · ${task.subject}", style = MaterialTheme.typography.labelLarge)
                        Text(task.title)
                        Text(stringResource(R.string.exam_plan_minutes_value, task.minutes))
                    }
                }
            }
            if (failed) MentorErrorNotice(stringResource(R.string.todo_import_error))
        } },
        confirmButton = { TextButton(enabled = !saving && selected.any { it }, onClick = {
            saving = true; failed = false
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        check(owner > 0 && ActiveUser.get(context) == owner)
                        val repo = (context.applicationContext as AiMentorApplication).services().scheduleImport()
                        try {
                            val rows = tasks.filterIndexed { index, _ -> selected[index] }.map { task ->
                                TodoTask(accountId = owner.toString(), title = "${task.startTime} · ${context.getString(R.string.exam_plan_task_prefix)} · ${task.title}",
                                    dayOfWeek = LocalDate.parse(task.dueDate).dayOfWeek.value,
                                    priority = task.priority, linkedSubject = task.subject,
                                    dueDate = task.dueDate, planStartDate = today.toString())
                            }
                            repo.importTodos(owner.toString(), rows)
                        } finally { repo.close() }
                    }
                    ReminderHelper.setExamPlanHour(context.applicationContext, reminderHour)
                    saved = true; preview = false
                } catch (_: Exception) { failed = true }
                finally { saving = false }
            }
        }) { Text(stringResource(R.string.todo_import_save)) } },
        dismissButton = { TextButton(onClick = { preview = false }, enabled = !saving) {
            Text(stringResource(android.R.string.cancel))
        } })
}
