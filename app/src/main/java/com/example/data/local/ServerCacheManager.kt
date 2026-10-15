package com.example.data.local

import android.content.Context
import java.io.File

class ServerCacheManager(private val context: Context) {

    private val cacheFile: File
        get() = File(context.filesDir, "vpngate_cache.csv")

    private val timestampFile: File
        get() = File(context.filesDir, "vpngate_cache_timestamp.txt")

    fun saveRawCsv(csv: String) {
        try {
            cacheFile.writeText(csv, Charsets.UTF_8)
            timestampFile.writeText(System.currentTimeMillis().toString(), Charsets.UTF_8)
        } catch (_: Exception) {
        }
    }

    fun getRawCsv(): String? {
        return try {
            if (cacheFile.exists() && cacheFile.length() > 0) {
                cacheFile.readText(Charsets.UTF_8)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun getLastUpdatedTimestamp(): Long {
        return try {
            if (timestampFile.exists()) {
                timestampFile.readText().trim().toLongOrNull() ?: 0L
            } else {
                0L
            }
        } catch (_: Exception) {
            0L
        }
    }

    fun clearCache() {
        try {
            if (cacheFile.exists()) cacheFile.delete()
            if (timestampFile.exists()) timestampFile.delete()
        } catch (_: Exception) {
        }
    }
}
