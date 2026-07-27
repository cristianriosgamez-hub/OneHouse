package com.onehouse.app.feature.climate

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onehouse.app.feature.weather.WeatherUiState
import com.onehouse.app.feature.weather.rememberWeatherState
import com.onehouse.app.knx.KnxHomeStateRepository

@Composable
fun ClimateScreen(onBack: () -> Unit) {
    var showProgramming by remember { mutableStateOf(false) }
    if (showProgramming) {
        ClimateScheduleScreen(onBack = { showProgramming = false })
        return
    }

    val context = LocalContext.current
    val repository = remember(context.applicationContext) {
        KnxHomeStateRepository(context.applicationContext)
    }
    val snapshot by repository.stateFlow.collectAsState(initial = repository.snapshot())
    val climate = snapshot.climate
    val exteriorWeather = rememberWeatherState()

    val selectedMode = climate.mode.toClimateMode()
    val selectedFanSpeed = climate.fanSpeed.toFanSpeed()
    val enabled = climate.powered == true

    val diningTemperature = snapshot.numericAt("5/1/3", "9.001")
        ?: snapshot.numericAt("5/3/3", "9.001")
    val suiteTemperature = snapshot.numericAt("5/1/4", "9.001")
    val humidity = snapshot.numericAt("5/1/2", "5.001")?.toInt()
    val co2Ppm = snapshot.numericAt("5/1/1", "14.000")?.toInt()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .background(Brush.verticalGradient(listOf(ClimateBackgroundTop, ClimateBackgroundBottom)))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(14.dp))
        ClimateHeader(exteriorWeather = exteriorWeather, onBack = onBack)
        Spacer(Modifier.height(18.dp))

        AmbientTemperatures(
            diningTemperature = diningTemperature,
            suiteTemperature = suiteTemperature
        )
        Spacer(Modifier.height(12.dp))

        ClimateSystemCard(
            enabled = enabled,
            selectedMode = selectedMode ?: ClimateMode.COLD,
            onEnabledChange = { }
        )
        Spacer(Modifier.height(12.dp))

        TemperatureControl(
            targetTemperature = climate.targetTemperature,
            enabled = enabled && climate.targetTemperature != null,
            mode = selectedMode ?: ClimateMode.COLD,
            onDecrease = { },
            onIncrease = { }
        )
        Spacer(Modifier.height(20.dp))

        ClimateModeSelector(
            selectedMode = selectedMode ?: ClimateMode.COLD,
            enabled = false,
            onModeSelected = { }
        )
        Spacer(Modifier.height(24.dp))

        FanSpeedSelector(
            selectedSpeed = selectedFanSpeed ?: FanSpeed.MEDIUM,
            enabled = false,
            onSpeedSelected = { }
        )
        Spacer(Modifier.height(16.dp))

        ClimateInformationCard(
            fanSpeed = selectedFanSpeed,
            humidity = humidity,
            selectedMode = selectedMode,
            co2Ppm = co2Ppm
        )
        Spacer(Modifier.height(14.dp))

        ProgrammingCard(onClick = { showProgramming = true })
        Spacer(Modifier.height(30.dp))
    }
}

private fun String?.toClimateMode(): ClimateMode? = when (this?.trim()?.lowercase()) {
    "frío", "frio", "cold", "cool" -> ClimateMode.COLD
    "calor", "heat", "heating" -> ClimateMode.HEAT
    "vent.", "vent", "fan", "ventilador" -> ClimateMode.FAN
    "dry", "seco", "deshumidificación", "deshumidificacion" -> ClimateMode.DRY
    "auto", "automático", "automatico" -> ClimateMode.AUTO
    else -> null
}

private fun String?.toFanSpeed(): FanSpeed? = when (this?.trim()?.lowercase()) {
    "baja", "low", "1" -> FanSpeed.LOW
    "media", "medium", "2" -> FanSpeed.MEDIUM
    "alta", "high", "3" -> FanSpeed.HIGH
    else -> null
}

@Composable
private fun ClimateHeader(exteriorWeather: WeatherUiState, onBack: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "‹",
            color = ClimateText,
            fontSize = 40.sp,
            fontWeight = FontWeight.Light,
            modifier = Modifier.clip(CircleShape).clickable(onClick = onBack)
                .padding(horizontal = 10.dp, vertical = 2.dp)
        )
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Climatización", color = ClimateText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(7.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Text("Sistema centralizado", color = ClimateTextSecondary, fontSize = 12.sp)
                Spacer(Modifier.size(9.dp))
                Box(
                    modifier = Modifier
                        .background(
                            Brush.horizontalGradient(listOf(ClimateCyan.copy(alpha = 0.14f), ClimateBlue.copy(alpha = 0.10f))),
                            RoundedCornerShape(50)
                        )
                        .border(1.dp, ClimateCyan.copy(alpha = 0.24f), RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        "${exteriorWeather.conditionSymbol}  ${exteriorWeather.temperature}",
                        color = ClimateText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
