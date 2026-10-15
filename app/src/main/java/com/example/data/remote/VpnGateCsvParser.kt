package com.example.data.remote

import android.util.Base64
import com.example.domain.model.OvpnProfile
import com.example.domain.model.VpnProto
import com.example.domain.model.VpnServer
import java.io.BufferedReader
import java.io.StringReader

object VpnGateCsvParser {

    fun parse(csvContent: String): List<VpnServer> {
        val servers = mutableListOf<VpnServer>()
        val seenIps = mutableSetOf<String>()

        val reader = BufferedReader(StringReader(csvContent))
        var line: String?
        var headers: List<String>? = null

        while (reader.readLine().also { line = it } != null) {
            val trimmed = line?.trim() ?: continue
            if (trimmed.isEmpty() || trimmed == "*" || trimmed == "*vpn_servers") {
                continue
            }

            if (trimmed.startsWith("#")) {
                headers = parseCsvRow(trimmed.removePrefix("#"))
                continue
            }

            if (headers == null) {
                continue
            }

            val values = parseCsvRow(trimmed)
            if (values.size < headers.size) {
                continue
            }

            val map = headers.zip(values).toMap()
            val hostName = map["HostName"]?.trim().orEmpty()
            val ip = map["IP"]?.trim().orEmpty()
            if (ip.isBlank() || seenIps.contains(ip)) {
                continue
            }

            val configBase64 = map["OpenVPN_ConfigData_Base64"]?.trim().orEmpty()
            if (configBase64.isBlank()) {
                continue
            }

            val profile = parseOvpnProfile(configBase64)
            if (profile == null || !profile.isValid) {
                continue
            }

            val server = VpnServer(
                id = "${ip}_${profile.remotePort}",
                hostName = hostName,
                ip = ip,
                score = map["Score"]?.toLongOrNull() ?: 0L,
                publishedPingMs = map["Ping"]?.toLongOrNull() ?: 999L,
                speedBps = map["Speed"]?.toLongOrNull() ?: 0L,
                countryLong = map["CountryLong"]?.trim() ?: "Unknown",
                countryShort = map["CountryShort"]?.trim() ?: "UN",
                numSessions = map["NumVpnSessions"]?.toLongOrNull() ?: 0L,
                uptimeSeconds = map["Uptime"]?.toLongOrNull() ?: 0L,
                totalUsers = map["TotalUsers"]?.toLongOrNull() ?: 0L,
                totalTrafficBytes = map["TotalTraffic"]?.toLongOrNull() ?: 0L,
                logType = map["LogType"]?.trim().orEmpty(),
                operator = map["Operator"]?.trim().orEmpty(),
                message = map["Message"]?.trim().orEmpty(),
                ovpnConfigBase64 = configBase64,
                profile = profile
            )

            seenIps.add(ip)
            servers.add(server)
        }

        return servers
    }

    fun parseOvpnProfile(base64Config: String): OvpnProfile? {
        val rawConfig = try {
            val clean = base64Config.replace("\n", "").replace("\r", "").trim()
            val bytes = java.util.Base64.getDecoder().decode(clean)
            String(bytes, Charsets.UTF_8)
        } catch (_: Throwable) {
            try {
                val bytes = Base64.decode(base64Config, Base64.DEFAULT)
                String(bytes, Charsets.UTF_8)
            } catch (_: Throwable) {
                return null
            }
        }

        var remoteHost = ""
        var remotePort = 1194
        var proto = VpnProto.UDP
        val caBuilder = StringBuilder()
        val certBuilder = StringBuilder()
        val keyBuilder = StringBuilder()
        var cipher: String? = null
        var auth: String? = null
        var hasAuthUserPass = false

        var insideCa = false
        var insideCert = false
        var insideKey = false

        rawConfig.lines().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#") || line.startsWith(";")) return@forEach

            when {
                line.startsWith("<ca>") -> { insideCa = true; return@forEach }
                line.startsWith("</ca>") -> { insideCa = false; return@forEach }
                line.startsWith("<cert>") -> { insideCert = true; return@forEach }
                line.startsWith("</cert>") -> { insideCert = false; return@forEach }
                line.startsWith("<key>") -> { insideKey = true; return@forEach }
                line.startsWith("</key>") -> { insideKey = false; return@forEach }
            }

            if (insideCa) {
                caBuilder.appendLine(line)
                return@forEach
            }
            if (insideCert) {
                certBuilder.appendLine(line)
                return@forEach
            }
            if (insideKey) {
                keyBuilder.appendLine(line)
                return@forEach
            }

            val tokens = line.split("\\s+".toRegex())
            if (tokens.isEmpty()) return@forEach

            when (tokens[0].lowercase()) {
                "remote" -> {
                    if (tokens.size >= 2 && remoteHost.isBlank()) {
                        remoteHost = tokens[1]
                    }
                    if (tokens.size >= 3) {
                        tokens[2].toIntOrNull()?.let { remotePort = it }
                    }
                }
                "proto" -> {
                    if (tokens.size >= 2) {
                        val p = tokens[1].lowercase()
                        proto = if (p.contains("tcp")) VpnProto.TCP else VpnProto.UDP
                    }
                }
                "port" -> {
                    if (tokens.size >= 2) {
                        tokens[1].toIntOrNull()?.let { remotePort = it }
                    }
                }
                "cipher" -> {
                    if (tokens.size >= 2) {
                        cipher = tokens[1]
                    }
                }
                "auth" -> {
                    if (tokens.size >= 2) {
                        auth = tokens[1]
                    }
                }
                "auth-user-pass" -> {
                    hasAuthUserPass = true
                }
            }
        }

        val caCert = caBuilder.toString().trim()
        if (remoteHost.isBlank() || caCert.isBlank()) {
            return null
        }

        return OvpnProfile(
            remoteHost = remoteHost,
            remotePort = remotePort,
            proto = proto,
            caCertificate = caCert,
            clientCertificate = certBuilder.toString().trim().ifBlank { null },
            clientKey = keyBuilder.toString().trim().ifBlank { null },
            cipher = cipher,
            auth = auth,
            hasAuthUserPass = hasAuthUserPass,
            rawConfig = rawConfig
        )
    }

    private fun parseCsvRow(line: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val c = line[i]
            when {
                c == '\"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                        sb.append('\"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == ',' && !inQuotes -> {
                    result.add(sb.toString())
                    sb.setLength(0)
                }
                else -> {
                    sb.append(c)
                }
            }
            i++
        }
        result.add(sb.toString())
        return result
    }
}
