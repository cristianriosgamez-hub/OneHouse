package com.onehouse.app.feature.scenes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.onehouse.app.knx.AppKnxConfigurationRepository
import com.onehouse.app.design.FondoInferior
import com.onehouse.app.design.FondoSuperior
import com.onehouse.app.design.OneHouseCard
import com.onehouse.app.design.TextoDesactivado
import com.onehouse.app.design.TextoPrincipal
import com.onehouse.app.design.TextoSecundario
import java.text.DateFormat
import java.util.Date

@Composable
fun ScenesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { SharedPreferencesSceneRepository(context) }
    val engine = remember { SceneExecutionEngine(context) }
    var state by remember { mutableStateOf(repository.load()) }
    var showAddScene by remember { mutableStateOf(false) }
    var addingActionToSceneId by remember { mutableStateOf<Long?>(null) }
    var previewScene by remember { mutableStateOf<SmartScene?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    DisposableEffect(engine) { onDispose { engine.close() } }

    fun persist(newState: SceneState) {
        state = newState
        repository.save(newState)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(FondoSuperior, FondoInferior)))
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "‹",
                color = TextoPrincipal,
                fontSize = 40.sp,
                modifier = Modifier.clickable(onClick = onBack).padding(end = 14.dp)
            )
            Column(Modifier.weight(1f)) {
                Text("Escenas inteligentes", color = TextoPrincipal, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Text("Varias acciones KNX con una sola pulsación", color = TextoSecundario, fontSize = 13.sp)
            }
        }

        Text(
            "Las escenas admiten luces DPT 1.x, movimiento y posición de persianas, y temperatura objetivo de climatización. Las acciones se ejecutan en orden.",
            color = TextoSecundario,
            fontSize = 12.sp
        )

        Button(onClick = { showAddScene = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Crear escena")
        }

        message?.let {
            OneHouseCard { Text(it, modifier = Modifier.fillMaxWidth().padding(16.dp), color = TextoPrincipal) }
        }

        if (state.scenes.isEmpty()) {
            OneHouseCard {
                Text("Todavía no hay escenas.", modifier = Modifier.fillMaxWidth().padding(18.dp), color = TextoSecundario)
            }
        }

        state.scenes.forEachIndexed { index, scene ->
            OneHouseCard {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(scene.name, color = TextoPrincipal, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                            if (scene.description.isNotBlank()) {
                                Text(scene.description, color = TextoSecundario, fontSize = 12.sp)
                            }
                            Text(
                                "${scene.actions.size} acciones · ${if (scene.stopOnError) "detener al fallar" else "continuar si falla"}",
                                color = TextoDesactivado,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = scene.enabled,
                            onCheckedChange = { enabled ->
                                persist(state.copy(scenes = state.scenes.map { if (it.id == scene.id) it.copy(enabled = enabled) else it }))
                            }
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Mostrar en Mi Hogar", color = TextoPrincipal, fontSize = 13.sp)
                            Text("Puedes guardar hasta 4 escenas favoritas", color = TextoDesactivado, fontSize = 11.sp)
                        }
                        Switch(
                            checked = scene.favorite,
                            onCheckedChange = { favorite ->
                                val favoriteCount = state.scenes.count { it.favorite }
                                if (favorite && !scene.favorite && favoriteCount >= 4) {
                                    message = "Solo puedes guardar 4 escenas favoritas."
                                } else {
                                    persist(state.copy(scenes = state.scenes.map {
                                        if (it.id == scene.id) it.copy(favorite = favorite) else it
                                    }))
                                }
                            }
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Confirmar desde Mi Hogar", color = TextoPrincipal, fontSize = 13.sp)
                            Text("Pide confirmación antes de ejecutar esta escena favorita", color = TextoDesactivado, fontSize = 11.sp)
                        }
                        Switch(
                            checked = scene.requireConfirmation,
                            enabled = scene.favorite,
                            onCheckedChange = { requireConfirmation ->
                                persist(state.copy(scenes = state.scenes.map {
                                    if (it.id == scene.id) it.copy(requireConfirmation = requireConfirmation) else it
                                }))
                            }
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Detener escena si una acción falla", color = TextoPrincipal, fontSize = 13.sp)
                            Text("Desactívalo para intentar ejecutar el resto de acciones", color = TextoDesactivado, fontSize = 11.sp)
                        }
                        Switch(
                            checked = scene.stopOnError,
                            onCheckedChange = { stopOnError ->
                                persist(state.copy(scenes = state.scenes.map {
                                    if (it.id == scene.id) it.copy(stopOnError = stopOnError) else it
                                }))
                            }
                        )
                    }

                    scene.lastExecutionMillis?.let {
                        Text(
                            "Última ejecución: ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(it))} · ${scene.executionCount} veces",
                            color = TextoDesactivado,
                            fontSize = 11.sp
                        )
                    }

                    scene.actions.forEach { action ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                buildString {
                                    append("• ${action.name}: ${action.valueDescription} (${action.groupAddress} · DPT ${action.dpt})")
                                    if (action.delayAfterMillis > 0L) append(" · pausa ${action.delayAfterMillis / 1000.0} s")
                                },
                                modifier = Modifier.weight(1f),
                                color = TextoSecundario,
                                fontSize = 12.sp
                            )
                            Text(
                                "×",
                                color = TextoSecundario,
                                fontSize = 22.sp,
                                modifier = Modifier.clickable {
                                    persist(state.copy(scenes = state.scenes.map {
                                        if (it.id == scene.id) it.copy(actions = it.actions.filterNot { item -> item.id == action.id }) else it
                                    }))
                                }.padding(start = 8.dp)
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { addingActionToSceneId = scene.id },
                            modifier = Modifier.weight(1f)
                        ) { Text("Añadir acción") }
                        OutlinedButton(
                            onClick = { previewScene = scene },
                            modifier = Modifier.weight(1f)
                        ) { Text("Vista previa") }
                    }

                    Button(
                        enabled = scene.enabled && scene.actions.isNotEmpty(),
                        onClick = {
                            engine.execute(scene) { result ->
                                if (result.errorMessage == null) {
                                    val now = System.currentTimeMillis()
                                    persist(state.copy(scenes = state.scenes.map {
                                        if (it.id == scene.id) it.copy(
                                            lastExecutionMillis = now,
                                            executionCount = it.executionCount + 1
                                        ) else it
                                    }))
                                    message = "${scene.name}: ${result.successfulActions}/${result.totalActions} acciones ejecutadas."
                                } else if (!scene.stopOnError && result.successfulActions + result.failedActions == result.totalActions) {
                                    message = "${scene.name}: ${result.successfulActions} correctas y ${result.failedActions} con error. Primer error: ${result.errorMessage}"
                                } else {
                                    message = "${scene.name}: ${result.successfulActions}/${result.totalActions}. ${result.errorMessage}"
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Ejecutar escena") }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = {
                                val copy = scene.copy(
                                    id = System.currentTimeMillis(),
                                    name = "${scene.name} (copia)",
                                    lastExecutionMillis = null,
                                    executionCount = 0,
                                    favorite = false,
                                    requireConfirmation = false,
                                    actions = scene.actions.map { it.copy(id = System.nanoTime()) }
                                )
                                persist(state.copy(scenes = state.scenes + copy))
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Duplicar") }
                        TextButton(
                            enabled = index > 0,
                            onClick = {
                                val mutable = state.scenes.toMutableList()
                                val item = mutable.removeAt(index)
                                mutable.add(index - 1, item)
                                persist(state.copy(scenes = mutable))
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Subir") }
                        TextButton(
                            enabled = index < state.scenes.lastIndex,
                            onClick = {
                                val mutable = state.scenes.toMutableList()
                                val item = mutable.removeAt(index)
                                mutable.add(index + 1, item)
                                persist(state.copy(scenes = mutable))
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Bajar") }
                        TextButton(
                            onClick = { persist(state.copy(scenes = state.scenes.filterNot { it.id == scene.id })) },
                            modifier = Modifier.weight(1f)
                        ) { Text("Eliminar") }
                    }
                }
            }
        }
    }

    if (showAddScene) {
        AddSceneDialog(
            onDismiss = { showAddScene = false },
            onSave = { scene -> persist(state.copy(scenes = state.scenes + scene)); showAddScene = false }
        )
    }

    addingActionToSceneId?.let { sceneId ->
        AddSceneActionDialog(
            onDismiss = { addingActionToSceneId = null },
            onSave = { action ->
                persist(state.copy(scenes = state.scenes.map {
                    if (it.id == sceneId) it.copy(actions = it.actions + action) else it
                }))
                addingActionToSceneId = null
            }
        )
    }

    previewScene?.let { scene ->
        AlertDialog(
            onDismissRequest = { previewScene = null },
            title = { Text(scene.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (scene.actions.isEmpty()) Text("La escena no contiene acciones.")
                    scene.actions.forEachIndexed { index, action ->
                        Text(buildString {
                            append("${index + 1}. ${action.name}: ${action.valueDescription} · ${action.groupAddress} · DPT ${action.dpt}")
                            if (action.delayAfterMillis > 0L) append(" · pausa ${action.delayAfterMillis / 1000.0} s")
                        })
                    }
                }
            },
            confirmButton = { TextButton(onClick = { previewScene = null }) { Text("Cerrar") } }
        )
    }
}

@Composable
private fun AddSceneDialog(onDismiss: () -> Unit, onSave: (SmartScene) -> Unit) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva escena") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nombre") }, singleLine = true)
                OutlinedTextField(description, { description = it }, label = { Text("Descripción") })
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = {
                onSave(SmartScene(name = name.trim(), description = description.trim()))
            }) { Text("Crear") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun AddSceneActionDialog(onDismiss: () -> Unit, onSave: (SceneAction) -> Unit) {
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var typeIndex by remember { mutableIntStateOf(0) }
    var numericValue by remember { mutableStateOf("") }
    var delayIndex by remember { mutableIntStateOf(0) }
    val delayOptions = listOf(0L, 500L, 1_000L, 2_000L, 5_000L, 10_000L)
    val selectedType = SceneActionType.values()[typeIndex]
    val requiredDpt = when (selectedType) {
        SceneActionType.ON, SceneActionType.OFF -> "1.001"
        SceneActionType.BLIND_UP, SceneActionType.BLIND_DOWN -> "1.008"
        SceneActionType.BLIND_POSITION -> "5.001"
        SceneActionType.CLIMATE_SETPOINT -> "9.001"
    }
    val parsedValue = numericValue.replace(',', '.').toDoubleOrNull()
    val valueIsValid = when (selectedType) {
        SceneActionType.BLIND_POSITION -> parsedValue != null && parsedValue in 0.0..100.0
        SceneActionType.CLIMATE_SETPOINT -> parsedValue != null && parsedValue in 16.0..34.0
        else -> true
    }
    val valid = name.isNotBlank() &&
        AppKnxConfigurationRepository.isValidGroupAddress(address) &&
        valueIsValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva acción") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(name, { name = it }, label = { Text("Nombre de la acción") }, singleLine = true)
                OutlinedTextField(address, { address = it }, label = { Text("Dirección KNX X/X/X") }, singleLine = true)
                OutlinedButton(
                    onClick = {
                        typeIndex = (typeIndex + 1) % SceneActionType.values().size
                        numericValue = ""
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(selectedType.displayName) }

                OutlinedTextField(
                    value = requiredDpt,
                    onValueChange = {},
                    label = { Text("DPT de escritura") },
                    readOnly = true,
                    singleLine = true
                )

                when (selectedType) {
                    SceneActionType.BLIND_POSITION -> OutlinedTextField(
                        value = numericValue,
                        onValueChange = { numericValue = it },
                        label = { Text("Posición 0–100 %") },
                        singleLine = true
                    )
                    SceneActionType.CLIMATE_SETPOINT -> OutlinedTextField(
                        value = numericValue,
                        onValueChange = { numericValue = it },
                        label = { Text("Temperatura objetivo 16–34 °C") },
                        singleLine = true
                    )
                    else -> Unit
                }

                Text(
                    when (selectedType) {
                        SceneActionType.BLIND_UP -> "KNX DPT 1.008: subir se envía como 0."
                        SceneActionType.BLIND_DOWN -> "KNX DPT 1.008: bajar se envía como 1."
                        SceneActionType.BLIND_POSITION -> "KNX DPT 5.001: 0 % cerrado y 100 % abierto."
                        SceneActionType.CLIMATE_SETPOINT -> "KNX DPT 9.001: consigna en grados Celsius."
                        else -> "KNX DPT 1.x: acción binaria."
                    },
                    fontSize = 11.sp,
                    color = TextoSecundario
                )

                OutlinedButton(
                    onClick = { delayIndex = (delayIndex + 1) % delayOptions.size },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val delay = delayOptions[delayIndex]
                    Text(if (delay == 0L) "Sin pausa posterior" else "Pausa posterior: ${delay / 1000.0} s")
                }
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = {
                onSave(
                    SceneAction(
                        name = name.trim(),
                        groupAddress = address.trim(),
                        dpt = requiredDpt,
                        type = selectedType,
                        delayAfterMillis = delayOptions[delayIndex],
                        numericValue = when (selectedType) {
                            SceneActionType.BLIND_POSITION,
                            SceneActionType.CLIMATE_SETPOINT -> parsedValue
                            else -> null
                        }
                    )
                )
            }) { Text("Añadir") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
