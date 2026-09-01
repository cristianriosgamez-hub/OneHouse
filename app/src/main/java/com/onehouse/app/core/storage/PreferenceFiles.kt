package com.onehouse.app.core.storage

/**
 * Canonical SharedPreferences file names used by OneHouse.
 *
 * Keeping them in one place prevents repositories and backup/restore logic from
 * silently diverging when a preference file is renamed or added.
 */
object PreferenceFiles {
    const val KNX_SETTINGS = "onehouse_knx_settings"
    const val KNX_CONFIGURATION = "onehouse_knx_configuration"
    const val INSIDE_CONTROL_IMPORT = "insidecontrol_import"
    const val IMPORT_PROJECT_UI = "import_project_ui"
    const val CLIMATE_SCHEDULE = "onehouse_climate_schedule"
    const val WEEKLY_SCHEDULE = "onehouse_weekly_schedule"
    const val SOLAR_SCHEDULE = "onehouse_solar_schedule"
    const val CONDITIONAL_AUTOMATIONS = "onehouse_conditional_automations"
    const val SMART_SCENES = "onehouse_smart_scenes"
    const val HOME_ASSISTANT = "onehouse_home_assistant"
    const val SECURITY_MONITORING = "onehouse_security_monitoring"
    const val SECURITY_NOTIFICATIONS = "onehouse_security_notifications"

    /**
     * Ficheros que pertenecen realmente a OneHouse y pueden viajar en una copia.
     *
     * INSIDE_CONTROL_IMPORT e IMPORT_PROJECT_UI son únicamente fuentes/histórico
     * del importador antiguo. Mantenerlos fuera evita reintroducir objetos KNX que
     * OneHouse ya ha eliminado de su modelo activo.
     */
    val backupFiles: List<String> = listOf(
        KNX_SETTINGS,
        KNX_CONFIGURATION,
        CLIMATE_SCHEDULE,
        WEEKLY_SCHEDULE,
        SOLAR_SCHEDULE,
        CONDITIONAL_AUTOMATIONS,
        SMART_SCENES,
        HOME_ASSISTANT,
        SECURITY_MONITORING,
        SECURITY_NOTIFICATIONS
    )

    val legacyImportFiles: List<String> = listOf(
        INSIDE_CONTROL_IMPORT,
        IMPORT_PROJECT_UI
    )
}
