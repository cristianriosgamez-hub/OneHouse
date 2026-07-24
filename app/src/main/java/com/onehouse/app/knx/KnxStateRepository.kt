package com.onehouse.app.knx

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.Closeable
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Caché persistente del último estado conocido de cada dirección de grupo KNX.
 *
 * En esta primera fase almacena valores DPT 1.x recibidos mediante
 * GroupValueWrite o GroupValueResponse. El modelo deja preparado el campo
 * [rawValue] para ampliar posteriormente la decodificación a otros DPT.
 */
class KnxStateRepository(context: Context) {

    data class State(
        val groupAddress: String,
        val booleanValue: Boolean?,
        val rawValue: String?,
        val sourceAddress: String,
        val telegramKind: KnxConnectionManager.IncomingGroupTelegram.Kind,
        val apci: String,
        val timestampMillis: Long
    )

    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    /** Devuelve el último estado conocido para una dirección de grupo. */
    fun get(groupAddress: String): State? = synchronized(lock) {
        loadLocked()[groupAddress]
    }

    /** Devuelve una instantánea de todos los estados conocidos. */
    fun snapshot(): Map<String, State> = synchronized(lock) {
        loadLocked().toMap()
    }

    /**
     * Registra un telegrama entrante.
     *
     * @return `true` cuando el estado se ha aceptado, o `false` cuando se ha
     * considerado un duplicado inmediato del telegrama anterior.
     */
    fun record(telegram: KnxConnectionManager.IncomingGroupTelegram): Boolean {
        val now = System.currentTimeMillis()
        val address = telegram.destination.toString()

        synchronized(lock) {
            val states = loadLocked().toMutableMap()
            val previous = states[address]
            val isDuplicate = previous != null &&
                previous.booleanValue == telegram.booleanValue &&
                previous.sourceAddress == telegram.sourceAddress &&
                previous.telegramKind == telegram.kind &&
                previous.apci == telegram.apci &&
                now - previous.timestampMillis in 0..DUPLICATE_WINDOW_MILLIS

            if (isDuplicate) return false

            states[address] = State(
                groupAddress = address,
                booleanValue = telegram.booleanValue,
                rawValue = telegram.booleanValue?.let { if (it) "1" else "0" },
                sourceAddress = telegram.sourceAddress,
                telegramKind = telegram.kind,
                apci = telegram.apci,
                timestampMillis = now
            )
            saveLocked(states)
        }

        notifyObservers()
        return true
    }

    /** Observa cambios de la caché y entrega inmediatamente su valor actual. */
    fun observe(observer: (Map<String, State>) -> Unit): Closeable {
        observers += observer
        observer(snapshot())
        return Closeable { observers -= observer }
    }

    fun clear() {
        synchronized(lock) {
            preferences.edit().remove(KEY_STATES).apply()
        }
        notifyObservers()
    }

    private fun loadLocked(): Map<String, State> {
        val raw = preferences.getString(KEY_STATES, null) ?: return emptyMap()
        return runCatching {
            val array = JSONArray(raw)
            buildMap {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    val address = item.optString("groupAddress")
                    if (address.isBlank()) continue
                    put(
                        address,
                        State(
                            groupAddress = address,
                            booleanValue = when {
                                item.isNull("booleanValue") -> null
                                else -> item.optBoolean("booleanValue")
                            },
                            rawValue = item.optString("rawValue").ifBlank { null },
                            sourceAddress = item.optString("sourceAddress"),
                            telegramKind = enumValueOrDefault(
                                item.optString("telegramKind"),
                                KnxConnectionManager.IncomingGroupTelegram.Kind.RESPONSE
                            ),
                            apci = item.optString("apci"),
                            timestampMillis = item.optLong("timestampMillis")
                        )
                    )
                }
            }
        }.getOrDefault(emptyMap())
    }

    private fun saveLocked(states: Map<String, State>) {
        val array = JSONArray()
        states.values
            .sortedBy { it.groupAddress }
            .forEach { state ->
                array.put(JSONObject().apply {
                    put("groupAddress", state.groupAddress)
                    if (state.booleanValue == null) {
                        put("booleanValue", JSONObject.NULL)
                    } else {
                        put("booleanValue", state.booleanValue)
                    }
                    put("rawValue", state.rawValue.orEmpty())
                    put("sourceAddress", state.sourceAddress)
                    put("telegramKind", state.telegramKind.name)
                    put("apci", state.apci)
                    put("timestampMillis", state.timestampMillis)
                })
            }
        preferences.edit().putString(KEY_STATES, array.toString()).apply()
    }

    private fun notifyObservers() {
        val value = snapshot()
        observers.forEach { observer -> observer(value) }
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String, default: T): T =
        runCatching { enumValueOf<T>(value) }.getOrDefault(default)

    private companion object {
        const val PREFERENCES_NAME = "knx_state_repository"
        const val KEY_STATES = "states"
        const val DUPLICATE_WINDOW_MILLIS = 175L
        val lock = Any()
        val observers = CopyOnWriteArraySet<(Map<String, State>) -> Unit>()
    }
}
