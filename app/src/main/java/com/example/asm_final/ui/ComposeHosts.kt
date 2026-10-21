package com.example.asm_final.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.example.mentor.ui.design.MentorTokens
import com.example.mentor.ui.design.MentorTheme

internal fun composeHost(context: Context, content: @Composable () -> Unit): ComposeView =
    ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent { MentorTheme {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.TopCenter) {
                Box(Modifier.widthIn(max = MentorTokens.ContentMaxWidth).fillMaxSize()) { content() }
            }
        } }
    }
