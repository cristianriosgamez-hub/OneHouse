package com.onehouse.app.feature.more

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.onehouse.app.design.FondoInferior
import com.onehouse.app.design.FondoSuperior
import com.onehouse.app.design.OneHouseCard
import com.onehouse.app.design.TextoPrincipal
import com.onehouse.app.design.TextoSecundario
import com.onehouse.app.knx.AppKnxConfigurationRepository
import com.onehouse.app.knx.KnxAddressBook
import com.onehouse.app.knx.KnxCommand
import com.onehouse.app.knx.KnxCommandExecutor
import com.onehouse.app.knx.KnxCommandType
import com.onehouse.app.knx.KnxGroupAddress
import com.onehouse.app.knx.KnxPerformanceMetrics
import com.onehouse.app.knx.KnxStateRepository
import com.onehouse.app.knx.KnxTelegramEvent
import com.onehouse.app.knx.KnxTelegramMonitorRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private data class DiagnosticObject(
    val room: String,
    val name: String,
    val category: String,
    val dpt: String,
    val address: String,
    val kind: String,
    val canWriteBoolean: Boolean
)

@Composable
fun KnxDiagnosticsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val projectRepository = remember { AppKnxConfigurationRepository(context.applicationContext).also(KnxAddressBook::apply) }
    val stateRepository = remember { KnxStateRepository(context.applicationContext) }
    val monitorRepository = remember { KnxTelegramMonitorRepository(context.applicationContext) }
    val executor = remember { KnxCommandExecutor(context.applicationContext) }

    var states by remember { mutableStateOf(stateRepository.snapshot()) }
    var events by remember { mutableStateOf(monitorRepository.recent(20)) }
    var performanceMetrics by remember { mutableStateOf(KnxPerformanceMetrics.snapshot()) }
    var filter by remember { mutableStateOf("") }
    var testingAddress by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        val stateCloseable = stateRepository.observe { states = it }
        val eventCloseable = monitorRepository.observe { events = it.take(20) }
        onDispose {
            stateCloseable.close()
            eventCloseable.close()
            executor.close()
        }
    }

    val objects = remember {
        val globalObjects = KnxAddressBook.entries.map { entry ->
            DiagnosticObject(
                room = entry.room,
                name = entry.name,
                category = entry.category,
                dpt = entry.dpt,
                address = projectRepository.globalAddress(entry.key, entry.defaultAddress),
                kind = entry.kind,
                canWriteBoolean = entry.kind.contains("mando", ignoreCase = true) && entry.dpt.startsWith("1.")
            )
        }
        val roomObjects = projectRepository.loadProject()?.rooms.orEmpty().flatMap { room ->
            room.devices.filter { device -> AppKnxObjectFilter.isVisible(room.name, device) }.flatMap { device ->
                val reads = device.readAddresses.map { address ->
                    DiagnosticObject(
                        room = room.name,
                        name = device.name,
                        category = device.category.displayName,
                        dpt = device.dataPointType.orEmpty().ifBlank { "Sin DPT" },
                        address = address,
                        kind = "Lectura/estado",
                        canWriteBoolean = false
                    )
                }
                val writes = device.writeAddresses.filterNot { it in device.readAddresses }.map { address ->
                    val dpt = device.dataPointType.orEmpty().ifBlank { "Sin DPT" }
                    DiagnosticObject(
                        room = room.name,
                        name = device.name,
                        category = device.category.displayName,
                        dpt = dpt,
                        address = address,
                        kind = "Mando/escritura",
                        canWriteBoolean = dpt.startsWith("1.")
                    )
                }
                reads + writes
            }
        }
        (globalObjects + roomObjects).distinctBy { listOf(it.room, it.name, it.address, it.kind) }
    }

    val invalidAddresses = objects.map { it.address }
        .filterNot(AppKnxConfigurationRepository::isValidGroupAddress)
        .distinct()
    val duplicateAddresses = objects.map { it.address }
        .filter(AppKnxConfigurationRepository::isValidGroupAddress)
        .groupingBy { it }
        .eachCount()
        .filterValues { it > 1 }
        .keys

    val visible = objects.filter { item ->
        filter.isBlank() || listOf(item.room, item.name, item.category, item.dpt, item.address)
            .any { it.contains(filter, ignoreCase = true) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(FondoSuperior, FondoInferior)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "‹",
                    color = TextoPrincipal,
                    fontSize = 42.sp,
                    modifier = Modifier
                        .padding(end = 10.dp)
                        .clickable(onClick = onBack)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text("Diagnóstico KNX", color = TextoPrincipal, style = MaterialTheme.typography.headlineMedium)
                    Text("Diagnóstico exclusivo de la configuración independiente de OneHouse", color = TextoSecundario)
                }
            }

            OneHouseCard {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Resumen", color = TextoPrincipal, fontWeight = FontWeight.Bold)
                    Text("Objetos de OneHouse: ${objects.size}", color = TextoSecundario)
                    Text("Direcciones con respuesta: ${states.keys.count { address -> objects.any { it.address == address } }}", color = TextoSecundario)
                    Text("Telegramas recientes: ${events.size}", color = TextoSecundario)
                    Text(
                        text = when {
                            invalidAddresses.isNotEmpty() -> "Direcciones no válidas: ${invalidAddresses.joinToString()}"
                            duplicateAddresses.isNotEmpty() -> "Direcciones compartidas: ${duplicateAddresses.joinToString()}"
                            else -> "Configuración verificada · sin errores de formato"
                        },
                        color = if (invalidAddresses.isEmpty()) AzulClaro else MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            }

            OneHouseCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Métricas motor KNX", color = TextoPrincipal, fontWeight = FontWeight.Bold)
                    Text(
                        "Medición interna de la ruta actual de estados KNX.",
                        color = TextoSecundario,
                        fontSize = 12.sp
                    )
                    Text("Estados aceptados: ${performanceMetrics.acceptedStateUpdates}", color = TextoSecundario)
                    Text("Estados duplicados: ${performanceMetrics.duplicateStateUpdates}", color = TextoSecundario)
                    Text(
                        "Caché · último / media / máximo: " +
                            "${formatMicros(performanceMetrics.lastCacheUpdateMicros)} / " +
                            "${formatMicros(performanceMetrics.averageCacheUpdateMicros)} / " +
                            formatMicros(performanceMetrics.maxCacheUpdateMicros),
                        color = TextoSecundario
                    )
                    Text(
                        "Observers · último / media / máximo: " +
                            "${formatMicros(performanceMetrics.lastObserverDispatchMicros)} / " +
                            "${formatMicros(performanceMetrics.averageObserverDispatchMicros)} / " +
                            formatMicros(performanceMetrics.maxObserverDispatchMicros),
                        color = TextoSecundario
                    )
                    Text(
                        "Motor paralelo v1.12.1 · eventos: ${performanceMetrics.parallelEventsReceived}",
                        color = AzulClaro,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "EventBus · último / media / máximo: " +
                            "${formatMicros(performanceMetrics.lastParallelDispatchMicros)} / " +
                            "${formatMicros(performanceMetrics.averageParallelDispatchMicros)} / " +
                            formatMicros(performanceMetrics.maxParallelDispatchMicros),
                        color = TextoSecundario
                    )
                    Text(
                        "Túnel v1.12.1.5 · aperturas / reutilizados / conectados: " +
                            "${performanceMetrics.tunnelOpenAttempts} / " +
                            "${performanceMetrics.tunnelReuses} / " +
                            performanceMetrics.tunnelConnectSuccesses,
                        color = AzulClaro,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Código 36: ${performanceMetrics.tunnelRejected36} · cierres: ${performanceMetrics.tunnelDisconnects}",
                        color = TextoSecundario
                    )
                    Text(
                        "Origen aperturas: ${formatSourceCounts(performanceMetrics.tunnelOpenAttemptsBySource)}",
                        color = TextoSecundario,
                        fontSize = 12.sp
                    )
                    Text(
                        "Origen reutilizaciones: ${formatSourceCounts(performanceMetrics.tunnelReusesBySource)}",
                        color = TextoSecundario,
                        fontSize = 12.sp
                    )
                    Text(
                        "Origen código 36: ${formatSourceCounts(performanceMetrics.tunnelRejected36BySource)}",
                        color = if (performanceMetrics.tunnelRejected36 == 0L) TextoSecundario else MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { performanceMetrics = KnxPerformanceMetrics.snapshot() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Actualizar")
                        }
                        OutlinedButton(
                            onClick = {
                                KnxPerformanceMetrics.clear()
                                performanceMetrics = KnxPerformanceMetrics.snapshot()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Limpiar")
                        }
                    }
                }
            }

            OutlinedTextField(
                value = filter,
                onValueChange = { filter = it },
                label = { Text("Buscar estancia, objeto, DPT o X/X/X") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            visible.forEach { item ->
                val state = states[item.address]
                val age = state?.let { formatAge(System.currentTimeMillis() - it.timestampMillis) }
                OneHouseCard {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, color = TextoPrincipal, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                                Text("${item.room} · ${item.category}", color = TextoSecundario, fontSize = 13.sp)
                            }
                            Text(
                                text = if (state != null) "● OK" else "○ SIN RESPUESTA",
                                color = if (state != null) AzulClaro else TextoSecundario,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Text("${item.kind}: ${item.address}", color = TextoPrincipal)
                        Text("DPT: ${item.dpt}", color = TextoSecundario)
                        if (state == null) {
                            Text("Último valor: ---", color = TextoSecundario)
                        } else {
                            Text("Último valor: ${displayValue(state)}", color = TextoPrincipal)
                            Text("Recibido: $age · origen ${state.sourceAddress} · ${state.apci}", color = TextoSecundario, fontSize = 12.sp)
                        }
                        Button(
                            onClick = {
                                executeDiagnosticCommand(
                                    context = context,
                                    executor = executor,
                                    item = item,
                                    type = KnxCommandType.READ,
                                    onStarted = { testingAddress = item.address },
                                    onFinished = { testingAddress = null }
                                )
                            },
                            enabled = testingAddress == null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (testingAddress == item.address) "Leyendo…" else "Probar lectura")
                        }

                        if (item.canWriteBoolean) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        executeDiagnosticCommand(
                                            context = context,
                                            executor = executor,
                                            item = item,
                                            type = KnxCommandType.ON,
                                            onStarted = { testingAddress = item.address },
                                            onFinished = { testingAddress = null }
                                        )
                                    },
                                    enabled = testingAddress == null,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Probar ON")
                                }
                                OutlinedButton(
                                    onClick = {
                                        executeDiagnosticCommand(
                                            context = context,
                                            executor = executor,
                                            item = item,
                                            type = KnxCommandType.OFF,
                                            onStarted = { testingAddress = item.address },
                                            onFinished = { testingAddress = null }
                                        )
                                    },
                                    enabled = testingAddress == null,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Probar OFF")
                                }
                            }
                            Text(
                                "Escritura habilitada únicamente para objetos binarios de mando.",
                                color = TextoSecundario,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Text("Monitor de telegramas", color = TextoPrincipal, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            if (events.isEmpty()) {
                Text("Todavía no hay operaciones KNX registradas.", color = TextoSecundario)
            } else {
                events.forEach { event -> TelegramEventCard(event) }
                OutlinedButton(onClick = { monitorRepository.clear() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Limpiar monitor")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

private fun executeDiagnosticCommand(
    context: android.content.Context,
    executor: KnxCommandExecutor,
    item: DiagnosticObject,
    type: KnxCommandType,
    onStarted: () -> Unit,
    onFinished: () -> Unit
) {
    val address = runCatching { KnxGroupAddress.parse(item.address) }.getOrNull()
    if (address == null) {
        Toast.makeText(context, "Dirección KNX inválida", Toast.LENGTH_SHORT).show()
        return
    }

    onStarted()
    executor.execute(
        KnxCommand(
            type = type,
            destination = address,
            dpt = item.dpt
        ),
        retryReadOnce = false
    ) { result ->
        onFinished()
        val message = when (result) {
            is KnxCommandExecutor.Result.Success -> when (type) {
                KnxCommandType.READ -> result.busValue?.let { "Respuesta: $it" }
                    ?: "Lectura enviada; sin respuesta confirmada"
                KnxCommandType.ON -> "Orden ON enviada y verificada"
                KnxCommandType.OFF -> "Orden OFF enviada y verificada"
                else -> "Operación KNX completada"
            }
            is KnxCommandExecutor.Result.Failure -> result.message
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun TelegramEventCard(event: KnxTelegramEvent) {
    val formatter = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()) }
    OneHouseCard {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                "${formatter.format(Date(event.timestampMillis))} · ${event.status.name}",
                color = TextoPrincipal,
                fontWeight = FontWeight.SemiBold
            )
            Text("${event.direction.name} · ${event.kind.name}", color = TextoSecundario, fontSize = 12.sp)
            event.groupAddress?.let { Text("GA $it${event.value?.let { value -> " · $value" }.orEmpty()}", color = TextoPrincipal) }
            event.detail?.let { Text(it, color = TextoSecundario, fontSize = 12.sp) }
        }
    }
}

private fun displayValue(state: KnxStateRepository.State): String = when {
    state.booleanValue != null -> if (state.booleanValue) "Encendido / 1" else "Apagado / 0"
    !state.rawValue.isNullOrBlank() -> "0x${state.rawValue}"
    else -> "---"
}

private fun formatSourceCounts(values: Map<String, Long>): String =
    if (values.isEmpty()) "—" else values.entries.joinToString(" · ") { (source, count) -> "$source=$count" }

private fun formatMicros(value: Long?): String = value?.let { "$it µs" } ?: "—"

private fun formatAge(milliseconds: Long): String {
    val safe = milliseconds.coerceAtLeast(0L)
    return when {
        safe < 1_000L -> "ahora"
        safe < 60_000L -> "hace ${TimeUnit.MILLISECONDS.toSeconds(safe)} s"
        safe < 3_600_000L -> "hace ${TimeUnit.MILLISECONDS.toMinutes(safe)} min"
        else -> "hace ${TimeUnit.MILLISECONDS.toHours(safe)} h"
    }
}
