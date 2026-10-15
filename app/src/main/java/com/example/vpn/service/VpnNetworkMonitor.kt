package com.example.vpn.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest

class VpnNetworkMonitor(
    private val context: Context,
    private val onNetworkChanged: (isAvailable: Boolean) -> Unit
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var isRegistered = false

    fun startMonitoring() {
        if (isRegistered || connectivityManager == null) return

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                onNetworkChanged(true)
            }

            override fun onLost(network: Network) {
                onNetworkChanged(false)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                onNetworkChanged(hasInternet)
            }
        }

        try {
            connectivityManager.registerNetworkCallback(request, networkCallback!!)
            isRegistered = true
        } catch (_: Exception) {
        }
    }

    fun stopMonitoring() {
        if (!isRegistered || connectivityManager == null || networkCallback == null) return
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback!!)
        } catch (_: Exception) {
        } finally {
            networkCallback = null
            isRegistered = false
        }
    }
}
