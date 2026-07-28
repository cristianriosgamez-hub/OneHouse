package com.onehouse.app.feature.scenes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.weight
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
import com.onehouse.app.data.knx.AppKnxConfigurationRepository
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
            "En esta primera fase se ejecutan de forma secuencial acciones binarias DPT 1.x. La aplicación detiene la escena si una acción falla.",
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
                            Text("${scene.actions.size} acciones", color = TextoDesactivado, fontSize = 11.sp)
                        }
                        Switch(
                            checked = scene.enabled,
                            onCheckedChange = { enabled ->
                                persist(state.copy(scenes = state.scenes.map { if (it.id == scene.id) it.copy(enabled = enabled) else it }))
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
                                "• ${action.name}: ${action.type.displayName} (${action.groupAddress})",
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
                        Text("${index + 1}. ${action.name}: ${action.type.displayName} · ${action.groupAddress} · DPT ${action.dpt}")
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
    var dpt by remember { mutableStateOf("1.001") }
    var typeIndex by remember { mutableIntStateOf(0) }
    val valid = name.isNotBlank() &&
        AppKnxConfigurationRepository.isValidGroupAddress(address) &&
        dpt.trim().startsWith("1")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva acción") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nombre de la acción") }, singleLine = true)
                OutlinedTextField(address, { address = it }, label = { Text("Dirección KNX X/X/X") }, singleLine = true)
                OutlinedTextField(dpt, { dpt = it }, label = { Text("DPT de escritura (1.x)") }, singleLine = true)
                OutlinedButton(
                    onClick = { typeIndex = (typeIndex + 1) % SceneActionType.values().size },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(SceneActionType.values()[typeIndex].displayName) }
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = {
                onSave(
                    SceneAction(
                        name = name.trim(),
                        groupAddress = address.trim(),
                        dpt = dpt.trim(),
                        type = SceneActionType.values()[typeIndex]
                    )
                )
            }) { Text("Añadir") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
