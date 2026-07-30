package com.onehouse.app.feature.programming

import android.content.Context
import com.onehouse.app.core.storage.PreferenceFiles
import java.time.DayOfWeek

interface WeeklyScheduleRepository {
    fun load(): WeeklyScheduleState
    fun save(state: WeeklyScheduleState)
}

class SharedPreferencesWeeklyScheduleRepository(context: Context) : WeeklyScheduleRepository {
    private val preferences = context.applicationContext.getSharedPreferences(
        PreferenceFiles.WEEKLY_SCHEDULE,
        Context.MODE_PRIVATE
    )

    override fun load(): WeeklyScheduleState {
        val events = preferences.getStringSet("events", emptySet()).orEmpty()
            .mapNotNull(::decode)
            .sortedWith(compareBy({ it.dayOfWeek.value }, { it.hour }, { it.minute }))
        return WeeklyScheduleState(
            globallyEnabled = preferences.getBoolean("enabled", false),
            events = events
        )
    }

    override fun save(state: WeeklyScheduleState) {
        preferences.edit()
            .putBoolean("enabled", state.globallyEnabled)
            .putStringSet("events", state.events.map(::encode).toSet())
            .apply()
    }

    private fun encode(event: WeeklyScheduleEvent): String = listOf(
        event.id, event.name, event.dayOfWeek.value, event.hour, event.minute,
        event.targetType.name, event.actionType.name, event.groupAddress,
        event.dpt, event.value.orEmpty(), event.enabled
    ).joinToString("|") { it.toString().replace("|", " ") }

    private fun decode(raw: String): WeeklyScheduleEvent? = runCatching {
        val p = raw.split('|')
        WeeklyScheduleEvent(
            id = p[0].toLong(),
            name = p[1],
            dayOfWeek = DayOfWeek.of(p[2].toInt()),
            hour = p[3].toInt(),
            minute = p[4].toInt(),
            targetType = WeeklyTargetType.valueOf(p[5]),
            actionType = WeeklyActionType.valueOf(p[6]),
            groupAddress = p[7],
            dpt = p[8],
            value = p[9].ifBlank { null },
            enabled = p[10].toBoolean()
        )
    }.getOrNull()
}
