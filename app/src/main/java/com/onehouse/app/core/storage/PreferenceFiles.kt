package com.onehouse.app.core.storage

/**
 * Canonical SharedPreferences file names used by the active OneHouse features.
 */
object PreferenceFiles {
    const val KNX_SETTINGS = "onehouse_knx_settings"
    const val KNX_CONFIGURATION = "onehouse_knx_configuration"
    const val CLIMATE_SCHEDULE = "onehouse_climate_schedule"

    /** Únicamente datos activos de la versión actual de OneHouse. */
    val backupFiles: List<String> = listOf(
        KNX_SETTINGS,
        KNX_CONFIGURATION,
        CLIMATE_SCHEDULE
    )
}
