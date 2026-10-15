package com.example.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AppThemeMode
import com.example.ui.components.glass.GlassButton
import com.example.ui.components.glass.GlassCard
import com.example.ui.components.glass.GlassDialog
import com.example.ui.components.glass.GlassSurface
import com.example.ui.theme.GlassTokens
import com.example.ui.viewmodel.VpnViewModel

@Composable
fun SettingsScreen(
    viewModel: VpnViewModel,
    onShowDiagnostics: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val themeMode by viewModel.themeMode.collectAsState()
    val autoSelectFastest by viewModel.autoSelectFastest.collectAsState()
    val reconnectOnNetworkChange by viewModel.reconnectOnNetworkChange.collectAsState()

    var showResetDialog by remember { mutableStateOf(false) }
    var showLicensesDialog by remember { mutableStateOf(false) }
    var showVolunteerDisclaimerDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
    ) {
        // Title
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Preferences, security controls & diagnostics",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Appearance Section
        SettingsSectionHeader(title = "APPEARANCE", icon = Icons.Filled.ColorLens)
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(
                    text = "Interface Theme",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppThemeMode.entries.forEach { mode ->
                        val isSelected = themeMode == mode
                        GlassButton(
                            onClick = { viewModel.setThemeMode(mode) },
                            isPrimary = isSelected,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("theme_btn_${mode.name.lowercase()}"),
                            shape = GlassTokens.CornerMedium
                        ) {
                            Text(
                                text = when (mode) {
                                    AppThemeMode.SYSTEM -> "System"
                                    AppThemeMode.DARK -> "Dark"
                                    AppThemeMode.LIGHT -> "Light"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Connection Preferences
        SettingsSectionHeader(title = "CONNECTION BEHAVIOR", icon = Icons.Filled.Security)
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                SettingsSwitchRow(
                    title = "Auto-Select Fastest Server",
                    subtitle = "Automatically connects to lowest latency verified relay on one-tap connect",
                    checked = autoSelectFastest,
                    onCheckedChange = { viewModel.setAutoSelectFastest(it) },
                    testTag = "switch_auto_select"
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                SettingsSwitchRow(
                    title = "Reconnect on Network Change",
                    subtitle = "Seamlessly restores VPN tunnel when switching between Wi-Fi and mobile data",
                    checked = reconnectOnNetworkChange,
                    onCheckedChange = { viewModel.setReconnectOnNetworkChange(it) },
                    testTag = "switch_reconnect_network"
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Kill Switch & Android System Lockdown Section
        SettingsSectionHeader(title = "KILL SWITCH & SYSTEM LOCKDOWN", icon = Icons.Filled.VpnLock)
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(
                    text = "App-Level vs OS-Level Protection",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "TRVPN provides real-time connection state recovery. If a tunnel drops, the app halts data transmission and alerts you immediately.\n\nFor guaranteed 100% leak protection against network drops, enable Android's built-in System Lockdown below.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                GlassButton(
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_VPN_SETTINGS)
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            try {
                                val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                try {
                                    val intent = Intent(Settings.ACTION_SETTINGS)
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                        }
                    },
                    isPrimary = true,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Open Android VPN Settings",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Tip: In Android VPN settings, tap the gear next to TRVPN and enable 'Always-on VPN' and 'Block connections without VPN'.",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Diagnostics & Cache
        SettingsSectionHeader(title = "DIAGNOSTICS & CACHE", icon = Icons.Filled.BugReport)
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                SettingsActionRow(
                    title = "Connection Diagnostics & Logs",
                    subtitle = "Inspect real-time handshake events and socket status",
                    icon = Icons.Filled.BugReport,
                    onClick = onShowDiagnostics
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                    modifier = Modifier.padding(vertical = 10.dp)
                )

                SettingsActionRow(
                    title = "Refresh Server Directory",
                    subtitle = "Download latest list of public relay servers from VPN Gate",
                    icon = Icons.Filled.Refresh,
                    onClick = { viewModel.refreshServers() }
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                    modifier = Modifier.padding(vertical = 10.dp)
                )

                SettingsActionRow(
                    title = "Clear Cached Directory Metadata",
                    subtitle = "Removes saved servers and resets to default seed nodes",
                    icon = Icons.Filled.DeleteSweep,
                    onClick = { viewModel.clearCache() }
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                    modifier = Modifier.padding(vertical = 10.dp)
                )

                SettingsActionRow(
                    title = "Reset All Preferences",
                    subtitle = "Reverts all settings to their default values",
                    icon = Icons.Filled.RestartAlt,
                    onClick = { showResetDialog = true }
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // About & Disclosures
        SettingsSectionHeader(title = "ABOUT & PRIVACY", icon = Icons.Filled.Info)
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                SettingsActionRow(
                    title = "Volunteer Server Disclaimer",
                    subtitle = "Learn about the VPN Gate community and node limitations",
                    icon = Icons.Filled.Security,
                    onClick = { showVolunteerDisclaimerDialog = true }
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                    modifier = Modifier.padding(vertical = 10.dp)
                )

                SettingsActionRow(
                    title = "Open Source Licenses",
                    subtitle = "Legal and licensing information",
                    icon = Icons.Filled.Info,
                    onClick = { showLicensesDialog = true }
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                    modifier = Modifier.padding(vertical = 10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TRVPN Version",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "v1.0.0 (Production Architecture)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "Build 1",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }

    // Reset Preferences Dialog
    if (showResetDialog) {
        GlassDialog(
            onDismissRequest = { showResetDialog = false },
            title = "Reset Preferences?",
            confirmButton = {
                GlassButton(
                    onClick = {
                        viewModel.resetPreferences()
                        showResetDialog = false
                    },
                    isPrimary = true
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        ) {
            Text(
                text = "This will reset all theme preferences, auto-connect toggles, and favorites back to factory defaults.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }

    // Volunteer Disclaimer Dialog
    if (showVolunteerDisclaimerDialog) {
        GlassDialog(
            onDismissRequest = { showVolunteerDisclaimerDialog = false },
            title = "Volunteer Relay Disclaimer",
            confirmButton = {
                GlassButton(
                    onClick = { showVolunteerDisclaimerDialog = false },
                    isPrimary = true
                ) {
                    Text("I Understand")
                }
            }
        ) {
            Text(
                text = "TRVPN routes encrypted traffic through public relay servers hosted by academic and individual volunteers worldwide via the University of Tsukuba's VPN Gate project.\n\n• Volunteer operators cannot inspect encrypted TLS payload content.\n• TRVPN does not collect any user activity or browsing logs.\n• Relay operators may record transport packet logs according to their local jurisdiction.\n• Do not rely on volunteer nodes for complete anonymity against state-level intelligence.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }

    // Open Source Licenses Dialog
    if (showLicensesDialog) {
        GlassDialog(
            onDismissRequest = { showLicensesDialog = false },
            title = "Open Source Licenses",
            confirmButton = {
                GlassButton(
                    onClick = { showLicensesDialog = false },
                    isPrimary = true
                ) {
                    Text("Close")
                }
            }
        ) {
            Text(
                text = "TRVPN is built with open-source technologies:\n\n• Jetpack Compose & Material 3 (Apache 2.0)\n• OpenVPN Protocol (GPLv2 / OpenVPN project)\n• VPN Gate Public Academic Directory (University of Tsukuba)\n• Kotlin Coroutines & Flow (Apache 2.0)\n• OkHttp (Apache 2.0)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun SettingsSectionHeader(
    title: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                fontSize = 11.sp
            ),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
private fun SettingsActionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp)
        )
    }
}
