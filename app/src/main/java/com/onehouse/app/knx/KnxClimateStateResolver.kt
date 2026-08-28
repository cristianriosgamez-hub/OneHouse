package com.onehouse.app.knx

import kotlin.math.abs

/**
 * v1.12.4
 *
 * Fuente única de verdad para los estados de climatización.
 * Lee exclusivamente las direcciones KNX de ESTADO configuradas en OneHouse.
 * No deduce estado a partir del último mando enviado.
 */
internal object KnxClimateStateResolver {

    data class Snapshot(
        val powered: Boolean?,
        val currentTemperature: Float?,
        val targetTemperature: Float?,
        val mode: String?,
        val fanSpeed: String?
    )

    fun resolve(states: Map<String, KnxStateRepository.State>): Snapshot {
        fun trusted(address: String): KnxStateRepository.State? =
            states[address]?.takeIf(StateFreshness::isTrusted)

        fun boolAt(address: String): Boolean? =
            trusted(address)?.booleanValue

        fun numberAt(address: String, dpt: String): Float? =
            KnxValueDecoder.decode(trusted(address)?.rawValue, dpt)

        fun strictTemperatureAt(address: String, range: ClosedFloatingPointRange<Float>): Float? {
            val raw = trusted(address)?.rawValue ?: return null
            // DPT 9.001 real: dos bytes. Evitamos reinterpretar otras longitudes.
            if (raw.length != 4) return null
            return KnxValueDecoder.decode(raw, "9.001")
                ?.takeIf { it.isFinite() && it in range }
        }

        val modeCode = numberAt(KnxAddressBook.Climate.MODE_STATE, "20.105")?.toInt()
        val mode = when (modeCode) {
            0 -> "Auto"
            1 -> "Calor"
            3 -> "Frío"
            9 -> "Ventilador"
            14 -> "Dry"
            else -> null
        }

        val fanPercent = numberAt(KnxAddressBook.Climate.FAN_SPEED_STATE, "5.001")
        val fanSpeed = fanPercent?.let { value ->
            listOf(
                "Baja" to KnxAddressBook.Climate.FAN_SPEED_LOW_VALUE.toFloat(),
                "Media" to KnxAddressBook.Climate.FAN_SPEED_MEDIUM_VALUE.toFloat(),
                "Alta" to KnxAddressBook.Climate.FAN_SPEED_HIGH_VALUE.toFloat()
            ).minByOrNull { (_, configured) -> abs(value - configured) }?.first
        }

        return Snapshot(
            powered = boolAt(KnxAddressBook.Climate.POWER_STATE),
            currentTemperature = strictTemperatureAt(
                KnxAddressBook.Climate.CURRENT_TEMPERATURE,
                -20f..60f
            ),
            targetTemperature = strictTemperatureAt(
                KnxAddressBook.Climate.TARGET_TEMPERATURE_PRIMARY,
                16f..34f
            ),
            mode = mode,
            fanSpeed = fanSpeed
        )
    }
}
