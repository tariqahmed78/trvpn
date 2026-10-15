package com.example.ui.components.glass

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.VpnState
import com.example.ui.theme.GlassTokens
import com.example.ui.theme.TrAmberConnecting
import com.example.ui.theme.TrGreenConnected
import com.example.ui.theme.TrRedDisconnected

@Composable
fun GlassStatusChip(
    vpnState: VpnState,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ChipPulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "DotPulse"
    )

    val (statusText, statusColor) = when (vpnState) {
        is VpnState.Connected -> "PROTECTED" to TrGreenConnected
        is VpnState.Connecting -> "CONNECTING" to TrAmberConnecting
        is VpnState.Reconnecting -> "RECONNECTING" to TrAmberConnecting
        is VpnState.Disconnecting -> "DISCONNECTING" to TrAmberConnecting
        is VpnState.Failed -> "FAILED" to TrRedDisconnected
        is VpnState.Preparing -> "PREPARING" to TrAmberConnecting
        else -> "UNPROTECTED" to TrRedDisconnected
    }

    GlassSurface(
        modifier = modifier,
        shape = GlassTokens.CornerPill,
        borderWidth = GlassTokens.BorderWidthThin
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .then(
                        if (vpnState.isConnected || vpnState.isConnectingOrReconnecting) {
                            Modifier.scale(scale)
                        } else {
                            Modifier
                        }
                    )
                    .background(statusColor, CircleShape)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = statusText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    fontSize = 11.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
