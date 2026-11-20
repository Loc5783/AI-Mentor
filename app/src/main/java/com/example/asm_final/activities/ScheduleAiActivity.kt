package com.example.asm_final.activities

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.asm_final.R
import com.example.asm_final.backend.BackendClient
import com.example.asm_final.supabase.SchedulePlannerGateway
import com.example.asm_final.ui.ScheduleImportAction
import com.example.asm_final.ui.TodoImportAction
import com.example.asm_final.ui.ExamPlanImportAction
import com.example.asm_final.ui.composeHost
import com.example.mentor.data.ActiveUser
import com.example.mentor.ui.design.MentorArtwork
import com.example.mentor.ui.design.mentorHeader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import java.time.LocalDate
import java.time.ZoneId

/** Dedicated planning tools. No study-chat thread or free-text timetable prompt is created. */
class ScheduleAiActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_ACTION = "schedule_ai_action"
        const val EXTRA_DETAILS = "schedule_ai_details"
        const val PHOTO = "timetable_photo"
        const val TODO = "todo_plan"
        const val EXAM = "exam_plan"
    }

    private var selectedImage by mutableStateOf<Uri?>(null)
    private var cameraFile: File? = null
    private var pendingCamera: Uri? = null
    private val gallery = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) { discardCamera(); selectedImage = uri }
    }
    private val camera = registerForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        if (saved) selectedImage = pendingCamera else discardCamera()
        pendingCamera = null
    }
    private val cameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera() else Toast.makeText(this, R.string.planner_camera_permission,
            Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.getString("selected_image")?.let { selectedImage = Uri.parse(it) }
        savedInstanceState?.getString("camera_path")?.let { cameraFile = File(it) }
        savedInstanceState?.getString("pending_camera")?.let { pendingCamera = Uri.parse(it) }
        val action = intent.getStringExtra(EXTRA_ACTION)
        if (action != PHOTO && action != TODO && action != EXAM) { finish(); return }
        val owner = ActiveUser.get(this)
        if (owner <= 0) { finish(); return }
        val details = intent.getStringExtra(EXTRA_DETAILS).orEmpty()
        setContentView(composeHost(this, scenicBackground = true) {
            PlannerScreen(action, owner, details, selectedImage,
                onBack = ::finish,
                onGallery = { gallery.launch("image/*") },
                onCamera = ::takePhoto,
                onClearImage = { selectedImage = null; discardCamera() })
        })
    }

    private fun takePhoto() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            cameraPermission.launch(Manifest.permission.CAMERA)
            return
        }
        launchCamera()
    }

    private fun launchCamera() {
        try {
            discardCamera()
            val directory = File(cacheDir, "schedule_ai_camera").apply { mkdirs() }
            val file = File(directory, "${UUID.randomUUID()}.jpg")
            check(file.createNewFile())
            cameraFile = file
            pendingCamera = FileProvider.getUriForFile(this,
                "$packageName.fileprovider", file)
            camera.launch(pendingCamera!!)
        } catch (_: Exception) {
            discardCamera()
            Toast.makeText(this, R.string.planner_camera_unavailable, Toast.LENGTH_LONG).show()
        }
    }

    private fun discardCamera() {
        cameraFile?.delete()
        cameraFile = null
        pendingCamera = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("selected_image", selectedImage?.toString())
        outState.putString("camera_path", cameraFile?.absolutePath)
        outState.putString("pending_camera", pendingCamera?.toString())
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        if (!isChangingConfigurations) discardCamera()
        super.onDestroy()
    }
}

@Composable
private fun PlannerScreen(
    action: String, owner: Int, details: String, image: Uri?,
    onBack: () -> Unit, onGallery: () -> Unit, onCamera: () -> Unit,
    onClearImage: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val gateway = remember { SchedulePlannerGateway(context) }
    var examMode by remember { mutableStateOf(action == ScheduleAiActivity.EXAM) }
    val isPhoto = action == ScheduleAiActivity.PHOTO
    val isExam = examMode
    val today = remember { LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")) }
    val defaultExamDate = remember { today.plusDays(30).toString() }
    val defaultSampleGoal = remember {
        "Toán: Hàm số lượng giác, Cấp số cộng, Giới hạn\nVật lý: Dao động điều hòa, Sóng cơ, Điện trường\nHóa học: Cân bằng hóa học, Hydrocarbon"
    }

    var goal by remember { mutableStateOf(if (action == ScheduleAiActivity.EXAM) defaultSampleGoal else "") }
    var examDate by remember { mutableStateOf(if (action == ScheduleAiActivity.EXAM) defaultExamDate else "") }
    var dailyMinutes by remember { mutableStateOf("60") }
    var reminderHour by remember { mutableStateOf("19") }
    var availability by remember { mutableStateOf(if (action == ScheduleAiActivity.EXAM) "Tối chủ nhật rảnh 5 tiếng, các tối khác từ 19h-21h" else "") }
    var useOldTimetable by remember { mutableStateOf(true) }
    var result by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var job by remember { mutableStateOf<Job?>(null) }
    var generation by remember { mutableIntStateOf(0) }
    LaunchedEffect(image) {
        generation++
        job?.cancel()
        job = null
        busy = false
        result = null
        error = 0
    }
    val parsedExamDate = remember(examDate) { runCatching { LocalDate.parse(examDate) }.getOrNull() ?: runCatching { LocalDate.parse(defaultExamDate) }.getOrNull() }
    val parsedSubjects = remember(goal) { parseExamSubjects(goal) ?: if (isExam) parseExamSubjects(defaultSampleGoal) else null }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TextButton(onClick = onBack) { Text(stringResource(R.string.planner_back)) }

        Surface(
            Modifier.fillMaxWidth().mentorHeader(MentorArtwork.PLANNER),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 2.dp
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    stringResource(if (isPhoto) R.string.planner_photo_title else if (isExam) R.string.exam_plan_title else R.string.planner_todo_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    stringResource(if (isPhoto) R.string.planner_photo_hint else if (isExam) R.string.exam_plan_hint else R.string.planner_todo_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (isPhoto) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCamera,
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(painterResource(R.drawable.ic_home), null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.planner_take_photo))
                }
                OutlinedButton(
                    onClick = onGallery,
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(painterResource(R.drawable.ic_category), null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.planner_choose_photo))
                }
            }

            Surface(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shadowElevation = 1.dp
            ) {
                Column(
                    Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (image != null) {
                        AsyncImage(
                            image,
                            stringResource(R.string.planner_selected_photo),
                            Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                                .clip(RoundedCornerShape(14.dp)),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                        OutlinedButton(
                            onClick = onClearImage,
                            enabled = !busy,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text(stringResource(R.string.planner_remove_photo))
                        }
                    } else {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_book_course),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    "Chưa chọn ảnh thời khóa biểu",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "Chụp ảnh hoặc chọn từ thư viện để AI tự động nhận diện thời khóa biểu của bạn.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
            ) {
                Row(
                    Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_lightning),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "💡 Mẹo chụp hiệu quả: Chụp đủ sáng, giữ trọn vẹn bảng và đảm bảo chữ viết các tiết rõ nét.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !isExam, enabled = !busy, onClick = { examMode = false; result = null },
                    label = { Text(stringResource(R.string.planner_todo_title)) })
                FilterChip(selected = isExam, enabled = !busy, onClick = {
                    examMode = true; result = null
                    if (goal.isBlank()) goal = defaultSampleGoal
                    if (examDate.isBlank()) examDate = defaultExamDate
                    if (availability.isBlank()) availability = "Tối chủ nhật rảnh 5 tiếng, các tối khác từ 19h-21h"
                }, label = { Text(stringResource(R.string.exam_plan_title)) })
            }
            if (isExam) {
                OutlinedButton(onClick = {
                    val initial = parsedExamDate ?: today.plusDays(7)
                    android.app.DatePickerDialog(context, { _, year, month, day ->
                        examDate = LocalDate.of(year, month + 1, day).toString(); result = null
                    }, initial.year, initial.monthValue - 1, initial.dayOfMonth).apply {
                        datePicker.minDate = today.plusDays(2).atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh"))
                            .toInstant().toEpochMilli()
                    }.show()
                }) { Text(stringResource(R.string.exam_plan_pick_date)) }
                OutlinedTextField(examDate, onValueChange = { if (it.length <= 10) { examDate = it; result = null } },
                    label = { Text(stringResource(R.string.exam_plan_date)) },
                    placeholder = { Text("2026-12-15") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Text(stringResource(R.string.exam_plan_days,
                    parsedExamDate?.let { java.time.temporal.ChronoUnit.DAYS.between(today, it).toInt().coerceAtLeast(0) } ?: 0))
                OutlinedTextField(dailyMinutes, onValueChange = { if (it.length <= 3 && it.all(Char::isDigit)) { dailyMinutes = it; result = null } },
                    label = { Text(stringResource(R.string.exam_plan_minutes)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(reminderHour, onValueChange = { if (it.length <= 2 && it.all(Char::isDigit)) { reminderHour = it; result = null } },
                    label = { Text(stringResource(R.string.exam_plan_reminder)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(availability, onValueChange = { if (it.length <= 300) { availability = it; result = null } },
                    label = { Text(stringResource(R.string.exam_plan_availability)) },
                    placeholder = { Text(stringResource(R.string.exam_plan_availability_example)) },
                    modifier = Modifier.fillMaxWidth(), minLines = 2)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(useOldTimetable, onCheckedChange = { useOldTimetable = it; result = null })
                    Text(stringResource(R.string.exam_plan_use_old_timetable))
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.exam_plan_subjects),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = {
                        goal = defaultSampleGoal
                        if (examDate.isBlank()) examDate = defaultExamDate
                        if (availability.isBlank()) availability = "Tối chủ nhật rảnh 5 tiếng, các tối khác từ 19h-21h"
                        result = null
                        error = 0
                    }) {
                        Text("✨ Nạp môn học mẫu", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            OutlinedTextField(
                value = goal,
                onValueChange = { if (it.length <= 1000) { goal = it; result = null; error = 0 } },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6,
                label = { Text(stringResource(if (isExam) R.string.exam_plan_subjects else R.string.planner_goal_label)) },
                placeholder = { Text(stringResource(if (isExam) R.string.exam_plan_subjects_example else R.string.planner_goal_example)) },
                enabled = !busy,
                shape = RoundedCornerShape(14.dp)
            )
            if (isExam && goal.isNotBlank()) {
                if (parsedSubjects != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_lightning),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                "✓ Đã nhận diện ${parsedSubjects.size} môn học. Sẵn sàng để AI lập lộ trình!",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            "⚠️ Cần nhập đúng định dạng mỗi dòng: Môn: Bài 1, Bài 2... (Hoặc bấm 'Nạp môn học mẫu' ở trên)",
                            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        if (busy) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(
                    Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(
                        stringResource(if (isPhoto) R.string.planner_photo_busy else if (isExam) R.string.exam_plan_busy else R.string.planner_todo_busy),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = { generation++; job?.cancel(); job = null; busy = false }) {
                        Text(stringResource(R.string.planner_cancel))
                    }
                }
            }
        } else {
            Button(
                onClick = {
                    error = 0; result = null; busy = true
                    val effectiveAvail = availability.ifBlank { "Tối chủ nhật rảnh 5 tiếng, các tối khác từ 19h-21h" }
                    val effectiveExamDate = parsedExamDate ?: today.plusDays(30)
                    val effectiveSubjects = parsedSubjects ?: parseExamSubjects(defaultSampleGoal)!!
                    val token = ++generation
                    job = scope.launch {
                        try {
                            val proposal = if (isPhoto) gateway.scanTimetable(owner, image!!)
                                else if (isExam) gateway.planExam(owner, today, effectiveExamDate,
                                    effectiveSubjects, dailyMinutes.toIntOrNull() ?: 60, effectiveAvail, useOldTimetable)
                                else gateway.planTodos(owner, details, goal)
                            if (token == generation) result = proposal
                        } catch (cancelled: CancellationException) { throw cancelled }
                        catch (failure: Exception) { if (token == generation) error = plannerMessage(failure, isPhoto) }
                        finally { if (token == generation) { busy = false; job = null } }
                    }
                },
                enabled = if (isPhoto) image != null else if (isExam)
                    (parsedExamDate != null || examDate.isBlank()) && (parsedSubjects != null || goal.isBlank())
                    else goal.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(27.dp)
            ) {
                Text(
                    if (isPhoto) stringResource(R.string.planner_scan)
                    else if (isExam) "🤖 AI Lập Lộ Trình Ôn Thi"
                    else stringResource(R.string.planner_create),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (error != 0) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Text(
                    stringResource(error),
                    Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        result?.let { text ->
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        stringResource(R.string.planner_review_note),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (isPhoto) ScheduleImportAction(text, owner, onBack)
                    else if (isExam) ExamPlanImportAction(text, owner, today, parsedExamDate!!,
                        parsedSubjects!!.keys, dailyMinutes.toInt(), reminderHour.toInt(), onBack)
                    else TodoImportAction(text, owner, onBack)
                }
            }
        }
    }
}

private fun parseExamSubjects(input: String): Map<String, List<String>>? = runCatching {
    val lines = input.lines().map(String::trim).filter(String::isNotBlank)
    require(lines.size in 1..9)
    val parsed = lines.associate { line ->
        val parts = line.split(':', limit = 2)
        require(parts.size == 2 && parts[0].trim().length in 1..80)
        val lessons = parts[1].split(',').map(String::trim).filter(String::isNotBlank)
        require(lessons.isNotEmpty() && lessons.size <= 30)
        parts[0].trim() to lessons
    }
    require(parsed.size == lines.size)
    parsed
}.getOrNull()

private fun plannerMessage(error: Exception, photo: Boolean): Int {
    val code = (error as? BackendClient.Failure)?.code
    return when (code) {
        "IMAGE_BLURRY", "TIMETABLE_UNCLEAR" -> R.string.planner_blurry
        "NOT_TIMETABLE" -> R.string.planner_not_timetable
        "TODO_DETAILS_REQUIRED" -> R.string.planner_need_details
        "INVALID_IMAGE", "IMAGE_TOO_LARGE" -> R.string.planner_invalid_image
        "INVALID_AI_OUTPUT" -> R.string.planner_invalid_result
        else -> if (photo) R.string.planner_photo_error else R.string.planner_todo_error
    }
}
