package com.example.ui.screens.disclosure

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.glass.GlassButton
import com.example.ui.components.glass.GlassDialog

@Composable
fun PrivacyDisclosureDialog(
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    GlassDialog(
        onDismissRequest = onDecline,
        title = "Network Privacy Notice",
        confirmButton = {
            GlassButton(
                onClick = onAccept,
                isPrimary = true
            ) {
                Text("Accept & Continue")
            }
        },
        dismissButton = {
            TextButton(onClick = onDecline) {
                Text("Decline", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            DisclosureSection(
                title = "Android VpnService Usage",
                content = "TRVPN creates a local Virtual Private Network (VPN) tunnel on your device using Android's VpnService API to route your device network packets through an encrypted OpenVPN connection."
            )

            Spacer(modifier = Modifier.height(10.dp))

            DisclosureSection(
                title = "Traffic Routing & Remote Relays",
                content = "Your network traffic is forwarded through public volunteer relay nodes provided via the University of Tsukuba's VPN Gate directory."
            )

            Spacer(modifier = Modifier.height(10.dp))

            DisclosureSection(
                title = "Zero Data Collection by TRVPN",
                content = "The TRVPN application does not collect, record, track, or sell your browsing history, DNS lookups, downloaded files, or personal identity."
            )

            Spacer(modifier = Modifier.height(10.dp))

            DisclosureSection(
                title = "Volunteer Operator Role & Anonymity Limits",
                content = "Because servers are maintained by academic volunteers around the world, volunteer operators cannot view encrypted HTTPS contents, but they may maintain connection connection timestamp logs. Do not rely on volunteer nodes for complete anonymity against targeted state surveillance."
            )
        }
    }
}

@Composable
private fun DisclosureSection(title: String, content: String) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = content,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
