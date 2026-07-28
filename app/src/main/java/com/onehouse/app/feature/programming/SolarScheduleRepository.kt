package com.onehouse.app.feature.programming

import android.content.Context

interface SolarScheduleRepository {
    fun load(): SolarScheduleState
    fun save(state: SolarScheduleState)
}

class SharedPreferencesSolarScheduleRepository(context: Context) : SolarScheduleRepository {
    private val preferences = context.applicationContext.getSharedPreferences(
        "onehouse_solar_schedule",
        Context.MODE_PRIVATE
    )

    override fun load(): SolarScheduleState = SolarScheduleState(
        globallyEnabled = preferences.getBoolean("enabled", false),
        latitude = java.lang.Double.longBitsToDouble(
            preferences.getLong("latitude", java.lang.Double.doubleToRawLongBits(41.3597))
        ),
        longitude = java.lang.Double.longBitsToDouble(
            preferences.getLong("longitude", java.lang.Double.doubleToRawLongBits(2.1003))
        ),
        events = preferences.getStringSet("events", emptySet()).orEmpty()
            .mapNotNull(::decode)
            .sortedWith(compareBy({ it.trigger.ordinal }, { it.offsetMinutes }, { it.name }))
    )

    override fun save(state: SolarScheduleState) {
        preferences.edit()
            .putBoolean("enabled", state.globallyEnabled)
            .putLong("latitude", java.lang.Double.doubleToRawLongBits(state.latitude))
            .putLong("longitude", java.lang.Double.doubleToRawLongBits(state.longitude))
            .putStringSet("events", state.events.map(::encode).toSet())
            .apply()
    }

    private fun encode(event: SolarScheduleEvent): String = listOf(
        event.id,
        event.name,
        event.trigger.name,
        event.offsetMinutes,
        event.targetType.name,
        event.actionType.name,
        event.groupAddress,
        event.dpt,
        event.value.orEmpty(),
        event.enabled
    ).joinToString("|") { it.toString().replace("|", " ") }

    private fun decode(raw: String): SolarScheduleEvent? = runCatching {
        val p = raw.split('|')
        SolarScheduleEvent(
            id = p[0].toLong(),
            name = p[1],
            trigger = SolarTrigger.valueOf(p[2]),
            offsetMinutes = p[3].toInt(),
            targetType = WeeklyTargetType.valueOf(p[4]),
            actionType = WeeklyActionType.valueOf(p[5]),
            groupAddress = p[6],
            dpt = p[7],
            value = p[8].ifBlank { null },
            enabled = p[9].toBoolean()
        )
    }.getOrNull()
}
