package com.onehouse.app.feature.more

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import com.onehouse.app.importer.ImportedKnxProject
import com.onehouse.app.knx.AppKnxConfigurationRepository
import com.onehouse.app.knx.KnxAddressBook

private data class EditableProjectObject(
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
    val repository = remember { AppKnxConfigurationRepository(context.applicationContext) }
    val originalProject = remember { repository.loadProject() }
    var project by remember { mutableStateOf(originalProject) }
    var globalValues by remember {
        mutableStateOf(KnxAddressBook.entries.associate { it.key to repository.globalAddress(it.key, it.defaultAddress) })
    }
    var filter by remember { mutableStateOf("") }

    val projectObjects = remember(project) {
        project?.rooms.orEmpty().flatMapIndexed { roomIndex, room ->
            room.devices.mapIndexedNotNull { deviceIndex, device ->
                if (!AppKnxObjectFilter.isVisible(room.name, device)) return@mapIndexedNotNull null
                EditableProjectObject(
                    roomIndex, deviceIndex, room.name, device.name, device.category.displayName,
                    device.dataPointType,
                    originalProject?.rooms?.getOrNull(roomIndex)?.devices?.getOrNull(deviceIndex)?.readAddresses?.joinToString(", ").orEmpty(),
                    originalProject?.rooms?.getOrNull(roomIndex)?.devices?.getOrNull(deviceIndex)?.writeAddresses?.joinToString(", ").orEmpty(),
                    device.readAddresses.joinToString(", "), device.writeAddresses.joinToString(", ")
                )
            }
        }
    }

    fun matches(vararg values: String): Boolean = filter.isBlank() || values.any { it.contains(filter, true) }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(FondoSuperior, FondoInferior)))) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("‹", color = TextoPrincipal, fontSize = 42.sp, modifier = Modifier.padding(end = 10.dp).clickable(onClick = onBack))
                Column(Modifier.weight(1f)) {
                    Text("Direcciones KNX", color = TextoPrincipal, style = MaterialTheme.typography.headlineMedium)
                    Text("Configuración independiente y exclusiva de OneHouse", color = TextoSecundario)
                }
            }

            OneHouseCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Pruebas seguras", color = TextoPrincipal, fontWeight = FontWeight.Bold)
                    Text("Los cambios de esta pantalla solo afectan a OneHouse. El proyecto importado de InsideControl no se modifica.", color = TextoSecundario, fontSize = 13.sp)
                    OutlinedTextField(filter, { filter = it }, Modifier.fillMaxWidth(), label = { Text("Buscar estancia, parámetro o X/X/X") }, singleLine = true)
                }
            }

            Text("Parámetros propios de OneHouse", color = TextoPrincipal, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            KnxAddressBook.entries.filter { matches(it.room, it.name, it.category, it.dpt, globalValues[it.key].orEmpty()) }.forEach { entry ->
                OneHouseCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(entry.room, color = AzulClaro, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(entry.name, color = TextoPrincipal, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Text("${entry.category} · DPT ${entry.dpt} · ${entry.kind}", color = TextoSecundario, fontSize = 12.sp)
                        OutlinedTextField(
                            value = globalValues[entry.key].orEmpty(),
                            onValueChange = { globalValues = globalValues + (entry.key to it.trim()) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Dirección utilizada por OneHouse") },
                            isError = !AppKnxConfigurationRepository.isValidGroupAddress(globalValues[entry.key].orEmpty())
                        )
                        OutlinedButton(onClick = { globalValues = globalValues + (entry.key to entry.defaultAddress) }) { Text("Restaurar ${entry.defaultAddress}") }
                    }
                }
            }

            if (project != null) {
                Text("Luces, persianas y controles de estancias", color = TextoPrincipal, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                projectObjects.filter { matches(it.roomName, it.name, it.category, it.read, it.write) }.forEach { item ->
                    OneHouseCard {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(item.roomName, color = AzulClaro, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(item.name, color = TextoPrincipal, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                            Text(listOfNotNull(item.category, item.dpt?.let { "DPT $it" }).joinToString(" · "), color = TextoSecundario, fontSize = 12.sp)
                            OutlinedTextField(item.write, { project = project?.replaceAddresses(item.roomIndex, item.deviceIndex, write = it) }, Modifier.fillMaxWidth(), label = { Text("Mando / escritura") }, isError = item.write.splitAddresses().any { !AppKnxConfigurationRepository.isValidGroupAddress(it) })
                            OutlinedTextField(item.read, { project = project?.replaceAddresses(item.roomIndex, item.deviceIndex, read = it) }, Modifier.fillMaxWidth(), label = { Text("Estado / lectura") }, isError = item.read.splitAddresses().any { !AppKnxConfigurationRepository.isValidGroupAddress(it) })
                            OutlinedButton(onClick = { project = project?.replaceAddresses(item.roomIndex, item.deviceIndex, item.originalRead, item.originalWrite) }) { Text("Restaurar dirección original de OneHouse") }
                        }
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = {
                    project = originalProject
                    globalValues = KnxAddressBook.entries.associate { it.key to it.defaultAddress }
                }, modifier = Modifier.weight(1f)) { Text("Restaurar todo") }
                Button(onClick = {
                    val invalidGlobal = globalValues.values.firstOrNull { !AppKnxConfigurationRepository.isValidGroupAddress(it) }
                    val invalidProject = project?.devices?.flatMap { it.readAddresses + it.writeAddresses }?.firstOrNull { !AppKnxConfigurationRepository.isValidGroupAddress(it) }
                    val invalid = invalidGlobal ?: invalidProject
                    if (invalid != null) Toast.makeText(context, "Dirección KNX no válida: $invalid", Toast.LENGTH_LONG).show()
                    else {
                        project?.let(repository::saveProject)
                        repository.saveGlobalAddresses(globalValues)
                        notifySaved(context)
                    }
                }, modifier = Modifier.weight(1f)) { Text("Guardar") }
            }
            Text("Cierra y vuelve a abrir la pantalla afectada para aplicar una prueba. InsideControl permanece intacto.", color = TextoSecundario, fontSize = 12.sp)
            Spacer(Modifier.height(20.dp))
        }
    }
}

private fun ImportedKnxProject.replaceAddresses(roomIndex: Int, deviceIndex: Int, read: String? = null, write: String? = null): ImportedKnxProject =
    copy(rooms = rooms.mapIndexed { r, room ->
        if (r != roomIndex) room else room.copy(devices = room.devices.mapIndexed { d, device ->
            if (d != deviceIndex) device else device.copy(
                readAddresses = read?.splitAddresses() ?: device.readAddresses,
                writeAddresses = write?.splitAddresses() ?: device.writeAddresses
            )
        })
    })

private fun String.splitAddresses(): List<String> = split(',').map(String::trim).filter(String::isNotBlank).distinct()
private fun notifySaved(context: Context) = Toast.makeText(context, "Direcciones KNX de OneHouse guardadas", Toast.LENGTH_LONG).show()
