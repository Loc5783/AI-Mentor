package com.example.asm_final.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.asm_final.R
import com.example.asm_final.chat.ScheduleProposal
import com.example.mentor.data.ActiveUser
import com.example.asm_final.AiMentorApplication
import com.example.asm_final.backend.BackendClient
import com.example.mentor.schedule.domain.ScheduleEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class ReviewSlot(val day: Int, val period: Int, val subject: String, val selected: Boolean = true)

@Composable
internal fun ScheduleImportAction(text: String, owner: Int, onDone: () -> Unit) {
    val slots = remember(text) { ScheduleProposal.parse(text) }
    if (slots == null) {
        if (text.contains("```ai_schedule", ignoreCase = true)) {
            MentorErrorNotice(stringResource(R.string.schedule_import_invalid))
        }
        return
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var preview by remember { mutableStateOf(false) }
    var edits by remember(text) { mutableStateOf(slots.map { ReviewSlot(it.day, it.period, it.subject) }) }
    var saving by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf<Int?>(null) }
    var skipped by remember { mutableStateOf(0) }
    var retainedBeforeImport by remember { mutableIntStateOf(0) }
    var replacedExisting by remember { mutableIntStateOf(0) }
    var replacedDays by remember { mutableIntStateOf(0) }
    var didReplace by remember { mutableStateOf(false) }
    var existingCountsLoaded by remember(owner) { mutableStateOf(false) }
    var existingCountsFailed by remember(owner) { mutableStateOf(false) }
    var saveFailure by remember { mutableStateOf<String?>(null) }
    var invalid by remember { mutableStateOf(false) }
    var selectedDay by remember(text) { mutableIntStateOf(1) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showImportOptions by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<List<ReviewSlot>>(emptyList()) }
    val existingCounts = remember(owner) { mutableStateMapOf<Int, Int>() }
    val dayNames = stringResource(R.string.schedule_import_day_names).split(",")

    val importDays = remember(edits) {
        edits.filter { it.selected && it.subject.isNotBlank() }.map { it.day }.distinct().sorted()
    }
    LaunchedEffect(preview, owner, importDays) {
        if (!preview || owner <= 0) return@LaunchedEffect
        existingCountsLoaded = false
        existingCountsFailed = false
        existingCounts.clear()
        withContext(Dispatchers.IO) {
            val repository = (context.applicationContext as AiMentorApplication).services().scheduleImport()
            try {
                importDays.forEach { day ->
                    val count = repository.getEntries(owner.toString(), day).size
                    withContext(Dispatchers.Main) { existingCounts[day] = count }
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                withContext(Dispatchers.Main) { existingCountsFailed = true }
            } finally {
                repository.close()
                withContext(Dispatchers.Main) { existingCountsLoaded = true }
            }
        }
    }

    fun update(index: Int, change: (ReviewSlot) -> ReviewSlot) {
        edits = edits.mapIndexed { position, slot -> if (position == index) change(slot) else slot }
        invalid = false
    }

    fun remove(index: Int) {
        edits = edits.filterIndexed { position, _ -> position != index }
        invalid = false
    }

    fun saveChosen(chosen: List<ReviewSlot>, replaceDays: Boolean) {
        retainedBeforeImport = if (replaceDays) 0 else importDays.sumOf { existingCounts[it] ?: 0 }
        didReplace = replaceDays
        saving = true
        saveFailure = null
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    check(owner > 0 && ActiveUser.get(context) == owner)
                    val repository = (context.applicationContext as AiMentorApplication).services().scheduleImport()
                    val entries = chosen.map {
                        ScheduleEntry(
                            accountId = owner.toString(),
                            title = it.subject.trim(),
                            dayOfWeek = it.day,
                            period = it.period
                        )
                    }
                    try {
                        if (replaceDays) repository.replaceEntriesForImportedDays(owner.toString(), entries)
                        else com.example.mentor.schedule.domain.ScheduleReplaceResult(
                            removed = 0,
                            inserted = repository.importVacantEntries(owner.toString(), entries)
                        )
                    } finally {
                        repository.close()
                    }
                }
                try {
                    val helperClass = Class.forName("com.example.asm_final.utils.ReminderHelper")
                    val method = helperClass.getMethod("reschedule", android.content.Context::class.java)
                    method.invoke(null, context.applicationContext)
                } catch (_: Exception) {}

                saved = result.inserted
                skipped = if (replaceDays) 0 else chosen.size - result.inserted
                replacedExisting = result.removed
                replacedDays = chosen.map { it.day }.distinct().size
                preview = false
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                saveFailure = when {
                    failure is BackendClient.Failure && failure.status == 401 -> "Phiên đăng nhập đã hết hạn. Hãy đăng nhập lại để lưu lịch."
                    failure is BackendClient.Failure && failure.status == 409 -> "Một số tiết đã có trong lịch. Hãy chọn thêm vào ô trống hoặc thay lịch các ngày đã chọn."
                    failure is java.io.IOException && failure !is BackendClient.Failure -> "Chưa kết nối được máy chủ. Kiểm tra mạng rồi thử lại."
                    else -> "Không thể lưu lịch học. Vui lòng thử lại."
                }
            } finally {
                saving = false
            }
        }
    }

    if (saved != null) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.schedule_import_result, saved!!, skipped),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                if (didReplace) {
                    Text(
                        "Đã thay lịch trong $replacedDays ngày: xóa $replacedExisting tiết cũ và lưu ${saved!!} tiết theo ảnh.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else if (retainedBeforeImport > 0) {
                    Text(
                        "Trong các ngày được nhập: $retainedBeforeImport tiết đã có sẵn + ${saved!!} tiết mới = ${retainedBeforeImport + saved!!} tiết. Các tiết cũ được giữ nguyên.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Button(onClick = onDone, shape = RoundedCornerShape(12.dp)) {
                    Text(stringResource(R.string.home_schedule_title))
                }
            }
        }
        return
    }

    if (!preview) {
        Button(
            onClick = { preview = true },
            enabled = !saving,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text(stringResource(R.string.schedule_import_preview), fontWeight = FontWeight.Bold)
        }
    } else {
        Surface(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = 2.dp
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Review Header
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            stringResource(R.string.schedule_import_preview),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Kiểm tra và chỉnh sửa các tiết học thực tế",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = { if (!saving) preview = false }) {
                        Text("Đóng")
                    }
                }

                Text(
                    stringResource(R.string.schedule_import_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val retainedExisting = importDays.sumOf { existingCounts[it] ?: 0 }
                if (existingCounts.isNotEmpty() && retainedExisting > 0) {
                    Text(
                        "Các tiết đã có trong lịch sẽ được giữ lại. Hiện có $retainedExisting tiết ở những ngày trong ảnh; sau khi xác nhận, tổng lịch có thể nhiều hơn số tiết mới nhập.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }

                // Day Selector Chips
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (1..7).forEach { day ->
                        val countForDay = edits.count { it.day == day && it.selected && it.subject.isNotBlank() }
                        FilterChip(
                            selected = selectedDay == day,
                            onClick = { selectedDay = day },
                            label = {
                                val alreadySaved = existingCounts[day]
                                Text(if (alreadySaved == null) "${dayNames[day - 1]} ($countForDay)"
                                    else "${dayNames[day - 1]} ($countForDay · $alreadySaved cũ)")
                            },
                            enabled = !saving
                        )
                    }
                }

                // Day Title & Add Period Button
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dayNames[selectedDay - 1],
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedButton(
                        onClick = { showAddDialog = true },
                        enabled = !saving,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("+ Thêm tiết")
                    }
                }

                // Dynamic Slots for Selected Day (sorted by period)
                val daySlots = edits.mapIndexed { index, slot -> index to slot }
                    .filter { it.second.day == selectedDay }
                    .sortedBy { it.second.period }

                Column(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (daySlots.isEmpty()) {
                        Box(
                            Modifier.fillMaxWidth().height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Chưa có tiết học nào trong ngày này. Nhấn '+ Thêm tiết' để bổ sung.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        daySlots.forEach { (index, slot) ->
                            ScheduleSlotCard(
                                index = index,
                                slot = slot,
                                saving = saving,
                                update = ::update,
                                remove = ::remove
                            )
                        }
                    }
                }

                if (invalid) {
                    MentorErrorNotice(stringResource(R.string.schedule_import_check_entries))
                }
                saveFailure?.let { message ->
                    MentorErrorNotice(message)
                }
                if (existingCountsFailed) {
                    MentorErrorNotice(
                        "Chưa tải được lịch hiện tại. Bạn vẫn có thể thêm vào các tiết trống; hãy thử lại khi muốn thay lịch cũ."
                    )
                }

                // Save Action Button
                Button(
                    onClick = {
                        val chosen = edits.filter { it.selected && it.subject.isNotBlank() }
                        if (chosen.any { it.subject.any(Char::isISOControl) } ||
                            chosen.distinctBy { it.day to it.period }.size != chosen.size) {
                            invalid = true
                            return@Button
                        }
                        val priorCount = importDays.sumOf { existingCounts[it] ?: 0 }
                        if (priorCount > 0 && !existingCountsFailed) {
                            pendingImport = chosen
                            showImportOptions = true
                        } else {
                            saveChosen(chosen, replaceDays = false)
                        }
                    },
                    enabled = !saving && existingCountsLoaded && edits.any { it.selected && it.subject.isNotBlank() },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        if (saving) stringResource(R.string.schedule_import_saving) else stringResource(R.string.schedule_import_save),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showImportOptions) {
        val priorCount = importDays.sumOf { existingCounts[it] ?: 0 }
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { if (!saving) showImportOptions = false }
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Lịch các ngày này đã có tiết", style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        "Đã có $priorCount tiết trong ${importDays.size} ngày của ảnh. Thay thế sẽ xóa các tiết cũ trong đúng những ngày này rồi lưu ${pendingImport.size} tiết đã chọn. Các ngày khác được giữ nguyên.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = {
                            showImportOptions = false
                            saveChosen(pendingImport, replaceDays = true)
                        },
                        enabled = !saving,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Thay lịch các ngày trong ảnh", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = {
                            showImportOptions = false
                            saveChosen(pendingImport, replaceDays = false)
                        },
                        enabled = !saving,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text("Giữ lịch cũ và bổ sung")
                    }
                    TextButton(
                        onClick = { showImportOptions = false },
                        enabled = !saving,
                        modifier = Modifier.align(Alignment.End).heightIn(min = 48.dp)
                    ) {
                        Text("Quay lại")
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var newPeriod by remember { mutableIntStateOf(1) }
        var newSubject by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Thêm tiết học mới") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Chọn số tiết (1-10) và tên môn học cho ${dayNames[selectedDay - 1]}:", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = newPeriod.toString(),
                        onValueChange = { newPeriod = it.toIntOrNull()?.coerceIn(1, 10) ?: 1 },
                        label = { Text("Số tiết") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newSubject,
                        onValueChange = { newSubject = it.take(120) },
                        label = { Text("Tên môn học") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newSubject.isNotBlank()) {
                            val existingIndex = edits.indexOfFirst { it.day == selectedDay && it.period == newPeriod }
                            if (existingIndex >= 0) {
                                update(existingIndex) { it.copy(subject = newSubject.trim(), selected = true) }
                            } else {
                                edits = edits + ReviewSlot(selectedDay, newPeriod, newSubject.trim(), true)
                            }
                            showAddDialog = false
                        }
                    },
                    enabled = newSubject.isNotBlank()
                ) {
                    Text("Thêm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }
}

@Composable
private fun ScheduleSlotCard(
    index: Int,
    slot: ReviewSlot,
    saving: Boolean,
    update: (Int, (ReviewSlot) -> ReviewSlot) -> Unit,
    remove: (Int) -> Unit
) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = if (slot.selected && slot.subject.isNotBlank()) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, if (slot.selected && slot.subject.isNotBlank()) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text("T${slot.period}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }

            Checkbox(
                checked = slot.selected,
                onCheckedChange = { checked -> update(index) { it.copy(selected = checked) } },
                enabled = !saving
            )

            var localSubj by remember(slot.subject) { mutableStateOf(slot.subject) }
            OutlinedTextField(
                value = localSubj,
                onValueChange = { value ->
                    localSubj = value
                    update(index) { it.copy(subject = value.take(120)) }
                },
                placeholder = { Text(stringResource(R.string.schedule_import_empty_slot)) },
                singleLine = true,
                enabled = !saving && slot.selected,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            )

            TextButton(
                onClick = { remove(index) },
                enabled = !saving,
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                Text("Xóa", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
