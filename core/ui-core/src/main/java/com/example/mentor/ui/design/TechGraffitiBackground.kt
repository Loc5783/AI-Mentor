package com.example.mentor.ui.design

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Decorative drawing only. It never captures taps or contributes accessibility nodes. */
@Composable
fun TechGraffitiBackground(dark: Boolean) {
    val ink = if (dark) Color.White else Color(0xFF0F172A)
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val radius = size.minDimension * 0.95f
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xFFFFD166).copy(alpha = if (dark) 0.13f else 0.06f), Color.Transparent),
                center = Offset(w, 0f), radius = radius,
            ),
            radius = radius, center = Offset(w, 0f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xFF8B5CF6).copy(alpha = if (dark) 0.12f else 0.05f), Color.Transparent),
                center = Offset(0f, h), radius = radius,
            ),
            radius = radius, center = Offset(0f, h),
        )

        val doodle = ink.copy(alpha = 0.04f)
        val symbols = listOf(
            Triple("∑", 0.82f, 0.15f), Triple("π", 0.23f, 0.34f),
            Triple("∫", 0.75f, 0.52f), Triple("E = mc²", 0.14f, 0.76f),
        )
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = doodle.toArgb()
            textSize = 40.sp.toPx()
        }
        drawIntoCanvas { canvas ->
            symbols.forEach { (symbol, x, y) -> canvas.nativeCanvas.drawText(symbol, w * x, h * y, paint) }
        }

        val stroke = 1.dp.toPx()
        val orbitCenter = Offset(w * 0.77f, h * 0.68f)
        drawCircle(doodle, radius = 30.dp.toPx(), center = orbitCenter, style = Stroke(stroke))
        drawCircle(doodle, radius = 4.dp.toPx(), center = orbitCenter)
        val points = listOf(Offset(w * 0.18f, h * 0.22f), Offset(w * 0.31f, h * 0.19f),
            Offset(w * 0.40f, h * 0.25f), Offset(w * 0.53f, h * 0.21f))
        points.zipWithNext().forEach { (a, b) -> drawLine(doodle, a, b, stroke) }
        points.forEach { drawCircle(doodle, radius = 3.dp.toPx(), center = it) }
        listOf(Offset(w * 0.14f, h * 0.62f), Offset(w * 0.9f, h * 0.39f)).forEach { star ->
            val arm = 8.dp.toPx()
            drawLine(doodle, star.copy(x = star.x - arm), star.copy(x = star.x + arm), stroke)
            drawLine(doodle, star.copy(y = star.y - arm), star.copy(y = star.y + arm), stroke)
        }
    }
}
