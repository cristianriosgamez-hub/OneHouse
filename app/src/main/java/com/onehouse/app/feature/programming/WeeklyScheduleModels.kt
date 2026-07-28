package com.onehouse.app.feature.programming

import java.time.DayOfWeek

enum class WeeklyTargetType(val displayName: String) {
    LIGHT("Luz"),
    CLIMATE("Climatización"),
    BLIND("Persiana")
}

enum class WeeklyActionType(val displayName: String) {
    ON("Encender"),
    OFF("Apagar"),
    CLIMATE_SETPOINT("Temperatura objetivo"),
    BLIND_UP("Subir"),
    BLIND_DOWN("Bajar"),
    BLIND_POSITION("Posición")
}

data class WeeklyScheduleEvent(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val dayOfWeek: DayOfWeek,
    val hour: Int,
    val minute: Int,
    val targetType: WeeklyTargetType,
    val actionType: WeeklyActionType,
    val groupAddress: String,
    val dpt: String,
    val value: String? = null,
    val enabled: Boolean = true
) {
    val minutesOfDay: Int get() = hour * 60 + minute
    fun formattedTime(): String = "%02d:%02d".format(hour, minute)
}

data class WeeklyScheduleState(
    val globallyEnabled: Boolean = false,
    val events: List<WeeklyScheduleEvent> = emptyList()
)
