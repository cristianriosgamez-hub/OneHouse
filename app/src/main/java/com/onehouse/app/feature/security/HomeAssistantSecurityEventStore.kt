package com.onehouse.app.feature.security

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class SecurityEvent(
    val entityId: String,
    val name: String,
    val category: SecurityEntityCategory,
    val active: Boolean,
    val occurredAtEpochMillis: Long
)

data class SecurityEventProcessResult(
    val events: List<SecurityEvent>,
    val newEvents: List<SecurityEvent>
)

class HomeAssistantSecurityEventStore(context: Context) {
    private val preferences = context.getSharedPreferences("onehouse_security_events", Context.MODE_PRIVATE)

    fun process(snapshot: HomeAssistantSecuritySnapshot): SecurityEventProcessResult {
        val previous = readStates()
        val events = readEvents().toMutableList()
        val newEvents = mutableListOf<SecurityEvent>()
        val current = JSONObject()

        (snapshot.openings + snapshot.motions).forEach { entity ->
            if (!entity.available) return@forEach
            current.put(entity.entityId, entity.active)
            if (previous.has(entity.entityId)) {
                val wasActive = previous.optBoolean(entity.entityId, entity.active)
                if (wasActive != entity.active) {
                    val event = SecurityEvent(
                        entityId = entity.entityId,
                        name = entity.name,
                        category = entity.category,
                        active = entity.active,
                        occurredAtEpochMillis = System.currentTimeMillis()
                    )
                    events.add(0, event)
                    newEvents.add(event)
                }
            }
        }

        val limited = events.take(MAX_EVENTS)
        preferences.edit()
            .putString(KEY_STATES, current.toString())
            .putString(KEY_EVENTS, eventsToJson(limited).toString())
            .apply()
        return SecurityEventProcessResult(events = limited, newEvents = newEvents)
    }

    fun readEvents(): List<SecurityEvent> = runCatching {
        val array = JSONArray(preferences.getString(KEY_EVENTS, "[]") ?: "[]")
        buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val category = runCatching {
                    SecurityEntityCategory.valueOf(item.optString("category"))
                }.getOrNull() ?: continue
                add(
                    SecurityEvent(
                        entityId = item.optString("entityId"),
                        name = item.optString("name"),
                        category = category,
                        active = item.optBoolean("active"),
                        occurredAtEpochMillis = item.optLong("occurredAt")
                    )
                )
            }
        }
    }.getOrDefault(emptyList())

    fun clear() {
        preferences.edit().remove(KEY_EVENTS).apply()
    }

    private fun readStates(): JSONObject = runCatching {
        JSONObject(preferences.getString(KEY_STATES, "{}") ?: "{}")
    }.getOrDefault(JSONObject())

    private fun eventsToJson(events: List<SecurityEvent>) = JSONArray().apply {
        events.forEach { event ->
            put(
                JSONObject()
                    .put("entityId", event.entityId)
                    .put("name", event.name)
                    .put("category", event.category.name)
                    .put("active", event.active)
                    .put("occurredAt", event.occurredAtEpochMillis)
            )
        }
    }

    private companion object {
        const val KEY_STATES = "entity_states"
        const val KEY_EVENTS = "security_events"
        const val MAX_EVENTS = 50
    }
}
