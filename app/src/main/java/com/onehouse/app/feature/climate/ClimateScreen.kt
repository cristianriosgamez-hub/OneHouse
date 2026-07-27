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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .background(
                Brush.verticalGradient(
                    listOf(ClimateBackgroundTop, ClimateBackgroundBottom)
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(14.dp))
        ClimateHeader(exteriorWeather = exteriorWeather, onBack = onBack)
        Spacer(Modifier.height(18.dp))

        ClimateRealStateCard(
            powered = climate.powered,
            currentTemperature = climate.currentTemperature,
            targetTemperature = climate.targetTemperature,
            mode = climate.mode,
            fanSpeed = climate.fanSpeed
        )

        Spacer(Modifier.height(14.dp))
        ClimateKnxAddressesCard()
        Spacer(Modifier.height(14.dp))
        ProgrammingCard(onClick = { showProgramming = true })
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun ClimateRealStateCard(
    powered: Boolean?,
    currentTemperature: Float?,
    targetTemperature: Float?,
    mode: String?,
    fanSpeed: String?
) {
    val hasAnyData = powered != null ||
        currentTemperature != null ||
        targetTemperature != null ||
        mode != null ||
        fanSpeed != null

    PremiumCard {
        Column {
            Text(
                text = "Estado real KNX",
                color = ClimateText,
                fontSize = 21.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = if (hasAnyData) {
                    "Valores recibidos desde la instalación"
                } else {
                    "Esperando respuestas del bus KNX"
                },
                color = ClimateTextSecondary,
                fontSize = 12.sp
            )

            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ClimateRealValue(
                    modifier = Modifier.weight(1f),
                    label = "Sistema",
                    value = when (powered) {
                        true -> "Encendido"
                        false -> "Apagado"
                        null -> "--"
                    }
                )
                ClimateRealValue(
                    modifier = Modifier.weight(1f),
                    label = "Temperatura",
                    value = currentTemperature?.let { "%.1f °C".format(it) } ?: "--"
                )
            }

            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ClimateRealValue(
                    modifier = Modifier.weight(1f),
                    label = "Consigna",
                    value = targetTemperature?.let { "%.1f °C".format(it) } ?: "--"
                )
                ClimateRealValue(
                    modifier = Modifier.weight(1f),
                    label = "Modo",
                    value = mode ?: "--"
                )
                ClimateRealValue(
                    modifier = Modifier.weight(1f),
                    label = "Ventilador",
                    value = fanSpeed ?: "--"
                )
            }
        }
    }
}

@Composable
private fun ClimateRealValue(
    modifier: Modifier,
    label: String,
    value: String
) {
    Column(
        modifier = modifier
            .background(
                ClimateCardSecondary.copy(alpha = 0.75f),
                RoundedCornerShape(16.dp)
            )
            .border(
                1.dp,
                ClimateCyan.copy(alpha = 0.16f),
                RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 12.dp, vertical = 14.dp)
    ) {
        Text(label, color = ClimateTextSecondary, fontSize = 10.sp)
        Spacer(Modifier.height(5.dp))
        Text(
            value,
            color = ClimateText,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ClimateKnxAddressesCard() {
    PremiumCard {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(
                "Objetos de estado",
                color = ClimateText,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
            ClimateAddressRow("ON/OFF", "5/3/2")
            ClimateAddressRow("Temperatura ambiente", "5/3/3")
            ClimateAddressRow("Modo", "5/3/4")
            ClimateAddressRow("Ventilador", "5/3/5")
            ClimateAddressRow("Consigna", "5/3/10")
        }
    }
}

@Composable
private fun ClimateAddressRow(label: String, address: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, color = ClimateTextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(address, color = ClimateCyan, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ClimateHeader(
    exteriorWeather: WeatherUiState,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "‹",
            color = ClimateText,
            fontSize = 40.sp,
            fontWeight = FontWeight.Light,
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onBack)
                .padding(horizontal = 10.dp, vertical = 2.dp)
        )

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Climatización",
                color = ClimateText,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(7.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Sistema centralizado",
                    color = ClimateTextSecondary,
                    fontSize = 12.sp
                )
                Spacer(Modifier.padding(horizontal = 4.dp))
                Box(
                    modifier = Modifier
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    ClimateCyan.copy(alpha = 0.14f),
                                    ClimateBlue.copy(alpha = 0.10f)
                                )
                            ),
                            RoundedCornerShape(50)
                        )
                        .border(
                            1.dp,
                            ClimateCyan.copy(alpha = 0.24f),
                            RoundedCornerShape(50)
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "${exteriorWeather.conditionSymbol}  ${exteriorWeather.temperature}",
                        color = ClimateText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
