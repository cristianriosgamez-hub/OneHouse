package com.onehouse.app.feature.more

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
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
import com.onehouse.app.importer.ImportedKnxObject
import com.onehouse.app.importer.ImportedKnxProject
import com.onehouse.app.importer.ImportedKnxRoom
import com.onehouse.app.importer.InsideControlProjectRepository

private data class EditableKnxObject(
    val roomIndex: Int,
    val deviceIndex: Int,
    val roomName: String,
    val name: String,
    val category: String,
    val dpt: String?,
    val originalRead: String,
    val originalWrite: String,
    val read: String,
    val write: String
)

@Composable
fun KnxAddressEditorScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { InsideControlProjectRepository(context.applicationContext) }
    val originalProject = remember { repository.load() }
    var project by remember { mutableStateOf(originalProject) }
    var filter by remember { mutableStateOf("") }

    val editableObjects = remember(project) {
        project?.rooms.orEmpty().flatMapIndexed { roomIndex, room ->
            room.devices.mapIndexed { deviceIndex, device ->
                EditableKnxObject(
                    roomIndex = roomIndex,
                    deviceIndex = deviceIndex,
                    roomName = room.name,
                    name = device.name,
                    category = device.category.displayName,
                    dpt = device.dataPointType,
                    originalRead = originalProject?.rooms?.getOrNull(roomIndex)?.devices?.getOrNull(deviceIndex)
                        ?.readAddresses?.joinToString(", ").orEmpty(),
                    originalWrite = originalProject?.rooms?.getOrNull(roomIndex)?.devices?.getOrNull(deviceIndex)
                        ?.writeAddresses?.joinToString(", ").orEmpty(),
                    read = device.readAddresses.joinToString(", "),
                    write = device.writeAddresses.joinToString(", ")
                )
            }
        }
    }

    val visibleObjects = editableObjects.filter { item ->
        filter.isBlank() || listOf(item.roomName, item.name, item.category, item.read, item.write)
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
                    modifier = Modifier.padding(end = 10.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Direcciones KNX",
                        color = TextoPrincipal,
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "Edita las direcciones importadas por estancia",
                        color = TextoSecundario,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                OutlinedButton(onClick = onBack) { Text("Volver") }
            }

            if (project == null) {
                OneHouseCard {
                    Column(Modifier.padding(18.dp)) {
                        Text("No hay ningún proyecto InsideControl importado", color = TextoPrincipal)
                        Text(
                            "Importa primero el proyecto desde Más > Importar InsideControl.",
                            color = TextoSecundario
                        )
                    }
                }
                return@Column
            }

            OneHouseCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = project?.projectName?.ifBlank { "Proyecto KNX" } ?: "Proyecto KNX",
                        color = TextoPrincipal,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Formato admitido: X/X/X. Para varias direcciones, sepáralas con comas.",
                        color = TextoSecundario,
                        fontSize = 13.sp
                    )
                    OutlinedTextField(
                        value = filter,
                        onValueChange = { filter = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Buscar estancia, objeto o dirección") },
                        singleLine = true
                    )
                }
            }

            visibleObjects.forEach { item ->
                KnxAddressObjectCard(
                    item = item,
                    onReadChanged = { value ->
                        project = project?.replaceAddresses(item.roomIndex, item.deviceIndex, read = value)
                    },
                    onWriteChanged = { value ->
                        project = project?.replaceAddresses(item.roomIndex, item.deviceIndex, write = value)
                    },
                    onRestore = {
                        project = project?.replaceAddresses(
                            item.roomIndex,
                            item.deviceIndex,
                            read = item.originalRead,
                            write = item.originalWrite
                        )
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { project = originalProject },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Restaurar todo")
                }
                Button(
                    onClick = {
                        val current = project ?: return@Button
                        val invalid = current.devices.flatMap { it.readAddresses + it.writeAddresses }
                            .firstOrNull { !isValidGroupAddress(it) }
                        if (invalid != null) {
                            Toast.makeText(
                                context,
                                "Dirección KNX no válida: $invalid",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            repository.save(current)
                            notifySaved(context)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Guardar")
                }
            }
            Text(
                text = "Los cambios se aplicarán al volver a abrir las pantallas KNX. No se modifican el DPT ni el tipo de objeto.",
                color = TextoSecundario,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun KnxAddressObjectCard(
    item: EditableKnxObject,
    onReadChanged: (String) -> Unit,
    onWriteChanged: (String) -> Unit,
    onRestore: () -> Unit
) {
    OneHouseCard {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(item.roomName, color = AzulClaro, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(item.name, color = TextoPrincipal, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(
                listOfNotNull(item.category, item.dpt?.let { "DPT $it" }).joinToString(" · "),
                color = TextoSecundario,
                fontSize = 12.sp
            )
            OutlinedTextField(
                value = item.write,
                onValueChange = onWriteChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Dirección de mando / escritura") },
                supportingText = { Text("Ejemplo: 1/1/4") },
                isError = item.write.splitAddresses().any { !isValidGroupAddress(it) }
            )
            OutlinedTextField(
                value = item.read,
                onValueChange = onReadChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Dirección de estado / lectura") },
                supportingText = { Text("Ejemplo: 1/2/4") },
                isError = item.read.splitAddresses().any { !isValidGroupAddress(it) }
            )
            OutlinedButton(onClick = onRestore) { Text("Restaurar importada") }
        }
    }
}

private fun ImportedKnxProject.replaceAddresses(
    roomIndex: Int,
    deviceIndex: Int,
    read: String? = null,
    write: String? = null
): ImportedKnxProject {
    val updatedRooms = rooms.mapIndexed { currentRoomIndex, room ->
        if (currentRoomIndex != roomIndex) room else room.copy(
            devices = room.devices.mapIndexed { currentDeviceIndex, device ->
                if (currentDeviceIndex != deviceIndex) device else device.copy(
                    readAddresses = read?.splitAddresses() ?: device.readAddresses,
                    writeAddresses = write?.splitAddresses() ?: device.writeAddresses
                )
            }
        )
    }
    return copy(rooms = updatedRooms)
}

private fun String.splitAddresses(): List<String> = split(',')
    .map(String::trim)
    .filter(String::isNotBlank)
    .distinct()

private fun isValidGroupAddress(value: String): Boolean {
    val parts = value.trim().split('/')
    if (parts.size != 3) return false
    val main = parts[0].toIntOrNull() ?: return false
    val middle = parts[1].toIntOrNull() ?: return false
    val sub = parts[2].toIntOrNull() ?: return false
    return main in 0..31 && middle in 0..7 && sub in 0..255
}

private fun notifySaved(context: Context) {
    Toast.makeText(
        context,
        "Direcciones KNX guardadas correctamente",
        Toast.LENGTH_LONG
    ).show()
}
