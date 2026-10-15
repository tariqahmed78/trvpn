package com.example.domain.model

sealed class VpnState {
    object Disconnected : VpnState()

    data class Preparing(
        val server: VpnServer
    ) : VpnState()

    data class Connecting(
        val server: VpnServer,
        val stage: String = "Initializing handshake..."
    ) : VpnState()

    data class Connected(
        val server: VpnServer,
        val connectedSince: Long = System.currentTimeMillis(),
        val bytesIn: Long = 0L,
        val bytesOut: Long = 0L,
        val latencyMs: Long? = null
    ) : VpnState()

    data class Reconnecting(
        val server: VpnServer,
        val attempt: Int = 1,
        val maxAttempts: Int = 3
    ) : VpnState()

    object Disconnecting : VpnState()

    data class Failed(
        val server: VpnServer?,
        val message: String,
        val canRetry: Boolean = true
    ) : VpnState()

    val isConnected: Boolean
        get() = this is Connected

    val isConnectingOrReconnecting: Boolean
        get() = this is Preparing || this is Connecting || this is Reconnecting

    val isTransitioning: Boolean
        get() = isConnectingOrReconnecting || this is Disconnecting

    val activeServer: VpnServer?
        get() = when (this) {
            is Preparing -> server
            is Connecting -> server
            is Connected -> server
            is Reconnecting -> server
            is Failed -> server
            else -> null
        }
}

data class VpnSessionStats(
    val durationSeconds: Long = 0L,
    val bytesIn: Long = 0L,
    val bytesOut: Long = 0L,
    val currentLatencyMs: Long? = null
)
