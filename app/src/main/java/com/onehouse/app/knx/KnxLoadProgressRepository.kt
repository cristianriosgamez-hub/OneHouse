package com.onehouse.app.knx

import com.onehouse.app.device.ControlKind
import com.onehouse.app.device.ImportedKnxDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class KnxLoadKind(val label: String) {
    LIGHT("Luces"),
    BLIND("Persianas"),
    CLIMATE("Climatización"),
    TEMPERATURE("Temperaturas"),
    SENSOR("Sensores"),
    METER("Mediciones"),
    OTHER("Otros")
}

data class KnxLoadTarget(
    val address: String,
    val rooms: Set<String>,
    val kinds: Set<KnxLoadKind>,
    val names: Set<String>
)

data class KnxLoadProgressSnapshot(
    val targets: Map<String, KnxLoadTarget> = emptyMap(),
    val receivedAddresses: Set<String> = emptySet(),
    val noResponseAddresses: Set<String> = emptySet(),
    val currentAddress: String? = null,
    val finished: Boolean = false
) {
    val total: Int get() = targets.size
    val received: Int get() = receivedAddresses.size
    val noResponse: Int get() = noResponseAddresses.size
    val resolved: Int get() = (receivedAddresses + noResponseAddresses).size
    val pending: Int get() = (total - resolved).coerceAtLeast(0)
    val percent: Int
        get() = when {
            total == 0 -> if (finished) 100 else 0
            else -> ((resolved * 100f) / total).toInt().coerceIn(0, 100)
        }
}

/**
 * Progreso visible de la primera carga KNX del proceso actual.
 *
 * El porcentaje representa direcciones ya resueltas: con respuesta o agotados
 * sus reintentos. Por eso puede llegar al 100 % aunque alguna GA haya terminado
 * "sin respuesta"; el detalle separa ambas situaciones.
 */
object KnxLoadProgressRepository {
    private val mutableProgress = MutableStateFlow(KnxLoadProgressSnapshot())
    val progress: StateFlow<KnxLoadProgressSnapshot> = mutableProgress.asStateFlow()

    @Synchronized
    fun begin(targets: Map<String, KnxLoadTarget>) {
        mutableProgress.value = KnxLoadProgressSnapshot(targets = targets)
    }

    @Synchronized
    fun setCurrent(address: String?) {
        val current = mutableProgress.value
        if (current.finished) return
        mutableProgress.value = current.copy(currentAddress = address)
    }

    @Synchronized
    fun syncReceived(states: Map<String, KnxStateRepository.State>) {
        val current = mutableProgress.value
        if (current.targets.isEmpty()) return
        val received = current.targets.keys.filterTo(mutableSetOf()) { address ->
            states[address]?.let(StateFreshness::isTrusted) == true
        }
        mutableProgress.value = current.copy(receivedAddresses = received)
    }

    @Synchronized
    fun finish(finalNoResponse: Collection<String>, states: Map<String, KnxStateRepository.State>) {
        val current = mutableProgress.value
        if (current.targets.isEmpty()) return
        val received = current.targets.keys.filterTo(mutableSetOf()) { address ->
            states[address]?.let(StateFreshness::isTrusted) == true
        }
        val unresolved = finalNoResponse
            .asSequence()
            .filter { it in current.targets }
            .filterNot { it in received }
            .toSet()
        mutableProgress.value = current.copy(
            receivedAddresses = received,
            noResponseAddresses = unresolved,
            currentAddress = null,
            finished = true
        )
    }

    fun buildTargets(
        devices: List<ImportedKnxDevice>,
        addresses: List<String>
    ): Map<String, KnxLoadTarget> {
        val normalizedAddresses = addresses.toSet()
        val descriptors = linkedMapOf<String, MutableDescriptor>()

        fun descriptor(address: String): MutableDescriptor =
            descriptors.getOrPut(address) { MutableDescriptor() }

        devices.forEach { device ->
            val kind = kindFor(device.controlKind)
            val room = device.roomName.trim().ifBlank { "Vivienda" }
            val related = buildList {
                addAll(device.readAddresses.map { it.toString() })
                if (device.controlKind == ControlKind.BOOLEAN_SWITCH ||
                    device.controlKind == ControlKind.CLIMATE
                ) {
                    addAll(device.writeAddresses.map { it.toString() })
                }
            }.distinct()

            related.filter { it in normalizedAddresses }.forEach { address ->
                descriptor(address).apply {
                    rooms += room
                    kinds += kind
                    names += device.name.trim().ifBlank { kind.label }
                }
            }
        }

        addresses.forEach { address ->
            if (address !in descriptors) {
                val fallback = explicitDescriptor(address)
                descriptor(address).apply {
                    rooms += fallback.first
                    kinds += fallback.second
                    names += fallback.third
                }
            }
        }

        return addresses.associateWith { address ->
            val item = descriptors.getValue(address)
            KnxLoadTarget(
                address = address,
                rooms = item.rooms.toSet(),
                kinds = item.kinds.toSet(),
                names = item.names.toSet()
            )
        }
    }

    private fun kindFor(controlKind: ControlKind): KnxLoadKind = when (controlKind) {
        ControlKind.BOOLEAN_SWITCH -> KnxLoadKind.LIGHT
        ControlKind.BLIND -> KnxLoadKind.BLIND
        ControlKind.CLIMATE -> KnxLoadKind.CLIMATE
        ControlKind.TEMPERATURE -> KnxLoadKind.TEMPERATURE
        ControlKind.ALARM,
        ControlKind.SENSOR,
        ControlKind.READ_ONLY -> KnxLoadKind.SENSOR
        ControlKind.METER -> KnxLoadKind.METER
        else -> KnxLoadKind.OTHER
    }

    private fun explicitDescriptor(address: String): Triple<String, KnxLoadKind, String> = when (address) {
        KnxAddressBook.Climate.POWER_STATE,
        KnxAddressBook.Climate.CURRENT_TEMPERATURE,
        KnxAddressBook.Climate.MODE_STATE,
        KnxAddressBook.Climate.FAN_SPEED_STATE,
        KnxAddressBook.Climate.TARGET_TEMPERATURE_PRIMARY,
        KnxAddressBook.Climate.TARGET_TEMPERATURE_FALLBACK ->
            Triple("Vivienda", KnxLoadKind.CLIMATE, "Climatización")

        KnxAddressBook.Indoor.CO2_DINING -> Triple("Comedor", KnxLoadKind.SENSOR, "CO₂")
        KnxAddressBook.Indoor.HUMIDITY_DINING -> Triple("Comedor", KnxLoadKind.SENSOR, "Humedad")
        KnxAddressBook.Indoor.TEMPERATURE_DINING -> Triple("Comedor", KnxLoadKind.TEMPERATURE, "Temperatura")
        KnxAddressBook.Indoor.TEMPERATURE_SUITE -> Triple("Suite", KnxLoadKind.TEMPERATURE, "Temperatura")
        KnxAddressBook.Indoor.PIR_BLOCK_ENTRANCE -> Triple("Entrada", KnxLoadKind.SENSOR, "PIR")
        KnxAddressBook.Indoor.FLOOD_KITCHEN -> Triple("Cocina", KnxLoadKind.SENSOR, "Inundación")
        KnxAddressBook.Indoor.FLOOD_BATHROOM -> Triple("Baño", KnxLoadKind.SENSOR, "Inundación")
        KnxAddressBook.Indoor.FIRE_HALLWAY -> Triple("Pasillo", KnxLoadKind.SENSOR, "Incendio")
        KnxAddressBook.Terrace.LUMINOSITY -> Triple("Terraza", KnxLoadKind.SENSOR, "Luminosidad")
        KnxAddressBook.Terrace.EXCESSIVE_WIND -> Triple("Terraza", KnxLoadKind.SENSOR, "Viento excesivo")
        KnxAddressBook.Terrace.WIND_SPEED -> Triple("Terraza", KnxLoadKind.SENSOR, "Velocidad del viento")
        KnxAddressBook.Terrace.RAINING -> Triple("Terraza", KnxLoadKind.SENSOR, "Lluvia")
        else -> Triple("Vivienda", KnxLoadKind.OTHER, address)
    }

    private class MutableDescriptor {
        val rooms = linkedSetOf<String>()
        val kinds = linkedSetOf<KnxLoadKind>()
        val names = linkedSetOf<String>()
    }
}
