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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.asm_final.R
import com.example.asm_final.backend.BackendClient
import com.example.asm_final.supabase.SchedulePlannerGateway
import com.example.asm_final.ui.ScheduleImportAction
import com.example.asm_final.ui.TodoImportAction
import com.example.asm_final.ui.composeHost
import com.example.mentor.data.ActiveUser
import com.example.mentor.ui.design.MentorArtwork
import com.example.mentor.ui.design.mentorHeader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

/** Dedicated planning tools. No study-chat thread or free-text timetable prompt is created. */
class ScheduleAiActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_ACTION = "schedule_ai_action"
        const val EXTRA_DETAILS = "schedule_ai_details"
        const val PHOTO = "timetable_photo"
        const val TODO = "todo_plan"
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
        if (action != PHOTO && action != TODO) { finish(); return }
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
    var goal by remember { mutableStateOf("") }
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
    val isPhoto = action == ScheduleAiActivity.PHOTO
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TextButton(onClick = onBack) { Text(stringResource(R.string.planner_back)) }
        Surface(Modifier.fillMaxWidth().mentorHeader(MentorArtwork.PLANNER),
            shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(stringResource(if (isPhoto) R.string.planner_photo_title else R.string.planner_todo_title),
                    style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(if (isPhoto) R.string.planner_photo_hint else R.string.planner_todo_hint),
                    style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (isPhoto) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onGallery, enabled = !busy) { Text(stringResource(R.string.planner_choose_photo)) }
                OutlinedButton(onClick = onCamera, enabled = !busy) { Text(stringResource(R.string.planner_take_photo)) }
            }
            image?.let {
                Card(Modifier.fillMaxWidth()) {
                    AsyncImage(it, stringResource(R.string.planner_selected_photo),
                        Modifier.fillMaxWidth().height(220.dp).clip(MaterialTheme.shapes.medium))
                    TextButton(onClick = onClearImage, enabled = !busy) { Text(stringResource(R.string.planner_remove_photo)) }
                }
            }
        } else {
            OutlinedTextField(value = goal, onValueChange = { if (it.length <= 1000) { goal = it; result = null; error = 0 } },
                modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 6,
                label = { Text(stringResource(R.string.planner_goal_label)) },
                placeholder = { Text(stringResource(R.string.planner_goal_example)) },
                enabled = !busy)
        }
        if (busy) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            TextButton(onClick = { generation++; job?.cancel(); job = null; busy = false }) {
                Text(stringResource(R.string.planner_cancel))
            }
        } else {
            Button(onClick = {
                error = 0; result = null; busy = true
                val token = ++generation
                job = scope.launch {
                    try {
                        val proposal = if (isPhoto) gateway.scanTimetable(owner, image!!)
                            else gateway.planTodos(owner, details, goal)
                        if (token == generation) result = proposal
                    } catch (cancelled: CancellationException) { throw cancelled }
                    catch (failure: Exception) { if (token == generation) error = plannerMessage(failure, isPhoto) }
                    finally { if (token == generation) { busy = false; job = null } }
                }
            }, enabled = if (isPhoto) image != null else goal.isNotBlank(),
                modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(if (isPhoto) R.string.planner_scan else R.string.planner_create))
            }
        }
        if (error != 0) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Text(stringResource(error), Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
        result?.let { text ->
            Text(stringResource(R.string.planner_review_note), style = MaterialTheme.typography.bodyMedium)
            if (isPhoto) ScheduleImportAction(text, owner, onBack)
            else TodoImportAction(text, owner, onBack)
        }
    }
}

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
