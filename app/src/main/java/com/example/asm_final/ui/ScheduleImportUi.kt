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
import com.example.asm_final.R
import com.example.asm_final.chat.ScheduleProposal
import com.example.asm_final.activities.HomeActivity
import com.example.mentor.data.ActiveUser
import com.example.asm_final.AiMentorApplication
import com.example.mentor.schedule.domain.ScheduleEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun ScheduleImportAction(text: String, owner: Int) {
    val slots = remember(text) { ScheduleProposal.parse(text) }
    if (slots == null) {
        if (text.contains("```ai_schedule", ignoreCase = true)) Text(stringResource(R.string.schedule_import_invalid), color = MaterialTheme.colorScheme.error)
        return
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var preview by remember { mutableStateOf(false) }
    var selected by remember(text) { mutableStateOf(slots.toSet()) }
    var saving by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf<Int?>(null) }
    var skipped by remember { mutableStateOf(0) }
    var failed by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { preview = true }, enabled = !saving) { Text(stringResource(R.string.schedule_import_preview)) }
    saved?.let { Text(stringResource(R.string.schedule_import_result, it, skipped))
        TextButton(onClick = { (context as? HomeActivity)?.navigateTo(0, true) }) { Text(stringResource(R.string.home_schedule_title)) }
    }
    if (preview) AlertDialog(
        onDismissRequest = { if (!saving) preview = false },
        title = { Text(stringResource(R.string.schedule_import_preview)) },
        text = { Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
            Text(stringResource(R.string.schedule_import_note))
            slots.forEach { slot ->
                Row(Modifier.fillMaxWidth()) {
                    Checkbox(slot in selected, onCheckedChange = { checked -> selected = if (checked) selected + slot else selected - slot }, enabled = !saving)
                    Text(stringResource(R.string.schedule_import_slot, if (slot.day == 7) stringResource(R.string.schedule_import_sunday) else stringResource(R.string.schedule_import_weekday, slot.day + 1), slot.period, slot.subject), Modifier.padding(top = 12.dp))
                }
            }
            if (failed) Text(stringResource(R.string.schedule_import_error), color = MaterialTheme.colorScheme.error)
        } },
        confirmButton = { TextButton(enabled = !saving && selected.isNotEmpty(), onClick = {
            saving = true; failed = false
            val chosen = selected.toList()
            scope.launch {
                try {
                    val count = withContext(Dispatchers.IO) {
                        check(owner > 0 && ActiveUser.get(context) == owner)
                        val repository = (context.applicationContext as AiMentorApplication).services().scheduleImport()
                        try { repository.importVacantEntries(owner.toString(), chosen.map { ScheduleEntry(accountId = owner.toString(), title = it.subject, dayOfWeek = it.day, period = it.period) }) }
                        finally { repository.close() }
                    }
                    saved = count; skipped = chosen.size - count; preview = false
                } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                catch (_: Exception) { failed = true }
                finally { saving = false }
            }
        }) { Text(stringResource(if (saving) R.string.schedule_import_saving else R.string.schedule_import_save)) } },
        dismissButton = { TextButton(onClick = { preview = false }, enabled = !saving) { Text(stringResource(android.R.string.cancel)) } }
    )
}
