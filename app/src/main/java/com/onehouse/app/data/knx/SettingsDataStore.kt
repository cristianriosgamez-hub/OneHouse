package com.onehouse.app.data.knx

import android.content.Context

class SettingsDataStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun read(): KnxSettings = KnxSettings(
        localIp = preferences.getString(KEY_LOCAL_IP, "192.168.1.10").orEmpty(),
        localPort = preferences.getString(KEY_LOCAL_PORT, "3671").orEmpty(),
        remoteIp = preferences.getString(KEY_REMOTE_IP, "").orEmpty(),
        remotePort = preferences.getString(KEY_REMOTE_PORT, "3671").orEmpty(),
        autoReconnect = preferences.getBoolean(KEY_AUTO_RECONNECT, true),
        lastUpdatedEpochMillis = preferences.getLong(KEY_LAST_UPDATED, 0L)
    )

    fun write(settings: KnxSettings) {
        preferences.edit()
            .putString(KEY_LOCAL_IP, settings.localIp)
            .putString(KEY_LOCAL_PORT, settings.localPort)
            .putString(KEY_REMOTE_IP, settings.remoteIp)
            .putString(KEY_REMOTE_PORT, settings.remotePort)
            .putBoolean(KEY_AUTO_RECONNECT, settings.autoReconnect)
            .putLong(KEY_LAST_UPDATED, settings.lastUpdatedEpochMillis)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "onehouse_knx_settings"
        const val KEY_LOCAL_IP = "local_ip"
        const val KEY_LOCAL_PORT = "local_port"
        const val KEY_REMOTE_IP = "remote_ip"
        const val KEY_REMOTE_PORT = "remote_port"
        const val KEY_AUTO_RECONNECT = "auto_reconnect"
        const val KEY_LAST_UPDATED = "last_updated"
    }
}
