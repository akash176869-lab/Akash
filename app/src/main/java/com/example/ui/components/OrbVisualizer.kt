package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.AssistantState
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun OrbVisualizer(
    assistantState: AssistantState,
    inputVolume: Float,
    outputVolume: Float,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orbTransition")

    val pulse1 by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse1"
    )

    val pulse2 by infiniteTransition.animateFloat(
        initialValue = 1.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse2"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Compute volume scale factor
    val effectiveVolume = when (assistantState) {
        AssistantState.SPEAKING -> outputVolume.coerceIn(0f, 1f)
        AssistantState.LISTENING -> inputVolume.coerceIn(0f, 1f)
        else -> 0f
    }
    val dynamicScale = 1.0f + (effectiveVolume * 0.45f)

    // Colors based on state
    val (coreColors, outerRingColor) = when (assistantState) {
        AssistantState.SPEAKING -> Pair(
            listOf(Color(0xFFFF007F), Color(0xFF9D00FF), Color(0xFF00E5FF)),
            Color(0xFFFF007F)
        )
        AssistantState.LISTENING -> Pair(
            listOf(Color(0xFF00E5FF), Color(0xFF7C4DFF), Color(0xFF18FFFF)),
            Color(0xFF00E5FF)
        )
        AssistantState.THINKING, AssistantState.EXECUTING_ACTION -> Pair(
            listOf(Color(0xFFFFD600), Color(0xFFFF6D00), Color(0xFFE040FB)),
            Color(0xFFFFAB00)
        )
        AssistantState.ERROR -> Pair(
            listOf(Color(0xFFFF1744), Color(0xFFD50000), Color(0xFFFF5252)),
            Color(0xFFFF1744)
        )
        AssistantState.IDLE -> Pair(
            listOf(Color(0xFF7C4DFF), Color(0xFF3F51B5), Color(0xFF00BCD4)),
            Color(0xFF7C4DFF)
        )
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = (this.size.minDimension / 2.6f) * dynamicScale

            // 1. Outermost subtle diffuse halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        outerRingColor.copy(alpha = 0.25f),
                        outerRingColor.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.6f * pulse1
                ),
                radius = baseRadius * 1.6f * pulse1,
                center = center
            )

            // 2. Secondary energy ring with ripples
            val rippleRadius = baseRadius * 1.25f * pulse2
            drawCircle(
                color = outerRingColor.copy(alpha = 0.35f),
                radius = rippleRadius,
                center = center,
                style = Stroke(width = 3.dp.toPx())
            )

            // 3. Orbiting energy dots
            val dotCount = 6
            val radStep = (2 * Math.PI / dotCount)
            for (i in 0 until dotCount) {
                val angle = Math.toRadians(rotationAngle.toDouble()) + (i * radStep)
                val dotX = center.x + (rippleRadius * cos(angle)).toFloat()
                val dotY = center.y + (rippleRadius * sin(angle)).toFloat()
                drawCircle(
                    color = outerRingColor.copy(alpha = 0.8f),
                    radius = 3.5.dp.toPx(),
                    center = Offset(dotX, dotY)
                )
            }

            // 4. Main glowing gradient core
            val coreRadius = baseRadius * pulse1
            drawCircle(
                brush = Brush.radialGradient(
                    colors = coreColors,
                    center = center,
                    radius = coreRadius
                ),
                radius = coreRadius,
                center = center
            )

            // 5. Specular inner highlight
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.6f),
                        Color.White.copy(alpha = 0.1f),
                        Color.Transparent
                    ),
                    center = Offset(center.x - coreRadius * 0.3f, center.y - coreRadius * 0.3f),
                    radius = coreRadius * 0.6f
                ),
                radius = coreRadius * 0.6f,
                center = Offset(center.x - coreRadius * 0.3f, center.y - coreRadius * 0.3f)
            )
        }
    }
}
