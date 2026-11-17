package com.example.asm_final.ui

import com.example.mentor.ui.design.mentorHeader

import android.content.Context
import android.view.View
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.asm_final.utils.ThemeHelper
import com.example.asm_final.R
import com.example.asm_final.learning.LessonPathData
import com.example.asm_final.learning.LessonViewModel
import com.example.asm_final.utils.LocaleHelper
import com.example.mentor.learning.SubjectId
import com.example.mentor.data.ActiveUser
import com.example.mentor.ui.design.*
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.delay
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import org.json.JSONObject

object LearningUi {
    @JvmStatic fun screen(
        context: Context,
        model: LessonViewModel,
        subjectId: SubjectId,
        grade: Int,
        hasLessons: Boolean,
        leave: Runnable,
        openTool: Runnable?,
        ask: Consumer<String>
    ): View = composeHost(context, scenicBackground = true) {
        val revision by model.changes().observeAsState(0)
        SideEffect {
            android.util.Log.d("PlacementTrace", "REVISION_OBSERVED: revision=$revision, vm=${System.identityHashCode(model)}")
        }
        val state = remember(revision) { LessonUiState(model.busy, model.needsLogin, model.hasPending(),
            model.error, model.path, model.attempt, model.completion, model.grade(), revision) }
        LessonScreen(model, state, subjectId, grade, hasLessons, { leave.run() }, openTool, { ask.accept(it) })
    }
}

@Composable
internal fun PathContent(path: LessonPathData, open: (String) -> Unit) {
    var selected by remember(path) { mutableStateOf<LessonPathData.Unit?>(null) }
    val colors = MaterialTheme.colorScheme
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
        color = colors.primaryContainer,
        border = BorderStroke(1.dp, colors.primary.copy(alpha = .3f)),
        shadowElevation = 7.dp) {
        Column(Modifier.padding(MentorTokens.SpaceLarge),
            verticalArrangement = Arrangement.spacedBy(MentorTokens.SpaceSmall)) {
            Text(stringResource(R.string.design_path), style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold, color = colors.onPrimaryContainer)
            Text(if (path.visibleStages.isEmpty() && path.totalLessons > 0)
                stringResource(R.string.course_path_all_mastered, path.skippedLessons)
                else stringResource(R.string.course_path_progress,
                    path.completedLessons, path.totalLessons - path.skippedLessons, path.skippedLessons),
                style = MaterialTheme.typography.bodyMedium, color = colors.onPrimaryContainer)
            val lessonsToStudy = path.totalLessons - path.skippedLessons
            MentorProgress(if (lessonsToStudy == 0) {
                if (path.totalLessons > 0) 1f else 0f
            } else path.completedLessons.toFloat() / lessonsToStudy)
        }
    }
    path.visibleStages.forEach { stage ->
        val units = stage.units
        Surface(Modifier.fillMaxWidth().padding(top = 8.dp),
            shape = RoundedCornerShape(24.dp), color = colors.surface,
            border = BorderStroke(1.dp, colors.outlineVariant), shadowElevation = 5.dp) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom) {
                    Text(stringResource(R.string.course_stage_title, stage.number),
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                        color = colors.onSurface)
                    Text(stringResource(R.string.course_stage_count,
                        units.count { it.state == "COMPLETED" && !it.review }, units.count { !it.review }),
                        style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
                LessonPathStage(units, path.units.firstOrNull { it.state == "AVAILABLE" }?.id) { selected = it }
            }
        }
    }
    selected?.let { unit ->
        LessonPathSheet(unit, { selected = null }) {
            selected = null
            open(unit.id)
        }
    }
}

@Composable
private fun LessonPathStage(
    units: List<LessonPathData.Unit>, currentId: String?, select: (LessonPathData.Unit) -> Unit,
) {
    val dark = ThemeHelper.isDarkMode(androidx.compose.ui.platform.LocalContext.current)
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth()) {
        units.forEachIndexed { index, unit ->
            val locked = unit.state == "LOCKED"
            val done = unit.state == "COMPLETED"
            val current = unit.id == currentId
            val successColor = if (dark) MentorTokens.DarkSuccess else MentorTokens.Success
            val status = stringResource(when {
                done -> R.string.course_done_short
                locked -> R.string.course_locked
                else -> R.string.course_available
            })
            Surface(onClick = { select(unit) }, modifier = Modifier.fillMaxWidth()
                .testTag("path-unit:${unit.id}")
                .semantics { contentDescription = "${unit.title}, $status" },
                shape = RoundedCornerShape(18.dp),
                color = when {
                    done -> successColor.copy(alpha = if (dark) .16f else .10f)
                    else -> scheme.surfaceVariant.copy(alpha = if (current) .85f else .55f)
                },
                border = BorderStroke(if (current) 2.dp else 1.dp, when {
                    done -> successColor.copy(alpha = .55f)
                    current -> scheme.primary
                    else -> scheme.outlineVariant
                }), shadowElevation = if (current) 4.dp else 0.dp) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(shape = if (unit.review) RoundedCornerShape(13.dp) else CircleShape,
                        color = if (done) successColor else scheme.surfaceVariant,
                        contentColor = if (done) {
                            if (dark) MentorTokens.DarkBackground else Color.White
                        } else scheme.onSurfaceVariant) {
                        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                            Text(if (done) "✓" else if (unit.review) stringResource(R.string.course_review_badge)
                                else unit.ordinal.toString(), style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold)
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(if (unit.review) stringResource(R.string.course_review_range,
                            maxOf(1, unit.ordinal - 4), unit.ordinal) else unit.title,
                            style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold,
                            color = scheme.onSurface)
                        Text(status, style = MaterialTheme.typography.bodySmall,
                            color = if (done) successColor else if (current) scheme.primary else scheme.onSurfaceVariant)
                    }
                    if (!locked) Text("›", style = MaterialTheme.typography.headlineSmall,
                        color = if (done) successColor else scheme.primary)
                }
            }
            if (index < units.lastIndex) Box(Modifier.padding(start = 35.dp).width(2.dp).height(12.dp)
                .background(if (done && units[index + 1].state == "COMPLETED") successColor.copy(alpha = .6f)
                    else scheme.outlineVariant))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LessonPathSheet(unit: LessonPathData.Unit, dismiss: () -> Unit, open: () -> Unit) {
    val locked = unit.state == "LOCKED"
    val done = unit.state == "COMPLETED"
    ModalBottomSheet(onDismissRequest = dismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = MentorTokens.PagePadding)
            .padding(bottom = MentorTokens.SpaceLarge),
            verticalArrangement = Arrangement.spacedBy(MentorTokens.SpaceMedium)) {
            Text(unit.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(stringResource(if (locked) R.string.course_locked else if (done) R.string.course_done
                else R.string.course_available), style = MaterialTheme.typography.bodyMedium)
            if (locked) {
                Text(stringResource(R.string.course_unlock_note), style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(if (done) stringResource(R.string.course_path_retake)
                    else stringResource(R.string.course_stage_reward, unit.xp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                MentorButton(stringResource(if (done) R.string.course_done else R.string.course_available), open,
                    Modifier.fillMaxWidth())
            }
            TextButton(dismiss) { Text(stringResource(R.string.course_back)) }
        }
    }
}

private data class LessonUiState(val busy: Boolean, val needsLogin: Boolean, val pending: Boolean,
    val error: String?, val path: JSONObject?, val attempt: JSONObject?, val completion: JSONObject?, val grade: Int, val revision: Int = 0)

@Composable
private fun LessonScreen(
    model: LessonViewModel,
    state: LessonUiState,
    subjectId: SubjectId,
    grade: Int,
    hasLessons: Boolean,
    leave: () -> Unit,
    openTool: Runnable?,
    ask: (String) -> Unit
) {
    val configuration = LocalConfiguration.current
    val attemptId = state.attempt?.optString("id").orEmpty()
    val isAnswering = state.attempt?.let {
        it.optString("kind") != "LESSON" || it.optInt("step") >= 2
    } == true
    ActiveStudyTime(isAnswering, subjectId.wireId, grade)
    SideEffect {
        android.util.Log.d("PlacementTrace", "LESSON_SCREEN_COMPOSED: revision=${state.revision}, attemptId=$attemptId, vm=${System.identityHashCode(model)}")
    }
    val scroll = rememberScrollState()
    val stage = when {
        state.completion != null -> "complete:${state.completion.optString("id")}"
        state.attempt != null -> "attempt:${state.attempt.optString("id")}:${state.attempt.optInt("step")}"
        state.path != null -> "path:${state.path.optJSONObject("enrollment")?.optString("id")}"
        else -> "initial"
    }
    LaunchedEffect(stage) { scroll.scrollTo(0) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val subjectLabel = subjectId.label(LocaleHelper.getLanguage(context))

    Column(Modifier.fillMaxSize()
        .verticalScroll(scroll).padding(MentorTokens.PagePadding), verticalArrangement = Arrangement.spacedBy(MentorTokens.SpaceMedium)) {
        
        Row(Modifier.fillMaxWidth().mentorHeader(MentorArtwork.LEARNING)
            .padding(horizontal = 14.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(leave) { Text(stringResource(R.string.course_back)) }
            Surface(shape = RoundedCornerShape(14.dp),
                color = SubjectPalette.getAccent(subjectId.wireId),
                shadowElevation = 3.dp) {
                Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                    Text(subjectLabel.take(1).uppercase(), fontWeight = FontWeight.Bold,
                        color = MentorTokens.OnBrand)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(subjectLabel, style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(stringResource(R.string.chat_grade, grade), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Supporting Tools Section if available
        if (openTool != null) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Công cụ hỗ trợ", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            text = when (subjectId) {
                                SubjectId.MATH -> "Máy tính giải toán nhanh"
                                SubjectId.PHYSICS -> "Mô phỏng mạch điện tương tác"
                                SubjectId.CHEMISTRY -> "Bảng tuần hoàn nguyên tố"
                                else -> "Công cụ chuyên môn"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(onClick = { openTool.run() }, shape = RoundedCornerShape(12.dp)) {
                        Text("Mở công cụ")
                    }
                }
            }
        }

        if (!hasLessons) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Nội dung lớp này đang được cập nhật",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            if (state.busy && (state.path != null || state.attempt != null))
                MentorSkeleton(stringResource(R.string.course_loading))
            when {
                state.busy && state.path == null && state.attempt == null -> {
                    Text(stringResource(R.string.course_loading))
                    repeat(3) { MentorSkeleton(stringResource(R.string.course_loading)) }
                }
                state.error != null -> {
                    val code = state.error
                    val label = when {
                        code == "BACKEND_NOT_CONFIGURED" -> R.string.course_no_server
                        code == "CURRICULUM_NOT_PUBLISHED" -> R.string.course_not_published
                        state.needsLogin -> if (code == "ACCOUNT_LINK_REQUIRED") R.string.course_account_mismatch else R.string.course_connect_description
                        state.pending -> R.string.course_sync_error
                        else -> R.string.course_network_error
                    }
                    MentorCard { Text(stringResource(label)) }
                    if (state.needsLogin) LoginContent(model)
                    else if (code != "BACKEND_NOT_CONFIGURED" && code != "CURRICULUM_NOT_PUBLISHED")
                        MentorButton(stringResource(R.string.course_retry), model::retry)
                }
                state.pending -> {
                    Text(stringResource(R.string.course_sync_error))
                    MentorButton(stringResource(R.string.course_retry), model::retry)
                }
                state.completion != null -> CompletionContent(model, state.completion!!)
                state.attempt != null -> key(attemptId) { PracticeContent(state.attempt!!, model, state.busy, state.revision, ask) }
                state.path != null -> {
                    val path = state.path!!
                    var showSourceNote by remember(path) { mutableStateOf(false) }
                    var showPlacementInfo by remember(path) { mutableStateOf(false) }
                    val ownerId = ActiveUser.get(context)
                    var placementRequested by remember(ownerId, state.grade, subjectId) { mutableStateOf(false) }
                    val enrollment = path.optJSONObject("enrollment")
                    val placement = enrollment?.optJSONObject("placement")
                    val startedFromBeginning = placement?.optString("mode") == "FROM_BEGINNING"
                    val placementComplete = placement != null && !startedFromBeginning
                    val units = path.optJSONArray("units")
                    val hasPriorProgress = units != null && (0 until units.length()).any {
                        units.optJSONObject(it)?.optString("state") in listOf("COMPLETED", "SKIPPED")
                    }
                    MentorCard(Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.course_intro_title, subjectLabel), style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text(stringResource(R.string.chat_grade, state.grade),
                            color = MaterialTheme.colorScheme.primary)
                        Text(stringResource(R.string.course_intro_description), style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val sourceNote = path.optString("sourceNote")
                        if (sourceNote.isNotBlank()) {
                            TextButton(onClick = { showSourceNote = !showSourceNote }) {
                                Text(stringResource(R.string.course_source_info))
                            }
                            if (showSourceNote) Text(sourceNote, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (configuration.locales[0].language == "en") Text(stringResource(R.string.course_content_language))
                    val pending = path.optJSONArray("pendingAttempts")
                    if (pending != null && pending.length() > 0)
                        MentorButton(stringResource(R.string.course_continue), { model.resume(pending.getJSONObject(0).getString("id")) })
                    if (!placementComplete && !hasPriorProgress && !startedFromBeginning &&
                        !state.busy && !placementRequested && !showPlacementInfo && (pending == null || pending.length() == 0)) {
                        AlertDialog(
                            onDismissRequest = leave,
                            title = { Text(stringResource(R.string.course_start_choice_title, subjectLabel)) },
                            text = { Text(stringResource(R.string.course_start_choice_message)) },
                            confirmButton = {
                                Button(onClick = { showPlacementInfo = true }) {
                                    Text(stringResource(R.string.course_placement))
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { model.startFromBeginning() }) {
                                    Text(stringResource(R.string.course_start_from_beginning))
                                }
                            }
                        )
                    }
                    if (showPlacementInfo && !placementComplete) {
                        AlertDialog(
                            onDismissRequest = { showPlacementInfo = false },
                            title = { Text(stringResource(R.string.course_placement)) },
                            text = { Text(stringResource(R.string.course_placement_note)) },
                            confirmButton = {
                                Button(onClick = {
                                    showPlacementInfo = false
                                    placementRequested = true
                                    model.start("placement", true)
                                }) { Text(stringResource(R.string.course_understood)) }
                            },
                            dismissButton = {
                                TextButton(onClick = { showPlacementInfo = false }) {
                                    Text(stringResource(R.string.course_exit))
                                }
                            }
                        )
                    }
                    if (units != null) PathContent(LessonPathData(units)) { model.start(it, false) }
                }
            }
        }
    }
}

@Composable
private fun LoginContent(model: LessonViewModel) {
    var name by rememberSaveable { mutableStateOf("") }
    // Password deliberately never enters saved state.
    var password by remember { mutableStateOf("") }
    Text(stringResource(R.string.course_connect), style = MaterialTheme.typography.titleLarge)
    OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.course_identifier)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    OutlinedTextField(password, { password = it }, label = { Text(stringResource(R.string.course_password)) },
        visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true)
    MentorButton(stringResource(R.string.course_sign_in), {
        val secret = password; password = ""; model.login(name.trim(), secret)
    }, enabled = name.isNotBlank() && password.isNotEmpty())
}

@Composable
private fun PracticeContent(attempt: JSONObject, model: LessonViewModel, busy: Boolean, @Suppress("UNUSED_PARAMETER") revision: Int, ask: (String) -> Unit) {
    val step = attempt.optInt("step")
    val lesson = attempt.optString("kind") == "LESSON"
    val placement = attempt.optString("kind") == "PLACEMENT"
    if (lesson && step < 2) {
        Text(stringResource(if (step == 0) R.string.course_guidance else R.string.course_example), style = MaterialTheme.typography.headlineMedium)
        val paragraphs = attempt.optJSONArray(if (step == 0) "guidance" else "examples")
        if (paragraphs != null) repeat(paragraphs.length()) { MentorCard { Text(paragraphs.getString(it), style = MaterialTheme.typography.bodyLarge) } }
        MentorButton(stringResource(R.string.course_next), { model.save(step + 1, null, null) })
        return
    }
    val questions = attempt.getJSONArray("questions")
    val index = if (placement) model.placementIndex() else maxOf(0, step - if (lesson) 2 else 0)
    SideEffect {
        val qidText = if (index < questions.length()) questions.getJSONObject(index).optString("id") else ""
        android.util.Log.d("PlacementTrace", "PRACTICE_CONTENT_COMPOSED: revision=$revision, index=$index, qid=$qidText, selection=${model.placementAnswer(qidText)}, vm=${System.identityHashCode(model)}")
    }
    var confirmPlacementSubmit by rememberSaveable(attempt.optString("id")) { mutableStateOf(false) }
    if (placement && step >= questions.length()) {
        if (!busy && !model.hasPending()) LaunchedEffect(attempt.optString("id"), step) { model.complete() }
        Text(stringResource(R.string.course_placement_scoring), style = MaterialTheme.typography.titleMedium)
        return
    }
    if (!placement && index >= questions.length()) {
        MentorButton(stringResource(R.string.course_finish), model::complete)
        return
    }
    val context = LocalContext.current
    val question = questions.getJSONObject(index)
    val qid = question.getString("id")
    val answers = attempt.getJSONArray("answers")
    val previous = (0 until answers.length()).map { answers.getJSONObject(it) }.lastOrNull { it.optString("questionId") == qid }
    val correct = previous?.optBoolean("correct") == true
    val answered = previous != null
    var lastHandledAnswersCount by rememberSaveable(qid, attempt.optString("id")) { mutableStateOf(answers.length()) }
    LaunchedEffect(answers.length(), qid, attempt.optString("id")) {
        val currentCount = answers.length()
        if (currentCount > lastHandledAnswersCount) {
            lastHandledAnswersCount = currentCount
            com.example.asm_final.utils.HapticHelper.performHaptic(context)
        }
    }
    var lessonSelection by rememberSaveable(attempt.optString("id"), qid) {
        mutableStateOf(previous?.optString("selectedOptionId").orEmpty())
    }
    val selection = if (placement) model.placementAnswer(qid) else lessonSelection
    if (placement) {
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shadowElevation = 3.dp) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.course_placement_screen_title), style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(stringResource(R.string.course_question, index + 1, questions.length()) +
                    " · " + stringResource(R.string.course_placement_no_xp),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                MentorProgress((0 until questions.length()).count {
                    model.placementAnswer(questions.getJSONObject(it).getString("id")).isNotBlank()
                }.toFloat() / questions.length().coerceAtLeast(1))
            }
        }
    } else {
        Text(stringResource(R.string.course_question, index + 1, questions.length()),
            style = MaterialTheme.typography.titleMedium)
        MentorProgress(index.toFloat() / questions.length().coerceAtLeast(1))
        Text(stringResource(when (question.optString("difficulty")) {
            "EASY" -> R.string.course_easy; "MEDIUM" -> R.string.course_medium; else -> R.string.course_applied
        }), style = MaterialTheme.typography.labelLarge)
    }
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .3f)),
        shadowElevation = 4.dp) {
        Text(question.getString("prompt"), Modifier.padding(18.dp),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
    val submittingPlacement = placement && model.isSubmittingPlacement()
    val canInteract = if (placement) !submittingPlacement else (!busy && !correct)
    val options = question.getJSONArray("options")
    Column(Modifier.selectableGroup().testTag("quiz-options"), verticalArrangement = Arrangement.spacedBy(MentorTokens.SpaceSmall)) {
        repeat(options.length()) { i ->
            val option = options.getJSONObject(i)
            val id = option.getString("id")
            Surface(shape = RoundedCornerShape(16.dp),
                color = if (selection == id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                border = BorderStroke(if (selection == id) 2.dp else 1.dp,
                    if (selection == id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                shadowElevation = if (selection == id) 4.dp else 2.dp) {
                Row(Modifier.fillMaxWidth().selectable(selection == id, enabled = canInteract,
                    role = Role.RadioButton, onClick = {
                        if (selection != id) {
                            com.example.asm_final.utils.HapticHelper.performHaptic(context)
                            if (placement) model.setPlacementAnswer(qid, id) else lessonSelection = id
                        }
                    }).padding(MentorTokens.SpaceCompact), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selection == id, onClick = null, enabled = canInteract)
                    Text(option.getString("text"), Modifier.padding(start = MentorTokens.SpaceSmall), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
    if (answered && !placement) {
        val explanation = question.optString("explanation")
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
            color = if (correct) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
            border = BorderStroke(1.dp, if (correct) MaterialTheme.colorScheme.primary.copy(alpha = .4f)
                else MaterialTheme.colorScheme.error.copy(alpha = .4f))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(if (correct) R.string.course_correct else R.string.course_wrong),
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    color = if (correct) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onErrorContainer)
                Text(if (explanation.isNotBlank()) explanation
                    else stringResource(R.string.course_explanation_pending),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (correct) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onErrorContainer)
                if (!correct) {
                    val selectedId = previous?.optString("selectedOptionId")
                    val selectedText = (0 until options.length()).map { options.getJSONObject(it) }
                        .firstOrNull { it.optString("id") == selectedId }?.optString("text").orEmpty()
                    val aiQuestion = stringResource(R.string.course_ai_wrong_context,
                        question.optString("prompt"), selectedText)
                    TextButton(onClick = { ask(aiQuestion) },
                        enabled = !busy) { Text(stringResource(R.string.course_ask_ai_wrong)) }
                }
            }
        }
    }
    if (placement) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { model.setPlacementIndex(index - 1) },
                enabled = !submittingPlacement && index > 0, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.course_placement_previous))
            }
            if (index + 1 < questions.length()) {
                Button(onClick = { model.setPlacementIndex(index + 1) },
                    enabled = !submittingPlacement, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.course_placement_next))
                }
            } else {
                Button(onClick = { confirmPlacementSubmit = true }, enabled = !submittingPlacement,
                    modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.course_placement_submit))
                }
            }
        }
        if (confirmPlacementSubmit) {
            val missing = (0 until questions.length()).filter {
                model.placementAnswer(questions.getJSONObject(it).getString("id")).isBlank()
            }.map { it + 1 }
            val count = questions.length() - missing.size
            AlertDialog(
                onDismissRequest = { confirmPlacementSubmit = false },
                title = { Text(stringResource(R.string.course_placement_submit_title)) },
                text = { Text(if (missing.isEmpty())
                    stringResource(R.string.course_placement_submit_complete, count, questions.length())
                    else stringResource(R.string.course_placement_submit_missing,
                        count, questions.length(), missing.joinToString(", "))) },
                confirmButton = {
                    Button(onClick = {
                        confirmPlacementSubmit = false
                        model.submitPlacement()
                    }) { Text(stringResource(R.string.course_placement_submit)) }
                },
                dismissButton = {
                    TextButton(onClick = { confirmPlacementSubmit = false }) {
                        Text(stringResource(R.string.course_placement_return))
                    }
                }
            )
        }
    } else if (correct) MentorButton(stringResource(if (index + 1 == questions.length())
        R.string.course_finish else R.string.course_next), {
        if (index + 1 == questions.length()) model.complete() else model.save(step + 1, null, null)
    }) else MentorButton(stringResource(R.string.course_check), { model.save(step, qid, selection) },
        enabled = !busy && selection.isNotBlank())
    Text(stringResource(R.string.course_readonly), style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun CompletionContent(model: LessonViewModel, result: JSONObject) {
    val placement = result.optString("kind") == "PLACEMENT"
    Box(Modifier.fillMaxWidth()) {
      MentorCard(Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.course_congratulations), style = MaterialTheme.typography.headlineMedium)
        if (placement) {
            val assessment = result.getJSONObject("assessment")
            Text(stringResource(R.string.course_assessment, assessment.getInt("correct"), assessment.getInt("total")))
            if (assessment.has("totalLessonCount")) Text(stringResource(R.string.course_assessment_skipped,
                assessment.optInt("skippedLessonCount"), assessment.optInt("totalLessonCount")))
            val review = result.optJSONObject("__placementReview")
            if (review != null) {
                var showAnswers by rememberSaveable(result.optString("id")) { mutableStateOf(false) }
                TextButton(onClick = { showAnswers = !showAnswers }) {
                    Text(stringResource(R.string.course_placement_review_answers))
                }
                if (showAnswers) {
                    val questions = review.optJSONArray("questions")
                    val answers = review.optJSONArray("answers")
                    if (questions != null) repeat(questions.length()) { number ->
                        val question = questions.getJSONObject(number)
                        val qid = question.optString("id")
                        val answer = (0 until (answers?.length() ?: 0)).mapNotNull { answers?.optJSONObject(it) }
                            .firstOrNull { it.optString("questionId") == qid }
                        val options = question.optJSONArray("options")
                        fun optionText(id: String): String = (0 until (options?.length() ?: 0))
                            .mapNotNull { options?.optJSONObject(it) }
                            .firstOrNull { it.optString("id") == id }?.optString("text").orEmpty()
                        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(stringResource(R.string.course_question, number + 1, questions.length()),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary)
                                Text(question.optString("prompt"), style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold)
                                Text(stringResource(R.string.course_placement_your_answer,
                                    optionText(answer?.optString("selectedOptionId").orEmpty())
                                        .ifBlank { stringResource(R.string.course_placement_unanswered) }),
                                    color = if (answer?.optBoolean("correct") == true)
                                        MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                                Text(stringResource(R.string.course_placement_correct_answer,
                                    optionText(question.optString("correctOptionId"))))
                                if (question.optString("explanation").isNotBlank())
                                    Text(question.optString("explanation"),
                                        style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        } else {
            val xp = result.optInt("xpAwarded")
            Text(stringResource(if (xp > 0) R.string.course_completed_reward else R.string.course_completed_retake, xp), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(if (result.optBoolean("streakLit")) R.string.course_streak_lit else R.string.course_streak_already))
        }
        MentorButton(stringResource(R.string.course_path), model::backToPath)
      }
      CompletionCelebration(model, result.optString("id"), Modifier.matchParentSize())
    }
}

@Composable
private fun CompletionCelebration(model: LessonViewModel, id: String, modifier: Modifier) {
    val moving = motionEnabled()
    var active by remember(id) { mutableStateOf(false) }
    val colors = listOf(MentorTokens.Primary.toArgb(), MentorTokens.WarmAccent.toArgb(), MentorTokens.Success.toArgb())
    val parties = remember(id) { listOf(Party(speed = 0f, maxSpeed = 18f, damping = .9f,
        spread = 360, colors = colors, position = Position.Relative(.5, .1),
        emitter = Emitter(duration = 120, TimeUnit.MILLISECONDS).max(36))) }
    LaunchedEffect(id) {
        val first = model.animateCompletion()
        active = first && moving
        if (active) { delay(1400); active = false }
    }
    if (active && moving) KonfettiView(modifier, parties)
}
