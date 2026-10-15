package com.example.data.repository

import com.example.data.local.AppPreferences
import com.example.data.local.ServerCacheManager
import com.example.data.remote.LatencyTester
import com.example.data.remote.SeedServers
import com.example.data.remote.VpnGateApi
import com.example.data.remote.VpnGateCsvParser
import com.example.domain.model.VpnServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ServerRepository(
    private val api: VpnGateApi,
    private val cacheManager: ServerCacheManager,
    private val preferences: AppPreferences
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _servers = MutableStateFlow<List<VpnServer>>(emptyList())
    val servers: StateFlow<List<VpnServer>> = _servers.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _lastUpdated = MutableStateFlow(cacheManager.getLastUpdatedTimestamp())
    val lastUpdated: StateFlow<Long> = _lastUpdated.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        scope.launch {
            val cachedCsv = cacheManager.getRawCsv()
            val initialCsv = if (!cachedCsv.isNullOrBlank()) {
                cachedCsv
            } else {
                SeedServers.BUNDLED_SEED_CSV
            }

            val parsed = VpnGateCsvParser.parse(initialCsv)
            val favorites = preferences.favoriteServerIdsFlow.first()
            val enriched = parsed.map { it.copy(isFavorite = favorites.contains(it.id)) }
            _servers.value = enriched

            // Auto-refresh in background if cache is older than 2 hours or using seed
            val lastUpdate = cacheManager.getLastUpdatedTimestamp()
            if (System.currentTimeMillis() - lastUpdate > 2 * 60 * 60 * 1000L) {
                refreshServers()
            }
        }
    }

    suspend fun refreshServers(): Result<Int> = withContext(Dispatchers.IO) {
        _isRefreshing.value = true
        _error.value = null
        try {
            val result = api.fetchServerDirectory()
            if (result.isSuccess) {
                val csv = result.getOrThrow()
                val parsed = VpnGateCsvParser.parse(csv)
                if (parsed.isNotEmpty()) {
                    cacheManager.saveRawCsv(csv)
                    val favorites = preferences.favoriteServerIdsFlow.first()
                    val enriched = parsed.map { it.copy(isFavorite = favorites.contains(it.id)) }
                    _servers.value = enriched
                    _lastUpdated.value = System.currentTimeMillis()

                    // Test latency on top 5 candidates asynchronously
                    testTopServersLatency(enriched.take(5))

                    return@withContext Result.success(parsed.size)
                }
            }

            // Fallback: retain cached data if refresh failed
            val errMsg = result.exceptionOrNull()?.message ?: "Unable to refresh servers"
            _error.value = errMsg
            Result.failure(result.exceptionOrNull() ?: Exception(errMsg))
        } catch (e: Exception) {
            _error.value = e.message
            Result.failure(e)
        } finally {
            _isRefreshing.value = false
        }
    }

    suspend fun testServerLatency(server: VpnServer): Long? = withContext(Dispatchers.IO) {
        val port = server.profile?.remotePort ?: 1194
        val ping = LatencyTester.measureLatency(server.ip, port)
        if (ping != null) {
            updateServerLatency(server.id, ping)
        }
        ping
    }

    private suspend fun testTopServersLatency(candidates: List<VpnServer>) {
        val targets = candidates.map { it.ip to (it.profile?.remotePort ?: 1194) }
        val results = LatencyTester.measureMultiple(targets)
        _servers.value = _servers.value.map { server ->
            val measured = results[server.ip]
            if (measured != null) server.copy(measuredPingMs = measured) else server
        }
    }

    private fun updateServerLatency(serverId: String, pingMs: Long) {
        _servers.value = _servers.value.map {
            if (it.id == serverId) it.copy(measuredPingMs = pingMs) else it
        }
    }

    suspend fun toggleFavorite(serverId: String) {
        preferences.toggleFavorite(serverId)
        val favorites = preferences.favoriteServerIdsFlow.first()
        _servers.value = _servers.value.map {
            it.copy(isFavorite = favorites.contains(it.id))
        }
    }

    fun clearCache() {
        cacheManager.clearCache()
        _lastUpdated.value = 0L
        _servers.value = VpnGateCsvParser.parse(SeedServers.BUNDLED_SEED_CSV)
    }

    /**
     * Auto-selects the fastest and most reliable server candidate:
     * 1. Filters servers with valid profiles.
     * 2. Considers published score, ping, and speed.
     * 3. Probes top candidate with live socket test.
     */
    suspend fun autoSelectFastestServer(): VpnServer? = withContext(Dispatchers.IO) {
        val list = _servers.value
        if (list.isEmpty()) return@withContext null

        val sorted = list.sortedWith(
            compareBy<VpnServer> { it.bestPingMs }
                .thenByDescending { it.score }
                .thenByDescending { it.speedBps }
        )

        // Probe up to top 3 candidates for real TCP reachability
        for (candidate in sorted.take(3)) {
            val ping = testServerLatency(candidate)
            if (ping != null && ping < 800) {
                return@withContext candidate.copy(measuredPingMs = ping)
            }
        }

        // Return best sorted if all probes timed out
        sorted.firstOrNull()
    }
}
