package com.onehouse.app.knx

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.onehouse.app.device.ControlKind
import com.onehouse.app.device.ImportedKnxDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale
import kotlin.math.abs
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
            normalized.isNotBlank() && aliases.any { alias ->
                normalized.contains(alias) || alias.contains(normalized)
            }
        }
    }

    /**
     * Devuelve únicamente un estado confirmado por el objeto de lectura.
     *
     * Cuando el dispositivo dispone de dirección de estado nunca usamos como
     * respaldo la dirección de mando: un GroupValueWrite antiguo podría hacer
     * aparecer una luz encendida aunque el actuador esté realmente apagado.
     */
    fun booleanValue(device: ImportedKnxDevice): Boolean? =
        KnxLightStateResolver.confirmedBoolean(device, states)

    fun numericValue(device: ImportedKnxDevice): Float? {
        val state = stateAddresses(device)
            .asSequence()
            .mapNotNull { address -> states[address.toString()]?.takeIf(StateFreshness::isTrusted) }
            .firstOrNull() ?: return null
        return KnxValueDecoder.decodeFlexible(state.rawValue, device.resolvedDpt)
    }

    fun booleanAt(address: String): Boolean? =
        states[address]
            ?.takeIf(StateFreshness::isTrusted)
            ?.booleanValue

    fun numericAt(address: String, dpt: String): Float? {
        val raw = states[address]
            ?.takeIf(StateFreshness::isTrusted)
            ?.rawValue
            ?: return null
        return if (KnxDptResolver.mainNumber(dpt) == 9) {
            // Las temperaturas DPT 9 deben ocupar exactamente dos bytes.
            if (raw.length != 4) null else KnxValueDecoder.decode(raw, dpt)
        } else {
            KnxValueDecoder.decode(raw, dpt)
        }
    }

    fun hasTrustedState(address: String): Boolean =
        states[address]?.let(StateFreshness::isTrusted) == true

    private fun stateAddresses(device: ImportedKnxDevice) =
        (device.readAddresses + device.writeAddresses).distinct()

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
    private val centralResources = KnxCentralEngine.get(appContext)
    private val stateRepository = centralResources.stateRepository
    private val subscriptionManager = centralResources.subscriptionManager
    private val appConfiguration = AppKnxConfigurationRepository(appContext).also(KnxAddressBook::apply)
    private val devices: List<ImportedKnxDevice> = appConfiguration.loadProject()
        ?.let(KnxDeviceFactory::create)
        .orEmpty()

    init {
        KnxProcessSession.prepare(stateRepository)
        PeriodicKnxStateRefresh.ensureStarted(appContext, devices)
    }

    val stateFlow: Flow<KnxHomeSnapshot> = subscriptionManager
        .observe(allObservedAddresses())
        .map { states ->
            buildSnapshot(devices, states)
        }

    fun snapshot(): KnxHomeSnapshot =
        buildSnapshot(devices, subscriptionManager.snapshot(allObservedAddresses()))

    private fun allObservedAddresses(): Set<String> = buildSet {
        devices.forEach { device ->
            device.readAddresses.forEach { add(it.toString()) }
            device.writeAddresses.forEach { add(it.toString()) }
        }
        addAll(PeriodicKnxStateRefresh.explicitStateAddresses())
    }

    private fun buildSnapshot(
        devices: List<ImportedKnxDevice>,
        states: Map<String, KnxStateRepository.State>
    ): KnxHomeSnapshot {
        fun stateFor(device: ImportedKnxDevice): KnxStateRepository.State? =
            (device.readAddresses + device.writeAddresses)
                .distinct()
                .asSequence()
                .mapNotNull { states[it.toString()]?.takeIf(StateFreshness::isTrusted) }
                .firstOrNull()

        val lights = devices.filter { it.controlKind == ControlKind.BOOLEAN_SWITCH }
        val blinds = devices.filter { it.controlKind == ControlKind.BLIND }
        // v1.12.2: el contador de luces usa la misma resolución de estado
        // confirmado que las estancias. No se deduce el estado del último mando.
        val lightOn = lights.count { device ->
            KnxLightStateResolver.confirmedBoolean(device, states) == true
        }
        val blindOpen = blinds.count { device ->
            val value = stateFor(device)?.let { KnxValueDecoder.decode(it.rawValue, "5.001") }
            value != null && value < 95f
        }

        fun findByAddress(address: String): KnxStateRepository.State? =
            states[address]?.takeIf(StateFreshness::isTrusted)
        fun boolAt(address: String) = findByAddress(address)?.booleanValue
        fun numericAt(address: String, dpt: String) =
            KnxValueDecoder.decode(findByAddress(address)?.rawValue, dpt)

        fun normalized(value: String): String = value
            .lowercase(Locale.ROOT)
            .replace("á", "a").replace("é", "e").replace("í", "i")
            .replace("ó", "o").replace("ú", "u").replace("ñ", "n")

        fun semanticDevices(vararg terms: String): List<ImportedKnxDevice> = devices.filter { device ->
            val text = normalized("${device.roomName} ${device.name} ${device.unit.orEmpty()}")
            terms.all { term -> text.contains(normalized(term)) }
        }

        fun numericFromDevices(candidates: List<ImportedKnxDevice>, range: ClosedFloatingPointRange<Float>? = null): Float? =
            candidates.asSequence().mapNotNull { device ->
                val state = (device.readAddresses + device.writeAddresses).distinct()
                    .asSequence()
                    .mapNotNull { address -> states[address.toString()]?.takeIf(StateFreshness::isTrusted) }
                    .firstOrNull() ?: return@mapNotNull null
                KnxValueDecoder.decodeFlexible(state.rawValue, device.resolvedDpt)
                    ?.takeIf { it.isFinite() && (range == null || it in range) }
            }.firstOrNull()

        fun booleanFromDevices(candidates: List<ImportedKnxDevice>): Boolean? =
            candidates.asSequence().mapNotNull { device ->
                (device.readAddresses + device.writeAddresses).distinct()
                    .asSequence()
                    .mapNotNull { address -> states[address.toString()]?.takeIf(StateFreshness::isTrusted)?.booleanValue }
                    .firstOrNull()
            }.firstOrNull()

        val climateDevices = devices.filter { device ->
            device.controlKind == ControlKind.CLIMATE ||
                normalized(device.name).contains("daikin") ||
                normalized(device.name).contains("clima") ||
                normalized(device.name).contains("termostato")
        }
        val modeDevices = climateDevices.filter { device ->
            val name = normalized(device.name)
            name.contains("modo") || name.contains("mode")
        }
        val fanDevices = climateDevices.filter { device ->
            val name = normalized(device.name)
            name.contains("ventil") || name.contains("fan") || name.contains("velocidad")
        }
        val setpointDevices = climateDevices.filter { device ->
            val name = normalized(device.name)
            name.contains("consigna") || name.contains("setpoint") || name.contains("objetivo")
        }
        val powerDevices = climateDevices.filter { device ->
            val name = normalized(device.name)
            name.contains("on/off") || name.contains("encendido") || name.contains("marcha") ||
                KnxDptResolver.mainNumber(device.resolvedDpt) == 1
        }

        // El objeto de ESTADO es la fuente autoritativa. Solo usamos el objeto
        // semántico importado como respaldo si todavía no existe lectura real.
        val modeCode = numericAt(KnxAddressBook.Climate.MODE_STATE, "20.105")?.toInt()
            ?: numericFromDevices(modeDevices)?.toInt()
        val fanPercent = numericAt(KnxAddressBook.Climate.FAN_SPEED_STATE, "5.001")
            ?: numericFromDevices(fanDevices)

        // Los códigos son los mismos que utiliza OneHouse al escribir sobre
        // 5/2/4. Antes 9 y 14 se interpretaban como modos distintos al enviado.
        val mode = when (modeCode) {
            0 -> "Auto"
            1 -> "Calor"
            3 -> "Frío"
            9 -> "Ventilador"
            14 -> "Dry"
            else -> null
        }

        // Schneider configura 25 / 37 / 100 %. No utilizamos umbrales
        // genéricos: elegimos el valor configurado más próximo a la lectura.
        val fan = fanPercent?.let { value ->
            listOf(
                "Baja" to KnxAddressBook.Climate.FAN_SPEED_LOW_VALUE.toFloat(),
                "Media" to KnxAddressBook.Climate.FAN_SPEED_MEDIUM_VALUE.toFloat(),
                "Alta" to KnxAddressBook.Climate.FAN_SPEED_HIGH_VALUE.toFloat()
            ).minByOrNull { (_, configured) -> abs(value - configured) }?.first
        }

        val diningTemperature = numericFromDevices(
            devices.filter { device ->
                val text = normalized("${device.roomName} ${device.name}")
                (text.contains("comedor") || text.contains("salon")) && text.contains("temperatura")
            }, -20f..60f
        ) ?: strictDpt9At(states, KnxAddressBook.Indoor.TEMPERATURE_DINING, -20f..60f)

        val climateTemperature = numericFromDevices(
            climateDevices.filter { normalized(it.name).contains("temperatura") &&
                !normalized(it.name).contains("consigna") }, -20f..60f
        ) ?: strictDpt9At(states, KnxAddressBook.Climate.CURRENT_TEMPERATURE, -20f..60f)

        val targetTemperature = numericFromDevices(setpointDevices, 16f..34f)
            ?: strictDpt9At(states, KnxAddressBook.Climate.TARGET_TEMPERATURE_PRIMARY, 16f..34f)
            ?: strictDpt9At(states, KnxAddressBook.Climate.TARGET_TEMPERATURE_FALLBACK, 16f..34f)

        val climatePowered = booleanFromDevices(powerDevices)
            ?: boolAt(KnxAddressBook.Climate.POWER_STATE)

        return KnxHomeSnapshot(
            devices = devices,
            states = states,
            lightsOn = lightOn,
            lightsTotal = lights.size,
            blindsOpen = blindOpen,
            blindsTotal = blinds.size,
            climate = KnxClimateSnapshot(
                powered = climatePowered,
                // La portada debe mostrar la sonda del comedor, no una lectura interna
                // del equipo de climatización. Se conserva como respaldo la sonda Daikin.
                currentTemperature = diningTemperature ?: climateTemperature,
                targetTemperature = targetTemperature,
                mode = mode,
                fanSpeed = fan
            )
        )
    }

    private fun strictDpt9At(
        states: Map<String, KnxStateRepository.State>,
        address: String,
        range: ClosedFloatingPointRange<Float>
    ): Float? {
        val raw = states[address]?.takeIf(StateFreshness::isTrusted)?.rawValue ?: return null
        // DPT 9.001 ocupa exactamente dos bytes. No reinterpretamos automáticamente
        // datos de otra longitud como temperatura.
        if (raw.length != 4) return null
        return KnxValueDecoder.decode(raw, "9.001")?.takeIf { it.isFinite() && it in range }
    }

}

private object KnxProcessSession {
    private val lock = Any()
    @Volatile private var prepared = false
    @Volatile var startedAtMillis: Long = Long.MAX_VALUE
        private set

    fun prepare(repository: KnxStateRepository) {
        if (prepared) return
        synchronized(lock) {
            if (prepared) return
            // Se elimina una sola vez la caché de la ejecución anterior. Desde
            // este instante cualquier telegrama recibido pertenece a la sesión
            // actual y no puede quedar invalidado por inicializaciones tardías.
            repository.clear()
            startedAtMillis = System.currentTimeMillis()
            prepared = true
        }
    }
}

private object StateFreshness {
    fun isTrusted(state: KnxStateRepository.State): Boolean =
        state.timestampMillis >= KnxProcessSession.startedAtMillis
}

/**
 * Mantiene actualizados los objetos KNX de lectura mientras la aplicación está
 * activa. Las lecturas se realizan de forma secuencial para no saturar el túnel
 * KNX/IP y se repiten cada minuto al terminar el ciclo anterior.
 */
private object PeriodicKnxStateRefresh {
    private const val REFRESH_INTERVAL_MILLIS = 60_000L
    private const val STARTUP_RETRY_DELAY_MILLIS = 8_000L
    private val lock = Any()
    private val handler = Handler(Looper.getMainLooper())
    private var activeReader: KnxBulkStateReader? = null
    private var activeSignature: String? = null
    private var scheduledSignature: String? = null
    private var scheduledRunnable: Runnable? = null
    private var fastRetrySignature: String? = null

    fun explicitStateAddresses(): List<String> = listOf(
        KnxAddressBook.Climate.POWER_STATE,
        KnxAddressBook.Climate.CURRENT_TEMPERATURE,
        KnxAddressBook.Climate.MODE_STATE,
        KnxAddressBook.Climate.FAN_SPEED_STATE,
        KnxAddressBook.Climate.TARGET_TEMPERATURE_PRIMARY,
        KnxAddressBook.Climate.TARGET_TEMPERATURE_FALLBACK,
        KnxAddressBook.Indoor.CO2_DINING,
        KnxAddressBook.Indoor.HUMIDITY_DINING,
        KnxAddressBook.Indoor.TEMPERATURE_DINING,
        KnxAddressBook.Indoor.TEMPERATURE_SUITE,
        KnxAddressBook.Indoor.PIR_BLOCK_ENTRANCE,
        KnxAddressBook.Indoor.FLOOD_KITCHEN,
        KnxAddressBook.Indoor.FLOOD_BATHROOM,
        KnxAddressBook.Indoor.FIRE_HALLWAY,
        KnxAddressBook.Terrace.LUMINOSITY,
        KnxAddressBook.Terrace.EXCESSIVE_WIND,
        KnxAddressBook.Terrace.WIND_SPEED,
        KnxAddressBook.Terrace.RAINING
    ).distinct()

    fun ensureStarted(context: Context, devices: List<ImportedKnxDevice>) {
        val readable = devices.filter { it.canRead }
        val explicitStateAddresses = explicitStateAddresses()
        if (readable.isEmpty() && explicitStateAddresses.isEmpty()) return

        val signature = (
            readable.flatMap { device -> device.readAddresses.map { it.toString() } } +
                explicitStateAddresses
            )
            .distinct()
            .sorted()
            .joinToString("|")

        synchronized(lock) {
            if (signature == activeSignature || signature == scheduledSignature) return
            startCycleLocked(context.applicationContext, readable, signature)
        }
    }

    private fun startCycleLocked(
        context: Context,
        readable: List<ImportedKnxDevice>,
        signature: String
    ) {
        scheduledRunnable?.let(handler::removeCallbacks)
        scheduledRunnable = null
        scheduledSignature = null
        activeReader?.close()

        val reader = KnxBulkStateReader(context)
        activeReader = reader
        activeSignature = signature

        val prioritized = readable.sortedBy { device ->
            when {
                device.controlKind == ControlKind.CLIMATE -> 0
                device.name.contains("daikin", ignoreCase = true) -> 0
                device.name.contains("termostato", ignoreCase = true) -> 0
                device.controlKind == ControlKind.TEMPERATURE -> 1
                device.controlKind == ControlKind.BOOLEAN_SWITCH -> 2
                device.controlKind == ControlKind.BLIND -> 3
                else -> 4
            }
        }

        // Además de los sensores globales, adelantamos las direcciones de estado
        // de las luces. Así, al abrir la app, los interruptores visibles y el
        // contador de la portada se pintan antes de recorrer todo el proyecto.
        val startupPriorityAddresses = (
            explicitStateAddresses() +
                prioritized
                    .filter { it.controlKind == ControlKind.BOOLEAN_SWITCH }
                    .flatMap { device -> device.readAddresses.map { it.toString() } }
            ).distinct()

        reader.read(
            devices = prioritized,
            extraReadAddresses = startupPriorityAddresses,
            onProgress = { },
            onComplete = {
                reader.close()
                synchronized(lock) {
                    if (activeReader !== reader) return@synchronized
                    activeReader = null
                    activeSignature = null

                    val next = Runnable {
                        synchronized(lock) {
                            if (scheduledSignature != signature) return@synchronized
                            scheduledRunnable = null
                            scheduledSignature = null
                            startCycleLocked(context, readable, signature)
                        }
                    }
                    scheduledRunnable = next
                    scheduledSignature = signature
                    // Primera recuperación rápida: algunos actuadores/sensores KNX
                    // responden solo después de que el túnel ya ha quedado activo.
                    // Se repite pronto una sola vez; después se vuelve al minuto.
                    val delay = if (fastRetrySignature != signature) {
                        fastRetrySignature = signature
                        STARTUP_RETRY_DELAY_MILLIS
                    } else {
                        REFRESH_INTERVAL_MILLIS
                    }
                    handler.postDelayed(next, delay)
                }
            }
        )
    }
}

object KnxValueDecoder {
    /**
     * Decodifica respetando el DPT indicado y corrige exportaciones de
     * InsideControl donde una magnitud de 4 bytes llega etiquetada como DPT 9.
     */
    fun decodeFlexible(rawHex: String?, dpt: String): Float? {
        val primary = decode(rawHex, dpt)
        val main = KnxDptResolver.mainNumber(dpt)
        val byteCount = rawHex?.length?.div(2) ?: 0

        return when {
            main == 9 && byteCount >= 4 && (primary == null || primary !in -100f..1000f) ->
                decode(rawHex, "14.000") ?: primary
            main == 14 && byteCount == 2 && (primary == null || !primary.isFinite()) ->
                decode(rawHex, "9.001") ?: primary
            else -> primary
        }
    }

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
            14 -> decodeDpt14(bytes)
            20 -> bytes.lastOrNull()?.toFloat()
            else -> bytes.lastOrNull()?.toFloat()
        }
    }

    private fun decodeDpt14(bytes: List<Int>): Float? {
        if (bytes.size < 4) return null
        val start = bytes.size - 4
        val bits = ((bytes[start].toLong() and 0xFF) shl 24) or
            ((bytes[start + 1].toLong() and 0xFF) shl 16) or
            ((bytes[start + 2].toLong() and 0xFF) shl 8) or
            (bytes[start + 3].toLong() and 0xFF)
        return Float.fromBits(bits.toInt())
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
