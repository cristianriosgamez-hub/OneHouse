package com.onehouse.app.knx

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.io.Closeable
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Caché persistente del último estado conocido de cada dirección de grupo KNX.
 *
 * Almacena valores DPT 1.x recibidos mediante GroupValueWrite o
 * GroupValueResponse y publica los cambios en tiempo real mediante [StateFlow].
 *
 * El campo [State.rawValue] queda preparado para ampliar posteriormente la
 * decodificación a otros DPT.
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

    /**
     * Flujo compartido con la instantánea completa del estado KNX.
     *
     * Es compartido entre todas las instancias del repositorio para que una
     * pantalla y el gestor de conexión observen exactamente la misma caché.
     */
    val stateFlow: StateFlow<Map<String, State>>
        get() = sharedStateFlow.asStateFlow()

    init {
        synchronized(lock) {
            if (!sharedStateInitialized) {
                sharedStateFlow.value = loadLocked()
                sharedStateInitialized = true
            }
        }
    }

    /** Devuelve el último estado conocido para una dirección de grupo. */
    fun get(groupAddress: String): State? =
        sharedStateFlow.value[groupAddress] ?: synchronized(lock) {
            loadLocked()[groupAddress]
        }

    /** Devuelve una instantánea de todos los estados conocidos. */
    fun snapshot(): Map<String, State> = sharedStateFlow.value.toMap()

    /**
     * Observa en tiempo real el estado de una dirección de grupo concreta.
     *
     * El flujo emite inmediatamente el último valor conocido y vuelve a emitir
     * cuando cambia el estado asociado a [groupAddress].
     */
    fun observeState(groupAddress: String): Flow<State?> =
        stateFlow
            .map { states -> states[groupAddress] }
            .distinctUntilChanged()

    /**
     * Registra un telegrama entrante.
     *
     * @return `true` cuando el estado se ha aceptado, o `false` cuando se ha
     * considerado un duplicado inmediato del telegrama anterior.
     */
    fun record(telegram: KnxConnectionManager.IncomingGroupTelegram): Boolean {
        val now = System.currentTimeMillis()
        val address = telegram.destination.toString()
        val updatedStates: Map<String, State>

        synchronized(lock) {
            val states = sharedStateFlow.value.toMutableMap()
            val previous = states[address]

            val isDuplicate = previous != null &&
                previous.booleanValue == telegram.booleanValue &&
                previous.sourceAddress == telegram.sourceAddress &&
                previous.telegramKind == telegram.kind &&
                previous.apci == telegram.apci &&
                now - previous.timestampMillis in 0..DUPLICATE_WINDOW_MILLIS

            if (isDuplicate) {
                return false
            }

            states[address] = State(
                groupAddress = address,
                booleanValue = telegram.booleanValue,
                rawValue = when {
                    telegram.payload.isNotEmpty() -> telegram.payload.joinToString("") { byte ->
                        "%02X".format(byte.toInt() and 0xFF)
                    }
                    telegram.booleanValue != null -> if (telegram.booleanValue) "1" else "0"
                    else -> null
                },
                sourceAddress = telegram.sourceAddress,
                telegramKind = telegram.kind,
                apci = telegram.apci,
                timestampMillis = now
            )

            updatedStates = states.toMap()
            saveLocked(updatedStates)
            sharedStateFlow.value = updatedStates
        }

        notifyObservers(updatedStates)
        return true
    }

    /**
     * API clásica de observación conservada por compatibilidad.
     *
     * Entrega inmediatamente la instantánea actual y notifica cada cambio
     * posterior. Para código nuevo se recomienda [stateFlow] u [observeState].
     */
    fun observe(observer: (Map<String, State>) -> Unit): Closeable {
        observers += observer
        observer(snapshot())
        return Closeable { observers -= observer }
    }

    /** Elimina la caché persistente y publica inmediatamente el mapa vacío. */
    fun clear() {
        synchronized(lock) {
            preferences.edit().remove(KEY_STATES).apply()
            sharedStateFlow.value = emptyMap()
            sharedStateInitialized = true
        }

        notifyObservers(emptyMap())
    }

    private fun loadLocked(): Map<String, State> {
        val raw = preferences.getString(KEY_STATES, null) ?: return emptyMap()

        return runCatching {
            val array = JSONArray(raw)

            buildMap {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    val address = item.optString("groupAddress")

                    if (address.isBlank()) {
                        continue
                    }

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
            .sortedBy { state -> state.groupAddress }
            .forEach { state ->
                array.put(
                    JSONObject().apply {
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
                    }
                )
            }

        preferences
            .edit()
            .putString(KEY_STATES, array.toString())
            .apply()
    }

    private fun notifyObservers(states: Map<String, State> = snapshot()) {
        observers.forEach { observer ->
            observer(states)
        }
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(
        value: String,
        default: T
    ): T = runCatching {
        enumValueOf<T>(value)
    }.getOrDefault(default)

    private companion object {
        const val PREFERENCES_NAME = "knx_state_repository"
        const val KEY_STATES = "states"
        const val DUPLICATE_WINDOW_MILLIS = 175L

        val lock = Any()
        val observers = CopyOnWriteArraySet<(Map<String, State>) -> Unit>()
        val sharedStateFlow = MutableStateFlow<Map<String, State>>(emptyMap())

        var sharedStateInitialized = false
    }
}
