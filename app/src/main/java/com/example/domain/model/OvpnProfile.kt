package com.example.domain.model

data class OvpnProfile(
    val remoteHost: String,
    val remotePort: Int,
    val proto: VpnProto,
    val caCertificate: String,
    val clientCertificate: String? = null,
    val clientKey: String? = null,
    val cipher: String? = null,
    val auth: String? = null,
    val hasAuthUserPass: Boolean = false,
    val rawConfig: String
) {
    val isValid: Boolean
        get() = remoteHost.isNotBlank() &&
                remotePort in 1..65535 &&
                caCertificate.contains("BEGIN CERTIFICATE")

    val summary: String
        get() = "$proto $remoteHost:$remotePort (${cipher ?: "AES"})"
}

enum class VpnProto {
    UDP,
    TCP
}
