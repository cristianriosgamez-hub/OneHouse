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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import com.onehouse.app.importer.ImportedKnxCategory
import com.onehouse.app.importer.ImportedKnxObject
import com.onehouse.app.importer.ImportedKnxProject
import com.onehouse.app.importer.InsideControlImporter
import com.onehouse.app.importer.InsideControlProjectRepository

@Composable
fun ImportProjectScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember(context) { InsideControlProjectRepository(context.applicationContext) }
    var project by remember { mutableStateOf(repository.load()) }
    var message by remember { mutableStateOf<String?>(null) }
    var selectedCategory by remember { mutableStateOf<ImportedKnxCategory?>(null) }
    var expandedDevice by remember { mutableStateOf<ImportedKnxObject?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            when (val result = InsideControlImporter.import(context.contentResolver, uri)) {
                is InsideControlImporter.Result.Success -> {
                    repository.save(result.project)
                    project = result.project
                    selectedCategory = null
                    message = "Proyecto importado correctamente"
                }
                is InsideControlImporter.Result.Failure -> message = result.message
            }
        }
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

            if (project == null) {
                EmptyImportState(
                    message = message,
                    onSelectFile = { launcher.launch(arrayOf("*/*")) }
                )
            } else {
                ProjectContent(
                    project = requireNotNull(project),
                    message = message,
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = if (selectedCategory == it) null else it },
                    expandedDevice = expandedDevice,
                    onDeviceSelected = { expandedDevice = if (expandedDevice == it) null else it },
                    onReplaceProject = { launcher.launch(arrayOf("*/*")) },
                    onDeleteProject = {
                        repository.clear()
                        project = null
                        message = "Importación eliminada"
                    }
                )
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
                text = "Importar InsideControl",
                color = TextoPrincipal,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Direcciones y objetos del proyecto .knx",
                color = TextoSecundario,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun EmptyImportState(message: String?, onSelectFile: () -> Unit) {
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
            colors = ButtonDefaults.buttonColors(containerColor = AzulOneHouse)
        ) {
            Text("Seleccionar archivo .knx")
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
    onCategorySelected: (ImportedKnxCategory) -> Unit,
    expandedDevice: ImportedKnxObject?,
    onDeviceSelected: (ImportedKnxObject) -> Unit,
    onReplaceProject: () -> Unit,
    onDeleteProject: () -> Unit
) {
    val visibleDevices = remember(project, selectedCategory) {
        project.devices.filter { selectedCategory == null || it.category == selectedCategory }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            ProjectSummary(project)
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
            CategoryFilters(project, selectedCategory, onCategorySelected)
        }

        items(visibleDevices, key = { "${it.roomName}:${it.name}:${it.insideControlType}" }) { device ->
            DeviceCard(
                device = device,
                expanded = expandedDevice == device,
                onClick = { onDeviceSelected(device) }
            )
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

@Composable
private fun ProjectSummary(project: ImportedKnxProject) {
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
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatChip("${project.devices.size}", "objetos")
                StatChip("${project.uniqueGroupAddresses.size}", "direcciones")
            }
        }
    }
}

@Composable
private fun StatChip(value: String, label: String) {
    Surface(color = AzulOneHouse.copy(alpha = 0.16f), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
            Text(value, color = AzulClaro, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(label, color = TextoSecundario, fontSize = 11.sp)
        }
    }
}

@Composable
private fun CategoryFilters(
    project: ImportedKnxProject,
    selected: ImportedKnxCategory?,
    onSelected: (ImportedKnxCategory) -> Unit
) {
    val categories = remember(project) { project.devices.map { it.category }.distinct() }
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text("Filtrar por tipo", color = TextoSecundario, fontSize = 12.sp)
        categories.chunked(3).forEach { rowCategories ->
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                rowCategories.forEach { category ->
                    Surface(
                        color = if (selected == category) AzulOneHouse else FondoTarjeta,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.clickable { onSelected(category) }
                    ) {
                        Text(
                            text = category.displayName,
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
private fun DeviceCard(device: ImportedKnxObject, expanded: Boolean, onClick: () -> Unit) {
    Surface(
        color = FondoTarjeta,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                        text = "${device.roomName} · ${device.category.displayName}",
                        color = TextoSecundario,
                        fontSize = 12.sp
                    )
                }
                Text(
                    text = device.groupAddresses.firstOrNull() ?: "Sin dirección",
                    color = AzulClaro,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(10.dp))
                DetailLine("Lectura", device.readAddresses.joinToString().ifBlank { "—" })
                DetailLine("Escritura", device.writeAddresses.joinToString().ifBlank { "—" })
                DetailLine("DPT", device.dataPointType ?: "No determinado")
                DetailLine("Unidad", device.unit ?: "—")
                DetailLine("Tipo InsideControl", device.insideControlType.toString())
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, color = TextoSecundario, fontSize = 12.sp, modifier = Modifier.weight(0.35f))
        Text(value, color = TextoPrincipal, fontSize = 12.sp, modifier = Modifier.weight(0.65f))
    }
}
