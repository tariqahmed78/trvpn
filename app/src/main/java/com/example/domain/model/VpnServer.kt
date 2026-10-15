package com.example.domain.model

data class VpnServer(
    val id: String,
    val hostName: String,
    val ip: String,
    val score: Long,
    val publishedPingMs: Long,
    val speedBps: Long,
    val countryLong: String,
    val countryShort: String,
    val numSessions: Long,
    val uptimeSeconds: Long,
    val totalUsers: Long,
    val totalTrafficBytes: Long,
    val logType: String,
    val operator: String,
    val message: String,
    val ovpnConfigBase64: String,
    val measuredPingMs: Long? = null,
    val isFavorite: Boolean = false,
    val lastCheckedTimestamp: Long = System.currentTimeMillis(),
    val lastSuccessTimestamp: Long? = null,
    val profile: OvpnProfile? = null
) {
    val speedMbps: Double
        get() = (speedBps.toDouble() / 1_000_000.0)

    val bestPingMs: Long
        get() = measuredPingMs ?: (if (publishedPingMs > 0) publishedPingMs else 999L)

    val flagEmoji: String
        get() = countryCodeToEmoji(countryShort)

    val displayName: String
        get() = if (hostName.isNotBlank()) hostName.substringBefore(".") else ip

    companion object {
        fun countryCodeToEmoji(code: String): String {
            if (code.length != 2) return "🌐"
            val firstChar = Character.codePointAt(code.uppercase(), 0) - 0x41 + 0x1F1E6
            val secondChar = Character.codePointAt(code.uppercase(), 1) - 0x41 + 0x1F1E6
            return String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
        }
    }
}
