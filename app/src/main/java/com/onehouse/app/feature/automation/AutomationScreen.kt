package com.onehouse.app.feature.automation

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
import com.onehouse.app.knx.KnxCommandExecutor
import java.text.DateFormat
import java.util.Date

@Composable
fun AutomationScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember(context) { SharedPreferencesAutomationRepository(context) }
    val engine = remember(context) { AutomationEngine(context) }
    var state by remember { mutableStateOf(repository.load()) }
    var showAdd by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    DisposableEffect(engine) { onDispose { engine.close() } }

    fun persist(newState: AutomationState) {
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
                Text("Automatizaciones", color = TextoPrincipal, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Text("Condiciones KNX con acciones seguras DPT 1.x", color = TextoSecundario, fontSize = 13.sp)
            }
        }

        OneHouseCard {
            Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Motor condicional", color = TextoPrincipal, fontWeight = FontWeight.SemiBold)
                    Text(if (state.globallyEnabled) "Activo" else "Pausado", color = TextoSecundario)
                }
                Switch(state.globallyEnabled, { persist(state.copy(globallyEnabled = it)) })
            }
        }

        Text(
            "Las reglas solo se ejecutan al pulsar ‘Evaluar ahora’ en esta fase. Nunca se inventan valores: si no existe lectura KNX real, la regla no actúa.",
            color = TextoSecundario,
            fontSize = 12.sp
        )

        Button(onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Añadir automatización")
        }

        statusMessage?.let {
            OneHouseCard {
                Text(it, modifier = Modifier.fillMaxWidth().padding(16.dp), color = TextoPrincipal)
            }
        }

        if (state.rules.isEmpty()) {
            OneHouseCard {
                Text("Todavía no hay automatizaciones.", modifier = Modifier.fillMaxWidth().padding(18.dp), color = TextoSecundario)
            }
        }

        state.rules.forEach { rule ->
            OneHouseCard {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(rule.name, color = TextoPrincipal, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Si ${rule.conditionAddress} ${rule.operator.displayName.lowercase()} ${rule.conditionValue}",
                                color = TextoSecundario,
                                fontSize = 12.sp
                            )
                            Text(
                                "Entonces ${rule.action.displayName.lowercase()} ${rule.actionAddress}",
                                color = TextoDesactivado,
                                fontSize = 11.sp
                            )
                        }
                        Switch(rule.enabled, { enabled ->
                            persist(state.copy(rules = state.rules.map { if (it.id == rule.id) it.copy(enabled = enabled) else it }))
                        })
                        Text("×", color = TextoSecundario, fontSize = 26.sp,
                            modifier = Modifier.clickable {
                                persist(state.copy(rules = state.rules.filterNot { it.id == rule.id }))
                            }.padding(start = 10.dp))
                    }

                    rule.lastExecutionMillis?.let {
                        Text(
                            "Última ejecución: ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(it))} · ${rule.executionCount} veces",
                            color = TextoDesactivado,
                            fontSize = 11.sp
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            if (!state.globallyEnabled) {
                                statusMessage = "La programación condicional está pausada."
                            } else if (!rule.enabled) {
                                statusMessage = "La regla ‘${rule.name}’ está desactivada."
                            } else {
                                val evaluation = engine.evaluate(rule)
                                if (!evaluation.matches) {
                                    statusMessage = "${rule.name}: ${evaluation.message}. Valor actual: ${evaluation.currentValue ?: "---"}"
                                } else if (!engine.canExecute(rule)) {
                                    statusMessage = "${rule.name}: condición cumplida, pero está dentro del tiempo de espera."
                                } else {
                                    engine.execute(rule) { result ->
                                        when (result) {
                                            is KnxCommandExecutor.Result.Success -> {
                                                val now = System.currentTimeMillis()
                                                persist(state.copy(rules = state.rules.map {
                                                    if (it.id == rule.id) it.copy(
                                                        lastExecutionMillis = now,
                                                        executionCount = it.executionCount + 1
                                                    ) else it
                                                }))
                                                statusMessage = "${rule.name}: acción enviada correctamente."
                                            }
                                            is KnxCommandExecutor.Result.Failure -> {
                                                statusMessage = "${rule.name}: ${result.message}"
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Evaluar ahora") }
                }
            }
        }
    }

    if (showAdd) AddAutomationDialog(
        onDismiss = { showAdd = false },
        onSave = { rule -> persist(state.copy(rules = state.rules + rule)); showAdd = false }
    )
}

@Composable
private fun AddAutomationDialog(onDismiss: () -> Unit, onSave: (AutomationRule) -> Unit) {
    var name by remember { mutableStateOf("") }
    var conditionAddress by remember { mutableStateOf("") }
    var conditionDpt by remember { mutableStateOf("1.001") }
    var conditionValue by remember { mutableStateOf("1") }
    var actionAddress by remember { mutableStateOf("") }
    var actionDpt by remember { mutableStateOf("1.001") }
    var cooldown by remember { mutableStateOf("5") }
    var operatorIndex by remember { mutableIntStateOf(0) }
    var actionIndex by remember { mutableIntStateOf(0) }

    val parsedCooldown = cooldown.toIntOrNull()
    val valid = name.isNotBlank() &&
        AppKnxConfigurationRepository.isValidGroupAddress(conditionAddress) &&
        AppKnxConfigurationRepository.isValidGroupAddress(actionAddress) &&
        conditionValue.isNotBlank() &&
        parsedCooldown != null && parsedCooldown in 0..1440 &&
        actionDpt.trim().startsWith("1")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva automatización") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nombre") }, singleLine = true)
                Text("Condición", fontWeight = FontWeight.SemiBold)
                OutlinedTextField(conditionAddress, { conditionAddress = it }, label = { Text("Dirección KNX X/X/X") }, singleLine = true)
                OutlinedTextField(conditionDpt, { conditionDpt = it }, label = { Text("DPT de lectura") }, singleLine = true)
                AutomationChoiceRow("Operador", AutomationOperator.values()[operatorIndex].displayName) {
                    operatorIndex = (operatorIndex + 1) % AutomationOperator.values().size
                }
                OutlinedTextField(conditionValue, { conditionValue = it }, label = { Text("Valor esperado") }, singleLine = true)
                Text("Acción", fontWeight = FontWeight.SemiBold)
                OutlinedTextField(actionAddress, { actionAddress = it }, label = { Text("Dirección KNX X/X/X") }, singleLine = true)
                OutlinedTextField(actionDpt, { actionDpt = it }, label = { Text("DPT de escritura (1.x)") }, singleLine = true)
                AutomationChoiceRow("Acción", AutomationAction.values()[actionIndex].displayName) {
                    actionIndex = (actionIndex + 1) % AutomationAction.values().size
                }
                OutlinedTextField(cooldown, { cooldown = it.take(4) }, label = { Text("Espera entre ejecuciones (minutos)") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = {
                onSave(AutomationRule(
                    name = name.trim(),
                    conditionAddress = conditionAddress.trim(),
                    conditionDpt = conditionDpt.trim(),
                    operator = AutomationOperator.values()[operatorIndex],
                    conditionValue = conditionValue.trim(),
                    actionAddress = actionAddress.trim(),
                    actionDpt = actionDpt.trim(),
                    action = AutomationAction.values()[actionIndex],
                    cooldownMinutes = requireNotNull(parsedCooldown)
                ))
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun AutomationChoiceRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .background(AzulClaro.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(13.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextoSecundario)
        Text("$value  ›", color = TextoPrincipal, fontWeight = FontWeight.Medium)
    }
}
