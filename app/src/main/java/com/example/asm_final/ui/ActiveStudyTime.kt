package com.example.asm_final.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.asm_final.AiMentorApplication
import com.example.mentor.data.ActiveUser
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

/** Records time only while an assessment screen is visible in the foreground. */
@Composable
internal fun ActiveStudyTime(active: Boolean, subjectId: String?, grade: Int) {
    val context = LocalContext.current
    val owner = remember(context) { ActiveUser.get(context) }
    val repository = remember(context) {
        (context.applicationContext as? AiMentorApplication)?.services()?.study()
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    var foreground by remember(lifecycleOwner) {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, _ ->
            foreground = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(active, foreground, owner, subjectId, grade, repository) {
        if (!active || !foreground || owner <= 0 || subjectId.isNullOrBlank() || grade !in 10..12 || repository == null) {
            return@LaunchedEffect
        }
        val sessionId = UUID.randomUUID().toString()
        val started = try {
            withContext(Dispatchers.IO) { repository.start(owner, sessionId, subjectId, grade) }
            true
        } catch (_: Exception) {
            false
        }
        if (!started) return@LaunchedEffect

        try {
            while (isActive) {
                delay(60_000)
                try {
                    withContext(Dispatchers.IO) {
                        repository.heartbeat(owner, sessionId, UUID.randomUUID().toString())
                    }
                } catch (_: Exception) {
                    // The next heartbeat can recover after a transient connection failure.
                }
            }
        } finally {
            withContext(NonCancellable + Dispatchers.IO) {
                try { repository.stop(owner, sessionId, UUID.randomUUID().toString()) }
                catch (_: Exception) { /* Preserve the assessment UI if connectivity is lost. */ }
            }
        }
    }
}
