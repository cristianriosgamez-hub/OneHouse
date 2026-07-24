package com.onehouse.app.feature.importproject

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextOverflow
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
import com.onehouse.app.importer.ImportedKnxCategory
import com.onehouse.app.importer.ImportedKnxProject
import com.onehouse.app.knx.KnxBulkStateReader
import com.onehouse.app.knx.KnxDeviceFactory

@Composable
fun ImportProjectScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel = remember(context.applicationContext) {
        ImportProjectViewModel(context.applicationContext)
    }
    var snapshot by remember { mutableStateOf(
        ImportProjectViewModel.Snapshot(project = null, state = ImportState.Idle)
    ) }
    var selectedCategory by remember { mutableStateOf<ImportedKnxCategory?>(null) }
    var query by remember { mutableStateOf("") }
    var expandedDeviceId by remember { mutableStateOf<String?>(null) }
    var selectedControlDevice by remember { mutableStateOf<ImportedKnxDevice?>(null) }
    val uiPreferences = remember(context.applicationContext) {
        context.applicationContext.getSharedPreferences("import_project_ui", android.content.Context.MODE_PRIVATE)
    }
    var collapsedRooms by remember {
        mutableStateOf(uiPreferences.getStringSet("collapsed_rooms", emptySet()).orEmpty().toSet())
    }
    var sortMode by remember { mutableStateOf(DeviceSortMode.ROOM) }
    var bulkProgress by remember { mutableStateOf<KnxBulkStateReader.Progress?>(null) }
    var bulkMessage by remember { mutableStateOf<String?>(null) }
    val bulkReader = remember(context.applicationContext) { KnxBulkStateReader(context.applicationContext) }

    DisposableEffect(viewModel) {
        val observation = viewModel.observe { snapshot = it }
        onDispose {
            observation.close()
            viewModel.close()
            bulkReader.close()
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        viewModel.onFileSelected(context.contentResolver, uri)
    }

    val launchFilePicker = {
        viewModel.beginSelection()
        try {
            launcher.launch(arrayOf("application/octet-stream", "text/plain", "*/*"))
        } catch (error: Exception) {
            viewModel.selectionFailed(error)
        }
    }

    val state = snapshot.state
    val message = when (state) {
        is ImportState.Success -> state.message
        is ImportState.Error -> state.message
        else -> null
    }
    val isBusy = state is ImportState.SelectingFile || state is ImportState.Importing

    selectedControlDevice?.let { device ->
        DeviceControlScreen(device = device, onBack = { selectedControlDevice = null })
        return
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
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Header(onBack = onBack)
            Spacer(modifier = Modifier.height(12.dp))

            val currentProject = snapshot.project
            if (currentProject == null) {
                EmptyImportState(
                    message = message,
                    isBusy = isBusy,
                    importingFileName = (state as? ImportState.Importing)?.fileName,
                    onSelectFile = launchFilePicker
                )
            } else {
                ProjectContent(
                    project = currentProject,
                    message = message,
                    selectedCategory = selectedCategory,
                    query = query,
                    expandedDeviceId = expandedDeviceId,
                    onQueryChanged = { query = it },
                    onCategorySelected = {
                        selectedCategory = if (selectedCategory == it) null else it
                        expandedDeviceId = null
                    },
                    onDeviceSelected = {
                        expandedDeviceId = if (expandedDeviceId == it) null else it
                    },
                    onOpenControl = { selectedControlDevice = it },
                    collapsedRooms = collapsedRooms,
                    sortMode = sortMode,
                    bulkProgress = bulkProgress,
                    bulkMessage = bulkMessage,
                    onRoomToggle = { roomName ->
                        collapsedRooms = if (roomName in collapsedRooms) {
                            collapsedRooms - roomName
                        } else {
                            collapsedRooms + roomName
                        }
                        uiPreferences.edit().putStringSet("collapsed_rooms", collapsedRooms).apply()
                        expandedDeviceId = null
                    },
                    onSortModeChanged = { sortMode = it },
                    onRefreshStates = { devices ->
                        bulkMessage = null
                        bulkReader.read(
                            devices = devices,
                            onProgress = { bulkProgress = it },
                            onComplete = { result ->
                                bulkProgress = null
                                bulkMessage = if (result.total == 0) {
                                    "No hay direcciones de lectura disponibles"
                                } else if (result.failures == 0) {
                                    "Lectura solicitada para ${result.total} direcciones"
                                } else {
                                    "Lectura terminada: ${result.total - result.failures} correctas y ${result.failures} con error"
                                }
                            }
                        )
                    },
                    onReplaceProject = launchFilePicker,
                    onDeleteProject = {
                        viewModel.clearProject()
                        selectedCategory = null
                        query = ""
                        expandedDeviceId = null
                        collapsedRooms = emptySet()
                    }
                )
            }
        }

        if (isBusy) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(FondoInferior.copy(alpha = 0.68f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(color = FondoTarjeta, shape = RoundedCornerShape(18.dp)) {
                    Column(
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = AzulClaro)
                        Text(
                            text = when (state) {
                                is ImportState.Importing -> "Importando ${state.fileName}…"
                                else -> "Abriendo selector de archivos…"
                            },
                            color = TextoPrincipal
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "‹",
            color = AzulClaro,
            fontSize = 40.sp,
            modifier = Modifier
                .clickable(onClick = onBack)
                .padding(end = 12.dp)
        )
        Column {
            Text(
                text = "Objetos KNX importados",
                color = TextoPrincipal,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Habitaciones, funciones y direcciones de InsideControl",
                color = TextoSecundario,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun EmptyImportState(
    message: String?,
    isBusy: Boolean,
    importingFileName: String?,
    onSelectFile: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "⇩", color = AzulClaro, fontSize = 56.sp)
        Text(
            text = "Selecciona el archivo generado por\nInsideControl Builder",
            color = TextoPrincipal,
            fontWeight = FontWeight.SemiBold,
            fontSize = 19.sp
        )
        Text(
            text = "OneHouse lo descifrará y mostrará todas las habitaciones, dispositivos y direcciones KNX.",
            color = TextoSecundario,
            modifier = Modifier.padding(vertical = 14.dp),
            style = MaterialTheme.typography.bodyMedium
        )
        Button(
            onClick = onSelectFile,
            enabled = !isBusy,
            colors = ButtonDefaults.buttonColors(containerColor = AzulOneHouse)
        ) {
            Text("Seleccionar archivo .knx")
        }
        importingFileName?.let {
            Text(
                text = "Archivo: $it",
                color = AzulClaro,
                modifier = Modifier.padding(top = 14.dp)
            )
        }
        message?.let {
            Text(text = it, color = TextoSecundario, modifier = Modifier.padding(top = 16.dp))
        }
    }
}

@Composable
private fun ProjectContent(
    project: ImportedKnxProject,
    message: String?,
    selectedCategory: ImportedKnxCategory?,
    query: String,
    expandedDeviceId: String?,
    onQueryChanged: (String) -> Unit,
    onCategorySelected: (ImportedKnxCategory) -> Unit,
    onDeviceSelected: (String) -> Unit,
    onOpenControl: (ImportedKnxDevice) -> Unit,
    collapsedRooms: Set<String>,
    sortMode: DeviceSortMode,
    bulkProgress: KnxBulkStateReader.Progress?,
    bulkMessage: String?,
    onRoomToggle: (String) -> Unit,
    onSortModeChanged: (DeviceSortMode) -> Unit,
    onRefreshStates: (List<ImportedKnxDevice>) -> Unit,
    onReplaceProject: () -> Unit,
    onDeleteProject: () -> Unit
) {
    val allDevices = remember(project) { KnxDeviceFactory.create(project) }
    val filteredDevices = remember(allDevices, selectedCategory, query) {
        val normalizedQuery = query.trim()
        allDevices.filter { device ->
            val categoryMatches = selectedCategory == null || device.category == selectedCategory
            val queryMatches = normalizedQuery.isBlank() || listOf(
                device.name,
                device.roomName,
                device.category.displayName,
                device.controlKind.displayName,
                device.dataPointType.orEmpty(),
                device.unit.orEmpty(),
                device.source.readAddresses.joinToString(" "),
                device.source.writeAddresses.joinToString(" ")
            ).any { it.contains(normalizedQuery, ignoreCase = true) }
            categoryMatches && queryMatches
        }
    }
    val sortedDevices = remember(filteredDevices, sortMode) {
        when (sortMode) {
            DeviceSortMode.ROOM -> filteredDevices.sortedWith(
                Comparator { first, second ->
                    val roomComparison = first.roomName.compareTo(second.roomName, ignoreCase = true)
                    if (roomComparison != 0) {
                        roomComparison
                    } else {
                        first.name.compareTo(second.name, ignoreCase = true)
                    }
                }
            )
            DeviceSortMode.NAME -> filteredDevices.sortedWith(
                Comparator { first, second ->
                    first.name.compareTo(second.name, ignoreCase = true)
                }
            )
            DeviceSortMode.ADDRESS -> filteredDevices.sortedBy { it.primaryWriteAddress?.toString() ?: it.primaryReadAddress?.toString().orEmpty() }
        }
    }
    val groupedDevices = remember(sortedDevices, sortMode) {
        if (sortMode == DeviceSortMode.ROOM) {
            sortedDevices.groupBy { it.roomName }.toSortedMap(String.CASE_INSENSITIVE_ORDER)
        } else {
            linkedMapOf("Todos los objetos" to sortedDevices)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            ProjectSummary(project, allDevices)
            message?.let {
                Text(
                    text = it,
                    color = AzulClaro,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChanged,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Buscar objeto o dirección") },
                placeholder = { Text("Ej.: Luz salón o 1/1/1") }
            )
        }

        item {
            CategoryFilters(project, selectedCategory, onCategorySelected)
        }

        item {
            ProjectActions(
                sortMode = sortMode,
                progress = bulkProgress,
                message = bulkMessage,
                canRead = allDevices.any { it.canRead },
                onSortModeChanged = onSortModeChanged,
                onRefreshStates = { onRefreshStates(allDevices) }
            )
        }

        item {
            Text(
                text = "${filteredDevices.size} objetos encontrados",
                color = TextoSecundario,
                fontSize = 12.sp
            )
        }

        groupedDevices.forEach { (roomName, devices) ->
            val collapsed = roomName in collapsedRooms
            item(key = "room:$roomName") {
                RoomHeader(
                    roomName = roomName,
                    count = devices.size,
                    collapsed = collapsed,
                    onClick = { onRoomToggle(roomName) }
                )
            }
            if (!collapsed) {
                items(devices, key = { it.id }) { device ->
                    DeviceCard(
                        device = device,
                        expanded = expandedDeviceId == device.id,
                        onClick = { onDeviceSelected(device.id) },
                        onOpenControl = { onOpenControl(device) }
                    )
                }
            }
        }

        if (filteredDevices.isEmpty()) {
            item {
                Surface(color = FondoTarjeta, shape = RoundedCornerShape(16.dp)) {
                    Text(
                        text = "No hay objetos que coincidan con la búsqueda y el filtro seleccionados.",
                        color = TextoSecundario,
                        modifier = Modifier.padding(18.dp)
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(onClick = onReplaceProject, modifier = Modifier.weight(1f)) {
                    Text("Cambiar archivo")
                }
                OutlinedButton(onClick = onDeleteProject, modifier = Modifier.weight(1f)) {
                    Text("Eliminar")
                }
            }
        }
    }
}

private enum class DeviceSortMode(val label: String) {
    ROOM("Habitación"),
    NAME("Nombre"),
    ADDRESS("Dirección")
}

@Composable
private fun ProjectActions(
    sortMode: DeviceSortMode,
    progress: KnxBulkStateReader.Progress?,
    message: String?,
    canRead: Boolean,
    onSortModeChanged: (DeviceSortMode) -> Unit,
    onRefreshStates: () -> Unit
) {
    Surface(color = FondoTarjeta, shape = RoundedCornerShape(16.dp)) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Text("Ordenar objetos", color = TextoSecundario, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                DeviceSortMode.values().forEach { mode ->
                    Surface(
                        color = if (sortMode == mode) AzulOneHouse else FondoInferior,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.clickable { onSortModeChanged(mode) }
                    ) {
                        Text(
                            mode.label,
                            color = if (sortMode == mode) TextoPrincipal else TextoSecundario,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                        )
                    }
                }
            }
            Button(
                onClick = onRefreshStates,
                enabled = canRead && progress == null,
                colors = ButtonDefaults.buttonColors(containerColor = AzulOneHouse),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (progress == null) "Actualizar estados KNX" else "Leyendo ${progress.completed}/${progress.total}…")
            }
            progress?.let {
                Text(
                    text = "Dirección ${it.currentAddress ?: "—"} · errores ${it.failures}",
                    color = TextoSecundario,
                    fontSize = 11.sp
                )
            }
            message?.let { Text(it, color = AzulClaro, fontSize = 12.sp) }
        }
    }
}

@Composable
private fun ProjectSummary(project: ImportedKnxProject, devices: List<ImportedKnxDevice>) {
    Surface(color = FondoTarjeta, shape = RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = project.projectName,
                color = TextoPrincipal,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Text(
                text = "Builder ${project.builderVersion} · ${project.rooms.size} habitaciones",
                color = TextoSecundario,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatChip("${devices.size}", "objetos")
                StatChip("${project.uniqueGroupAddresses.size}", "direcciones")
                StatChip("${devices.count { it.canWrite }}", "controlables")
            }
        }
    }
}

@Composable
private fun StatChip(value: String, label: String) {
    Surface(color = AzulOneHouse.copy(alpha = 0.16f), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp)) {
            Text(value, color = AzulClaro, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text(label, color = TextoSecundario, fontSize = 10.sp)
        }
    }
}

@Composable
private fun CategoryFilters(
    project: ImportedKnxProject,
    selected: ImportedKnxCategory?,
    onSelected: (ImportedKnxCategory) -> Unit
) {
    val categories = remember(project) {
        project.devices.map { it.category }.distinct().sortedBy { it.displayName }
    }
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text("Filtrar por tipo", color = TextoSecundario, fontSize = 12.sp)
        categories.chunked(3).forEach { rowCategories ->
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                rowCategories.forEach { category ->
                    val count = project.devices.count { it.category == category }
                    Surface(
                        color = if (selected == category) AzulOneHouse else FondoTarjeta,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.clickable { onSelected(category) }
                    ) {
                        Text(
                            text = "${category.displayName} ($count)",
                            color = if (selected == category) TextoPrincipal else TextoSecundario,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RoomHeader(
    roomName: String,
    count: Int,
    collapsed: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = FondoTarjeta.copy(alpha = 0.72f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (collapsed) "▸" else "▾",
                color = AzulClaro,
                fontSize = 18.sp,
                modifier = Modifier.padding(end = 9.dp)
            )
            Text(
                text = roomName,
                color = AzulClaro,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                modifier = Modifier.weight(1f)
            )
            Text(text = "$count objetos", color = TextoSecundario, fontSize = 11.sp)
        }
    }
}

@Composable
private fun DeviceCard(
    device: ImportedKnxDevice,
    expanded: Boolean,
    onClick: () -> Unit,
    onOpenControl: () -> Unit
) {
    Surface(
        color = FondoTarjeta,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = AzulOneHouse.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(end = 12.dp)
                ) {
                    Text(
                        text = device.iconGlyph,
                        fontSize = 22.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = device.name,
                        color = TextoPrincipal,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${device.category.displayName} · ${device.controlKind.displayName}",
                        color = TextoSecundario,
                        fontSize = 12.sp
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = device.primaryWriteAddress?.toString()
                            ?: device.primaryReadAddress?.toString()
                            ?: "Sin dirección",
                        color = AzulClaro,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = if (device.canWrite) "Escritura disponible" else "Solo lectura",
                        color = TextoSecundario,
                        fontSize = 10.sp
                    )
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(10.dp))
                DetailLine("Habitación", device.roomName)
                DetailLine("Control OneHouse", device.controlKind.displayName)
                DetailLine("Lectura", device.source.readAddresses.joinToString().ifBlank { "—" })
                DetailLine("Escritura", device.source.writeAddresses.joinToString().ifBlank { "—" })
                DetailLine("DPT", device.dataPointType ?: "No determinado")
                DetailLine("Unidad", device.unit ?: "—")
                DetailLine("Favorito", if (device.isFavourite) "Sí" else "No")
                DetailLine("Tipo InsideControl", device.source.insideControlType.toString())
                if (device.commands.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onOpenControl,
                        colors = ButtonDefaults.buttonColors(containerColor = AzulOneHouse),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (device.canWrite) "Abrir control KNX" else "Abrir lectura KNX")
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, color = TextoSecundario, fontSize = 12.sp, modifier = Modifier.weight(0.38f))
        Text(value, color = TextoPrincipal, fontSize = 12.sp, modifier = Modifier.weight(0.62f))
    }
}
