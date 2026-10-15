package com.example.data.repository

import android.content.Context
import com.example.data.local.AppPreferences
import com.example.domain.model.VpnServer
import com.example.domain.model.VpnSessionStats
import com.example.domain.model.VpnState
import com.example.vpn.service.VpnServiceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class VpnRepository(
    private val preferences: AppPreferences,
    private val serverRepository: ServerRepository
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    val vpnState: StateFlow<VpnState> = VpnServiceManager.vpnState
    val sessionStats: StateFlow<VpnSessionStats> = VpnServiceManager.sessionStats
    val diagnosticsLogs: StateFlow<List<String>> = VpnServiceManager.diagnosticsLogs

    private val _selectedServer = MutableStateFlow<VpnServer?>(null)
    val selectedServer: StateFlow<VpnServer?> = _selectedServer.asStateFlow()

    init {
        scope.launch {
            // Restore last selected server or auto-select from repo
            val lastId = preferences.lastSelectedServerIdFlow.first()
            val servers = serverRepository.servers.value
            val match = servers.firstOrNull { it.id == lastId }
            if (match != null) {
                _selectedServer.value = match
            } else if (servers.isNotEmpty()) {
                _selectedServer.value = servers.first()
            }

            // Also keep listening to server list updates to ensure selectedServer has latest latency
            serverRepository.servers.collect { updatedList ->
                val current = _selectedServer.value
                if (current == null && updatedList.isNotEmpty()) {
                    _selectedServer.value = updatedList.first()
                } else if (current != null) {
                    val fresh = updatedList.firstOrNull { it.id == current.id }
                    if (fresh != null) {
                        _selectedServer.value = fresh
                    }
                }
            }
        }
    }

    fun setSelectedServer(server: VpnServer) {
        _selectedServer.value = server
        scope.launch {
            preferences.setLastSelectedServerId(server.id)
        }
    }

    fun connect(context: Context, server: VpnServer? = _selectedServer.value) {
        val target = server ?: _selectedServer.value ?: return
        VpnServiceManager.connect(context, target)
    }

    fun disconnect(context: Context) {
        VpnServiceManager.disconnect(context)
    }
}
