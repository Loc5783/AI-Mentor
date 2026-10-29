package com.example.mentor.ui.design

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.mentor.ui.R

/** Static bundled artwork: no network, animation or touch handling. */
@Composable
fun TechGraffitiBackground(dark: Boolean) {
    Box(Modifier.fillMaxSize()) {
        Image(painterResource(if (dark) R.drawable.sky_night else R.drawable.sky_day),
            contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        // A light scrim keeps text readable outside cards while retaining the artwork.
        Box(Modifier.fillMaxSize().background(
            if (dark) Color(0xFF071124).copy(alpha = .16f) else Color.White.copy(alpha = .28f)))
    }
}
