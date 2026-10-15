package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.AppThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "trvpn_preferences")

class AppPreferences(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val AUTO_SELECT_FASTEST = booleanPreferencesKey("auto_select_fastest")
        val RECONNECT_ON_NETWORK_CHANGE = booleanPreferencesKey("reconnect_on_network_change")
        val AUTO_CONNECT_UNTRUSTED = booleanPreferencesKey("auto_connect_untrusted")
        val SHOW_PERSISTENT_NOTIFICATION = booleanPreferencesKey("show_persistent_notification")
        val PRIVACY_NOTICE_ACCEPTED = booleanPreferencesKey("privacy_notice_accepted")
        val FAVORITE_SERVER_IDS = stringSetPreferencesKey("favorite_server_ids")
        val LAST_SELECTED_SERVER_ID = stringPreferencesKey("last_selected_server_id")
        val KILL_SWITCH_APP_RECOVERY = booleanPreferencesKey("kill_switch_app_recovery")
    }

    val themeModeFlow: Flow<AppThemeMode> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.THEME_MODE] ?: AppThemeMode.DARK.name
        try {
            AppThemeMode.valueOf(raw)
        } catch (_: Exception) {
            AppThemeMode.DARK
        }
    }

    val autoSelectFastestFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.AUTO_SELECT_FASTEST] ?: true
    }

    val reconnectOnNetworkChangeFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.RECONNECT_ON_NETWORK_CHANGE] ?: true
    }

    val autoConnectUntrustedFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.AUTO_CONNECT_UNTRUSTED] ?: false
    }

    val showPersistentNotificationFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.SHOW_PERSISTENT_NOTIFICATION] ?: true
    }

    val privacyNoticeAcceptedFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.PRIVACY_NOTICE_ACCEPTED] ?: false
    }

    val favoriteServerIdsFlow: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[Keys.FAVORITE_SERVER_IDS] ?: emptySet()
    }

    val lastSelectedServerIdFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.LAST_SELECTED_SERVER_ID]
    }

    val killSwitchAppRecoveryFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.KILL_SWITCH_APP_RECOVERY] ?: true
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.name
        }
    }

    suspend fun setAutoSelectFastest(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTO_SELECT_FASTEST] = enabled
        }
    }

    suspend fun setReconnectOnNetworkChange(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.RECONNECT_ON_NETWORK_CHANGE] = enabled
        }
    }

    suspend fun setAutoConnectUntrusted(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTO_CONNECT_UNTRUSTED] = enabled
        }
    }

    suspend fun setShowPersistentNotification(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SHOW_PERSISTENT_NOTIFICATION] = enabled
        }
    }

    suspend fun setPrivacyNoticeAccepted(accepted: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PRIVACY_NOTICE_ACCEPTED] = accepted
        }
    }

    suspend fun toggleFavorite(serverId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.FAVORITE_SERVER_IDS] ?: emptySet()
            if (current.contains(serverId)) {
                prefs[Keys.FAVORITE_SERVER_IDS] = current - serverId
            } else {
                prefs[Keys.FAVORITE_SERVER_IDS] = current + serverId
            }
        }
    }

    suspend fun setLastSelectedServerId(serverId: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_SELECTED_SERVER_ID] = serverId
        }
    }

    suspend fun setKillSwitchAppRecovery(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.KILL_SWITCH_APP_RECOVERY] = enabled
        }
    }

    suspend fun resetAllPreferences() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
