package com.example.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class VpnGateApi(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) {
    private val primaryUrl = "https://www.vpngate.net/api/iphone/"
    private val fallbackUrl = "http://www.vpngate.net/api/iphone/"

    suspend fun fetchServerDirectory(): Result<String> = withContext(Dispatchers.IO) {
        // Try HTTPS first
        try {
            val response = executeRequest(primaryUrl)
            if (response.isNotBlank() && response.contains("*vpn_servers")) {
                return@withContext Result.success(response)
            }
        } catch (_: Exception) {
            // Fall back to HTTP mirror
        }

        try {
            val response = executeRequest(fallbackUrl)
            if (response.isNotBlank() && response.contains("*vpn_servers")) {
                return@withContext Result.success(response)
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }

        Result.failure(IllegalStateException("Invalid or empty response from VPN Gate directory"))
    }

    private fun executeRequest(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "TRVPN-Client/1.0 (Android)")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("HTTP ${response.code}: ${response.message}")
            }
            return response.body?.string().orEmpty()
        }
    }
}
