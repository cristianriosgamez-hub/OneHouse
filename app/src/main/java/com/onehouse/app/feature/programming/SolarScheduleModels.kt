package com.onehouse.app.feature.programming

enum class SolarTrigger(val displayName: String) {
    SUNRISE("Amanecer"),
    SUNSET("Atardecer")
}

data class SolarScheduleEvent(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val trigger: SolarTrigger,
    val offsetMinutes: Int = 0,
    val targetType: WeeklyTargetType,
    val actionType: WeeklyActionType,
    val groupAddress: String,
    val dpt: String,
    val value: String? = null,
    val enabled: Boolean = true
)

data class SolarScheduleState(
    val globallyEnabled: Boolean = false,
    val latitude: Double = 41.3597,
    val longitude: Double = 2.1003,
    val events: List<SolarScheduleEvent> = emptyList()
)
