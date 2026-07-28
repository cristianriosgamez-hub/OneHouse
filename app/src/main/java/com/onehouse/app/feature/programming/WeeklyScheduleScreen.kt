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
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun WeeklyScheduleScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember(context) { SharedPreferencesWeeklyScheduleRepository(context) }
    var state by remember { mutableStateOf(repository.load()) }
    var showAdd by remember { mutableStateOf(false) }

    fun persist(newState: WeeklyScheduleState) {
        state = newState
        repository.save(newState)
    }

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
                Text("Horarios semanales", color = TextoPrincipal, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Text("Luces, climatización y persianas", color = TextoSecundario, fontSize = 13.sp)
            }
        }

        OneHouseCard {
            Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Programación automática", color = TextoPrincipal, fontWeight = FontWeight.SemiBold)
                    Text(if (state.globallyEnabled) "Activa" else "Pausada", color = TextoSecundario)
                }
                Switch(checked = state.globallyEnabled, onCheckedChange = { persist(state.copy(globallyEnabled = it)) })
            }
        }

        val next = WeeklyScheduleEngine.nextExecution(state)
        OneHouseCard {
            Column(Modifier.fillMaxWidth().padding(18.dp)) {
                Text("Próxima ejecución", color = TextoSecundario, fontSize = 12.sp)
                Text(
                    next?.let { "${it.event.name} · ${it.event.dayOfWeek.spanishName()} ${it.event.formattedTime()}" }
                        ?: "Sin ejecuciones pendientes",
                    color = TextoPrincipal,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Button(onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Añadir horario")
        }

        DayOfWeek.values().forEach { day ->
            val dayEvents = state.events.filter { it.dayOfWeek == day }
            if (dayEvents.isNotEmpty()) {
                Text(day.spanishName(), color = AzulClaro, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                dayEvents.forEach { event ->
                    OneHouseCard {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("${event.formattedTime()} · ${event.name}", color = TextoPrincipal, fontWeight = FontWeight.SemiBold)
                                Text("${event.targetType.displayName} · ${event.actionType.displayName}", color = TextoSecundario, fontSize = 12.sp)
                                Text("${event.groupAddress} · DPT ${event.dpt}", color = TextoDesactivado, fontSize = 11.sp)
                            }
                            Switch(
                                checked = event.enabled,
                                onCheckedChange = { enabled ->
                                    persist(state.copy(events = state.events.map { if (it.id == event.id) it.copy(enabled = enabled) else it }))
                                }
                            )
                            Text("×", color = TextoSecundario, fontSize = 26.sp,
                                modifier = Modifier.clickable { persist(state.copy(events = state.events.filterNot { it.id == event.id })) }.padding(start = 10.dp))
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddWeeklyScheduleDialog(
            onDismiss = { showAdd = false },
            onSave = { event -> persist(state.copy(events = state.events + event)); showAdd = false }
        )
    }
}

@Composable
private fun AddWeeklyScheduleDialog(onDismiss: () -> Unit, onSave: (WeeklyScheduleEvent) -> Unit) {
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var dpt by remember { mutableStateOf("1.001") }
    var hour by remember { mutableStateOf("08") }
    var minute by remember { mutableStateOf("00") }
    var value by remember { mutableStateOf("") }
    var dayIndex by remember { mutableIntStateOf(0) }
    var typeIndex by remember { mutableIntStateOf(0) }
    var actionIndex by remember { mutableIntStateOf(0) }

    val types = WeeklyTargetType.values()
    val actions = actionsFor(types[typeIndex])
    val selectedAction = actions[actionIndex.coerceIn(actions.indices)]
    val parsedHour = hour.toIntOrNull()
    val parsedMinute = minute.toIntOrNull()
    val valid = name.isNotBlank() &&
        AppKnxConfigurationRepository.isValidGroupAddress(address) &&
        parsedHour != null && parsedHour in 0..23 &&
        parsedMinute != null && parsedMinute in 0..59

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo horario semanal") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nombre") }, singleLine = true)
                ChoiceRow("Día", DayOfWeek.values()[dayIndex].spanishName()) { dayIndex = (dayIndex + 1) % 7 }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(hour, { hour = it.take(2) }, label = { Text("Hora") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(minute, { minute = it.take(2) }, label = { Text("Minuto") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                ChoiceRow("Tipo", types[typeIndex].displayName) {
                    typeIndex = (typeIndex + 1) % types.size
                    actionIndex = 0
                    dpt = when (types[typeIndex]) {
                        WeeklyTargetType.LIGHT -> "1.001"
                        WeeklyTargetType.CLIMATE -> "9.001"
                        WeeklyTargetType.BLIND -> "1.008"
                    }
                }
                ChoiceRow("Acción", selectedAction.displayName) { actionIndex = (actionIndex + 1) % actions.size }
                OutlinedTextField(address, { address = it }, label = { Text("Dirección KNX X/X/X") }, singleLine = true)
                OutlinedTextField(dpt, { dpt = it }, label = { Text("DPT") }, singleLine = true)
                if (selectedAction == WeeklyActionType.CLIMATE_SETPOINT || selectedAction == WeeklyActionType.BLIND_POSITION) {
                    OutlinedTextField(value, { value = it }, label = { Text(if (selectedAction == WeeklyActionType.CLIMATE_SETPOINT) "Temperatura" else "Posición 0–100") }, singleLine = true)
                }
                if (address.isNotBlank() && !AppKnxConfigurationRepository.isValidGroupAddress(address)) {
                    Text("La dirección KNX no es válida", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = {
                onSave(WeeklyScheduleEvent(
                    name = name.trim(),
                    dayOfWeek = DayOfWeek.values()[dayIndex],
                    hour = requireNotNull(parsedHour), minute = requireNotNull(parsedMinute),
                    targetType = types[typeIndex], actionType = selectedAction,
                    groupAddress = address.trim(), dpt = dpt.trim(), value = value.trim().ifBlank { null }
                ))
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun ChoiceRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(AzulClaro.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick).padding(13.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextoSecundario)
        Text("$value  ›", color = TextoPrincipal, fontWeight = FontWeight.Medium)
    }
}

private fun actionsFor(type: WeeklyTargetType): Array<WeeklyActionType> = when (type) {
    WeeklyTargetType.LIGHT -> arrayOf(WeeklyActionType.ON, WeeklyActionType.OFF)
    WeeklyTargetType.CLIMATE -> arrayOf(WeeklyActionType.ON, WeeklyActionType.OFF, WeeklyActionType.CLIMATE_SETPOINT)
    WeeklyTargetType.BLIND -> arrayOf(WeeklyActionType.BLIND_UP, WeeklyActionType.BLIND_DOWN, WeeklyActionType.BLIND_POSITION)
}

private fun DayOfWeek.spanishName(): String = getDisplayName(TextStyle.FULL, Locale("es", "ES"))
    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "ES")) else it.toString() }
