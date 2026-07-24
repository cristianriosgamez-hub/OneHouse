package com.onehouse.app.feature.importproject

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onehouse.app.design.AzulClaro
import com.onehouse.app.design.AzulOneHouse
import com.onehouse.app.design.FondoInferior
import com.onehouse.app.design.FondoSuperior
import com.onehouse.app.design.FondoTarjeta
import com.onehouse.app.design.TextoPrincipal
import com.onehouse.app.design.TextoSecundario
import com.onehouse.app.device.ImportedKnxDevice
import com.onehouse.app.knx.KnxCommand
import com.onehouse.app.knx.KnxCommandExecutor
import com.onehouse.app.knx.KnxCommandType
import com.onehouse.app.knx.KnxCommunicationStatus
import com.onehouse.app.knx.KnxDeviceState
import com.onehouse.app.knx.KnxDeviceStateRepository
import com.onehouse.app.knx.KnxTelegramEvent
import com.onehouse.app.knx.KnxTelegramMonitorRepository
import com.onehouse.app.knx.KnxSessionStatisticsRepository
import com.onehouse.app.knx.KnxStateRepository
import com.onehouse.app.knx.communicationStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DeviceControlScreen(device: ImportedKnxDevice, onBack: () -> Unit) {
    val context = LocalContext.current
    val executor = remember(context.applicationContext) { KnxCommandExecutor(context.applicationContext) }
    val stateRepository = remember(context.applicationContext) {
        KnxDeviceStateRepository(context.applicationContext)
    }
    val busStateRepository = remember(context.applicationContext) {
        KnxStateRepository(context.applicationContext)
    }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val monitorRepository = remember(context.applicationContext) {
        KnxTelegramMonitorRepository(context.applicationContext)
    }
    val statisticsRepository = remember(context.applicationContext) {
        KnxSessionStatisticsRepository(context.applicationContext)
    }
    var telegramEvents by remember { mutableStateOf(emptyList<KnxTelegramEvent>()) }
    var isBusy by remember { mutableStateOf(false) }
    var deviceState by remember(device.id) { mutableStateOf(stateRepository.get(device.id)) }
    var status by remember { mutableStateOf("Preparado para enviar al bus KNX") }
    var statistics by remember { mutableStateOf(statisticsRepository.snapshot()) }

    DisposableEffect(
        executor,
        stateRepository,
        busStateRepository,
        monitorRepository,
        device.id
    ) {
        val addresses = (device.writeAddresses + device.readAddresses)
            .map { it.toString() }
            .toSet()

        val stateObservation = stateRepository.observe(device.id) { newState ->
            mainHandler.post {
                deviceState = newState
            }
        }

        val busStateObservation = busStateRepository.observe { states ->
            val latestBusState = addresses
                .mapNotNull(states::get)
                .maxByOrNull { it.timestampMillis }
                ?: return@observe

            val displayValue = latestBusState.booleanValue?.let { value ->
                if (value) "Encendido" else "Apagado"
            } ?: latestBusState.rawValue
            ?: return@observe

            mainHandler.post {
                if (
                    latestBusState.timestampMillis > deviceState.updatedAtMillis ||
                    deviceState.value != displayValue ||
                    deviceState.source != KnxDeviceState.Source.BUS_RESPONSE
                ) {
                    stateRepository.updateFromBus(device.id, displayValue)
                    status = buildString {
                        append("Estado actualizado desde el bus: ")
                        append(displayValue)
                        if (latestBusState.sourceAddress.isNotBlank()) {
                            append(" · origen ")
                            append(latestBusState.sourceAddress)
                        }
                    }
                    statistics = statisticsRepository.snapshot()
                }
            }
        }

        val monitorObservation = monitorRepository.observe { events ->
            mainHandler.post {
                telegramEvents = events
                    .filter { it.groupAddress in addresses }
                    .take(8)
            }
        }

        onDispose {
            stateObservation.close()
            busStateObservation.close()
            monitorObservation.close()
            executor.close()
        }
    }

    fun run(command: KnxCommand, toggleValue: Boolean? = null) {
        if (isBusy) return
        isBusy = true
        status = "Conectando y enviando ${command.type.displayName.lowercase()}…"
        executor.execute(command, toggleValue) { result ->
            isBusy = false
            statistics = statisticsRepository.snapshot()
            when (result) {
                is KnxCommandExecutor.Result.Success -> {
                    when (command.type) {
                        KnxCommandType.ON -> if (result.busValue == null) stateRepository.updateFromLocalCommand(device.id, "Encendido")
                        KnxCommandType.OFF -> if (result.busValue == null) stateRepository.updateFromLocalCommand(device.id, "Apagado")
                        KnxCommandType.TOGGLE -> if (result.busValue == null) stateRepository.updateFromLocalCommand(
                            device.id,
                            if (toggleValue == true) "Encendido" else "Apagado"
                        )
                        else -> Unit
                    }
                    status = when (command.type) {
                        KnxCommandType.READ -> if (result.busValue != null) {
                            "Estado recibido del bus: ${result.busValue}"
                        } else {
                            "Solicitud confirmada, pero el dispositivo no respondió"
                        }
                        else -> "Telegrama ${command.type.displayName.lowercase()} confirmado"
                    }
                }
                is KnxCommandExecutor.Result.Failure -> status = result.message
            }
        }
    }

    val onCommand = device.commands.firstOrNull { it.type == KnxCommandType.ON }
    val offCommand = device.commands.firstOrNull { it.type == KnxCommandType.OFF }
    val toggleCommand = device.commands.firstOrNull { it.type == KnxCommandType.TOGGLE }
    val readCommand = device.commands.firstOrNull { it.type == KnxCommandType.READ }
    val assumedOn = deviceState.value == "Encendido"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(FondoSuperior, FondoInferior)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 14.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "‹",
                    color = AzulClaro,
                    fontSize = 40.sp,
                    modifier = Modifier.clickable(onClick = onBack).padding(end = 12.dp)
                )
                Column {
                    Text(device.name, color = TextoPrincipal, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(device.roomName, color = TextoSecundario, fontSize = 13.sp)
                }
            }

            Surface(color = FondoTarjeta, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${device.iconGlyph}  Control KNX", color = AzulClaro, fontWeight = FontWeight.Bold)
                    ControlDetail("Tipo", device.controlKind.displayName)
                    ControlDetail("Dirección de escritura", device.primaryWriteAddress?.toString() ?: "—")
                    ControlDetail("Dirección de lectura", device.primaryReadAddress?.toString() ?: "—")
                    ControlDetail("DPT resuelto", device.resolvedDpt)
                }
            }

            Surface(color = FondoTarjeta, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Estado del dispositivo", color = AzulClaro, fontWeight = FontWeight.Bold)
                    Text(
                        deviceState.displayValue,
                        color = TextoPrincipal,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    val communicationStatus = deviceState.communicationStatus()
                    Text(
                        when (communicationStatus) {
                            KnxCommunicationStatus.ONLINE -> "● ${communicationStatus.displayName}"
                            KnxCommunicationStatus.PENDING -> "● ${communicationStatus.displayName}"
                            KnxCommunicationStatus.STALE -> "● ${communicationStatus.displayName}; solicita una lectura"
                            KnxCommunicationStatus.NEVER_READ -> "○ ${communicationStatus.displayName}"
                        },
                        color = when (communicationStatus) {
                            KnxCommunicationStatus.ONLINE -> AzulClaro
                            KnxCommunicationStatus.PENDING, KnxCommunicationStatus.STALE -> TextoSecundario
                            KnxCommunicationStatus.NEVER_READ -> TextoSecundario
                        },
                        fontSize = 12.sp
                    )
                    if (deviceState.updatedAtMillis > 0L) {
                        val updatedAt = remember(deviceState.updatedAtMillis) {
                            SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(deviceState.updatedAtMillis))
                        }
                        Text("Última actualización: $updatedAt", color = TextoSecundario, fontSize = 11.sp)
                    }
                }
            }

            if (onCommand != null && offCommand != null) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { run(onCommand) },
                        enabled = !isBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = AzulOneHouse),
                        modifier = Modifier.weight(1f)
                    ) { Text("Encender") }
                    OutlinedButton(onClick = { run(offCommand) }, enabled = !isBusy, modifier = Modifier.weight(1f)) {
                        Text("Apagar")
                    }
                }
            } else if (toggleCommand != null) {
                Button(
                    onClick = { run(toggleCommand, !assumedOn) },
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AzulOneHouse)
                ) { Text(if (assumedOn) "Apagar" else "Encender") }
            }

            readCommand?.let { command ->
                OutlinedButton(onClick = { run(command) }, enabled = !isBusy, modifier = Modifier.fillMaxWidth()) {
                    Text("Solicitar lectura de estado")
                }
            }

            Surface(color = FondoTarjeta, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (isBusy) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 12.dp), color = AzulClaro)
                    }
                    Text(status, color = if (isBusy) AzulClaro else TextoSecundario, style = MaterialTheme.typography.bodyMedium)
                }
            }

            KnxDiagnosticsCard(
                statistics = statistics,
                onRefresh = { statistics = statisticsRepository.snapshot() },
                onCopy = {
                    val report = buildDiagnosticReport(
                        device = device,
                        statistics = statisticsRepository.snapshot(),
                        events = monitorRepository.exportText()
                    )
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Diagnóstico KNX OneHouse", report))
                    status = "Diagnóstico KNX copiado al portapapeles"
                }
            )

            TelegramMonitorCard(
                events = telegramEvents,
                onClear = { monitorRepository.clear() }
            )

            if (onCommand == null && offCommand == null && toggleCommand == null) {
                Text(
                    "En esta entrega el envío real está habilitado para luces e interruptores DPT 1.x. El resto de controles se incorporará con su codificación DPT específica.",
                    color = TextoSecundario,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun KnxDiagnosticsCard(
    statistics: KnxSessionStatisticsRepository.Snapshot,
    onRefresh: () -> Unit,
    onCopy: () -> Unit
) {
    Surface(color = FondoTarjeta, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Diagnóstico de sesión", color = AzulClaro, fontWeight = FontWeight.Bold)
            ControlDetail("Conexiones correctas", statistics.connections.toString())
            ControlDetail("Errores de conexión", statistics.connectionErrors.toString())
            ControlDetail("Telegramas enviados", statistics.telegramsSent.toString())
            ControlDetail("ACK del gateway", statistics.gatewayAcks.toString())
            ControlDetail("Telegramas del bus", statistics.busTelegrams.toString())
            ControlDetail("Errores de operación", statistics.operationErrors.toString())
            ControlDetail("Retransmisiones", statistics.retransmissions.toString())
            ControlDetail("Último ACK", statistics.lastAckMillis?.let { "$it ms" } ?: "—")
            ControlDetail("Latencia ACK media", statistics.averageAckMillis?.let { "$it ms" } ?: "—")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onRefresh, modifier = Modifier.weight(1f)) { Text("Actualizar") }
                Button(
                    onClick = onCopy,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = AzulOneHouse)
                ) { Text("Copiar diagnóstico") }
            }
        }
    }
}

private fun buildDiagnosticReport(
    device: ImportedKnxDevice,
    statistics: KnxSessionStatisticsRepository.Snapshot,
    events: String
): String = buildString {
    appendLine("OneHouse v1.5.0 - Diagnóstico KNX")
    appendLine("Dispositivo: ${device.name}")
    appendLine("Habitación: ${device.roomName}")
    appendLine("Escritura: ${device.primaryWriteAddress ?: "—"}")
    appendLine("Lectura: ${device.primaryReadAddress ?: "—"}")
    appendLine("DPT: ${device.resolvedDpt}")
    appendLine()
    appendLine("Conexiones: ${statistics.connections}")
    appendLine("Errores de conexión: ${statistics.connectionErrors}")
    appendLine("Telegramas TX: ${statistics.telegramsSent}")
    appendLine("ACK gateway: ${statistics.gatewayAcks}")
    appendLine("Telegramas RX bus: ${statistics.busTelegrams}")
    appendLine("Errores de operación: ${statistics.operationErrors}")
    appendLine("ACK ignorados: ${statistics.ignoredAcks}")
    appendLine("Paquetes inválidos: ${statistics.invalidPackets}")
    appendLine("Duplicados RX: ${statistics.duplicateIncoming}")
    appendLine("Retransmisiones: ${statistics.retransmissions}")
    appendLine("Último ACK: ${statistics.lastAckMillis?.let { "$it ms" } ?: "—"}")
    appendLine("ACK medio: ${statistics.averageAckMillis?.let { "$it ms" } ?: "—"}")
    appendLine()
    appendLine("Historial KNX:")
    append(events.ifBlank { "Sin eventos registrados" })
}

@Composable
private fun ControlDetail(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, color = TextoSecundario, modifier = Modifier.weight(1f), fontSize = 12.sp)
        Text(value, color = TextoPrincipal, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    }
}


@Composable
private fun TelegramMonitorCard(
    events: List<KnxTelegramEvent>,
    onClear: () -> Unit
) {
    Surface(color = FondoTarjeta, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Monitor KNX", color = AzulClaro, fontWeight = FontWeight.Bold)
                    Text("Últimas operaciones de este dispositivo", color = TextoSecundario, fontSize = 11.sp)
                }
                if (events.isNotEmpty()) {
                    Text("Limpiar", color = AzulClaro, fontSize = 12.sp, modifier = Modifier.clickable(onClick = onClear))
                }
            }
            if (events.isEmpty()) {
                Text("Todavía no hay telegramas registrados.", color = TextoSecundario, fontSize = 12.sp)
            } else {
                events.forEach { event -> TelegramEventRow(event) }
            }
        }
    }
}

@Composable
private fun TelegramEventRow(event: KnxTelegramEvent) {
    val time = remember(event.timestampMillis) {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(event.timestampMillis))
    }
    val operation = when (event.kind) {
        KnxTelegramEvent.Kind.CONNECT -> "Conexión"
        KnxTelegramEvent.Kind.GROUP_VALUE_READ -> "GroupValueRead"
        KnxTelegramEvent.Kind.GROUP_VALUE_WRITE -> "GroupValueWrite"
        KnxTelegramEvent.Kind.GROUP_VALUE_RESPONSE -> "GroupValueResponse"
        KnxTelegramEvent.Kind.DISCONNECT -> "Desconexión"
    }
    val statusText = when (event.status) {
        KnxTelegramEvent.Status.PENDING -> "pendiente"
        KnxTelegramEvent.Status.CONFIRMED -> "confirmado"
        KnxTelegramEvent.Status.RECEIVED -> "recibido"
        KnxTelegramEvent.Status.ERROR -> "error"
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(time, color = TextoSecundario, fontSize = 11.sp, modifier = Modifier.weight(0.8f))
            Text(operation, color = TextoPrincipal, fontSize = 11.sp, modifier = Modifier.weight(1.5f))
            Text(statusText, color = if (event.status == KnxTelegramEvent.Status.ERROR) MaterialTheme.colorScheme.error else AzulClaro, fontSize = 11.sp)
        }
        Text(
            listOfNotNull(event.groupAddress, event.value?.let { "valor $it" }, event.detail).joinToString(" · "),
            color = TextoSecundario,
            fontSize = 10.sp
        )
    }
}
