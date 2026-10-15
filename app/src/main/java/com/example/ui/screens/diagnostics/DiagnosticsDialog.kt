package com.example.ui.screens.diagnostics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.VpnServer
import com.example.domain.model.VpnState
import com.example.ui.components.glass.GlassButton
import com.example.ui.components.glass.GlassDialog
import com.example.ui.theme.TrGreenConnected
import com.example.ui.theme.TrRedDisconnected

@Composable
fun DiagnosticsDialog(
    logs: List<String>,
    vpnState: VpnState,
    activeServer: VpnServer?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassDialog(
        onDismissRequest = onDismiss,
        title = "Connection Diagnostics",
        confirmButton = {
            GlassButton(onClick = onDismiss, isPrimary = true) {
                Text("Close")
            }
        },
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Tunnel Status Summary
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.25f))
                    .padding(12.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "State: ",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = vpnState.javaClass.simpleName,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = if (vpnState is VpnState.Connected) TrGreenConnected else MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Remote Node: ${activeServer?.let { "${it.countryLong} (${it.ip}:${it.profile?.remotePort ?: 1194})" } ?: "None"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Protocol: ${activeServer?.profile?.proto ?: "UDP"} • TUN Addr: 10.8.0.2/24",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "EVENT LOGS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Scrollable Log Output
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.4f))
                    .padding(10.dp)
            ) {
                if (logs.isEmpty()) {
                    Text(
                        text = "No log events recorded yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn {
                        items(logs) { entry ->
                            Text(
                                text = entry,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                ),
                                color = if (entry.contains("failure", true) || entry.contains("error", true)) {
                                    TrRedDisconnected
                                } else if (entry.contains("established", true) || entry.contains("protected", true)) {
                                    TrGreenConnected
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
