package com.example.vpn.service

import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.domain.model.VpnServer
import com.example.domain.model.VpnSessionStats
import com.example.domain.model.VpnState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object VpnServiceManager {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _vpnState = MutableStateFlow<VpnState>(VpnState.Disconnected)
    val vpnState: StateFlow<VpnState> = _vpnState.asStateFlow()

    private val _sessionStats = MutableStateFlow(VpnSessionStats())
    val sessionStats: StateFlow<VpnSessionStats> = _sessionStats.asStateFlow()

    private val _diagnosticsLogs = MutableStateFlow<List<String>>(emptyList())
    val diagnosticsLogs: StateFlow<List<String>> = _diagnosticsLogs.asStateFlow()

    private var durationJob: Job? = null
    private var activeServer: VpnServer? = null
    private var connectionStartTime: Long = 0L

    init {
        logDiagnostic("TRVPN core engine initialized.")
    }

    fun logDiagnostic(message: String) {
        val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
        val entry = "[$time] $message"
        val current = _diagnosticsLogs.value
        _diagnosticsLogs.value = (listOf(entry) + current).take(100)
    }

    fun connect(context: Context, server: VpnServer) {
        activeServer = server
        _vpnState.value = VpnState.Preparing(server)
        logDiagnostic("Preparing tunnel to ${server.countryLong} (${server.ip}:${server.profile?.remotePort ?: 1194})...")

        val intent = Intent(context, TrVpnService::class.java).apply {
            action = TrVpnService.ACTION_CONNECT
            putExtra(TrVpnService.EXTRA_SERVER_IP, server.ip)
            putExtra(TrVpnService.EXTRA_SERVER_HOST, server.hostName)
            putExtra(TrVpnService.EXTRA_SERVER_PORT, server.profile?.remotePort ?: 1194)
            putExtra(TrVpnService.EXTRA_SERVER_PROTO, server.profile?.proto?.name ?: "UDP")
            putExtra(TrVpnService.EXTRA_SERVER_COUNTRY, server.countryLong)
            putExtra(TrVpnService.EXTRA_SERVER_CONFIG, server.ovpnConfigBase64)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun disconnect(context: Context) {
        if (_vpnState.value is VpnState.Disconnected) return
        _vpnState.value = VpnState.Disconnecting
        logDiagnostic("Disconnecting VPN tunnel...")

        val intent = Intent(context, TrVpnService::class.java).apply {
            action = TrVpnService.ACTION_DISCONNECT
        }
        context.startService(intent)
    }

    fun onServiceStateChanged(newState: VpnState) {
        _vpnState.value = newState
        when (newState) {
            is VpnState.Connected -> {
                connectionStartTime = System.currentTimeMillis()
                startDurationTimer()
                logDiagnostic("VPN tunnel successfully established. Device traffic protected.")
            }
            is VpnState.Disconnected -> {
                stopDurationTimer()
                _sessionStats.value = VpnSessionStats()
                activeServer = null
                logDiagnostic("VPN tunnel disconnected.")
            }
            is VpnState.Failed -> {
                stopDurationTimer()
                logDiagnostic("Connection failure: ${newState.message}")
            }
            is VpnState.Connecting -> {
                logDiagnostic("Handshake in progress with ${newState.server.ip}...")
            }
            is VpnState.Reconnecting -> {
                logDiagnostic("Reconnecting to ${newState.server.ip} (Attempt ${newState.attempt}/${newState.maxAttempts})...")
            }
            else -> {}
        }
    }

    fun onTrafficUpdated(bytesIn: Long, bytesOut: Long) {
        val current = _sessionStats.value
        _sessionStats.value = current.copy(
            bytesIn = bytesIn,
            bytesOut = bytesOut
        )
    }

    private fun startDurationTimer() {
        durationJob?.cancel()
        durationJob = scope.launch {
            while (isActive) {
                val elapsed = (System.currentTimeMillis() - connectionStartTime) / 1000
                val current = _sessionStats.value
                _sessionStats.value = current.copy(durationSeconds = elapsed)
                delay(1000)
            }
        }
    }

    private fun stopDurationTimer() {
        durationJob?.cancel()
        durationJob = null
    }

    fun getActiveServer(): VpnServer? = activeServer
}
