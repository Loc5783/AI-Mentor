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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.mentor.ui.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Decorative drawing only. It never captures taps or contributes accessibility nodes. */
@Composable
fun TechGraffitiBackground(dark: Boolean) {
    val cloudImage = ImageBitmap.imageResource(R.drawable.atmosphere_cloud_online)
    val ink = if (dark) Color.White else Color(0xFF0F172A)
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val radius = size.minDimension * .9f
        // Overlapping radial washes produce soft smoke without bitmap memory or animation cost.
        listOf(Offset(w*.08f,h*.1f), Offset(w*.8f,h*.35f), Offset(w*.2f,h*.85f)).forEachIndexed { i, center ->
            drawCircle(Brush.radialGradient(listOf(
                (if(i == 1) Color(0xFF38BDF8) else Color(0xFF3B82F6)).copy(alpha = if(dark) .18f else .16f),
                Color.Transparent), center = center, radius = radius), radius, center)
        }
        // Fade the bundled Pexels photograph on an isolated layer, so the sky blends into the canvas.
        // Fixed-size, draw-only accents have no animation, touch targets or layout cost.
        fun cloudAt(left: Float, top: Float, width: Float, opacity: Float) {
            val height = width * cloudImage.height / cloudImage.width
            val bounds = Rect(left, top, left + width, top + height)
            drawContext.canvas.saveLayer(bounds, androidx.compose.ui.graphics.Paint())
            drawImage(cloudImage, dstOffset = IntOffset(left.toInt(), top.toInt()),
                dstSize = IntSize(width.toInt(), height.toInt()), alpha = opacity)
            drawRect(brush = Brush.radialGradient(
                0f to Color.White, .45f to Color.White, 1f to Color.Transparent,
                center = bounds.center, radius = height * .52f),
                topLeft = bounds.topLeft, size = bounds.size, blendMode = BlendMode.DstIn)
            drawContext.canvas.restore()
        }
        cloudAt(w - 160.dp.toPx(), h * .02f, 210.dp.toPx(), if (dark) .24f else .2f)
        cloudAt(-60.dp.toPx(), h * .7f, 200.dp.toPx(), if (dark) .18f else .15f)

        if (dark) {
            // Stable positions keep stars still across recompositions and screen navigation.
            listOf(Offset(w * .87f, h * .19f), Offset(w * .12f, h * .47f),
                Offset(w * .76f, h * .78f)).forEach { center ->
                val glowRadius = 9.dp.toPx()
                drawCircle(Brush.radialGradient(listOf(Color(0xFFB6DCFF).copy(alpha = .22f),
                    Color.Transparent), center = center, radius = glowRadius), glowRadius, center)
                drawCircle(Color(0xFFE8F4FF).copy(alpha = .78f), 2.dp.toPx(), center)
                val arm = 5.dp.toPx()
                drawLine(Color(0xFFE8F4FF).copy(alpha = .58f), center - Offset(arm, 0f),
                    center + Offset(arm, 0f), 1.dp.toPx())
                drawLine(Color(0xFFE8F4FF).copy(alpha = .58f), center - Offset(0f, arm),
                    center + Offset(0f, arm), 1.dp.toPx())
            }
            repeat(32) { index ->
                val x = ((index * 73 + 19) % 997) / 997f * w
                val y = ((index * 137 + 47) % 991) / 991f * h
                val center = Offset(x, y)
                val radius = (if (index % 5 == 0) 1.5f else .8f).dp.toPx()
                drawCircle(Color(0xFFDCEEFF).copy(alpha = if (index % 5 == 0) .48f else .26f), radius, center)
                if (index % 8 == 0) {
                    val arm = 3.dp.toPx()
                    drawLine(Color.White.copy(alpha = .3f), center - Offset(arm,0f), center + Offset(arm,0f), .7.dp.toPx())
                    drawLine(Color.White.copy(alpha = .3f), center - Offset(0f,arm), center + Offset(0f,arm), .7.dp.toPx())
                }
            }
        }

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
