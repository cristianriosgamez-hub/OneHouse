package com.onehouse.app.feature.rooms.detail

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.onehouse.app.R
import com.onehouse.app.design.FondoInferior
import com.onehouse.app.design.FondoSuperior
import com.onehouse.app.device.ControlKind
import com.onehouse.app.device.ImportedKnxDevice
import com.onehouse.app.importer.InsideControlProjectRepository
import com.onehouse.app.knx.KnxBulkStateReader
import com.onehouse.app.knx.KnxCommandExecutor
import com.onehouse.app.knx.KnxCommandType
import com.onehouse.app.knx.KnxDeviceFactory
import com.onehouse.app.knx.KnxStateRepository
import java.text.Normalizer
import kotlin.math.pow

@Composable
fun RoomDetailScreen(roomType: RoomType, onBack: () -> Unit) {
    val context = LocalContext.current
    val commandExecutor = remember { KnxCommandExecutor(context) }
    val bulkReader = remember { KnxBulkStateReader(context) }
    val project = remember { InsideControlProjectRepository(context).load() }
    val allDevices = remember(project) { project?.let { KnxDeviceFactory.create(it) }.orEmpty() }
    val devices = remember(roomType, allDevices) { devicesForRoom(roomType, allDevices) }
    val knxStates by commandExecutor.stateFlow.collectAsState()
    var contentVisible by remember(roomType) { mutableStateOf(false) }

    DisposableEffect(commandExecutor, bulkReader) {
        onDispose {
            bulkReader.close()
            commandExecutor.close()
        }
    }

    LaunchedEffect(roomType, devices) {
        contentVisible = true
        if (devices.isNotEmpty()) {
            bulkReader.read(devices, onProgress = {}, onComplete = {})
        }
    }

    val lightDevices = devices.filter { it.controlKind == ControlKind.BOOLEAN_SWITCH }
    val lightOn = lightDevices.any { device -> stateFor(device, knxStates)?.booleanValue == true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .background(Brush.verticalGradient(listOf(FondoSuperior, FondoInferior, Color.Black)))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(14.dp))
        RoomHeader(title = roomTitle(roomType), onBack = onBack)
        Spacer(Modifier.height(16.dp))

        RoomHeroCard(type = roomType, lightOn = lightOn, imageRes = roomImage(roomType))
        Spacer(Modifier.height(14.dp))

        RoomControlAnimated(contentVisible) {
            Column {
                if (devices.isEmpty()) {
                    RoomKnxValueCard(
                        title = "Sin dispositivos importados",
                        value = "—",
                        subtitle = "No se encontró una estancia equivalente en el proyecto",
                        symbol = "⌂"
                    )
                } else {
                    devices.forEachIndexed { index, device ->
                        if (index > 0) Spacer(Modifier.height(12.dp))
                        KnxDeviceCard(device, knxStates, commandExecutor)
                    }
                }
            }
        }
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun KnxDeviceCard(
    device: ImportedKnxDevice,
    states: Map<String, KnxStateRepository.State>,
    executor: KnxCommandExecutor
) {
    val state = stateFor(device, states)
    when (device.controlKind) {
        ControlKind.BOOLEAN_SWITCH -> {
            val enabled = state?.booleanValue == true
            RoomLightCard(
                title = device.name,
                enabled = enabled,
                onEnabledChange = { requested ->
                    val type = if (requested) KnxCommandType.ON else KnxCommandType.OFF
                    device.commands.firstOrNull { it.type == type }?.let { command ->
                        executor.execute(command) { }
                    }
                }
            )
        }

        ControlKind.BLIND -> {
            val percent = decodePercent(state?.rawValue)
            RoomBlindStatusCard(
                title = device.name,
                positionPercent = percent,
                updated = state != null
            )
        }

        ControlKind.TEMPERATURE -> RoomKnxValueCard(
            title = device.name,
            value = decodeTemperature(state?.rawValue)?.let { "%.1f °C".format(it) } ?: "—",
            subtitle = stateSubtitle(state),
            symbol = "♨"
        )

        ControlKind.CLIMATE -> RoomKnxValueCard(
            title = device.name,
            value = climateValue(device, state),
            subtitle = stateSubtitle(state),
            symbol = climateSymbol(device.name)
        )

        else -> RoomKnxValueCard(
            title = device.name,
            value = genericValue(device, state),
            subtitle = stateSubtitle(state),
            symbol = device.iconGlyph
        )
    }
}

private fun stateFor(
    device: ImportedKnxDevice,
    states: Map<String, KnxStateRepository.State>
): KnxStateRepository.State? =
    device.readAddresses.asSequence().mapNotNull { states[it.toString()] }.firstOrNull()
        ?: device.writeAddresses.asSequence().mapNotNull { states[it.toString()] }.firstOrNull()

private fun stateSubtitle(state: KnxStateRepository.State?): String =
    if (state == null) "Esperando estado KNX" else "Estado real recibido del bus"

private fun climateValue(device: ImportedKnxDevice, state: KnxStateRepository.State?): String {
    val name = normalize(device.name)
    if (state == null) return "—"
    if ("temperatura" in name || "consigna" in name) {
        return decodeTemperature(state.rawValue)?.let { "%.1f °C".format(it) } ?: "—"
    }
    state.booleanValue?.let { return if (it) "Encendido" else "Apagado" }
    val byte = decodeByte(state.rawValue)
    return when {
        "velocidad" in name -> byte?.let { climateFanLabel(it) } ?: "—"
        "modo" in name -> byte?.let { climateModeLabel(it) } ?: "—"
        else -> byte?.toString() ?: state.rawValue.orEmpty().ifBlank { "—" }
    }
}

private fun genericValue(device: ImportedKnxDevice, state: KnxStateRepository.State?): String {
    if (state == null) return "—"
    state.booleanValue?.let { return if (it) "Activo" else "Inactivo" }
    val name = normalize(device.name)
    return when {
        "temperatura" in name -> decodeTemperature(state.rawValue)?.let { "%.1f °C".format(it) }
        "humedad" in name -> decodePercent(state.rawValue)?.let { "$it %" }
        "co2" in name -> decodeUnsigned(state.rawValue)?.let { "$it ppm" }
        "luminosidad" in name -> decodeUnsigned(state.rawValue)?.let { "$it lux" }
        else -> decodeUnsigned(state.rawValue)?.let { value -> "$value${device.unit?.let { " $it" }.orEmpty()}" }
    } ?: state.rawValue.orEmpty().ifBlank { "—" }
}

private fun decodeByte(raw: String?): Int? = raw?.takeIf { it.length >= 2 }?.take(2)?.toIntOrNull(16)
private fun decodeUnsigned(raw: String?): Int? = raw?.takeIf { it.isNotBlank() }?.toIntOrNull(16)
private fun decodePercent(raw: String?): Int? = decodeByte(raw)?.let { ((it * 100f) / 255f).toInt().coerceIn(0, 100) }

private fun decodeTemperature(raw: String?): Float? {
    val clean = raw?.takeIf { it.length >= 4 }?.takeLast(4) ?: return null
    val data = clean.toIntOrNull(16) ?: return null
    val exponent = (data shr 11) and 0x0F
    var mantissa = data and 0x07FF
    if ((data and 0x8000) != 0) mantissa -= 0x0800
    return 0.01f * mantissa * 2.0.pow(exponent.toDouble()).toFloat()
}

private fun climateFanLabel(value: Int): String = when (value) {
    0 -> "Automático"
    1 -> "Baja"
    2 -> "Media"
    3 -> "Alta"
    else -> "Nivel $value"
}

private fun climateModeLabel(value: Int): String = when (value) {
    0 -> "Automático"
    1 -> "Calor"
    3 -> "Frío"
    9 -> "Ventilación"
    14 -> "Seco"
    else -> "Modo $value"
}

private fun climateSymbol(name: String): String = when {
    "temperatura" in normalize(name) -> "♨"
    "velocidad" in normalize(name) -> "≋"
    "modo" in normalize(name) -> "❄"
    else -> "◉"
}

private fun devicesForRoom(roomType: RoomType, devices: List<ImportedKnxDevice>): List<ImportedKnxDevice> {
    val aliases = roomAliases(roomType).map(::normalize)
    return devices.filter { device ->
        val room = normalize(device.roomName)
        aliases.any { alias -> room == alias || room.contains(alias) || alias.contains(room) }
    }.sortedWith(compareBy({ deviceOrder(it.controlKind) }, { it.name }))
}

private fun deviceOrder(kind: ControlKind): Int = when (kind) {
    ControlKind.BOOLEAN_SWITCH -> 0
    ControlKind.BLIND -> 1
    ControlKind.CLIMATE, ControlKind.TEMPERATURE -> 2
    else -> 3
}

private fun roomAliases(roomType: RoomType): List<String> = when (roomType) {
    RoomType.ENTRANCE -> listOf("Entrada", "Hall")
    RoomType.HALLWAY -> listOf("Pasillo", "Distribuidor")
    RoomType.STORAGE -> listOf("Trastero", "Almacen")
    RoomType.BATHROOM -> listOf("Baño", "Aseo")
    RoomType.KITCHEN -> listOf("Cocina")
    RoomType.BEDROOM_1 -> listOf("Habitación 1", "Dormitorio 1", "Habitacion")
    RoomType.DINING_ROOM -> listOf("Comedor", "Salón", "Salon", "Sala")
    RoomType.SUITE -> listOf("Suite", "Dormitorio principal")
}

private fun normalize(value: String): String = Normalizer.normalize(value.lowercase(), Normalizer.Form.NFD)
    .replace("\\p{Mn}+".toRegex(), "")
    .trim()

private fun roomTitle(roomType: RoomType): String = when (roomType) {
    RoomType.ENTRANCE -> "Entrada"
    RoomType.HALLWAY -> "Pasillo"
    RoomType.STORAGE -> "Trastero"
    RoomType.BATHROOM -> "Baño"
    RoomType.KITCHEN -> "Cocina"
    RoomType.BEDROOM_1 -> "Habitación 1"
    RoomType.DINING_ROOM -> "Comedor"
    RoomType.SUITE -> "Suite"
}

@DrawableRes
private fun roomImage(roomType: RoomType): Int? = when (roomType) {
    RoomType.BATHROOM -> R.drawable.room_bathroom
    RoomType.KITCHEN -> R.drawable.room_kitchen
    RoomType.BEDROOM_1 -> R.drawable.room_bedroom_1
    RoomType.DINING_ROOM -> R.drawable.room_dining
    RoomType.SUITE -> R.drawable.room_suite
    RoomType.ENTRANCE -> R.drawable.room_entrance
    RoomType.HALLWAY -> R.drawable.room_hallway
    RoomType.STORAGE -> R.drawable.room_storage
}
