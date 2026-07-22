package com.onehouse.app.feature.consumption

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onehouse.app.data.energy.EnergyChartPoint
import com.onehouse.app.data.energy.EnergyOverview
import com.onehouse.app.data.energy.EnergyPeriod
import com.onehouse.app.data.energy.MeterSummary
import com.onehouse.app.data.energy.MeterType
import com.onehouse.app.design.BordeTarjeta
import com.onehouse.app.design.TextoPrincipal
import com.onehouse.app.design.TextoSecundario
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal val EnergyBlue = Color(0xFF168EFF)
internal val EnergyOrange = Color(0xFFFF9A3D)
internal val EnergyRed = Color(0xFFFF4D56)
internal val EnergyAqua = Color(0xFF35D6C7)
internal val EnergyGreen = Color(0xFF5BD18A)
internal val EnergyCard = Color(0xFF0A1926)
internal val EnergyCardAlt = Color(0xFF0D2130)

@Composable
internal fun EnergyOverviewCard(
    overview: EnergyOverview,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(EnergyCard, RoundedCornerShape(26.dp))
            .border(1.dp, EnergyBlue.copy(alpha = 0.32f), RoundedCornerShape(26.dp))
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Resumen anual",
                    color = TextoSecundario,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${formatNumber(overview.electricityYearKwh)} kWh",
                    color = TextoPrincipal,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(EnergyBlue.copy(alpha = 0.16f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("ϟ", color = EnergyBlue, fontSize = 27.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CompactMetric(
                "Agua anual",
                "${formatNumber(overview.waterYearM3)} m³",
                Modifier.weight(1f)
            )
            CompactMetric(
                "Coste estimado",
                "${formatNumber(overview.totalYearCost)} €",
                Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(10.dp))
        val status = when {
            overview.totalReadings == 0 -> "Todavía no hay lecturas registradas"
            overview.lastUpdatedAt != null ->
                "${overview.metersWithData}/${MeterType.entries.size} contadores · Actualizado ${formatDate(overview.lastUpdatedAt)}"
            else -> "${overview.totalReadings} lecturas registradas este año"
        }
        Text(status, color = TextoSecundario, fontSize = 11.sp)
    }
}

@Composable
internal fun EnergySummaryCard(
    summary: MeterSummary,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val accent = accentFor(summary.type)
    Column(
        modifier = modifier
            .background(EnergyCard, RoundedCornerShape(24.dp))
            .border(1.dp, BordeTarjeta, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(17.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(accent.copy(alpha = 0.15f), RoundedCornerShape(15.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(summary.type.symbol, color = accent, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(summary.type.shortTitle, color = TextoPrincipal, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    summary.latestReading?.let { "Actualizado ${formatDate(it.timestamp)}" } ?: "Sin lecturas",
                    color = TextoSecundario,
                    fontSize = 10.sp
                )
            }
            Text("›", color = accent, fontSize = 30.sp)
        }

        Spacer(Modifier.height(16.dp))
        Text("Consumo este mes", color = TextoSecundario, fontSize = 11.sp)
        Text(
            "${formatNumber(summary.monthConsumption)} ${summary.type.unit}",
            color = TextoPrincipal,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CompactMetric("Año", "${formatNumber(summary.yearConsumption)} ${summary.type.unit}", Modifier.weight(1f))
            CompactMetric("Coste", "${formatNumber(summary.yearCost)} €", Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        TrendLabel(summary.yearVariationPercent)
    }
}

@Composable
private fun CompactMetric(title: String, value: String, modifier: Modifier) {
    Column(
        modifier = modifier
            .background(EnergyCardAlt, RoundedCornerShape(15.dp))
            .padding(11.dp)
    ) {
        Text(title, color = TextoSecundario, fontSize = 10.sp)
        Text(value, color = TextoPrincipal, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun TrendLabel(variation: Double?) {
    val color = when {
        variation == null -> TextoSecundario
        variation <= 0 -> EnergyGreen
        else -> EnergyRed
    }
    val text = variation?.let {
        val arrow = if (it <= 0) "↓" else "↑"
        "$arrow ${String.format(Locale.getDefault(), "%.1f", kotlin.math.abs(it))}% frente al año anterior"
    } ?: "Sin comparativa anual"
    Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.Medium)
}

@Composable
internal fun PeriodSelector(
    selected: EnergyPeriod,
    onSelected: (EnergyPeriod) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(EnergyCardAlt, RoundedCornerShape(16.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        EnergyPeriod.entries.forEach { period ->
            val active = selected == period
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (active) EnergyBlue else Color.Transparent,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onSelected(period) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    period.label,
                    color = if (active) Color.White else TextoSecundario,
                    fontSize = 12.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
internal fun EnergyLineChart(
    points: List<EnergyChartPoint>,
    accent: Color,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("No hay datos en este periodo", color = TextoSecundario, fontSize = 13.sp)
        }
        return
    }

    val values = points.map { it.value }
    val min = values.minOrNull() ?: 0.0
    val max = values.maxOrNull() ?: 0.0
    val range = (max - min).takeIf { it > 0.0 } ?: 1.0

    Canvas(modifier = modifier) {
        val horizontalPadding = 8.dp.toPx()
        val verticalPadding = 12.dp.toPx()
        val widthAvailable = size.width - horizontalPadding * 2
        val heightAvailable = size.height - verticalPadding * 2

        for (index in 0..3) {
            val y = verticalPadding + heightAvailable * index / 3f
            drawLine(
                color = BordeTarjeta.copy(alpha = 0.5f),
                start = Offset(horizontalPadding, y),
                end = Offset(size.width - horizontalPadding, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        val path = Path()
        points.forEachIndexed { index, point ->
            val x = if (points.size == 1) size.width / 2f
            else horizontalPadding + widthAvailable * index / (points.lastIndex.toFloat())
            val normalized = ((point.value - min) / range).toFloat()
            val y = verticalPadding + heightAvailable * (1f - normalized)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(path, color = accent, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))

        points.forEachIndexed { index, point ->
            val x = if (points.size == 1) size.width / 2f
            else horizontalPadding + widthAvailable * index / (points.lastIndex.toFloat())
            val normalized = ((point.value - min) / range).toFloat()
            val y = verticalPadding + heightAvailable * (1f - normalized)
            drawCircle(color = accent, radius = 3.5.dp.toPx(), center = Offset(x, y))
        }
    }
}

internal fun accentFor(type: MeterType): Color = when (type) {
    MeterType.ENDESA -> EnergyBlue
    MeterType.CLIMATIZATION -> EnergyOrange
    MeterType.ACS -> EnergyRed
    MeterType.AGBAR -> EnergyAqua
}

internal val displayDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
internal val inputDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply { isLenient = false }
internal val monthDateFormat = SimpleDateFormat("MMM yy", Locale.getDefault())

internal fun formatDate(timestamp: Long): String = displayDateFormat.format(Date(timestamp))
internal fun formatMonth(timestamp: Long): String = monthDateFormat.format(Date(timestamp))
internal fun formatNumber(value: Double): String = String.format(Locale.getDefault(), "%.2f", value)
internal fun plainNumber(value: Double): String = value.toString().replace('.', ',')
internal fun String.toNormalizedDoubleOrNull(): Double? =
    trim().takeIf { it.isNotEmpty() }?.replace(',', '.')?.toDoubleOrNull()
