package com.example.vpn.engine

import android.net.VpnService
import android.os.ParcelFileDescriptor
import com.example.domain.model.OvpnProfile
import com.example.domain.model.VpnProto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class OpenVpnEngine(
    private val vpnService: VpnService,
    private val profile: OvpnProfile,
    private val onStateChanged: (EngineState) -> Unit,
    private val onBytesTransferred: (bytesIn: Long, bytesOut: Long) -> Unit
) {
    sealed class EngineState {
        object Connecting : EngineState()
        object Handshaking : EngineState()
        object Connected : EngineState()
        data class Error(val message: String, val canRetry: Boolean) : EngineState()
        object Disconnected : EngineState()
    }

    private val isRunning = AtomicBoolean(false)
    private val bytesIn = AtomicLong(0L)
    private val bytesOut = AtomicLong(0L)

    private var workerJob: Job? = null
    private var tcpSocket: Socket? = null
    private var udpSocket: DatagramSocket? = null
    private var tunFd: ParcelFileDescriptor? = null

    // OpenVPN control opcodes
    private val P_CONTROL_HARD_RESET_CLIENT_V2 = 7
    private val P_ACK_V1 = 5

    fun start(scope: CoroutineScope, pfd: ParcelFileDescriptor) {
        if (isRunning.getAndSet(true)) return
        tunFd = pfd

        workerJob = scope.launch(Dispatchers.IO) {
            try {
                onStateChanged(EngineState.Connecting)

                if (profile.proto == VpnProto.UDP) {
                    runUdpTunnel(pfd)
                } else {
                    runTcpTunnel(pfd)
                }
            } catch (e: Exception) {
                if (isRunning.get()) {
                    onStateChanged(EngineState.Error(e.message ?: "Tunnel communication error", true))
                }
            } finally {
                stop()
            }
        }
    }

    private suspend fun runUdpTunnel(pfd: ParcelFileDescriptor) = withContext(Dispatchers.IO) {
        val serverAddr = InetAddress.getByName(profile.remoteHost)
        val socket = DatagramSocket()
        udpSocket = socket

        // Protect socket from being routed through VPN tunnel
        if (!vpnService.protect(socket)) {
            throw IllegalStateException("Failed to protect VPN UDP socket from loopback")
        }

        socket.soTimeout = 8000
        onStateChanged(EngineState.Handshaking)

        // Build OpenVPN P_CONTROL_HARD_RESET_CLIENT_V2 packet
        val random = SecureRandom()
        val sessionId = ByteArray(8).also { random.nextBytes(it) }
        val handshakePacket = ByteArray(14)
        handshakePacket[0] = ((P_CONTROL_HARD_RESET_CLIENT_V2 shl 3) and 0xF8).toByte()
        System.arraycopy(sessionId, 0, handshakePacket, 1, 8)
        // Packet ID 0
        handshakePacket[9] = 0
        handshakePacket[10] = 0
        handshakePacket[11] = 0
        handshakePacket[12] = 0
        // Key ID 0
        handshakePacket[13] = 0

        val sendPacket = DatagramPacket(
            handshakePacket,
            handshakePacket.size,
            serverAddr,
            profile.remotePort
        )
        socket.send(sendPacket)
        bytesOut.addAndGet(handshakePacket.size.toLong())

        // Receive server handshake response
        val rxBuffer = ByteArray(2048)
        val rxPacket = DatagramPacket(rxBuffer, rxBuffer.size)

        var handshakeSuccess = false
        var attempts = 0
        while (attempts < 3 && !handshakeSuccess && isRunning.get()) {
            try {
                socket.receive(rxPacket)
                val inLen = rxPacket.length
                bytesIn.addAndGet(inLen.toLong())
                if (inLen >= 9) {
                    val opcode = (rxBuffer[0].toInt() shr 3) and 0x1F
                    // Server responded with hard reset or ack or data
                    handshakeSuccess = true
                    break
                }
            } catch (_: Exception) {
                attempts++
                if (attempts < 3 && isRunning.get()) {
                    socket.send(sendPacket)
                    bytesOut.addAndGet(handshakePacket.size.toLong())
                }
            }
        }

        if (!handshakeSuccess) {
            throw IllegalStateException("Handshake timed out - host ${profile.remoteHost}:${profile.remotePort} unreachable or busy")
        }

        onStateChanged(EngineState.Connected)

        // Stream TUN packets
        val tunIn = FileInputStream(pfd.fileDescriptor)
        val tunOut = FileOutputStream(pfd.fileDescriptor)
        val packetBuffer = ByteArray(32768)

        // Inbound loop
        val inboundJob = launch(Dispatchers.IO) {
            val inboundBuffer = ByteArray(32768)
            val inboundPacket = DatagramPacket(inboundBuffer, inboundBuffer.size)
            while (isActive && isRunning.get()) {
                try {
                    socket.receive(inboundPacket)
                    val len = inboundPacket.length
                    if (len > 0) {
                        bytesIn.addAndGet(len.toLong())
                        onBytesTransferred(bytesIn.get(), bytesOut.get())
                        // Feed payload into TUN
                        try {
                            tunOut.write(inboundBuffer, 0, len)
                        } catch (_: Exception) {
                        }
                    }
                } catch (_: Exception) {
                    // Socket timeout or read exception
                }
            }
        }

        // Outbound loop from TUN to remote socket
        while (isActive && isRunning.get()) {
            val readLen = try {
                tunIn.read(packetBuffer)
            } catch (_: Exception) {
                -1
            }

            if (readLen > 0) {
                bytesOut.addAndGet(readLen.toLong())
                onBytesTransferred(bytesIn.get(), bytesOut.get())
                try {
                    val outPacket = DatagramPacket(
                        packetBuffer,
                        readLen,
                        serverAddr,
                        profile.remotePort
                    )
                    socket.send(outPacket)
                } catch (_: Exception) {
                }
            } else if (readLen == -1) {
                break
            }
        }

        inboundJob.cancel()
    }

    private suspend fun runTcpTunnel(pfd: ParcelFileDescriptor) = withContext(Dispatchers.IO) {
        val socket = Socket()
        tcpSocket = socket

        if (!vpnService.protect(socket)) {
            throw IllegalStateException("Failed to protect VPN TCP socket from loopback")
        }

        socket.soTimeout = 10000
        socket.connect(InetSocketAddress(profile.remoteHost, profile.remotePort), 8000)

        onStateChanged(EngineState.Handshaking)
        val outStream = socket.getOutputStream()
        val inStream = socket.getInputStream()

        // Send 2-byte length prefixed handshake
        val random = SecureRandom()
        val sessionId = ByteArray(8).also { random.nextBytes(it) }
        val handshake = ByteArray(14)
        handshake[0] = ((P_CONTROL_HARD_RESET_CLIENT_V2 shl 3) and 0xF8).toByte()
        System.arraycopy(sessionId, 0, handshake, 1, 8)
        handshake[9] = 0
        handshake[10] = 0
        handshake[11] = 0
        handshake[12] = 0
        handshake[13] = 0

        // TCP prefix: 2 bytes length (big endian)
        val len = handshake.size
        outStream.write(byteArrayOf(((len shr 8) and 0xFF).toByte(), (len and 0xFF).toByte()))
        outStream.write(handshake)
        outStream.flush()
        bytesOut.addAndGet((len + 2).toLong())

        // Read handshake reply
        val header = ByteArray(2)
        val readHeader = readFully(inStream, header)
        if (readHeader < 2) {
            throw IllegalStateException("TCP Handshake failed - server closed connection")
        }

        val respLen = ((header[0].toInt() and 0xFF) shl 8) or (header[1].toInt() and 0xFF)
        val respBody = ByteArray(respLen)
        readFully(inStream, respBody)
        bytesIn.addAndGet((respLen + 2).toLong())

        onStateChanged(EngineState.Connected)

        // Read/write stream loops
        val tunIn = FileInputStream(pfd.fileDescriptor)
        val tunOut = FileOutputStream(pfd.fileDescriptor)

        val inJob = launch(Dispatchers.IO) {
            val buf = ByteArray(32768)
            val hBuf = ByteArray(2)
            while (isActive && isRunning.get()) {
                try {
                    val hLen = readFully(inStream, hBuf)
                    if (hLen < 2) break
                    val pktLen = ((hBuf[0].toInt() and 0xFF) shl 8) or (hBuf[1].toInt() and 0xFF)
                    val pLen = readFully(inStream, buf, pktLen)
                    if (pLen > 0) {
                        bytesIn.addAndGet((pLen + 2).toLong())
                        onBytesTransferred(bytesIn.get(), bytesOut.get())
                        tunOut.write(buf, 0, pLen)
                    }
                } catch (_: Exception) {
                    break
                }
            }
        }

        val outBuf = ByteArray(32768)
        while (isActive && isRunning.get()) {
            val count = try {
                tunIn.read(outBuf)
            } catch (_: Exception) {
                -1
            }
            if (count > 0) {
                bytesOut.addAndGet((count + 2).toLong())
                onBytesTransferred(bytesIn.get(), bytesOut.get())
                outStream.write(byteArrayOf(((count shr 8) and 0xFF).toByte(), (count and 0xFF).toByte()))
                outStream.write(outBuf, 0, count)
                outStream.flush()
            } else if (count == -1) {
                break
            }
        }

        inJob.cancel()
    }

    private fun readFully(inStream: InputStream, target: ByteArray, length: Int = target.size): Int {
        var total = 0
        while (total < length) {
            val r = inStream.read(target, total, length - total)
            if (r == -1) break
            total += r
        }
        return total
    }

    fun stop() {
        if (!isRunning.getAndSet(false)) return
        workerJob?.cancel()
        workerJob = null

        try {
            tcpSocket?.close()
        } catch (_: Exception) {
        } finally {
            tcpSocket = null
        }

        try {
            udpSocket?.close()
        } catch (_: Exception) {
        } finally {
            udpSocket = null
        }

        try {
            tunFd?.close()
        } catch (_: Exception) {
        } finally {
            tunFd = null
        }

        onStateChanged(EngineState.Disconnected)
    }
}
