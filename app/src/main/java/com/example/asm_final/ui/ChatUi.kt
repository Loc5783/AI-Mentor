package com.example.asm_final.ui

import android.content.Context
import android.view.View
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import coil3.compose.AsyncImage
import com.example.asm_final.R
import com.example.asm_final.chat.*
import com.example.mentor.ui.design.*

object ChatUi {
    interface Actions {
        fun history()
        fun options()
        fun grade()
        fun attach()
        fun file(file: ChatRecords.FileItem)
        fun copy(text: String)
        fun formula(text: String)
        fun returnToLesson()
    }
    @JvmStatic fun view(context: Context, model: ChatViewModel, actions: Actions, returning: Boolean): View =
        composeHost(context) {
            val revision by model.changes().observeAsState(0)
            ChatScreen(model, revision, actions, returning)
        }
}

@Composable
private fun ChatScreen(model: ChatViewModel, revision: Int, actions: ChatUi.Actions, returning: Boolean) {
    val ready = model.snapshot != null && !model.loading && !model.importing
    val thread = model.threadId()
    var draft by remember(thread) { mutableStateOf(model.draft) }
    LaunchedEffect(revision, thread) { if (draft != model.draft) draft = model.draft }
    val rows = model.snapshot?.messages.orEmpty()
    val list = rememberLazyListState()
    var previousThread by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(thread, rows.size) {
        val switched = previousThread != thread
        previousThread = thread
        val last = list.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        if (rows.isNotEmpty() && (switched || last >= rows.size - 4)) list.scrollToItem(rows.lastIndex)
    }
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = .8f),
        )))) {
        Row(Modifier.fillMaxWidth().padding(horizontal = MentorTokens.SpaceSmall, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            ChatIcon(R.drawable.ic_chat_history, R.string.chat_history, ready, actions::history)
            Column(Modifier.weight(1f)) {
                Text(model.snapshot?.thread?.title?.takeIf { it.isNotBlank() } ?: stringResource(R.string.chat_title),
                    style = MaterialTheme.typography.titleMedium, maxLines = 2)
                TextButton(actions::grade, enabled = ready) {
                    Text(if (model.grade in 10..12) stringResource(R.string.chat_grade, model.grade) else stringResource(R.string.chat_choose_grade))
                }
            }
            ChatIcon(R.drawable.ic_chat_new, R.string.chat_new, ready, model::newChat)
            ChatIcon(R.drawable.ic_chat_more, R.string.chat_options, ready, actions::options)
        }
        }
        if (returning) MentorButton(stringResource(R.string.design_understood), actions::returnToLesson,
            Modifier.fillMaxWidth().padding(horizontal = MentorTokens.SpaceMedium))
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = list,
            contentPadding = PaddingValues(MentorTokens.SpaceMedium), verticalArrangement = Arrangement.spacedBy(MentorTokens.SpaceMedium)) {
            if (model.loading) items(3) { MentorSkeleton(stringResource(R.string.course_loading)) }
            else if (rows.isEmpty()) item {
                MentorCard(Modifier.fillMaxWidth()) {
                    MentorPresence()
                    Text(stringResource(R.string.design_mentor), style = MaterialTheme.typography.labelLarge)
                    Text(stringResource(R.string.chat_empty_title), style = MaterialTheme.typography.headlineMedium)
                    Text(stringResource(R.string.chat_empty_body))
                    listOf(R.string.chat_suggest_physics, R.string.chat_suggest_chemistry, R.string.chat_suggest_biology).forEach { id ->
                        val suggestion = stringResource(id)
                        OutlinedButton({ draft = suggestion; model.setDraft(suggestion) }, enabled = ready) { Text(suggestion) }
                    }
                }
            }
            items(rows, key = { it.id }) { message ->
                MessageBubble(message, model.owner(), model.requesting, model.hasUnsavedReply(message.id), model::retry, actions)
            }
            if (model.busyHere()) item(key = "typing") { TypingDots() }
        }
        val status = when {
            model.status != 0 -> model.status
            model.importing -> R.string.chat_importing
            model.requesting && !model.busyHere() -> R.string.chat_thinking_other
            else -> 0
        }
        if (status != 0) Text(stringResource(status), Modifier.padding(horizontal = MentorTokens.PagePadding)
            .semantics { liveRegion = LiveRegionMode.Polite }, style = MaterialTheme.typography.bodySmall)
        if (model.importing) MentorSkeleton(stringResource(R.string.chat_importing), Modifier.padding(horizontal = MentorTokens.SpaceMedium))
        Surface(Modifier.fillMaxWidth().padding(MentorTokens.SpaceCompact), shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface.copy(alpha = .92f),
            border = BorderStroke(1.dp, Brush.linearGradient(listOf(
                MaterialTheme.colorScheme.secondary.copy(alpha = .55f),
                MaterialTheme.colorScheme.tertiary.copy(alpha = .35f)))),
            shadowElevation = MentorTokens.ActionElevation) {
            Column(Modifier.padding(MentorTokens.SpaceSmall)) {
                model.snapshot?.draftFiles.orEmpty().forEach { file ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton({ actions.file(file) }, Modifier.weight(1f), enabled = ready) { Text(file.name, maxLines = 2) }
                        TextButton({ model.removeFile(file.id) }, enabled = ready) { Text(stringResource(R.string.chat_remove_file, file.name)) }
                    }
                }
                OutlinedTextField(draft, {
                    if (it.length <= 30000) { draft = it; model.setDraft(it) }
                }, Modifier.fillMaxWidth().testTag("chat-input"), placeholder = { Text(stringResource(R.string.chat_hint)) },
                    enabled = !model.loading && !model.importing, maxLines = 5)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    ChatIcon(R.drawable.ic_chat_attach, R.string.chat_attach, ready, actions::attach)
                    ChatIcon(if (model.requesting) R.drawable.ic_chat_stop else R.drawable.ic_chat_send,
                        if (model.requesting) R.string.chat_stop else R.string.chat_send, ready) {
                        if (model.requesting) model.cancel()
                        else if (model.grade !in 10..12) actions.grade()
                        else model.send()
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatIcon(drawable: Int, label: Int, enabled: Boolean, action: () -> Unit) {
    IconButton(action, enabled = enabled, modifier = Modifier.sizeIn(minWidth = MentorTokens.TouchTarget, minHeight = MentorTokens.TouchTarget)) {
        Icon(painterResource(drawable), stringResource(label))
    }
}

@Composable
private fun MessageBubble(message: ChatRecords.Message, owner: Int, requesting: Boolean, retryable: Boolean,
    retry: (String) -> Unit, actions: ChatUi.Actions) {
    val user = message.role == "USER"
    var visible by remember(message.id) { mutableStateOf(false) }
    LaunchedEffect(message.id) { visible = true }
    val duration = if (motionEnabled()) MentorTokens.MotionMillis else 0
    AnimatedVisibility(visible, enter = fadeIn(tween(duration)) + slideInVertically(tween(duration)) { it / 8 }) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = if (user) Alignment.End else Alignment.Start) {
            Surface(shape = MaterialTheme.shapes.medium,
                color = if (user) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface) {
                Column(Modifier.widthIn(max = 600.dp).padding(MentorTokens.SpaceMedium), verticalArrangement = Arrangement.spacedBy(MentorTokens.SpaceSmall)) {
                    Text(stringResource(if (user) R.string.chat_user else R.string.chat_assistant), style = MaterialTheme.typography.labelMedium)
                    if (message.text.isNotBlank()) SelectionContainer { Text(remember(message.text) { readableMessage(message.text) }, style = MaterialTheme.typography.bodyLarge) }
                    message.files.forEach { file ->
                        if (file.mime.startsWith("image/")) {
                            val context = LocalContext.current
                            val source = remember(owner, file.id) {
                                runCatching { LocalChatFiles(context).resolve(owner, file) }.getOrNull()
                            }
                            AsyncImage(source, file.name, Modifier.fillMaxWidth().heightIn(max = 200.dp).clip(MaterialTheme.shapes.small))
                        }
                        TextButton({ actions.file(file) }) { Text(file.name) }
                    }
                    val status = when {
                        user && message.status == "PENDING" -> R.string.chat_thinking
                        user && message.status == "CANCELLED" -> R.string.chat_message_stopped
                        user && message.status == "FAILED" -> R.string.chat_no_response
                        !user && message.truncated -> R.string.chat_truncated
                        else -> 0
                    }
                    if (status != 0) Text(stringResource(status), style = MaterialTheme.typography.bodySmall)
                    if (message.text.isNotBlank()) TextButton({ actions.copy(message.text) }) { Text(stringResource(R.string.chat_copy)) }
                    if (user && !requesting && (message.status in listOf("FAILED", "CANCELLED") || retryable))
                        TextButton({ retry(message.id) }) { Text(stringResource(R.string.chat_retry)) }
                    if (!user && (message.text.contains('$') || message.text.contains("\\(") || message.text.contains("\\[")))
                        TextButton({ actions.formula(message.text) }) { Text(stringResource(R.string.chat_formula)) }
                }
            }
        }
    }
}

@Composable
private fun TypingDots() {
    val moving = motionEnabled()
    Row(Modifier.padding(MentorTokens.SpaceMedium).semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.chat_thinking), style = MaterialTheme.typography.bodySmall)
        repeat(3) { index ->
            val displacement = if (moving) {
                val animation = rememberInfiniteTransition(label = "typing")
                animation.animateFloat(0f, -5f,
                    infiniteRepeatable(tween(400, index * 100), RepeatMode.Reverse), label = "dot")
            } else remember { mutableFloatStateOf(0f) }
            Box(Modifier.size(6.dp).graphicsLayer { translationY = displacement.value * density }
                .clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.primary))
        }
    }
}

@Composable
private fun MentorPresence() {
    val scale = if (motionEnabled()) {
        val animation = rememberInfiniteTransition(label = "mentor-presence")
        animation.animateFloat(1f, 1.035f, infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "breathe")
    } else remember { mutableFloatStateOf(1f) }
    Surface(Modifier.size(MentorTokens.AvatarSize).graphicsLayer { scaleX = scale.value; scaleY = scale.value },
        shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer) {
        Box(contentAlignment = Alignment.Center) { Text("AI", style = MaterialTheme.typography.titleLarge) }
    }
}

private fun readableMessage(source: String) = buildAnnotatedString {
    val pattern = Regex("""\*\*(.+?)\*\*|`([^`]+)`""")
    var position = 0
    pattern.findAll(source).forEach { match ->
        append(source.substring(position, match.range.first))
        val bold = match.groupValues[1].isNotEmpty()
        withStyle(if (bold) SpanStyle(fontWeight = FontWeight.Bold) else SpanStyle(fontFamily = FontFamily.Monospace)) {
            append(if (bold) match.groupValues[1] else match.groupValues[2])
        }
        position = match.range.last + 1
    }
    append(source.substring(position))
}
