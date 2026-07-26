package com.onehouse.app.knx

import android.content.Context
import com.onehouse.app.device.ControlKind
import com.onehouse.app.device.ImportedKnxDevice
import com.onehouse.app.importer.InsideControlProjectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale
import kotlin.math.pow

/** Estado KNX agregado que comparten la portada y todas las estancias. */
data class KnxHomeSnapshot(
    val devices: List<ImportedKnxDevice>,
    val states: Map<String, KnxStateRepository.State>,
    val lightsOn: Int,
    val lightsTotal: Int,
    val blindsOpen: Int,
    val blindsTotal: Int,
    val climate: KnxClimateSnapshot
) {
    fun devicesForRoom(roomName: String): List<ImportedKnxDevice> {
        val aliases = roomAliases(roomName)
        return devices.filter { device ->
            val normalized = normalize(device.roomName)
            aliases.any { alias -> normalized.contains(alias) || alias.contains(normalized) }
        }
    }

    fun booleanValue(device: ImportedKnxDevice): Boolean? =
        device.readAddresses.asSequence()
            .mapNotNull { states[it.toString()]?.booleanValue }
            .firstOrNull()
            ?: device.writeAddresses.asSequence()
                .mapNotNull { states[it.toString()]?.booleanValue }
                .firstOrNull()

    fun numericValue(device: ImportedKnxDevice): Float? {
        val state = (device.readAddresses + device.writeAddresses)
            .asSequence()
            .mapNotNull { states[it.toString()] }
            .firstOrNull() ?: return null
        return KnxValueDecoder.decode(state.rawValue, device.resolvedDpt)
    }

    companion object {
        private fun normalize(value: String): String = value
            .lowercase(Locale.ROOT)
            .replace("á", "a")
            .replace("é", "e")
            .replace("í", "i")
            .replace("ó", "o")
            .replace("ú", "u")
            .replace("ñ", "n")
            .trim()

        private fun roomAliases(roomName: String): Set<String> = when (normalize(roomName)) {
            "entrada" -> setOf("entrada", "recibidor")
            "pasillo" -> setOf("pasillo", "distribuidor")
            "trastero" -> setOf("trastero", "almacen")
            "bano" -> setOf("bano", "aseo")
            "cocina" -> setOf("cocina")
            "habitacion 1" -> setOf("habitacion 1", "dormitorio 1", "estudio")
            "comedor" -> setOf("comedor", "salon", "sala")
            "suite" -> setOf("suite", "principal", "dormitorio principal")
            else -> setOf(normalize(roomName))
        }
    }
}

data class KnxClimateSnapshot(
    val powered: Boolean?,
    val currentTemperature: Float?,
    val targetTemperature: Float?,
    val mode: String?,
    val fanSpeed: String?
)

class KnxHomeStateRepository(context: Context) {
    private val appContext = context.applicationContext
    private val stateRepository = KnxStateRepository(appContext)
    private val devices: List<ImportedKnxDevice> = InsideControlProjectRepository(appContext)
        .load()
        ?.let(KnxDeviceFactory::create)
        .orEmpty()

    val stateFlow: Flow<KnxHomeSnapshot> = stateRepository.stateFlow.map { states ->
        buildSnapshot(devices, states)
    }

    fun snapshot(): KnxHomeSnapshot = buildSnapshot(devices, stateRepository.snapshot())

    private fun buildSnapshot(
        devices: List<ImportedKnxDevice>,
        states: Map<String, KnxStateRepository.State>
    ): KnxHomeSnapshot {
        fun stateFor(device: ImportedKnxDevice): KnxStateRepository.State? =
            (device.readAddresses + device.writeAddresses)
                .asSequence()
                .mapNotNull { states[it.toString()] }
                .firstOrNull()

        val lights = devices.filter { it.controlKind == ControlKind.BOOLEAN_SWITCH }
        val blinds = devices.filter { it.controlKind == ControlKind.BLIND }
        val lightOn = lights.count { stateFor(it)?.booleanValue == true }
        val blindOpen = blinds.count { device ->
            val value = stateFor(device)?.let { KnxValueDecoder.decode(it.rawValue, "5.001") }
            value != null && value < 95f
        }

        fun findByAddress(address: String): KnxStateRepository.State? = states[address]
        fun floatAt(address: String, dpt: String) = KnxValueDecoder.decode(findByAddress(address)?.rawValue, dpt)
        fun boolAt(address: String) = findByAddress(address)?.booleanValue
        fun byteAt(address: String) = KnxValueDecoder.decode(findByAddress(address)?.rawValue, "20.102")?.toInt()

        val mode = when (byteAt("5/3/4")) {
            0 -> "Automático"
            1 -> "Calor"
            2 -> "Noche"
            3 -> "Standby"
            9 -> "Frío"
            14 -> "Ventilación"
            else -> null
        }
        val fan = when (byteAt("5/3/5")) {
            0 -> "Auto"
            1 -> "Baja"
            2 -> "Media"
            3 -> "Alta"
            else -> null
        }

        return KnxHomeSnapshot(
            devices = devices,
            states = states,
            lightsOn = lightOn,
            lightsTotal = lights.size,
            blindsOpen = blindOpen,
            blindsTotal = blinds.size,
            climate = KnxClimateSnapshot(
                powered = boolAt("5/3/2"),
                currentTemperature = floatAt("5/3/3", "9.001"),
                targetTemperature = floatAt("5/3/10", "9.001")
                    ?: floatAt("5/3/1", "9.001"),
                mode = mode,
                fanSpeed = fan
            )
        )
    }
}

object KnxValueDecoder {
    fun decode(rawHex: String?, dpt: String): Float? {
        val bytes = rawHex
            ?.takeIf { it.length >= 2 && it.length % 2 == 0 }
            ?.chunked(2)
            ?.mapNotNull { it.toIntOrNull(16) }
            ?: return rawHex?.toFloatOrNull()

        return when (KnxDptResolver.mainNumber(dpt)) {
            1 -> (bytes.lastOrNull()?.and(1) ?: return null).toFloat()
            5 -> (bytes.lastOrNull() ?: return null) * 100f / 255f
            9 -> decodeDpt9(bytes)
            20 -> bytes.lastOrNull()?.toFloat()
            else -> bytes.lastOrNull()?.toFloat()
        }
    }

    private fun decodeDpt9(bytes: List<Int>): Float? {
        if (bytes.size < 2) return null
        val raw = (bytes[bytes.size - 2] shl 8) or bytes.last()
        val sign = if ((raw and 0x8000) != 0) -1 else 1
        val exponent = (raw ushr 11) and 0x0F
        var mantissa = raw and 0x07FF
        if (sign < 0) mantissa -= 2048
        return (0.01f * mantissa * 2.0.pow(exponent.toDouble())).toFloat()
    }
}
