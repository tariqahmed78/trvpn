package com.example.ui.screens.home

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.VpnServer
import com.example.domain.model.VpnState
import com.example.ui.components.glass.GlassCard
import com.example.ui.components.glass.GlassIconButton
import com.example.ui.components.glass.GlassStatusChip
import com.example.ui.components.glass.GlassSurface
import com.example.ui.theme.GlassTokens
import com.example.ui.theme.TrGreenConnected
import com.example.ui.theme.TrRedDisconnected
import com.example.ui.viewmodel.VpnViewModel

@Composable
fun HomeScreen(
    viewModel: VpnViewModel,
    onNavigateToLocations: () -> Unit,
    onShowDiagnostics: () -> Unit,
    onRequirePrivacyDisclosure: () -> Unit,
    onRequireVpnPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val vpnState by viewModel.vpnState.collectAsState()
    val sessionStats by viewModel.sessionStats.collectAsState()
    val selectedServer by viewModel.selectedServer.collectAsState()

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Logo & Wordmark
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassSurface(
                    modifier = Modifier.size(40.dp),
                    shape = GlassTokens.CornerMedium
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = "TRVPN Logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "TRVPN",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "OPEN SECURE RELAY",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            letterSpacing = 1.2.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassStatusChip(vpnState = vpnState)
                Spacer(modifier = Modifier.width(8.dp))
                GlassIconButton(
                    onClick = onShowDiagnostics,
                    size = 38.dp,
                    modifier = Modifier.testTag("btn_diagnostics")
                ) {
                    Icon(
                        imageVector = Icons.Filled.BugReport,
                        contentDescription = "Connection Diagnostics",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Center Connect Button
        ConnectionButton(
            vpnState = vpnState,
            onClick = {
                viewModel.onConnectToggled(
                    context = context,
                    onRequirePrivacyDisclosure = onRequirePrivacyDisclosure,
                    onRequireVpnPermission = onRequireVpnPermission
                )
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Status Label & Explanatory Subtext
        val statusHeadline = when (vpnState) {
            is VpnState.Connected -> "Tunnel Active & Encrypted"
            is VpnState.Connecting -> "Establishing Handshake..."
            is VpnState.Reconnecting -> "Reconnecting Tunnel..."
            is VpnState.Preparing -> "Preparing VpnService..."
            is VpnState.Disconnecting -> "Tearing Down Tunnel..."
            is VpnState.Failed -> (vpnState as VpnState.Failed).message
            is VpnState.Disconnected -> "Device Unprotected"
        }

        val statusDescription = when (vpnState) {
            is VpnState.Connected -> "All IP traffic is routed through encrypted OpenVPN relay."
            is VpnState.Connecting -> "Authenticating with volunteer node and negotiating TLS keys."
            is VpnState.Reconnecting -> "Physical network switched. Recovering VPN connection."
            is VpnState.Preparing -> "Configuring local virtual TUN interface."
            is VpnState.Disconnecting -> "Restoring normal network routing."
            is VpnState.Failed -> "Tap RETRY or switch to another server in Locations."
            is VpnState.Disconnected -> "Your IP address and DNS requests are exposed to local network."
        }

        Text(
            text = statusHeadline,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = if (vpnState is VpnState.Connected) TrGreenConnected else MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = statusDescription,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Selected Server Glass Card
        selectedServer?.let { server ->
            ServerSelectionCard(
                server = server,
                onClick = onNavigateToLocations
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Live Connection Statistics (When Connected or Active)
        AnimatedVisibility(
            visible = vpnState is VpnState.Connected || sessionStats.durationSeconds > 0,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            ConnectionStatsCard(
                durationSeconds = sessionStats.durationSeconds,
                bytesIn = sessionStats.bytesIn,
                bytesOut = sessionStats.bytesOut,
                latencyMs = selectedServer?.bestPingMs
            )
        }

        Spacer(modifier = Modifier.height(90.dp))
    }
}

@Composable
fun ServerSelectionCard(
    server: VpnServer,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("selected_server_card"),
        onClick = onClick,
        contentPadding = 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flag Box
            GlassSurface(
                modifier = Modifier.size(46.dp),
                shape = GlassTokens.CornerMedium
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = server.flagEmoji,
                        fontSize = 24.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = server.countryLong,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(${server.countryShort})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${server.ip} • ${server.profile?.proto ?: "UDP"} ${server.profile?.remotePort ?: 1194}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Latency Badge
            val ping = server.bestPingMs
            val pingColor = when {
                ping < 80 -> TrGreenConnected
                ping < 160 -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.error
            }

            GlassSurface(
                shape = GlassTokens.CornerPill,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.FlashOn,
                        contentDescription = "Ping",
                        tint = pingColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "${ping}ms",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = pingColor
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = "Switch Server",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun ConnectionStatsCard(
    durationSeconds: Long,
    bytesIn: Long,
    bytesOut: Long,
    latencyMs: Long?,
    modifier: Modifier = Modifier
) {
    val formattedDuration = String.format(
        "%02d:%02d:%02d",
        durationSeconds / 3600,
        (durationSeconds % 3600) / 60,
        durationSeconds % 60
    )

    val mbDown = String.format("%.2f MB", bytesIn.toDouble() / (1024 * 1024))
    val mbUp = String.format("%.2f MB", bytesOut.toDouble() / (1024 * 1024))

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = 16.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "SESSION TELEMETRY",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Duration
                StatItem(
                    title = "DURATION",
                    value = formattedDuration,
                    icon = Icons.Filled.Timer
                )

                // Download
                StatItem(
                    title = "DOWNLOAD",
                    value = mbDown,
                    icon = Icons.Filled.Speed
                )

                // Upload
                StatItem(
                    title = "UPLOAD",
                    value = mbUp,
                    icon = Icons.Filled.Lock
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(horizontalAlignment = Alignment.Start) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
