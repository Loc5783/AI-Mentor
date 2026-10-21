package com.example.asm_final.ui

import android.content.Context
import android.view.View
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.asm_final.R
import com.example.asm_final.learning.LessonPathData
import com.example.asm_final.learning.LessonViewModel
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
    @JvmStatic fun screen(context: Context, model: LessonViewModel, leave: Runnable, ask: Consumer<String>): View =
        composeHost(context) {
            val revision by model.changes().observeAsState(0)
            // Snapshot Java fields on each emission; passing the mutable model alone can be skipped by Compose.
            val state = remember(revision) { LessonUiState(model.busy, model.needsLogin, model.hasPending(),
                model.error, model.path, model.attempt, model.completion, model.grade()) }
            LessonScreen(model, state, { leave.run() }, { ask.accept(it) })
        }
}

@Composable
internal fun PathContent(path: LessonPathData, open: (String) -> Unit) {
    MentorCard(Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.design_path), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.course_path_count, path.completedLessons, path.totalLessons))
        MentorProgress(if (path.totalLessons == 0) 0f else path.completedLessons.toFloat() / path.totalLessons)
        Text(stringResource(R.string.course_path_note), style = MaterialTheme.typography.bodySmall)
    }
    path.units.forEachIndexed { index, unit ->
        val locked = unit.state == "LOCKED"
        val done = unit.state == "COMPLETED"
        var shown by remember(unit.id) { mutableStateOf(false) }
        val moving = motionEnabled()
        val rail = MaterialTheme.colorScheme.outlineVariant
        LaunchedEffect(unit.id) { shown = true }
        AnimatedVisibility(shown, enter = fadeIn(tween(if (moving) 220 else 0, if (moving) minOf(index, 5) * 30 else 0))) {
            Column(Modifier.fillMaxWidth().drawBehind {
                drawLine(rail, Offset(26.dp.toPx(), 0f), Offset(26.dp.toPx(), size.height), 2.dp.toPx())
            }.padding(top = MentorTokens.SpaceCompact)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MentorTokens.SpaceCompact)) {
                    Surface(Modifier.width(52.dp), shape = MaterialTheme.shapes.medium,
                        contentColor = if (done) MentorTokens.Ink else MaterialTheme.colorScheme.onSurface,
                        color = if (done) MentorTokens.Success else if (locked) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer) {
                        Box(Modifier.heightIn(min = 52.dp).padding(MentorTokens.SpaceSmall), contentAlignment = Alignment.Center) {
                            Text(if (unit.review) stringResource(R.string.course_review_badge) else unit.ordinal.toString(),
                                style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    MentorCard(Modifier.weight(1f).testTag("path-unit:${unit.id}")) {
                        Text(if (unit.review) stringResource(R.string.course_review_range, maxOf(1, unit.ordinal - 4), unit.ordinal)
                            else unit.title, style = MaterialTheme.typography.titleMedium)
                        Text(if (done) stringResource(R.string.course_path_retake) else stringResource(R.string.course_stage_reward, unit.xp),
                            style = MaterialTheme.typography.bodySmall)
                        MentorButton(stringResource(if (locked) R.string.course_locked else if (done) R.string.course_done else R.string.course_available),
                            { open(unit.id) }, enabled = !locked)
                        if (locked) Text(stringResource(R.string.course_unlock_note), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

private data class LessonUiState(val busy: Boolean, val needsLogin: Boolean, val pending: Boolean,
    val error: String?, val path: JSONObject?, val attempt: JSONObject?, val completion: JSONObject?, val grade: Int)

@Composable
private fun LessonScreen(model: LessonViewModel, state: LessonUiState, leave: () -> Unit, ask: (String) -> Unit) {
    // Do not key the whole tree on revision: selection, scroll and text must survive updates.
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
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        .verticalScroll(scroll).padding(MentorTokens.PagePadding), verticalArrangement = Arrangement.spacedBy(MentorTokens.SpaceMedium)) {
        TextButton(leave) { Text(stringResource(R.string.course_back)) }
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
    val colors = listOf(MentorTokens.Blue.toArgb(), MentorTokens.Indigo.toArgb(), MentorTokens.Amber.toArgb())
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
