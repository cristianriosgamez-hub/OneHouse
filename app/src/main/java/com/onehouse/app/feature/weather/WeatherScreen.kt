package com.onehouse.app.feature.weather

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onehouse.app.design.BordeTarjeta
import com.onehouse.app.design.FondoInferior
import com.onehouse.app.design.FondoSuperior
import com.onehouse.app.design.TextoPrincipal
import com.onehouse.app.design.TextoSecundario
import com.onehouse.app.feature.rooms.detail.RoomHeader

private val WeatherCardColor = Color(0xED091A28)
private val WeatherBlue = Color(0xFF2994FF)
private val WeatherGreen = Color(0xFF31D67B)
private val WeatherOrange = Color(0xFFFF8A22)
private val WeatherPurple = Color(0xFFB06CFF)

@Composable
fun WeatherScreen(onBack: (() -> Unit)? = null) {
    var state by remember { mutableStateOf(WeatherUiState()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(FondoSuperior, FondoInferior, Color.Black)))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(14.dp))
        if (onBack != null) {
            RoomHeader(title = "Tiempo", onBack = onBack)
        } else {
            Text(
                text = "Tiempo",
                color = TextoPrincipal,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 14.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        WeatherHeroCard(state)
        Spacer(Modifier.height(14.dp))
        WeatherMetricsGrid(state.metrics)
        Spacer(Modifier.height(14.dp))
        ForecastCard(state.forecast)
        Spacer(Modifier.height(14.dp))
        SunAndMoonCards()
        Spacer(Modifier.height(14.dp))
        AdditionalDetailsCard()
        Spacer(Modifier.height(96.dp))
    }
}

@Composable
private fun WeatherHeroCard(state: WeatherUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF0A4B78), Color(0xFF164D70), Color(0xFF10283A))
                ),
                RoundedCornerShape(26.dp)
            )
            .border(1.dp, BordeTarjeta, RoundedCornerShape(26.dp))
            .padding(18.dp)
    ) {
        Text(
            text = "⌖  ${state.location}",
            color = TextoPrincipal,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text("Actualizado: ${state.updatedAt}", color = TextoSecundario, fontSize = 13.sp)
        Spacer(Modifier.height(18.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(state.temperature, color = TextoPrincipal, fontSize = 48.sp, fontWeight = FontWeight.Light)
                Text(state.condition, color = TextoPrincipal, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Text("Sensación térmica ${state.feelsLike}", color = TextoPrincipal, fontSize = 14.sp)
                Spacer(Modifier.height(10.dp))
                Text("↑ ${state.high}    ↓ ${state.low}", color = TextoPrincipal, fontSize = 15.sp)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("☀", color = Color(0xFFFFC52B), fontSize = 58.sp)
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .background(Color(0xCC081725), RoundedCornerShape(18.dp))
                        .border(1.dp, BordeTarjeta, RoundedCornerShape(18.dp))
                        .padding(horizontal = 13.dp, vertical = 10.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Temperatura interior", color = TextoPrincipal, fontSize = 11.sp)
                        Text(state.indoorTemperature, color = WeatherBlue, fontSize = 23.sp, fontWeight = FontWeight.Medium)
                        Text("Confort ●", color = WeatherGreen, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherMetricsGrid(metrics: List<WeatherMetric>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        metrics.chunked(2).forEach { rowMetrics ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowMetrics.forEach { metric ->
                    val metricIndex = metrics.indexOf(metric)
                    val accent = when (metricIndex % 4) {
                        0 -> WeatherBlue
                        1 -> WeatherPurple
                        2 -> WeatherGreen
                        else -> WeatherOrange
                    }
                    WeatherMetricCard(metric, accent, Modifier.weight(1f))
                }
                if (rowMetrics.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun WeatherMetricCard(metric: WeatherMetric, accent: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(112.dp)
            .background(WeatherCardColor, RoundedCornerShape(22.dp))
            .border(1.dp, BordeTarjeta, RoundedCornerShape(22.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(metric.symbol, color = accent, fontSize = 28.sp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = metric.title,
                color = TextoPrincipal,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = metric.value,
                color = TextoPrincipal,
                fontSize = 23.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Text(
                text = metric.status,
                color = accent,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ForecastCard(forecast: List<DailyForecast>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(WeatherCardColor, RoundedCornerShape(24.dp))
            .border(1.dp, BordeTarjeta, RoundedCornerShape(24.dp))
            .padding(vertical = 18.dp)
    ) {
        Text(
            text = "Pronóstico 5 días",
            color = TextoPrincipal,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 18.dp)
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            forecast.forEach { day ->
                ForecastDay(day)
            }
        }
    }
}

@Composable
private fun ForecastDay(day: DailyForecast) {
    Column(
        modifier = Modifier
            .width(92.dp)
            .padding(horizontal = 5.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(day.day, color = TextoPrincipal, fontSize = 12.sp, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        Text(day.symbol, fontSize = 27.sp)
        Text(day.high, color = WeatherOrange, fontSize = 18.sp)
        Text(day.low, color = WeatherBlue, fontSize = 16.sp)
        Text(
            text = "◌ ${day.precipitation}",
            color = TextoSecundario,
            fontSize = 10.sp,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SunAndMoonCards() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SunCycleCard(modifier = Modifier.weight(1f))
        MoonPhaseCard(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SunCycleCard(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .height(156.dp)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF183247), Color(0xFF102637), Color(0xFF0A1A28))
                ),
                RoundedCornerShape(22.dp)
            )
            .border(1.dp, BordeTarjeta, RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(Color(0x22FFC52B), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("☀", color = Color(0xFFFFC52B), fontSize = 22.sp)
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Ciclo solar",
                color = TextoPrincipal,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(15.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TimePoint(label = "Salida", value = "06:20", accent = WeatherOrange)
            Text("—  ☀  —", color = Color(0x99FFC52B), fontSize = 13.sp)
            TimePoint(label = "Puesta", value = "21:08", accent = WeatherPurple)
        }
        Spacer(Modifier.height(10.dp))
        Text("14 h 48 min de luz", color = TextoSecundario, fontSize = 11.sp)
    }
}

@Composable
private fun TimePoint(label: String, value: String, accent: Color) {
    Column {
        Text(label, color = TextoSecundario, fontSize = 10.sp)
        Text(value, color = accent, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MoonPhaseCard(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .height(156.dp)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF18263E), Color(0xFF101D32), Color(0xFF091522))
                ),
                RoundedCornerShape(22.dp)
            )
            .border(1.dp, BordeTarjeta, RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(Color(0x22B06CFF), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("◐", color = WeatherPurple, fontSize = 25.sp)
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Fase lunar",
                color = TextoPrincipal,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(14.dp))
        Text("Luna creciente", color = TextoPrincipal, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .background(Color(0xFF172C3E), RoundedCornerShape(50))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.32f)
                    .height(7.dp)
                    .background(
                        Brush.horizontalGradient(listOf(WeatherPurple, WeatherBlue)),
                        RoundedCornerShape(50)
                    )
            )
        }
        Spacer(Modifier.height(8.dp))
        Text("32% iluminada", color = WeatherPurple, fontSize = 12.sp)
    }
}

@Composable
private fun AdditionalDetailsCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF0D2535), Color(0xFF091A28))
                ),
                RoundedCornerShape(24.dp)
            )
            .border(1.dp, BordeTarjeta, RoundedCornerShape(24.dp))
            .padding(18.dp)
    ) {
        Text(
            text = "Detalles adicionales",
            color = TextoPrincipal,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(14.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DetailTile("◉", "Presión", "1016 hPa", WeatherBlue, Modifier.weight(1f))
            DetailTile("◇", "Punto de rocío", "18.6°C", WeatherPurple, Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DetailTile("≈", "Sensación", "27.8°C", WeatherOrange, Modifier.weight(1f))
            DetailTile("☀", "Índice de calor", "28.3°C", WeatherGreen, Modifier.weight(1f))
        }
    }
}

@Composable
private fun DetailTile(
    symbol: String,
    title: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(78.dp)
            .background(Color(0xB30B1D2B), RoundedCornerShape(17.dp))
            .border(1.dp, Color(0x443B6D8C), RoundedCornerShape(17.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(accent.copy(alpha = 0.13f), RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(symbol, color = accent, fontSize = 18.sp)
        }
        Spacer(Modifier.width(9.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextoSecundario,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                color = TextoPrincipal,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

