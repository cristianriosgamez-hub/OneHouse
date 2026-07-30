package com.onehouse.app.feature.climate

import com.onehouse.app.knx.KnxAddressBook
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
import com.onehouse.app.knx.KnxGroupAddress
import com.onehouse.app.knx.KnxCommandType
import com.onehouse.app.knx.KnxCommandExecutor
import com.onehouse.app.knx.KnxCommand
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    val commandExecutor = remember(context.applicationContext) {
        KnxCommandExecutor(context.applicationContext)
    }
    val appKnxConfiguration = remember(context.applicationContext) {
        com.onehouse.app.knx.AppKnxConfigurationRepository(context.applicationContext).also(KnxAddressBook::apply)
    }
    DisposableEffect(commandExecutor) { onDispose(commandExecutor::close) }
    val snapshot by repository.stateFlow.collectAsStateWithLifecycle(initialValue = repository.snapshot())
    val climate = snapshot.climate
    val exteriorWeather = rememberWeatherState()

    val confirmedMode = climate.mode.toClimateMode()
    val confirmedFanSpeed = climate.fanSpeed.toFanSpeed()

    // Estado local optimista: permite manejar la climatización aunque KNX no esté
    // disponible. Los valores confirmados por el bus sustituyen al estado temporal
    // cuando coinciden con la petición enviada.
    var requestedPower by remember { mutableStateOf<Boolean?>(null) }
    var requestedTemperature by remember { mutableStateOf<Float?>(null) }
    var requestedMode by remember { mutableStateOf<ClimateMode?>(null) }
    var requestedFanSpeed by remember { mutableStateOf<FanSpeed?>(null) }

    val enabled = requestedPower ?: (climate.powered == true)
    val targetTemperature = requestedTemperature ?: climate.targetTemperature ?: 21f
    val selectedMode = requestedMode ?: confirmedMode
    val selectedFanSpeed = requestedFanSpeed ?: confirmedFanSpeed

    LaunchedEffect(climate.powered, requestedPower) {
        if (requestedPower != null && climate.powered == requestedPower) requestedPower = null
    }
    LaunchedEffect(climate.targetTemperature, requestedTemperature) {
        val requested = requestedTemperature
        val confirmed = climate.targetTemperature
        if (requested != null && confirmed != null && kotlin.math.abs(requested - confirmed) < 0.05f) {
            requestedTemperature = null
        }
    }
    LaunchedEffect(confirmedMode, requestedMode) {
        if (requestedMode != null && confirmedMode == requestedMode) requestedMode = null
    }
    LaunchedEffect(confirmedFanSpeed, requestedFanSpeed) {
        if (requestedFanSpeed != null && confirmedFanSpeed == requestedFanSpeed) requestedFanSpeed = null
    }

    val diningTemperature = snapshot.numericAt(KnxAddressBook.Indoor.TEMPERATURE_DINING, "9.001")
        ?: snapshot.numericAt(KnxAddressBook.Climate.CURRENT_TEMPERATURE, "9.001")
    val suiteTemperature = snapshot.numericAt(KnxAddressBook.Indoor.TEMPERATURE_SUITE, "9.001")
    val humidity = snapshot.numericAt(KnxAddressBook.Indoor.HUMIDITY_DINING, "5.001")?.toInt()
    val co2Ppm = snapshot.numericAt(KnxAddressBook.Indoor.CO2_DINING, "9.008")?.toInt()

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
            selectedMode = selectedMode,
            onEnabledChange = { requested ->
                requestedPower = requested
                commandExecutor.execute(
                    command = KnxCommand(
                        if (requested) KnxCommandType.ON else KnxCommandType.OFF,
                        KnxGroupAddress.parse(KnxAddressBook.Climate.POWER_COMMAND),
                        "1.001"
                    ),
                    verificationAddress = KnxAddressBook.Climate.POWER_STATE
                ) { result ->
                    if (result is KnxCommandExecutor.Result.Failure) {
                        requestedPower = null
                    }
                }
            }
        )
        Spacer(Modifier.height(12.dp))

        TemperatureControl(
            targetTemperature = targetTemperature,
            enabled = true,
            mode = selectedMode,
            onDecrease = {
                val value = (targetTemperature - 0.5f).coerceIn(16f, 34f)
                requestedTemperature = value
                commandExecutor.execute(KnxCommand(KnxCommandType.SET_VALUE, KnxGroupAddress.parse(KnxAddressBook.Climate.TARGET_TEMPERATURE_COMMAND), "9.001", true, value.toString())) { }
            },
            onIncrease = {
                val value = (targetTemperature + 0.5f).coerceIn(16f, 34f)
                requestedTemperature = value
                commandExecutor.execute(KnxCommand(KnxCommandType.SET_VALUE, KnxGroupAddress.parse(KnxAddressBook.Climate.TARGET_TEMPERATURE_COMMAND), "9.001", true, value.toString())) { }
            }
        )
        Spacer(Modifier.height(20.dp))

        ClimateModeSelector(
            selectedMode = selectedMode,
            enabled = true,
            onModeSelected = { mode ->
                requestedMode = mode
                val code = when (mode) {
                    ClimateMode.AUTO -> 0
                    ClimateMode.HEAT -> 1
                    ClimateMode.COLD -> 3
                    ClimateMode.FAN -> 9
                    ClimateMode.DRY -> 14
                }
                commandExecutor.execute(KnxCommand(KnxCommandType.SET_VALUE, KnxGroupAddress.parse(KnxAddressBook.Climate.MODE_COMMAND), "20.105", true, code.toString())) { }
            }
        )
        Spacer(Modifier.height(24.dp))

        FanSpeedSelector(
            selectedSpeed = selectedFanSpeed,
            enabled = true,
            onSpeedSelected = { speed ->
                requestedFanSpeed = speed
                KnxAddressBook.apply(appKnxConfiguration)
                val percent = when (speed) {
                    FanSpeed.LOW -> KnxAddressBook.Climate.FAN_SPEED_LOW_VALUE
                    FanSpeed.MEDIUM -> KnxAddressBook.Climate.FAN_SPEED_MEDIUM_VALUE
                    FanSpeed.HIGH -> KnxAddressBook.Climate.FAN_SPEED_HIGH_VALUE
                }
                commandExecutor.execute(KnxCommand(KnxCommandType.SET_VALUE, KnxGroupAddress.parse(KnxAddressBook.Climate.FAN_SPEED_COMMAND), "5.001", true, percent.toString())) { }
            }
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
