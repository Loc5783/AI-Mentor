package com.example.asm_final.ui

import com.example.mentor.ui.design.mentorHeader

import android.content.Context
import android.view.View
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.asm_final.R
import com.example.asm_final.learning.LessonPathData
import com.example.asm_final.learning.LessonViewModel
import com.example.asm_final.utils.LocaleHelper
import com.example.mentor.learning.SubjectId
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
    ): View = composeHost(context) {
        val revision by model.changes().observeAsState(0)
        val state = remember(revision) { LessonUiState(model.busy, model.needsLogin, model.hasPending(),
            model.error, model.path, model.attempt, model.completion, model.grade()) }
        LessonScreen(model, state, subjectId, grade, hasLessons, { leave.run() }, openTool, { ask.accept(it) })
    }
}

@Composable
internal fun PathContent(path: LessonPathData, open: (String) -> Unit) {
    val stages = remember(path) {
        buildList {
            var stage = mutableListOf<LessonPathData.Unit>()
            path.units.forEach { unit ->
                stage.add(unit)
                if (unit.review) {
                    add(stage.toList())
                    stage = mutableListOf()
                }
            }
            if (stage.isNotEmpty()) add(stage)
        }
    }
    var selected by remember(path) { mutableStateOf<LessonPathData.Unit?>(null) }
    Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer) {
        Column(Modifier.padding(MentorTokens.SpaceLarge),
            verticalArrangement = Arrangement.spacedBy(MentorTokens.SpaceSmall)) {
            Text(stringResource(R.string.design_path), style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.course_path_progress, path.completedLessons, path.totalLessons, path.skippedLessons),
                style = MaterialTheme.typography.bodyMedium)
            MentorProgress(if (path.totalLessons == 0) 0f else path.completedLessons.toFloat() / path.totalLessons)
            Text(stringResource(R.string.course_path_note), style = MaterialTheme.typography.bodySmall)
        }
    }
    stages.forEachIndexed { index, units ->
        Row(Modifier.fillMaxWidth().padding(top = MentorTokens.SpaceMedium),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text(stringResource(R.string.course_stage_title, index + 1),
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.course_stage_count,
                units.count { it.state == "COMPLETED" && !it.review }, units.count { !it.review }),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LessonPathStage(units, path.units.firstOrNull { it.state == "AVAILABLE" }?.id) { selected = it }
    }
    selected?.let { unit ->
        LessonPathSheet(unit, { selected = null }) {
            selected = null
            open(unit.id)
        }
    }
}

private fun nodeOffset(index: Int): Float = when (index % 5) {
    1 -> .27f
    3 -> .73f
    else -> .5f
}

@Composable
private fun LessonPathStage(
    units: List<LessonPathData.Unit>, currentId: String?, select: (LessonPathData.Unit) -> Unit,
) {
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    val fontScale = LocalDensity.current.fontScale
    val step = if (fontScale > 1.3f) 180.dp else 148.dp
    val nodeSize = 72.dp
    val scheme = MaterialTheme.colorScheme
    BoxWithConstraints(Modifier.fillMaxWidth().height(step * units.size)) {
        val labelWidth = minOf(maxWidth * .54f, 180.dp)
        Canvas(Modifier.matchParentSize()) {
            val stepPx = step.toPx()
            for (index in 0 until units.lastIndex) {
                val start = Offset(size.width * nodeOffset(index), index * stepPx + nodeSize.toPx() / 2)
                val end = Offset(size.width * nodeOffset(index + 1), (index + 1) * stepPx + nodeSize.toPx() / 2)
                val middle = (start.y + end.y) / 2
                val connector = Path().apply {
                    moveTo(start.x, start.y)
                    cubicTo(start.x, middle, end.x, middle, end.x, end.y)
                }
                drawPath(connector,
                    color = if (units[index + 1].state == "LOCKED") scheme.outlineVariant else scheme.primary.copy(alpha = .48f),
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))
            }
        }
        units.forEachIndexed { index, unit ->
            val locked = unit.state == "LOCKED"
            val done = unit.state == "COMPLETED"
            val current = unit.id == currentId
            val center = maxWidth * nodeOffset(index)
            val color = when {
                done -> if (dark) MentorTokens.DarkSuccess else MentorTokens.Success
                locked -> scheme.surfaceVariant
                unit.review -> if (dark) MentorTokens.WarmAccent else MentorTokens.Warning
                else -> scheme.primary
            }
            val contentColor = when {
                done -> if (dark) MentorTokens.DarkBackground else Color.White
                locked -> scheme.onSurfaceVariant
                unit.review && dark -> MentorTokens.DarkBackground
                unit.review -> Color.White
                else -> scheme.onPrimary
            }
            val status = stringResource(when {
                done -> R.string.course_done
                locked -> R.string.course_locked
                else -> R.string.course_available
            })
            Surface(onClick = { select(unit) },
                modifier = Modifier.offset(x = center - nodeSize / 2, y = step * index)
                    .size(nodeSize).testTag("path-unit:${unit.id}")
                    .semantics { contentDescription = "${unit.title}, $status" },
                shape = if (unit.review) RoundedCornerShape(24.dp) else CircleShape,
                color = color, contentColor = contentColor,
                border = if (current) BorderStroke(3.dp, scheme.primaryContainer) else null,
                shadowElevation = if (current) 8.dp else 2.dp) {
                Box(contentAlignment = Alignment.Center) {
                    Text(if (unit.review) stringResource(R.string.course_review_badge) else unit.ordinal.toString(),
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
            Text(if (unit.review) stringResource(R.string.course_review_range,
                maxOf(1, unit.ordinal - 4), unit.ordinal) else unit.title,
                modifier = Modifier.offset(x = center - labelWidth / 2, y = step * index + nodeSize + 6.dp)
                    .width(labelWidth), textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (current) FontWeight.Bold else FontWeight.Medium,
                color = if (locked) scheme.onSurfaceVariant else scheme.onBackground)
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
    val error: String?, val path: JSONObject?, val attempt: JSONObject?, val completion: JSONObject?, val grade: Int)

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
        
        Row(Modifier.fillMaxWidth().mentorHeader().padding(vertical = 8.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(leave) { Text(stringResource(R.string.course_back)) }
            Text("$subjectLabel • Lớp $grade", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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
                state.attempt != null -> key(attemptId) { PracticeContent(state.attempt!!, model, state.busy, ask) }
                state.path != null -> {
                    val path = state.path!!
                    Text(path.optString("title"), style = MaterialTheme.typography.headlineMedium)
                    Text(stringResource(R.string.chat_grade, state.grade))
                    Text(path.optString("sourceNote"), style = MaterialTheme.typography.bodySmall)
                    if (configuration.locales[0].language == "en") Text(stringResource(R.string.course_content_language))
                    val pending = path.optJSONArray("pendingAttempts")
                    if (pending != null && pending.length() > 0)
                        MentorButton(stringResource(R.string.course_continue), { model.resume(pending.getJSONObject(0).getString("id")) })
                    if (path.optJSONObject("enrollment")?.optString("placementStatus") != "COMPLETED") {
                        Text(stringResource(R.string.course_placement_note))
                        MentorButton(stringResource(R.string.course_placement), { model.start("placement", true) })
                    }
                    val units = path.optJSONArray("units")
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
private fun PracticeContent(attempt: JSONObject, model: LessonViewModel, busy: Boolean, ask: (String) -> Unit) {
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
    val index = maxOf(0, step - if (lesson) 2 else 0)
    if (index >= questions.length()) {
        MentorButton(stringResource(R.string.course_finish), model::complete); return
    }
    val question = questions.getJSONObject(index)
    val qid = question.getString("id")
    val answers = attempt.getJSONArray("answers")
    val previous = (0 until answers.length()).map { answers.getJSONObject(it) }.lastOrNull { it.optString("questionId") == qid }
    val correct = previous?.optBoolean("correct") == true
    val answered = previous != null
    var selection by rememberSaveable(attempt.optString("id"), qid) { mutableStateOf(previous?.optString("selectedOptionId").orEmpty()) }
    Text(stringResource(R.string.course_question, index + 1, questions.length()), style = MaterialTheme.typography.titleMedium)
    MentorProgress(index.toFloat() / questions.length().coerceAtLeast(1))
    Text(stringResource(when (question.optString("difficulty")) {
        "EASY" -> R.string.course_easy; "MEDIUM" -> R.string.course_medium; else -> R.string.course_applied
    }), style = MaterialTheme.typography.labelLarge)
    Text(question.getString("prompt"), style = MaterialTheme.typography.headlineSmall)
    val options = question.getJSONArray("options")
    Column(Modifier.selectableGroup().testTag("quiz-options"), verticalArrangement = Arrangement.spacedBy(MentorTokens.SpaceSmall)) {
        repeat(options.length()) { i ->
            val option = options.getJSONObject(i)
            val id = option.getString("id")
            Surface(shape = MaterialTheme.shapes.medium,
                color = if (selection == id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface) {
                Row(Modifier.fillMaxWidth().selectable(selection == id, enabled = !busy && !correct && !(placement && answered),
                    role = Role.RadioButton, onClick = { selection = id }).padding(MentorTokens.SpaceCompact), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selection == id, onClick = null, enabled = !busy && !correct && !(placement && answered))
                    Text(option.getString("text"), Modifier.padding(start = MentorTokens.SpaceSmall), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
    if (answered && !placement) MentorCard { Text(stringResource(if (correct) R.string.course_correct else R.string.course_wrong)) }
    if (question.has("explanation") && !placement) MentorCard {
        Text(stringResource(R.string.course_help), style = MaterialTheme.typography.titleMedium)
        Text(question.getString("explanation"))
    }
    if (correct || (placement && answered)) MentorButton(stringResource(if (index + 1 == questions.length()) R.string.course_finish else R.string.course_next), {
        if (index + 1 == questions.length()) model.complete() else model.save(step + 1, null, null)
    }) else MentorButton(stringResource(R.string.course_check), { model.save(step, qid, selection) }, enabled = !busy && selection.isNotBlank())
    if (!placement) TextButton({ ask(question.optString("prompt")) }, enabled = !busy) { Text(stringResource(R.string.course_ask_ai)) }
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
