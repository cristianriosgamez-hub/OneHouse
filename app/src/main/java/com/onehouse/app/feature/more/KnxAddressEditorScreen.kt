package com.onehouse.app.feature.more

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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

private const val ALL = "Todos"

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
    var selectedRoom by remember { mutableStateOf(ALL) }
    var selectedCategory by remember { mutableStateOf(ALL) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) runCatching {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(repository.exportConfiguration()) }
        }.onSuccess { toast(context, "Configuración KNX exportada") }
            .onFailure { toast(context, "No se pudo exportar la configuración") }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
        }.onSuccess { json ->
            if (repository.importConfiguration(json)) {
                project = repository.loadProject()
                globalValues = KnxAddressBook.entries.associate { it.key to repository.globalAddress(it.key, it.defaultAddress) }
                toast(context, "Configuración KNX importada")
            } else toast(context, "Archivo de configuración no válido")
        }.onFailure { toast(context, "No se pudo importar la configuración") }
    }

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

    val rooms = remember(projectObjects) { (KnxAddressBook.entries.map { it.room } + projectObjects.map { it.roomName }).distinct().sorted() }
    val categories = remember(projectObjects) { (KnxAddressBook.entries.map { it.category } + projectObjects.map { it.category }).distinct().sorted() }
    val allAddresses = buildList {
        addAll(globalValues.values)
        projectObjects.forEach { addAll(it.read.splitAddresses()); addAll(it.write.splitAddresses()) }
    }.filter(AppKnxConfigurationRepository::isValidGroupAddress)
    val duplicateAddresses = allAddresses.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
    val invalidCount = globalValues.values.count { !AppKnxConfigurationRepository.isValidGroupAddress(it) } +
        projectObjects.sumOf { item -> (item.read.splitAddresses() + item.write.splitAddresses()).count { !AppKnxConfigurationRepository.isValidGroupAddress(it) } }

    fun matches(room: String, category: String, vararg values: String): Boolean {
        val roomOk = selectedRoom == ALL || room == selectedRoom
        val categoryOk = selectedCategory == ALL || category == selectedCategory
        val textOk = filter.isBlank() || values.any { it.contains(filter, true) }
        return roomOk && categoryOk && textOk
    }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(FondoSuperior, FondoInferior)))) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("‹", color = TextoPrincipal, fontSize = 42.sp, modifier = Modifier.padding(end = 10.dp).clickable(onClick = onBack))
                Column(Modifier.weight(1f)) {
                    Text("Direcciones KNX", color = TextoPrincipal, style = MaterialTheme.typography.headlineMedium)
                    Text("Configuración independiente de OneHouse", color = TextoSecundario)
                }
            }

            OneHouseCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Validación", color = TextoPrincipal, fontWeight = FontWeight.Bold)
                    Text(
                        when {
                            invalidCount > 0 -> "$invalidCount direcciones no válidas"
                            duplicateAddresses.isNotEmpty() -> "${duplicateAddresses.size} direcciones repetidas: ${duplicateAddresses.joinToString()}"
                            else -> "Configuración válida · sin errores de formato"
                        },
                        color = if (invalidCount == 0) AzulClaro else MaterialTheme.colorScheme.error,
                        fontSize = 13.sp
                    )
                    Text("Una dirección repetida puede ser correcta cuando mando y estado comparten objeto. Revísala antes de guardar.", color = TextoSecundario, fontSize = 12.sp)
                    OutlinedTextField(filter, { filter = it }, Modifier.fillMaxWidth(), label = { Text("Buscar estancia, parámetro o X/X/X") }, singleLine = true)
                    FilterRow("Estancia", listOf(ALL) + rooms, selectedRoom) { selectedRoom = it }
                    FilterRow("Tipo", listOf(ALL) + categories, selectedCategory) { selectedCategory = it }
                }
            }

            Text("Parámetros propios de OneHouse", color = TextoPrincipal, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            KnxAddressBook.entries.filter { matches(it.room, it.category, it.room, it.name, it.category, it.dpt, globalValues[it.key].orEmpty()) }.forEach { entry ->
                val address = globalValues[entry.key].orEmpty()
                AddressCard(
                    room = entry.room,
                    name = entry.name,
                    detail = "${entry.category} · DPT ${entry.dpt} · ${entry.kind}",
                    address = address,
                    label = "Dirección utilizada por OneHouse",
                    duplicate = address in duplicateAddresses,
                    onAddressChange = { globalValues = globalValues + (entry.key to it.trim()) },
                    onRestore = { globalValues = globalValues + (entry.key to entry.defaultAddress) },
                    restoreText = "Restaurar ${entry.defaultAddress}"
                )
            }

            if (project != null) {
                Text("Luces, persianas y controles", color = TextoPrincipal, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                projectObjects.filter { matches(it.roomName, it.category, it.roomName, it.name, it.category, it.read, it.write) }.forEach { item ->
                    OneHouseCard {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(item.roomName, color = AzulClaro, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(item.name, color = TextoPrincipal, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                            Text(listOfNotNull(item.category, item.dpt?.let { "DPT $it" }).joinToString(" · "), color = TextoSecundario, fontSize = 12.sp)
                            AddressField(item.write, "Mando / escritura", duplicateAddresses) { project = project?.replaceAddresses(item.roomIndex, item.deviceIndex, write = it) }
                            AddressField(item.read, "Estado / lectura", duplicateAddresses) { project = project?.replaceAddresses(item.roomIndex, item.deviceIndex, read = it) }
                            OutlinedButton(onClick = { project = project?.replaceAddresses(item.roomIndex, item.deviceIndex, item.originalRead, item.originalWrite) }) { Text("Restaurar dirección original") }
                        }
                    }
                }
            }

            OneHouseCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Copia de seguridad", color = TextoPrincipal, fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { exportLauncher.launch("OneHouse_KNX.json") }, modifier = Modifier.weight(1f)) { Text("Exportar") }
                        OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) }, modifier = Modifier.weight(1f)) { Text("Importar") }
                    }
                    if (repository.hasAutomaticBackup()) {
                        OutlinedButton(onClick = {
                            if (repository.restoreAutomaticBackup()) {
                                project = repository.loadProject()
                                globalValues = KnxAddressBook.entries.associate { it.key to repository.globalAddress(it.key, it.defaultAddress) }
                                toast(context, "Última copia restaurada")
                            }
                        }, modifier = Modifier.fillMaxWidth()) { Text("Restaurar última copia automática") }
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = {
                    repository.resetAllToFactory()
                    project = repository.loadProject()
                    globalValues = KnxAddressBook.entries.associate { it.key to it.defaultAddress }
                    toast(context, "Valores originales de OneHouse restaurados")
                }, modifier = Modifier.weight(1f)) { Text("Valores originales") }
                Button(onClick = {
                    val invalidGlobal = globalValues.values.firstOrNull { !AppKnxConfigurationRepository.isValidGroupAddress(it) }
                    val invalidProject = projectObjects.flatMap { it.read.splitAddresses() + it.write.splitAddresses() }
                        .firstOrNull { !AppKnxConfigurationRepository.isValidGroupAddress(it) }
                    val invalid = invalidGlobal ?: invalidProject
                    if (invalid != null) toast(context, "Dirección KNX no válida: $invalid")
                    else {
                        project?.let(repository::saveProject)
                        repository.saveGlobalAddresses(globalValues)
                        toast(context, "Direcciones KNX guardadas")
                    }
                }, modifier = Modifier.weight(1f)) { Text("Guardar") }
            }
            Text("Los cambios solo afectan a OneHouse. InsideControl permanece intacto.", color = TextoSecundario, fontSize = 12.sp)
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun FilterRow(title: String, options: List<String>, selected: String, onSelected: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, color = TextoSecundario, fontSize = 12.sp)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                FilterChip(selected = option == selected, onClick = { onSelected(option) }, label = { Text(option) })
            }
        }
    }
}

@Composable
private fun AddressCard(
    room: String,
    name: String,
    detail: String,
    address: String,
    label: String,
    duplicate: Boolean,
    onAddressChange: (String) -> Unit,
    onRestore: () -> Unit,
    restoreText: String
) {
    OneHouseCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(room, color = AzulClaro, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(name, color = TextoPrincipal, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(detail, color = TextoSecundario, fontSize = 12.sp)
            AddressField(address, label, if (duplicate) setOf(address) else emptySet(), onAddressChange)
            OutlinedButton(onClick = onRestore) { Text(restoreText) }
        }
    }
}

@Composable
private fun AddressField(value: String, label: String, duplicates: Set<String>, onChange: (String) -> Unit) {
    val addresses = value.splitAddresses()
    val invalid = addresses.any { !AppKnxConfigurationRepository.isValidGroupAddress(it) }
    val duplicate = addresses.any { it in duplicates }
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        supportingText = {
            when {
                invalid -> Text("Formato válido: 0-31/0-7/0-255")
                duplicate -> Text("Dirección compartida con otro parámetro")
            }
        },
        isError = invalid
    )
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
private fun toast(context: Context, message: String) = Toast.makeText(context, message, Toast.LENGTH_LONG).show()
