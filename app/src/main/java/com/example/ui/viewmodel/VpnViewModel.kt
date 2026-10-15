package com.example.ui.viewmodel

import android.content.Context
import android.net.VpnService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppPreferences
import com.example.data.repository.ServerRepository
import com.example.data.repository.VpnRepository
import com.example.domain.model.AppThemeMode
import com.example.domain.model.VpnServer
import com.example.domain.model.VpnSessionStats
import com.example.domain.model.VpnState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ServerFilter {
    ALL,
    FAST,
    FAVORITES
}

enum class ServerSort {
    LATENCY,
    SPEED,
    COUNTRY
}

class VpnViewModel(
    private val vpnRepository: VpnRepository,
    private val serverRepository: ServerRepository,
    private val preferences: AppPreferences
) : ViewModel() {

    val vpnState: StateFlow<VpnState> = vpnRepository.vpnState
    val sessionStats: StateFlow<VpnSessionStats> = vpnRepository.sessionStats
    val selectedServer: StateFlow<VpnServer?> = vpnRepository.selectedServer
    val isRefreshing: StateFlow<Boolean> = serverRepository.isRefreshing
    val lastUpdated: StateFlow<Long> = serverRepository.lastUpdated
    val diagnosticsLogs: StateFlow<List<String>> = vpnRepository.diagnosticsLogs

    val themeMode: StateFlow<AppThemeMode> = preferences.themeModeFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        AppThemeMode.DARK
    )

    val autoSelectFastest: StateFlow<Boolean> = preferences.autoSelectFastestFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        true
    )

    val reconnectOnNetworkChange: StateFlow<Boolean> = preferences.reconnectOnNetworkChangeFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        true
    )

    val privacyNoticeAccepted: StateFlow<Boolean> = preferences.privacyNoticeAcceptedFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        false
    )

    // Search and filtering state
    val searchQuery = MutableStateFlow("")
    val activeFilter = MutableStateFlow(ServerFilter.ALL)
    val activeSort = MutableStateFlow(ServerSort.LATENCY)

    val filteredServers: StateFlow<List<VpnServer>> = combine(
        serverRepository.servers,
        searchQuery,
        activeFilter,
        activeSort
    ) { allServers, query, filter, sort ->
        var list = allServers

        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            list = list.filter {
                it.countryLong.lowercase().contains(q) ||
                        it.countryShort.lowercase().contains(q) ||
                        it.ip.contains(q) ||
                        it.hostName.lowercase().contains(q)
            }
        }

        list = when (filter) {
            ServerFilter.ALL -> list
            ServerFilter.FAST -> list.filter { it.bestPingMs in 1..120 }
            ServerFilter.FAVORITES -> list.filter { it.isFavorite }
        }

        when (sort) {
            ServerSort.LATENCY -> list.sortedBy { it.bestPingMs }
            ServerSort.SPEED -> list.sortedByDescending { it.speedBps }
            ServerSort.COUNTRY -> list.sortedBy { it.countryLong }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onConnectToggled(
        context: Context,
        onRequirePrivacyDisclosure: () -> Unit,
        onRequireVpnPermission: () -> Unit
    ) {
        val currentState = vpnState.value

        if (currentState is VpnState.Connected || currentState.isConnectingOrReconnecting) {
            vpnRepository.disconnect(context)
            return
        }

        viewModelScope.launch {
            val isAccepted = preferences.privacyNoticeAcceptedFlow.first()
            if (!isAccepted) {
                onRequirePrivacyDisclosure()
                return@launch
            }

            val vpnIntent = VpnService.prepare(context)
            if (vpnIntent != null) {
                onRequireVpnPermission()
                return@launch
            }

            var target = selectedServer.value
            if (target == null || (target.measuredPingMs == null && autoSelectFastest.value)) {
                val fastest = serverRepository.autoSelectFastestServer()
                if (fastest != null) {
                    target = fastest
                    vpnRepository.setSelectedServer(fastest)
                }
            }

            target?.let { vpnRepository.connect(context, it) }
        }
    }

    fun onServerSelected(
        server: VpnServer,
        context: Context,
        onConfirmSwitch: (() -> Unit) -> Unit
    ) {
        val currentState = vpnState.value
        if (currentState is VpnState.Connected || currentState.isConnectingOrReconnecting) {
            onConfirmSwitch {
                vpnRepository.setSelectedServer(server)
                vpnRepository.connect(context, server)
            }
        } else {
            vpnRepository.setSelectedServer(server)
        }
    }

    fun onAutoSelectFastest(context: Context) {
        viewModelScope.launch {
            val fastest = serverRepository.autoSelectFastestServer()
            if (fastest != null) {
                vpnRepository.setSelectedServer(fastest)
            }
        }
    }

    fun refreshServers() {
        viewModelScope.launch {
            serverRepository.refreshServers()
        }
    }

    fun toggleFavorite(serverId: String) {
        viewModelScope.launch {
            serverRepository.toggleFavorite(serverId)
        }
    }

    fun acceptPrivacyNotice(onAccepted: () -> Unit) {
        viewModelScope.launch {
            preferences.setPrivacyNoticeAccepted(true)
            onAccepted()
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            preferences.setThemeMode(mode)
        }
    }

    fun setAutoSelectFastest(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setAutoSelectFastest(enabled)
        }
    }

    fun setReconnectOnNetworkChange(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setReconnectOnNetworkChange(enabled)
        }
    }

    fun clearCache() {
        serverRepository.clearCache()
    }

    fun resetPreferences() {
        viewModelScope.launch {
            preferences.resetAllPreferences()
        }
    }
}
