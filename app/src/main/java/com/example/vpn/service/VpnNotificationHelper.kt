package com.example.vpn.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.domain.model.VpnServer
import com.example.domain.model.VpnState

object VpnNotificationHelper {

    const val CHANNEL_ID = "trvpn_tunnel_channel"
    const val NOTIFICATION_ID = 1001
    const val ACTION_DISCONNECT = "com.trtech.trvpn.ACTION_DISCONNECT"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "TRVPN Tunnel Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live status and controls for active TRVPN connection"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun buildNotification(
        context: Context,
        state: VpnState,
        server: VpnServer?,
        elapsedSeconds: Long = 0L,
        bytesIn: Long = 0L,
        bytesOut: Long = 0L
    ): Notification {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val disconnectIntent = Intent(context, VpnActionReceiver::class.java).apply {
            action = ACTION_DISCONNECT
            setPackage(context.packageName)
        }
        val disconnectPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            disconnectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = when (state) {
            is VpnState.Connected -> "TRVPN — Connected & Protected"
            is VpnState.Connecting -> "TRVPN — Connecting..."
            is VpnState.Reconnecting -> "TRVPN — Reconnecting..."
            is VpnState.Disconnecting -> "TRVPN — Disconnecting..."
            else -> "TRVPN — Network Relay"
        }

        val serverInfo = if (server != null) {
            "${server.flagEmoji} ${server.countryLong} (${server.ip})"
        } else {
            "OpenVPN Relay"
        }

        val formattedDuration = String.format(
            "%02d:%02d:%02d",
            elapsedSeconds / 3600,
            (elapsedSeconds % 3600) / 60,
            elapsedSeconds % 60
        )

        val subText = if (state is VpnState.Connected) {
            val totalInKb = bytesIn / 1024
            val totalOutKb = bytesOut / 1024
            "$serverInfo • $formattedDuration • ↓${totalInKb}KB ↑${totalOutKb}KB"
        } else {
            serverInfo
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(subText)
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)

        if (state is VpnState.Connected || state is VpnState.Connecting || state is VpnState.Reconnecting) {
            builder.addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Disconnect",
                disconnectPendingIntent
            )
        }

        return builder.build()
    }
}
