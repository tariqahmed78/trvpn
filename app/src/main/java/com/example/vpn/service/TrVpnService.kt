package com.example.vpn.service

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import com.example.data.remote.VpnGateCsvParser
import com.example.domain.model.OvpnProfile
import com.example.domain.model.VpnProto
import com.example.domain.model.VpnServer
import com.example.domain.model.VpnState
import com.example.vpn.engine.OpenVpnEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TrVpnService : VpnService() {

    companion object {
        const val ACTION_CONNECT = "com.trtech.trvpn.ACTION_CONNECT"
        const val ACTION_DISCONNECT = "com.trtech.trvpn.ACTION_DISCONNECT"

        const val EXTRA_SERVER_IP = "extra_server_ip"
        const val EXTRA_SERVER_HOST = "extra_server_host"
        const val EXTRA_SERVER_PORT = "extra_server_port"
        const val EXTRA_SERVER_PROTO = "extra_server_proto"
        const val EXTRA_SERVER_COUNTRY = "extra_server_country"
        const val EXTRA_SERVER_CONFIG = "extra_server_config"
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var vpnInterface: ParcelFileDescriptor? = null
    private var openVpnEngine: OpenVpnEngine? = null
    private var networkMonitor: VpnNetworkMonitor? = null
    private var activeServer: VpnServer? = null
    private var isConnected = false
    private var notificationJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        VpnNotificationHelper.createNotificationChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        when (action) {
            ACTION_CONNECT -> {
                val ip = intent.getStringExtra(EXTRA_SERVER_IP).orEmpty()
                val host = intent.getStringExtra(EXTRA_SERVER_HOST).orEmpty()
                val port = intent.getIntExtra(EXTRA_SERVER_PORT, 1194)
                val protoStr = intent.getStringExtra(EXTRA_SERVER_PROTO) ?: "UDP"
                val country = intent.getStringExtra(EXTRA_SERVER_COUNTRY) ?: "Remote"
                val configBase64 = intent.getStringExtra(EXTRA_SERVER_CONFIG).orEmpty()

                val profile = VpnGateCsvParser.parseOvpnProfile(configBase64)
                    ?: OvpnProfile(
                        remoteHost = if (host.isNotBlank()) host else ip,
                        remotePort = port,
                        proto = if (protoStr.contains("TCP", true)) VpnProto.TCP else VpnProto.UDP,
                        caCertificate = "-----BEGIN CERTIFICATE-----\n...\n-----END CERTIFICATE-----",
                        rawConfig = ""
                    )

                val server = VpnServer(
                    id = "${ip}_$port",
                    hostName = host,
                    ip = ip,
                    score = 1000,
                    publishedPingMs = 50,
                    speedBps = 10_000_000,
                    countryLong = country,
                    countryShort = "UN",
                    numSessions = 10,
                    uptimeSeconds = 10000,
                    totalUsers = 1000,
                    totalTrafficBytes = 10000000,
                    logType = "2wk",
                    operator = "Volunteer",
                    message = "",
                    ovpnConfigBase64 = configBase64,
                    profile = profile
                )

                startVpnTunnel(server, profile)
            }

            ACTION_DISCONNECT -> {
                disconnectTunnel()
            }
        }

        return START_NOT_STICKY
    }

    private fun startVpnTunnel(server: VpnServer, profile: OvpnProfile) {
        activeServer = server
        isConnected = false

        // Start in foreground immediately as required by Android
        val initialNotification = VpnNotificationHelper.buildNotification(
            context = this,
            state = VpnState.Connecting(server),
            server = server
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                VpnNotificationHelper.NOTIFICATION_ID,
                initialNotification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(VpnNotificationHelper.NOTIFICATION_ID, initialNotification)
        }
        VpnServiceManager.onServiceStateChanged(VpnState.Connecting(server, "Configuring TUN interface..."))

        serviceScope.launch(Dispatchers.IO) {
            try {
                // Build the Android virtual TUN interface
                val builder = Builder()
                    .setSession("TRVPN - ${server.countryLong}")
                    .setMtu(1500)
                    .addAddress("10.8.0.2", 24)
                    .addRoute("0.0.0.0", 0)
                    .addDnsServer("1.1.1.1")
                    .addDnsServer("8.8.8.8")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    builder.setMetered(false)
                }

                val pfd = builder.establish()
                if (pfd == null) {
                    throw IllegalStateException("Failed to establish VPN interface. Check device VPN restrictions.")
                }
                vpnInterface = pfd

                // Initialize OpenVPN Protocol Engine
                openVpnEngine = OpenVpnEngine(
                    vpnService = this@TrVpnService,
                    profile = profile,
                    onStateChanged = { engineState ->
                        handleEngineState(engineState, server)
                    },
                    onBytesTransferred = { bytesIn, bytesOut ->
                        VpnServiceManager.onTrafficUpdated(bytesIn, bytesOut)
                    }
                )

                // Start network monitor for auto-recovery
                setupNetworkMonitor(server)

                // Launch tunnel engine I/O
                openVpnEngine?.start(serviceScope, pfd)

            } catch (e: Exception) {
                VpnServiceManager.onServiceStateChanged(
                    VpnState.Failed(server, e.message ?: "Tunnel setup failed", true)
                )
                disconnectTunnel()
            }
        }
    }

    private fun handleEngineState(engineState: OpenVpnEngine.EngineState, server: VpnServer) {
        serviceScope.launch(Dispatchers.Main) {
            when (engineState) {
                is OpenVpnEngine.EngineState.Connecting -> {
                    VpnServiceManager.onServiceStateChanged(VpnState.Connecting(server, "Opening transport socket..."))
                }
                is OpenVpnEngine.EngineState.Handshaking -> {
                    VpnServiceManager.onServiceStateChanged(VpnState.Connecting(server, "OpenVPN TLS Handshake in progress..."))
                }
                is OpenVpnEngine.EngineState.Connected -> {
                    isConnected = true
                    val connectedState = VpnState.Connected(server)
                    VpnServiceManager.onServiceStateChanged(connectedState)
                    startNotificationUpdateLoop(server)
                }
                is OpenVpnEngine.EngineState.Error -> {
                    VpnServiceManager.onServiceStateChanged(VpnState.Failed(server, engineState.message, engineState.canRetry))
                    disconnectTunnel()
                }
                is OpenVpnEngine.EngineState.Disconnected -> {
                    if (isConnected) {
                        disconnectTunnel()
                    }
                }
            }
        }
    }

    private fun setupNetworkMonitor(server: VpnServer) {
        networkMonitor?.stopMonitoring()
        networkMonitor = VpnNetworkMonitor(this) { isAvailable ->
            if (!isAvailable && isConnected) {
                VpnServiceManager.logDiagnostic("Underlying physical network lost! Suspending tunnel...")
                VpnServiceManager.onServiceStateChanged(VpnState.Reconnecting(server, 1, 3))
            } else if (isAvailable && isConnected) {
                VpnServiceManager.logDiagnostic("Network restored. Tunnel communication resumed.")
            }
        }
        networkMonitor?.startMonitoring()
    }

    private fun startNotificationUpdateLoop(server: VpnServer) {
        notificationJob?.cancel()
        notificationJob = serviceScope.launch(Dispatchers.Main) {
            val startTime = System.currentTimeMillis()
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            while (isActive && isConnected) {
                val elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000
                val stats = VpnServiceManager.sessionStats.value
                val notif = VpnNotificationHelper.buildNotification(
                    context = this@TrVpnService,
                    state = VpnState.Connected(server),
                    server = server,
                    elapsedSeconds = elapsedSeconds,
                    bytesIn = stats.bytesIn,
                    bytesOut = stats.bytesOut
                )
                manager?.notify(VpnNotificationHelper.NOTIFICATION_ID, notif)
                delay(2000)
            }
        }
    }

    private fun disconnectTunnel() {
        notificationJob?.cancel()
        notificationJob = null
        isConnected = false

        networkMonitor?.stopMonitoring()
        networkMonitor = null

        openVpnEngine?.stop()
        openVpnEngine = null

        try {
            vpnInterface?.close()
        } catch (_: Exception) {
        } finally {
            vpnInterface = null
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        VpnServiceManager.onServiceStateChanged(VpnState.Disconnected)
        stopSelf()
    }

    override fun onDestroy() {
        disconnectTunnel()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onRevoke() {
        VpnServiceManager.logDiagnostic("VPN permission was revoked by the system.")
        disconnectTunnel()
        super.onRevoke()
    }
}
