package com.example.mentor.ui.design

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.mentor.ui.R

enum class MentorArtwork { ASSISTANT, PLANNER, GOALS }

/** Bundled unDraw artwork; decorative so screen readers read the adjacent message once. */
@Composable
fun MentorIllustration(artwork: MentorArtwork, modifier: Modifier = Modifier) {
    val resource = when (artwork) {
        MentorArtwork.ASSISTANT -> R.drawable.illustration_mentor
        MentorArtwork.PLANNER -> R.drawable.illustration_planner
        MentorArtwork.GOALS -> R.drawable.illustration_goals
    }
    Image(painterResource(resource), contentDescription = null,
        modifier = modifier.widthIn(max = 240.dp).height(124.dp), contentScale = ContentScale.Fit)
}
