package com.example.ui.components.glass

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.domain.model.VpnState
import com.example.ui.theme.TrBlueGlow
import com.example.ui.theme.TrCyanGlow
import com.example.ui.theme.TrGreenConnected
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LiquidGlassBackground(
    modifier: Modifier = Modifier,
    vpnState: VpnState = VpnState.Disconnected,
    content: @Composable () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    val infiniteTransition = rememberInfiniteTransition(label = "LiquidGlassTransition")

    // Slow organic floating animations
    val angle1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Orb1Angle"
    )

    val angle2 by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 32000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Orb2Angle"
    )

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AuraPulse"
    )

    val baseBg = MaterialTheme.colorScheme.background

    // Accent colors based on connection state
    val stateAccent = when (vpnState) {
        is VpnState.Connected -> TrGreenConnected
        is VpnState.Connecting, is VpnState.Reconnecting -> TrCyanGlow
        else -> TrBlueGlow
    }

    Box(modifier = modifier.fillMaxSize().background(baseBg)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Base ambient linear gradient
            drawRect(
                brush = Brush.verticalGradient(
                    colors = if (isDark) {
                        listOf(
                            baseBg,
                            Color(0xFF0C1428),
                            Color(0xFF060A14)
                        )
                    } else {
                        listOf(
                            baseBg,
                            Color(0xFFE6EFFB),
                            Color(0xFFF3F7FC)
                        )
                    }
                )
            )

            // Animated Floating Liquid Orbs (drawn via radial gradient for high performance)
            val rad1 = Math.toRadians(angle1.toDouble())
            val orb1X = (width * 0.35f + cos(rad1).toFloat() * (width * 0.25f))
            val orb1Y = (height * 0.25f + sin(rad1).toFloat() * (height * 0.12f))

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        (if (isDark) TrCyanGlow else Color(0xFF38BDF8)).copy(alpha = if (isDark) 0.18f else 0.22f),
                        Color.Transparent
                    ),
                    center = Offset(orb1X, orb1Y),
                    radius = width * 0.65f
                ),
                center = Offset(orb1X, orb1Y),
                radius = width * 0.65f
            )

            val rad2 = Math.toRadians(angle2.toDouble())
            val orb2X = (width * 0.65f + sin(rad2).toFloat() * (width * 0.22f))
            val orb2Y = (height * 0.60f + cos(rad2).toFloat() * (height * 0.15f))

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        (if (isDark) stateAccent else Color(0xFF818CF8)).copy(alpha = if (isDark) 0.16f else 0.18f),
                        Color.Transparent
                    ),
                    center = Offset(orb2X, orb2Y),
                    radius = width * 0.75f
                ),
                center = Offset(orb2X, orb2Y),
                radius = width * 0.75f
            )

            // Central Luminous Aura behind main button
            val centerX = width * 0.5f
            val centerY = height * 0.38f
            val auraRadius = (width * 0.42f) * (if (vpnState is VpnState.Connected) pulse else 1f)
            val auraAlpha = when (vpnState) {
                is VpnState.Connected -> 0.28f
                is VpnState.Connecting, is VpnState.Reconnecting -> 0.22f
                else -> 0.08f
            }

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        stateAccent.copy(alpha = auraAlpha),
                        stateAccent.copy(alpha = auraAlpha * 0.3f),
                        Color.Transparent
                    ),
                    center = Offset(centerX, centerY),
                    radius = auraRadius
                ),
                center = Offset(centerX, centerY),
                radius = auraRadius
            )

            // Subtle luminous light streak across the diagonal
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = if (isDark) 0.04f else 0.08f),
                        Color.Transparent
                    ),
                    start = Offset(0f, height * 0.15f),
                    end = Offset(width, height * 0.55f)
                ),
                start = Offset(0f, height * 0.15f),
                end = Offset(width, height * 0.55f),
                strokeWidth = 2f
            )
        }

        content()
    }
}
