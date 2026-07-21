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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ClimateScreen(
    onBack: () -> Unit
) {
    var showProgramming by remember {
        mutableStateOf(false)
    }

    if (showProgramming) {
        ClimateScheduleScreen(
            onBack = { showProgramming = false }
        )
        return
    }

    var enabled by remember {
        mutableStateOf(true)
    }

    var targetTemperature by remember {
        mutableFloatStateOf(22.5f)
    }

    var selectedMode by remember {
        mutableStateOf(ClimateMode.COLD)
    }

    var selectedFanSpeed by remember {
        mutableStateOf(FanSpeed.MEDIUM)
    }

    // Valores temporales: se sustituirán por Google Weather API y KNX.
    val exteriorTemperature = 18.5f
    val co2Ppm = 560

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .background(
                brush = Brush.verticalGradient(
                    listOf(
                        ClimateBackgroundTop,
                        ClimateBackgroundBottom
                    )
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        ClimateHeader(
            exteriorTemperature = exteriorTemperature,
            onBack = onBack
        )

        Spacer(modifier = Modifier.height(18.dp))

        AmbientTemperatures(
            diningTemperature = 22.4f,
            suiteTemperature = 22.1f
        )

        Spacer(modifier = Modifier.height(12.dp))

        ClimateSystemCard(
            enabled = enabled,
            selectedMode = selectedMode,
            onEnabledChange = {
                enabled = it
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        TemperatureControl(
            targetTemperature = targetTemperature,
            enabled = enabled,
            mode = selectedMode,
            onDecrease = {
                targetTemperature =
                    (targetTemperature - 0.5f).coerceAtLeast(16f)
            },
            onIncrease = {
                targetTemperature =
                    (targetTemperature + 0.5f).coerceAtMost(34f)
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        ClimateModeSelector(
            selectedMode = selectedMode,
            enabled = enabled,
            onModeSelected = {
                selectedMode = it
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        FanSpeedSelector(
            selectedSpeed = selectedFanSpeed,
            enabled = enabled,
            onSpeedSelected = {
                selectedFanSpeed = it
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        ClimateInformationCard(
            fanSpeed = selectedFanSpeed,
            humidity = 45,
            selectedMode = selectedMode,
            co2Ppm = co2Ppm
        )

        Spacer(modifier = Modifier.height(14.dp))

        ProgrammingCard(onClick = { showProgramming = true })

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
private fun ClimateHeader(
    exteriorTemperature: Float,
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
                .padding(
                    horizontal = 10.dp,
                    vertical = 2.dp
                )
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

            Spacer(modifier = Modifier.height(7.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Sistema centralizado",
                    color = ClimateTextSecondary,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.size(9.dp))

                Box(
                    modifier = Modifier
                        .background(
                            brush = Brush.horizontalGradient(
                                listOf(
                                    ClimateCyan.copy(alpha = 0.14f),
                                    ClimateBlue.copy(alpha = 0.10f)
                                )
                            ),
                            shape = RoundedCornerShape(50)
                        )
                        .border(
                            width = 1.dp,
                            color = ClimateCyan.copy(alpha = 0.24f),
                            shape = RoundedCornerShape(50)
                        )
                        .padding(
                            horizontal = 10.dp,
                            vertical = 5.dp
                        )
                ) {
                    Text(
                        text = "🌤  ${String.format("%.1f", exteriorTemperature)} °C",
                        color = ClimateText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

    }
}
