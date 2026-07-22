package com.onehouse.app.feature.consumption

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onehouse.app.data.energy.EnergyHistorySeeder
import com.onehouse.app.data.energy.EnergyRepository
import com.onehouse.app.data.local.OneHouseDatabase
import com.onehouse.app.design.FondoInferior
import com.onehouse.app.design.FondoSuperior
import com.onehouse.app.design.TextoPrincipal
import com.onehouse.app.design.TextoSecundario
import com.onehouse.app.feature.rooms.detail.RoomHeader

@Composable
fun ConsumptionScreen(onBack: (() -> Unit)? = null) {
    val context = LocalContext.current
    val database = remember(context.applicationContext) {
        OneHouseDatabase.getInstance(context.applicationContext)
    }
    val repository = remember(database) { EnergyRepository(database.energyReadingDao()) }
    val viewModel = remember(repository) { EnergyViewModel(repository) }
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(repository) {
        EnergyHistorySeeder(context.applicationContext, repository).seedIfEmpty()
    }

    DisposableEffect(viewModel) {
        onDispose { viewModel.close() }
    }

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.consumeMessage()
    }

    if (state.selectedType != null) {
        EnergyMeterDetailScreen(
            state = state,
            onBack = viewModel::closeMeter,
            onPeriodSelected = viewModel::selectPeriod,
            onAddReading = viewModel::addReading,
            onEditReading = viewModel::editReading,
            onDeleteReading = viewModel::requestDelete
        )
    } else {
        EnergyDashboardScreen(
            isLoading = state.isLoading,
            summaries = state.summaries,
            overview = state.overview,
            onBack = onBack,
            onMeterSelected = viewModel::openMeter
        )
    }

    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.padding(16.dp)
    )

    if (state.isEditorVisible) {
        val type = state.selectedType
        if (type != null) {
            ReadingEditorDialog(
                type = type,
                reading = state.editorReading,
                onDismiss = viewModel::dismissEditor,
                onSave = viewModel::saveReading
            )
        }
    }

    if (state.readingPendingDeletion != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDelete,
            title = { Text("Eliminar lectura") },
            text = { Text("La lectura manual se eliminará definitivamente de la base de datos.") },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDelete) {
                    Text("Eliminar", color = EnergyRed)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDelete) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun EnergyDashboardScreen(
    isLoading: Boolean,
    summaries: List<com.onehouse.app.data.energy.MeterSummary>,
    overview: com.onehouse.app.data.energy.EnergyOverview,
    onBack: (() -> Unit)?,
    onMeterSelected: (com.onehouse.app.data.energy.MeterType) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(FondoSuperior, FondoInferior, Color.Black)))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(14.dp))
        if (onBack != null) {
            RoomHeader("Centro energético", onBack)
        } else {
            Text(
                "Centro energético",
                color = TextoPrincipal,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 14.dp)
            )
        }

        Text(
            "Consumos, costes y evolución de toda la vivienda",
            color = TextoSecundario,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(18.dp))

        if (!isLoading) {
            EnergyOverviewCard(
                overview = overview,
                summaries = summaries
            )
            Spacer(Modifier.height(16.dp))
            EnergyDistributionCard(summaries = summaries)
            Spacer(Modifier.height(24.dp))
            Text(
                "Contadores",
                color = TextoPrincipal,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(5.dp))
            Text(
                "Consulta el detalle, histórico y coste de cada suministro",
                color = TextoSecundario,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(13.dp))
        }

        if (isLoading) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = EnergyBlue)
                Spacer(Modifier.height(12.dp))
                Text("Cargando históricos…", color = TextoSecundario)
            }
        } else {
            summaries.chunked(2).forEach { rowSummaries ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowSummaries.forEach { summary ->
                        EnergySummaryCard(
                            summary = summary,
                            modifier = Modifier.weight(1f),
                            onClick = { onMeterSelected(summary.type) }
                        )
                    }
                    if (rowSummaries.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
            }
        }

        Spacer(Modifier.height(90.dp))
    }
}
