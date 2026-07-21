package com.onehouse.app.feature.rooms.detail

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.onehouse.app.R
import com.onehouse.app.design.FondoInferior
import com.onehouse.app.design.FondoSuperior

@Composable
fun RoomDetailScreen(roomType: RoomType, onBack: () -> Unit) {
    var contentVisible by remember(roomType) { mutableStateOf(false) }
    var state by remember(roomType) { mutableStateOf(initialState(roomType)) }

    LaunchedEffect(roomType) { contentVisible = true }

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
        RoomHeader(title = roomTitle(roomType), onBack = onBack)
        Spacer(Modifier.height(16.dp))

        RoomHeroCard(
            type = roomType,
            lightOn = state.mainLightOn || state.secondaryLightOn || state.tertiaryLightOn,
            imageRes = roomImage(roomType)
        )

        Spacer(Modifier.height(14.dp))

        RoomControlAnimated(contentVisible) {
            Column {
                state.temperatureCelsius?.let {
                    RoomTemperatureCard(temperature = it)
                    Spacer(Modifier.height(12.dp))
                }

                RoomLightCard(
                    title = mainLightTitle(roomType),
                    enabled = state.mainLightOn,
                    onEnabledChange = { enabled ->
                        state = state.copy(mainLightOn = enabled)
                        RoomKnxGatewayProvider.gateway.setMainLight(roomType, enabled)
                    }
                )

                when (roomType) {
                    RoomType.KITCHEN -> {
                        SecondaryLight(roomType, "Luz de vitrocerámica", "▦", state) { state = it }
                        Spacer(Modifier.height(12.dp))
                        BlindControl(roomType, state) { state = it }
                        Spacer(Modifier.height(12.dp))
                        RoomFloodSensorCard(floodDetected = state.floodDetected)
                    }

                    RoomType.BEDROOM_1 -> {
                        SecondaryLight(roomType, "Luz lámpara", "☼", state) { state = it }
                        Spacer(Modifier.height(12.dp))
                        BlindControl(roomType, state) { state = it }
                    }

                    RoomType.DINING_ROOM -> {
                        SecondaryLight(roomType, "Luz comedor", "☼", state) { state = it }
                        TertiaryLight(roomType, "Luz lámpara", "♧", state) { state = it }
                        Spacer(Modifier.height(12.dp))
                        BlindControl(roomType, state) { state = it }
                    }

                    RoomType.SUITE -> {
                        SecondaryLight(roomType, "Mesita 1", "♧", state) { state = it }
                        TertiaryLight(roomType, "Mesita 2", "♧", state) { state = it }
                        Spacer(Modifier.height(12.dp))
                        BlindControl(roomType, state) { state = it }
                    }

                    RoomType.BATHROOM -> {
                        Spacer(Modifier.height(12.dp))
                        RoomFloodSensorCard(floodDetected = state.floodDetected)
                    }

                    RoomType.ENTRANCE -> {
                        Spacer(Modifier.height(12.dp))
                        RoomPirCard(
                            blocked = state.pirBlocked,
                            onBlockedChange = { blocked ->
                                state = state.copy(pirBlocked = blocked)
                                RoomKnxGatewayProvider.gateway.setPirBlocked(roomType, blocked)
                            }
                        )
                    }

                    RoomType.HALLWAY,
                    RoomType.STORAGE -> Unit
                }
            }
        }

        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun SecondaryLight(
    roomType: RoomType,
    title: String,
    symbol: String,
    state: RoomControlState,
    onStateChange: (RoomControlState) -> Unit
) {
    Spacer(Modifier.height(12.dp))
    RoomLightCard(
        title = title,
        symbol = symbol,
        enabled = state.secondaryLightOn,
        onEnabledChange = { enabled ->
            onStateChange(state.copy(secondaryLightOn = enabled))
            RoomKnxGatewayProvider.gateway.setSecondaryLight(roomType, enabled)
        }
    )
}

@Composable
private fun TertiaryLight(
    roomType: RoomType,
    title: String,
    symbol: String,
    state: RoomControlState,
    onStateChange: (RoomControlState) -> Unit
) {
    Spacer(Modifier.height(12.dp))
    RoomLightCard(
        title = title,
        symbol = symbol,
        enabled = state.tertiaryLightOn,
        onEnabledChange = { enabled ->
            onStateChange(state.copy(tertiaryLightOn = enabled))
            RoomKnxGatewayProvider.gateway.setTertiaryLight(roomType, enabled)
        }
    )
}

@Composable
private fun BlindControl(
    roomType: RoomType,
    state: RoomControlState,
    onStateChange: (RoomControlState) -> Unit
) {
    RoomBlindCard(
        lastCommand = state.lastBlindCommand,
        onCommand = { command ->
            onStateChange(state.copy(lastBlindCommand = command))
            RoomKnxGatewayProvider.gateway.moveBlind(roomType, command)
        }
    )
}

private fun initialState(roomType: RoomType): RoomControlState = when (roomType) {
    RoomType.ENTRANCE -> RoomControlState(mainLightOn = true, temperatureCelsius = 22.4f)
    RoomType.HALLWAY -> RoomControlState(mainLightOn = true)
    RoomType.STORAGE -> RoomControlState(mainLightOn = true)
    RoomType.BATHROOM -> RoomControlState(mainLightOn = true, floodDetected = false)
    RoomType.KITCHEN -> RoomControlState(
        mainLightOn = true,
        secondaryLightOn = true,
        floodDetected = false
    )
    RoomType.BEDROOM_1 -> RoomControlState(
        mainLightOn = true,
        secondaryLightOn = true,
        temperatureCelsius = 22.4f
    )
    RoomType.DINING_ROOM -> RoomControlState(
        mainLightOn = true,
        secondaryLightOn = true,
        tertiaryLightOn = true,
        temperatureCelsius = 22.4f
    )
    RoomType.SUITE -> RoomControlState(
        mainLightOn = true,
        secondaryLightOn = true,
        tertiaryLightOn = true,
        temperatureCelsius = 22.4f
    )
}

private fun mainLightTitle(roomType: RoomType): String = when (roomType) {
    RoomType.DINING_ROOM -> "Luz salón"
    else -> "Luz"
}

private fun roomTitle(roomType: RoomType): String = when (roomType) {
    RoomType.ENTRANCE -> "Entrada"
    RoomType.HALLWAY -> "Pasillo"
    RoomType.STORAGE -> "Trastero"
    RoomType.BATHROOM -> "Baño"
    RoomType.KITCHEN -> "Cocina"
    RoomType.BEDROOM_1 -> "Habitación 1"
    RoomType.DINING_ROOM -> "Comedor"
    RoomType.SUITE -> "Suite"
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
