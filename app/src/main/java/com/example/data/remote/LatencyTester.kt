package com.example.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

object LatencyTester {

    private const val MAX_CONCURRENT_PINGS = 6
    private const val DEFAULT_SOCKET_TIMEOUT_MS = 2500

    /**
     * Measures TCP connection latency to server host/port in milliseconds.
     * Returns measured ping in ms, or null if unreachable or timed out.
     */
    suspend fun measureLatency(host: String, port: Int, timeoutMs: Int = DEFAULT_SOCKET_TIMEOUT_MS): Long? =
        withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            var socket: Socket? = null
            try {
                socket = Socket()
                socket.connect(InetSocketAddress(host, port), timeoutMs)
                val elapsed = System.currentTimeMillis() - startTime
                elapsed
            } catch (_: Exception) {
                null
            } finally {
                try {
                    socket?.close()
                } catch (_: Exception) {
                }
            }
        }

    /**
     * Performs bounded concurrent latency tests across a list of endpoints.
     */
    suspend fun measureMultiple(
        targets: List<Pair<String, Int>>,
        maxConcurrent: Int = MAX_CONCURRENT_PINGS
    ): Map<String, Long?> = coroutineScope {
        val semaphore = Semaphore(maxConcurrent)
        val deferredList = targets.map { (host, port) ->
            async {
                semaphore.withPermit {
                    val ping = measureLatency(host, port)
                    host to ping
                }
            }
        }
        deferredList.awaitAll().toMap()
    }
}
