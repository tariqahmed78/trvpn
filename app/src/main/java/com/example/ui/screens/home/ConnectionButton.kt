package com.example.ui.screens.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.VpnState
import com.example.ui.components.glass.GlassSurface
import com.example.ui.theme.TrAmberConnecting
import com.example.ui.theme.TrCyanGlow
import com.example.ui.theme.TrGreenConnected
import com.example.ui.theme.TrRedDisconnected

@Composable
fun ConnectionButton(
    vpnState: VpnState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    val infiniteTransition = rememberInfiniteTransition(label = "ButtonTransition")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ButtonPulse"
    )

    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ButtonRingSpin"
    )

    val (glowColor, labelText) = when (vpnState) {
        is VpnState.Connected -> TrGreenConnected to "DISCONNECT"
        is VpnState.Connecting -> TrAmberConnecting to "CONNECTING"
        is VpnState.Reconnecting -> TrAmberConnecting to "RECONNECTING"
        is VpnState.Preparing -> TrAmberConnecting to "PREPARING"
        is VpnState.Disconnecting -> TrAmberConnecting to "DISCONNECTING"
        is VpnState.Failed -> TrRedDisconnected to "RETRY"
        is VpnState.Disconnected -> MaterialTheme.colorScheme.primary to "CONNECT"
    }

    val animatedGlowColor by animateColorAsState(targetValue = glowColor, label = "GlowColor")

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(230.dp)
            .testTag("main_connect_button"),
        contentAlignment = Alignment.Center
    ) {
        // Outer glowing animated rings
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .then(
                    if (vpnState is VpnState.Connected) {
                        Modifier.scale(pulseScale)
                    } else if (vpnState.isConnectingOrReconnecting) {
                        Modifier.rotate(spinAngle)
                    } else {
                        Modifier
                    }
                )
        ) {
            val strokeWidth = 2.dp.toPx()
            val radius = (size.minDimension / 2) - strokeWidth

            // Ambient outer glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        animatedGlowColor.copy(alpha = if (isDark) 0.35f else 0.25f),
                        animatedGlowColor.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    radius = size.minDimension / 2
                ),
                radius = size.minDimension / 2
            )

            // Fine luminous outline ring
            drawCircle(
                brush = Brush.sweepGradient(
                    listOf(
                        animatedGlowColor.copy(alpha = 0.9f),
                        animatedGlowColor.copy(alpha = 0.2f),
                        animatedGlowColor.copy(alpha = 0.9f)
                    )
                ),
                radius = radius,
                style = Stroke(width = strokeWidth)
            )
        }

        // Circular Glass Core
        GlassSurface(
            modifier = Modifier
                .size(180.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(bounded = true, radius = 90.dp),
                    onClick = onClick
                ),
            shape = CircleShape,
            backgroundColor = if (isDark) {
                Color(0xFF0F1E38).copy(alpha = 0.72f)
            } else {
                Color.White.copy(alpha = 0.85f)
            },
            borderBrush = Brush.sweepGradient(
                listOf(
                    animatedGlowColor.copy(alpha = 0.8f),
                    Color.White.copy(alpha = 0.3f),
                    animatedGlowColor.copy(alpha = 0.8f)
                )
            ),
            borderWidth = 1.5.dp
        ) {
            Column(
                modifier = Modifier.matchParentSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (vpnState.isConnectingOrReconnecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(54.dp),
                        color = animatedGlowColor,
                        strokeWidth = 3.5.dp
                    )
                } else {
                    Icon(
                        imageVector = if (vpnState is VpnState.Connected) {
                            Icons.Filled.Security
                        } else {
                            Icons.Filled.PowerSettingsNew
                        },
                        contentDescription = labelText,
                        tint = animatedGlowColor,
                        modifier = Modifier.size(54.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = labelText,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp,
                        fontSize = 13.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
