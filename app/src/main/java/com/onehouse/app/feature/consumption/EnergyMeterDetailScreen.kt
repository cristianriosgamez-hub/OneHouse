package com.onehouse.app.feature.consumption

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onehouse.app.data.energy.EnergyDashboardState
import com.onehouse.app.data.energy.MeterType
import com.onehouse.app.data.energy.ReadingSource
import com.onehouse.app.data.local.EnergyReadingEntity
import com.onehouse.app.design.BordeTarjeta
import com.onehouse.app.design.FondoInferior
import com.onehouse.app.design.FondoSuperior
import com.onehouse.app.design.TextoPrincipal
import com.onehouse.app.design.TextoSecundario
import com.onehouse.app.feature.rooms.detail.RoomHeader
import java.util.Date

@Composable
internal fun EnergyMeterDetailScreen(
    state: EnergyDashboardState,
    onBack: () -> Unit,
    onPeriodSelected: (com.onehouse.app.data.energy.EnergyPeriod) -> Unit,
    onAddReading: () -> Unit,
    onEditReading: (EnergyReadingEntity) -> Unit,
    onDeleteReading: (EnergyReadingEntity) -> Unit
) {
    val type = state.selectedType ?: return
    val accent = accentFor(type)
    val latest = state.summaries.firstOrNull { it.type == type }?.latestReading

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(FondoSuperior, FondoInferior, Color.Black)))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(14.dp))
        RoomHeader(type.title, onBack)
        Spacer(Modifier.height(10.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(EnergyCard, RoundedCornerShape(26.dp))
                .border(1.dp, BordeTarjeta, RoundedCornerShape(26.dp))
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(accent.copy(alpha = 0.16f), RoundedCornerShape(17.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(type.symbol, color = accent, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(13.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Última lectura", color = TextoSecundario, fontSize = 11.sp)
                    Text(
                        latest?.meterValue?.let { "${formatNumber(it)} ${type.unit}" } ?: "Sin lectura",
                        color = TextoPrincipal,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    latest?.let {
                        Text(formatDate(it.timestamp), color = TextoSecundario, fontSize = 11.sp)
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Button(onClick = onAddReading, modifier = Modifier.fillMaxWidth()) {
                Text("＋ Añadir lectura")
            }
        }

        Spacer(Modifier.height(14.dp))
        PeriodSelector(state.selectedPeriod, onPeriodSelected)
        Spacer(Modifier.height(14.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(EnergyCard, RoundedCornerShape(24.dp))
                .border(1.dp, BordeTarjeta, RoundedCornerShape(24.dp))
                .padding(18.dp)
        ) {
            Text("Evolución", color = TextoPrincipal, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(
                if (state.chartPoints.isNotEmpty()) {
                    "${formatMonth(state.chartPoints.first().timestamp)} — ${formatMonth(state.chartPoints.last().timestamp)}"
                } else {
                    "Sin datos para mostrar"
                },
                color = TextoSecundario,
                fontSize = 11.sp
            )
            Spacer(Modifier.height(14.dp))
            EnergyLineChart(
                points = state.chartPoints.takeLast(36),
                accent = accent,
                modifier = Modifier.fillMaxWidth().height(190.dp)
            )
        }

        Spacer(Modifier.height(14.dp))
        StatisticsPanel(type, state)
        Spacer(Modifier.height(14.dp))
        ReadingsPanel(
            readings = state.selectedReadings.takeLast(12).reversed(),
            onEdit = onEditReading,
            onDelete = onDeleteReading
        )
        Spacer(Modifier.height(90.dp))
    }
}

@Composable
private fun StatisticsPanel(type: MeterType, state: EnergyDashboardState) {
    val stats = state.statistics
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(EnergyCard, RoundedCornerShape(24.dp))
            .border(1.dp, BordeTarjeta, RoundedCornerShape(24.dp))
            .padding(18.dp)
    ) {
        Text("Estadísticas", color = TextoPrincipal, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DetailStat("Total", formatNumber(stats.totalConsumption), type.unit, Modifier.weight(1f))
            DetailStat("Coste", formatNumber(stats.totalCost), "€", Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DetailStat("Media", stats.average?.let(::formatNumber) ?: "—", type.unit, Modifier.weight(1f))
            DetailStat("Máximo", stats.maximum?.let(::formatNumber) ?: "—", type.unit, Modifier.weight(1f))
            DetailStat("Mínimo", stats.minimum?.let(::formatNumber) ?: "—", type.unit, Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        TrendLabel(stats.variationPercent)
    }
}

@Composable
private fun DetailStat(title: String, value: String, unit: String, modifier: Modifier) {
    Column(
        modifier = modifier
            .background(EnergyCardAlt, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Text(title, color = TextoSecundario, fontSize = 10.sp)
        Text(value, color = TextoPrincipal, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text(unit, color = TextoSecundario, fontSize = 10.sp)
    }
}

@Composable
private fun ReadingsPanel(
    readings: List<EnergyReadingEntity>,
    onEdit: (EnergyReadingEntity) -> Unit,
    onDelete: (EnergyReadingEntity) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(EnergyCard, RoundedCornerShape(24.dp))
            .border(1.dp, BordeTarjeta, RoundedCornerShape(24.dp))
            .padding(18.dp)
    ) {
        Text("Últimas lecturas", color = TextoPrincipal, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        if (readings.isEmpty()) {
            Text("No hay lecturas en este periodo", color = TextoSecundario, fontSize = 13.sp)
        }
        readings.forEachIndexed { index, reading ->
            if (index > 0) Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(EnergyCardAlt, RoundedCornerShape(16.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(formatDate(reading.timestamp), color = TextoPrincipal, fontWeight = FontWeight.Medium)
                    Text(
                        ReadingSource.entries.firstOrNull { it.storageValue == reading.source }?.label ?: reading.source,
                        color = TextoSecundario,
                        fontSize = 10.sp
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        reading.consumption?.let { "${formatNumber(it)} ${reading.unit}" } ?: "—",
                        color = TextoPrincipal,
                        fontWeight = FontWeight.Bold
                    )
                    reading.cost?.let { Text("${formatNumber(it)} €", color = TextoSecundario, fontSize = 10.sp) }
                }
                if (reading.source == ReadingSource.MANUAL.storageValue) {
                    Spacer(Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Editar", color = EnergyBlue, fontSize = 11.sp, modifier = Modifier.clickable { onEdit(reading) }.padding(3.dp))
                        Text("Borrar", color = EnergyRed, fontSize = 11.sp, modifier = Modifier.clickable { onDelete(reading) }.padding(3.dp))
                    }
                }
            }
        }
    }
}

@Composable
internal fun ReadingEditorDialog(
    type: MeterType,
    reading: EnergyReadingEntity?,
    onDismiss: () -> Unit,
    onSave: (EnergyReadingEntity) -> Unit
) {
    var dateText by remember(reading) {
        mutableStateOf(reading?.let { inputDateFormat.format(Date(it.timestamp)) } ?: inputDateFormat.format(Date()))
    }
    var meterText by remember(reading) { mutableStateOf(reading?.meterValue?.let(::plainNumber) ?: "") }
    var consumptionText by remember(reading) { mutableStateOf(reading?.consumption?.let(::plainNumber) ?: "") }
    var costText by remember(reading) { mutableStateOf(reading?.cost?.let(::plainNumber) ?: "") }
    var noteText by remember(reading) { mutableStateOf(reading?.note.orEmpty()) }
    var error by remember(reading) { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (reading == null) "Añadir lectura" else "Editar lectura") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text(type.title, color = accentFor(type), fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("Fecha (dd/MM/yyyy)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = meterText,
                    onValueChange = { meterText = it },
                    label = { Text("Lectura del contador (${type.unit})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = consumptionText,
                    onValueChange = { consumptionText = it },
                    label = { Text("Consumo del periodo (${type.unit})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = costText,
                    onValueChange = { costText = it },
                    label = { Text("Coste opcional (€)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Observaciones") }
                )
                error?.let { Text(it, color = EnergyRed, fontSize = 12.sp) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val timestamp = runCatching { inputDateFormat.parse(dateText)?.time }.getOrNull()
                val meter = meterText.toNormalizedDoubleOrNull()
                val consumption = consumptionText.toNormalizedDoubleOrNull()
                val cost = costText.toNormalizedDoubleOrNull()
                when {
                    timestamp == null -> error = "La fecha no es válida."
                    meter == null && consumption == null -> error = "Introduce la lectura o el consumo del periodo."
                    meter != null && meter < 0 -> error = "La lectura no puede ser negativa."
                    consumption != null && consumption < 0 -> error = "El consumo no puede ser negativo."
                    cost != null && cost < 0 -> error = "El coste no puede ser negativo."
                    else -> onSave(
                        EnergyReadingEntity(
                            id = reading?.id ?: 0,
                            meterType = type.storageValue,
                            timestamp = timestamp,
                            meterValue = meter,
                            consumption = consumption,
                            cost = cost,
                            unit = type.unit,
                            source = ReadingSource.MANUAL.storageValue,
                            note = noteText.trim().takeIf { it.isNotEmpty() }
                        )
                    )
                }
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
