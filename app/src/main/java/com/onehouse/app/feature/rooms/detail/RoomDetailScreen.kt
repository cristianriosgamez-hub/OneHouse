package com.onehouse.app.feature.rooms.detail

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.onehouse.app.R
import com.onehouse.app.design.FondoInferior
import com.onehouse.app.design.FondoSuperior
import com.onehouse.app.device.ImportedKnxDevice
import com.onehouse.app.feature.home.state.HomeStateMapper
import com.onehouse.app.knx.KnxCommandExecutor
import com.onehouse.app.knx.KnxCommandType
import com.onehouse.app.knx.KnxHomeStateRepository

@Composable
fun RoomDetailScreen(roomType: RoomType, onBack: () -> Unit) {
    val context = LocalContext.current
    val commandExecutor = remember { KnxCommandExecutor(context) }
    val homeRepository = remember { KnxHomeStateRepository(context) }
    val homeSnapshot by homeRepository.stateFlow.collectAsState(initial = homeRepository.snapshot())
    val roomState = remember(homeSnapshot, roomType) { HomeStateMapper.room(homeSnapshot, roomType) }

    var contentVisible by remember(roomType) { mutableStateOf(false) }
    var lastBlindCommand by remember(roomType) { mutableStateOf(BlindCommand.STOP) }
    val pendingLights = remember(roomType) { mutableStateMapOf<String, Boolean>() }

    // Un telegrama nuevo siempre tiene prioridad sobre la predicción local del interruptor.
    LaunchedEffect(homeSnapshot.states) {
        pendingLights.clear()
    }
    LaunchedEffect(roomType) { contentVisible = true }

    DisposableEffect(commandExecutor) {
        onDispose { commandExecutor.close() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .background(Brush.verticalGradient(listOf(FondoSuperior, FondoInferior, Color.Black)))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(14.dp))
        RoomHeader(title = HomeStateMapper.roomName(roomType), onBack = onBack)
        Spacer(Modifier.height(16.dp))

        RoomHeroCard(
            type = roomType,
            lightOn = roomState.lights.any { light ->
                pendingLights[light.device.id] ?: light.isOn
            },
            imageRes = roomImage(roomType)
        )

        Spacer(Modifier.height(14.dp))

        RoomControlAnimated(contentVisible) {
            Column {
                roomState.climate?.let { climate ->
                    RoomClimateCard(
                        powered = climate.powered,
                        currentTemperature = climate.currentTemperature,
                        targetTemperature = climate.targetTemperature,
                        mode = climate.mode,
                        fanSpeed = climate.fanSpeed
                    )
                    Spacer(Modifier.height(12.dp))
                } ?: roomState.temperatureCelsius?.let { temperature ->
                    RoomTemperatureCard(temperature = temperature)
                    Spacer(Modifier.height(12.dp))
                }

                roomState.lights.forEachIndexed { index, light ->
                    if (index > 0) Spacer(Modifier.height(12.dp))
                    val enabled = pendingLights[light.device.id] ?: light.isOn
                    RoomLightCard(
                        title = light.device.name,
                        symbol = when (index) {
                            1 -> "☼"
                            2 -> "♧"
                            else -> "☼"
                        },
                        enabled = enabled,
                        onEnabledChange = { requested ->
                            pendingLights[light.device.id] = requested
                            executeBoolean(commandExecutor, light.device, requested)
                        }
                    )
                }

                roomState.blind?.let { blind ->
                    Spacer(Modifier.height(12.dp))
                    RoomBlindCard(
                        lastCommand = lastBlindCommand,
                        positionPercent = blind.positionPercent,
                        onCommand = { command ->
                            lastBlindCommand = command
                            val type = when (command) {
                                BlindCommand.UP -> KnxCommandType.UP
                                BlindCommand.STOP -> KnxCommandType.STOP
                                BlindCommand.DOWN -> KnxCommandType.DOWN
                            }
                            blind.device.commands.firstOrNull { it.type == type }?.let { knxCommand ->
                                commandExecutor.execute(knxCommand) { }
                            }
                        }
                    )
                }

                when (roomType) {
                    RoomType.KITCHEN,
                    RoomType.BATHROOM -> {
                        Spacer(Modifier.height(12.dp))
                        RoomFloodSensorCard(floodDetected = false)
                    }
                    RoomType.ENTRANCE -> {
                        Spacer(Modifier.height(12.dp))
                        RoomPirCard(blocked = false, onBlockedChange = { })
                    }
                    else -> Unit
                }
            }
        }

        Spacer(Modifier.height(30.dp))
    }
}

private fun executeBoolean(
    executor: KnxCommandExecutor,
    device: ImportedKnxDevice,
    enabled: Boolean
) {
    val type = if (enabled) KnxCommandType.ON else KnxCommandType.OFF
    device.commands.firstOrNull { it.type == type }?.let { command ->
        executor.execute(command) { }
    }
}

@DrawableRes
private fun roomImage(roomType: RoomType): Int? = when (roomType) {
    RoomType.BATHROOM -> R.drawable.room_bathroom
    RoomType.KITCHEN -> R.drawable.room_kitchen
    RoomType.BEDROOM_1 -> R.drawable.room_bedroom_1
    RoomType.DINING_ROOM -> R.drawable.room_dining
    RoomType.SUITE -> R.drawable.room_suite
    RoomType.ENTRANCE -> R.drawable.room_entrance
    RoomType.HALLWAY -> R.drawable.room_hallway
    RoomType.STORAGE -> R.drawable.room_storage
}
