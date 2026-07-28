package com.onehouse.app.feature.programming

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onehouse.app.design.*
import com.onehouse.app.knx.AppKnxConfigurationRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun SolarScheduleScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember(context) { SharedPreferencesSolarScheduleRepository(context) }
    var state by remember { mutableStateOf(repository.load()) }
    var showLocation by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }

    fun persist(newState: SolarScheduleState) {
        state = newState
        repository.save(newState)
    }

    val today = remember(state.latitude, state.longitude) {
        SolarTimeCalculator.calculate(LocalDate.now(), state.latitude, state.longitude)
    }
    val next = SolarTimeCalculator.nextExecution(state)

    Column(
        modifier = Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(FondoSuperior, FondoInferior)))
            .statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("‹", color = TextoPrincipal, fontSize = 40.sp,
                modifier = Modifier.clickable(onClick = onBack).padding(end = 14.dp))
            Column(Modifier.weight(1f)) {
                Text("Amanecer y atardecer", color = TextoPrincipal, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Text("Acciones KNX según la luz solar", color = TextoSecundario, fontSize = 13.sp)
            }
        }

        OneHouseCard {
            Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Programación solar", color = TextoPrincipal, fontWeight = FontWeight.SemiBold)
                    Text(if (state.globallyEnabled) "Activa" else "Pausada", color = TextoSecundario)
                }
                Switch(state.globallyEnabled, { persist(state.copy(globallyEnabled = it)) })
            }
        }

        OneHouseCard(onClick = { showLocation = true }) {
            Column(Modifier.fillMaxWidth().padding(18.dp)) {
                Text("Ubicación", color = TextoSecundario, fontSize = 12.sp)
                Text("%.4f, %.4f".format(Locale.US, state.latitude, state.longitude), color = TextoPrincipal, fontWeight = FontWeight.SemiBold)
                Text("Pulsa para modificar las coordenadas", color = TextoDesactivado, fontSize = 11.sp)
            }
        }

        OneHouseCard {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Hoy", color = TextoSecundario, fontSize = 12.sp)
                Text("Amanecer  ${today.sunrise?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "---"}", color = TextoPrincipal)
                Text("Atardecer  ${today.sunset?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "---"}", color = TextoPrincipal)
            }
        }

        OneHouseCard {
            Column(Modifier.fillMaxWidth().padding(18.dp)) {
                Text("Próxima ejecución", color = TextoSecundario, fontSize = 12.sp)
                Text(
                    next?.let { "${it.first.name} · ${it.second.format(DateTimeFormatter.ofPattern("dd/MM HH:mm"))}" }
                        ?: "Sin ejecuciones pendientes",
                    color = TextoPrincipal,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Button(onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth()) { Text("Añadir acción solar") }

        state.events.forEach { event ->
            OneHouseCard {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(event.name, color = TextoPrincipal, fontWeight = FontWeight.SemiBold)
                        Text("${event.trigger.displayName}${formatOffset(event.offsetMinutes)} · ${event.actionType.displayName}", color = TextoSecundario, fontSize = 12.sp)
                        Text("${event.groupAddress} · DPT ${event.dpt}", color = TextoDesactivado, fontSize = 11.sp)
                    }
                    Switch(event.enabled, { enabled ->
                        persist(state.copy(events = state.events.map { if (it.id == event.id) it.copy(enabled = enabled) else it }))
                    })
                    Text("×", color = TextoSecundario, fontSize = 26.sp,
                        modifier = Modifier.clickable { persist(state.copy(events = state.events.filterNot { it.id == event.id })) }.padding(start = 10.dp))
                }
            }
        }
    }

    if (showLocation) LocationDialog(
        state.latitude,
        state.longitude,
        onDismiss = { showLocation = false },
        onSave = { latitude, longitude -> persist(state.copy(latitude = latitude, longitude = longitude)); showLocation = false }
    )
    if (showAdd) AddSolarScheduleDialog(
        onDismiss = { showAdd = false },
        onSave = { event -> persist(state.copy(events = state.events + event)); showAdd = false }
    )
}

@Composable
private fun LocationDialog(latitude: Double, longitude: Double, onDismiss: () -> Unit, onSave: (Double, Double) -> Unit) {
    var lat by remember { mutableStateOf(latitude.toString()) }
    var lon by remember { mutableStateOf(longitude.toString()) }
    val parsedLat = lat.replace(',', '.').toDoubleOrNull()
    val parsedLon = lon.replace(',', '.').toDoubleOrNull()
    val valid = parsedLat != null && parsedLat in -90.0..90.0 && parsedLon != null && parsedLon in -180.0..180.0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ubicación solar") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedTextField(lat, { lat = it }, label = { Text("Latitud") }, singleLine = true)
                OutlinedTextField(lon, { lon = it }, label = { Text("Longitud") }, singleLine = true)
                Text("Valores iniciales: L’Hospitalet de Llobregat", fontSize = 12.sp, color = TextoSecundario)
            }
        },
        confirmButton = { TextButton(enabled = valid, onClick = { onSave(requireNotNull(parsedLat), requireNotNull(parsedLon)) }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun AddSolarScheduleDialog(onDismiss: () -> Unit, onSave: (SolarScheduleEvent) -> Unit) {
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var dpt by remember { mutableStateOf("1.001") }
    var offset by remember { mutableStateOf("0") }
    var value by remember { mutableStateOf("") }
    var triggerIndex by remember { mutableIntStateOf(0) }
    var typeIndex by remember { mutableIntStateOf(0) }
    var actionIndex by remember { mutableIntStateOf(0) }
    val types = WeeklyTargetType.values()
    val actions = solarActionsFor(types[typeIndex])
    val selectedAction = actions[actionIndex.coerceIn(actions.indices)]
    val parsedOffset = offset.toIntOrNull()
    val valid = name.isNotBlank() && AppKnxConfigurationRepository.isValidGroupAddress(address) && parsedOffset != null && parsedOffset in -180..180

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva acción solar") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nombre") }, singleLine = true)
                SolarChoiceRow("Momento", SolarTrigger.values()[triggerIndex].displayName) { triggerIndex = (triggerIndex + 1) % SolarTrigger.values().size }
                OutlinedTextField(offset, { offset = it.take(4) }, label = { Text("Desplazamiento en minutos (-180 a 180)") }, singleLine = true)
                SolarChoiceRow("Tipo", types[typeIndex].displayName) {
                    typeIndex = (typeIndex + 1) % types.size
                    actionIndex = 0
                    dpt = when (types[typeIndex]) {
                        WeeklyTargetType.LIGHT -> "1.001"
                        WeeklyTargetType.CLIMATE -> "9.001"
                        WeeklyTargetType.BLIND -> "1.008"
                    }
                }
                SolarChoiceRow("Acción", selectedAction.displayName) { actionIndex = (actionIndex + 1) % actions.size }
                OutlinedTextField(address, { address = it }, label = { Text("Dirección KNX X/X/X") }, singleLine = true)
                OutlinedTextField(dpt, { dpt = it }, label = { Text("DPT") }, singleLine = true)
                if (selectedAction == WeeklyActionType.CLIMATE_SETPOINT || selectedAction == WeeklyActionType.BLIND_POSITION) {
                    OutlinedTextField(value, { value = it }, label = { Text(if (selectedAction == WeeklyActionType.CLIMATE_SETPOINT) "Temperatura" else "Posición 0–100") }, singleLine = true)
                }
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = {
                onSave(SolarScheduleEvent(
                    name = name.trim(), trigger = SolarTrigger.values()[triggerIndex], offsetMinutes = requireNotNull(parsedOffset),
                    targetType = types[typeIndex], actionType = selectedAction, groupAddress = address.trim(), dpt = dpt.trim(), value = value.trim().ifBlank { null }
                ))
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun SolarChoiceRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(AzulClaro.copy(alpha = 0.08f), RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(13.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextoSecundario)
        Text("$value  ›", color = TextoPrincipal, fontWeight = FontWeight.Medium)
    }
}

private fun solarActionsFor(type: WeeklyTargetType): Array<WeeklyActionType> = when (type) {
    WeeklyTargetType.LIGHT -> arrayOf(WeeklyActionType.ON, WeeklyActionType.OFF)
    WeeklyTargetType.CLIMATE -> arrayOf(WeeklyActionType.ON, WeeklyActionType.OFF, WeeklyActionType.CLIMATE_SETPOINT)
    WeeklyTargetType.BLIND -> arrayOf(WeeklyActionType.BLIND_UP, WeeklyActionType.BLIND_DOWN, WeeklyActionType.BLIND_POSITION)
}

private fun formatOffset(minutes: Int): String = when {
    minutes == 0 -> ""
    minutes > 0 -> " +${minutes} min"
    else -> " ${minutes} min"
}
