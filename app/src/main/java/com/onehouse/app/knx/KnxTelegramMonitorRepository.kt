package com.onehouse.app.knx

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.Closeable
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.atomic.AtomicLong

/**
 * Historial ligero y persistente de comunicaciones KNX.
 *
 * El mismo flujo admite eventos salientes y, cuando se active el receptor
 * permanente del túnel, telegramas entrantes GroupValueWrite/Response.
 */
class KnxTelegramMonitorRepository(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun recent(limit: Int = MAX_EVENTS): List<KnxTelegramEvent> = synchronized(lock) {
        loadLocked().takeLast(limit.coerceIn(1, MAX_EVENTS)).reversed()
    }

    fun observe(observer: (List<KnxTelegramEvent>) -> Unit): Closeable {
        observers += observer
        observer(recent())
        return Closeable { observers -= observer }
    }

    fun record(
        direction: KnxTelegramEvent.Direction,
        kind: KnxTelegramEvent.Kind,
        groupAddress: String? = null,
        value: String? = null,
        status: KnxTelegramEvent.Status,
        detail: String? = null
    ): KnxTelegramEvent {
        val event = KnxTelegramEvent(
            id = idCounter.incrementAndGet(),
            timestampMillis = System.currentTimeMillis(),
            direction = direction,
            kind = kind,
            groupAddress = groupAddress,
            value = value,
            status = status,
            detail = detail
        )
        synchronized(lock) {
            val events = (loadLocked() + event).takeLast(MAX_EVENTS)
            saveLocked(events)
        }
        notifyObservers()
        return event
    }

    fun exportText(limit: Int = MAX_EVENTS): String {
        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", java.util.Locale.getDefault())
        return recent(limit).asReversed().joinToString("\n\n") { event ->
            buildString {
                append(formatter.format(java.util.Date(event.timestampMillis)))
                append(" | ").append(event.direction.name)
                append(" | ").append(event.kind.name)
                append(" | ").append(event.status.name)
                event.groupAddress?.let { append(" | GA ").append(it) }
                event.value?.let { append(" | valor ").append(it) }
                event.detail?.let { append("\n").append(it) }
            }
        }
    }

    fun clear() {
        synchronized(lock) { preferences.edit().remove(KEY_EVENTS).apply() }
        notifyObservers()
    }

    private fun loadLocked(): List<KnxTelegramEvent> {
        val raw = preferences.getString(KEY_EVENTS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        KnxTelegramEvent(
                            id = item.optLong("id"),
                            timestampMillis = item.optLong("time"),
                            direction = enumValueOrDefault(item.optString("direction"), KnxTelegramEvent.Direction.SYSTEM),
                            kind = enumValueOrDefault(item.optString("kind"), KnxTelegramEvent.Kind.CONNECT),
                            groupAddress = item.optString("address").ifBlank { null },
                            value = item.optString("value").ifBlank { null },
                            status = enumValueOrDefault(item.optString("status"), KnxTelegramEvent.Status.ERROR),
                            detail = item.optString("detail").ifBlank { null }
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun saveLocked(events: List<KnxTelegramEvent>) {
        val array = JSONArray()
        events.forEach { event ->
            array.put(JSONObject().apply {
                put("id", event.id)
                put("time", event.timestampMillis)
                put("direction", event.direction.name)
                put("kind", event.kind.name)
                put("address", event.groupAddress.orEmpty())
                put("value", event.value.orEmpty())
                put("status", event.status.name)
                put("detail", event.detail.orEmpty())
            })
        }
        preferences.edit().putString(KEY_EVENTS, array.toString()).apply()
    }

    private fun notifyObservers() {
        val value = recent()
        observers.forEach { it(value) }
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String, default: T): T =
        runCatching { enumValueOf<T>(value) }.getOrDefault(default)

    private companion object {
        const val PREFERENCES_NAME = "knx_telegram_monitor"
        const val KEY_EVENTS = "events"
        const val MAX_EVENTS = 60
        val lock = Any()
        val observers = CopyOnWriteArraySet<(List<KnxTelegramEvent>) -> Unit>()
        val idCounter = AtomicLong(System.currentTimeMillis())
    }
}
