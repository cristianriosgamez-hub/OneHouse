package com.onehouse.app.core.storage

/**
 * Canonical SharedPreferences file names used by the active OneHouse features.
 */
object PreferenceFiles {
    const val KNX_SETTINGS = "onehouse_knx_settings"
    const val KNX_CONFIGURATION = "onehouse_knx_configuration"
    const val INSIDE_CONTROL_IMPORT = "insidecontrol_import"
    const val IMPORT_PROJECT_UI = "import_project_ui"
    const val CLIMATE_SCHEDULE = "onehouse_climate_schedule"

    /** Only active OneHouse data is exported. */
    val backupFiles: List<String> = listOf(
        KNX_SETTINGS,
        KNX_CONFIGURATION,
        CLIMATE_SCHEDULE
    )

    /**
     * Historical blocks accepted only so old backups can be opened safely.
     * They are never restored into the active application.
     */
    val retiredFiles: List<String> = listOf(
        INSIDE_CONTROL_IMPORT,
        IMPORT_PROJECT_UI,
        "onehouse_weekly_schedule",
        "onehouse_solar_schedule",
        "onehouse_conditional_automations",
        "onehouse_smart_scenes",
        "onehouse_home_assistant",
        "onehouse_security_monitoring",
        "onehouse_security_notifications"
    )
}
