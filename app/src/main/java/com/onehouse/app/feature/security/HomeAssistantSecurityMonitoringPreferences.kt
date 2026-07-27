package com.onehouse.app.feature.security

import android.content.Context

data class HomeAssistantSecurityMonitoringOptions(
    val armedEnabled: Boolean = false,
    val openingsEnabled: Boolean = true,
    val motionEnabled: Boolean = true,
    val cameraAutoRefreshEnabled: Boolean = true
)

class HomeAssistantSecurityMonitoringPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun read(): HomeAssistantSecurityMonitoringOptions = HomeAssistantSecurityMonitoringOptions(
        armedEnabled = preferences.getBoolean(KEY_ARMED, false),
        openingsEnabled = preferences.getBoolean(KEY_OPENINGS, true),
        motionEnabled = preferences.getBoolean(KEY_MOTION, true),
        cameraAutoRefreshEnabled = preferences.getBoolean(KEY_CAMERAS, true)
    )

    fun write(options: HomeAssistantSecurityMonitoringOptions) {
        preferences.edit()
            .putBoolean(KEY_ARMED, options.armedEnabled)
            .putBoolean(KEY_OPENINGS, options.openingsEnabled)
            .putBoolean(KEY_MOTION, options.motionEnabled)
            .putBoolean(KEY_CAMERAS, options.cameraAutoRefreshEnabled)
            .apply()
    }

    fun filterNotificationEvents(events: List<SecurityEvent>): List<SecurityEvent> {
        val options = read()
        if (!options.armedEnabled) return emptyList()
        return events.filter { event ->
            when (event.category) {
                SecurityEntityCategory.OPENING -> options.openingsEnabled
                SecurityEntityCategory.MOTION -> options.motionEnabled
            }
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "onehouse_security_monitoring"
        const val KEY_ARMED = "armed_enabled"
        const val KEY_OPENINGS = "openings_enabled"
        const val KEY_MOTION = "motion_enabled"
        const val KEY_CAMERAS = "camera_auto_refresh_enabled"
    }
}
