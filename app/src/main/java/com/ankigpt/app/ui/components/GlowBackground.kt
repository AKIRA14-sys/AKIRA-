package com.ankigpt.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.ankigpt.app.ui.theme.DarkBackground
import com.ankigpt.app.ui.theme.DarkSurface
import com.ankigpt.app.ui.theme.NeonCyan
import com.ankigpt.app.ui.theme.NeonPurple

@Composable
fun GlowBackground(content: @Composable () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "backgroundAnimation")

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val offsetAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 500f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offset"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Top-Right Glowing Orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(NeonCyan.copy(alpha = 0.25f * pulse), Color.Transparent),
                    center = Offset(width * 0.85f, height * 0.15f + (pulse * 50f)),
                    radius = width * 0.7f
                ),
                center = Offset(width * 0.85f, height * 0.15f),
                radius = width * 0.7f
            )

            // Bottom-Left Glowing Orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(NeonPurple.copy(alpha = 0.3f * pulse), Color.Transparent),
                    center = Offset(width * 0.15f, height * 0.85f - (pulse * 50f)),
                    radius = width * 0.8f
                ),
                center = Offset(width * 0.15f, height * 0.85f),
                radius = width * 0.8f
            )

            // Animated subtle Grid Lines
            val gridSpacing = 120f
            var x = (offsetAnim % gridSpacing)
            while (x < width) {
                drawLine(
                    color = Color(0x0C00F0FF),
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 1f
                )
                x += gridSpacing
            }
        }

        content()
    }
}
