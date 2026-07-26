package com.onehouse.app.feature.home.state

import com.onehouse.app.device.ControlKind
import com.onehouse.app.device.ImportedKnxDevice
import com.onehouse.app.feature.rooms.detail.RoomType
import com.onehouse.app.knx.KnxClimateSnapshot
import com.onehouse.app.knx.KnxHomeSnapshot

data class HomeDashboardUiState(
    val lightsOn: Int,
    val lightsTotal: Int,
    val blindsOpen: Int,
    val blindsTotal: Int,
    val climate: KnxClimateSnapshot
)

data class RoomLightUiState(
    val device: ImportedKnxDevice,
    val isOn: Boolean
)

data class RoomBlindUiState(
    val device: ImportedKnxDevice,
    val positionPercent: Float?
)

data class RoomUiState(
    val lights: List<RoomLightUiState>,
    val blind: RoomBlindUiState?,
    val temperatureCelsius: Float?,
    val climate: KnxClimateSnapshot?,
    val hasRealState: Boolean
) {
    val anyLightOn: Boolean get() = lights.any { it.isOn }
}

/**
 * Único punto de transformación entre la caché KNX y la interfaz.
 * Compose recibe modelos ya preparados y no conoce direcciones de grupo.
 */
object HomeStateMapper {
    fun dashboard(snapshot: KnxHomeSnapshot): HomeDashboardUiState = HomeDashboardUiState(
        lightsOn = snapshot.lightsOn,
        lightsTotal = snapshot.lightsTotal,
        blindsOpen = snapshot.blindsOpen,
        blindsTotal = snapshot.blindsTotal,
        climate = snapshot.climate
    )

    fun room(snapshot: KnxHomeSnapshot, roomType: RoomType): RoomUiState {
        val roomName = roomName(roomType)
        val devices = snapshot.devicesForRoom(roomName)

        val lights = devices
            .filter { it.controlKind == ControlKind.BOOLEAN_SWITCH }
            .map { device ->
                RoomLightUiState(
                    device = device,
                    isOn = snapshot.booleanValue(device) == true
                )
            }

        val blindDevice = devices.firstOrNull { it.controlKind == ControlKind.BLIND }
        val blind = blindDevice?.let { device ->
            RoomBlindUiState(device, snapshot.numericValue(device))
        }

        val temperatureDevice = devices.firstOrNull { device ->
            device.controlKind == ControlKind.TEMPERATURE ||
                device.name.contains("temperatura ambiente", ignoreCase = true) ||
                device.name.contains("temperatura", ignoreCase = true)
        }
        val roomTemperature = temperatureDevice?.let { snapshot.numericValue(it) }

        val roomHasClimate = devices.any { device ->
            device.controlKind == ControlKind.CLIMATE ||
                device.name.contains("daikin", ignoreCase = true) ||
                device.name.contains("termostato", ignoreCase = true)
        }
        val climate = if (roomHasClimate || roomType in CLIMATE_ROOMS) snapshot.climate else null
        val temperature = roomTemperature ?: climate?.currentTemperature

        val relevantAddresses = devices.flatMap { device ->
            device.readAddresses.map { it.toString() } + device.writeAddresses.map { it.toString() }
        }
        val hasRealState = relevantAddresses.any(snapshot.states::containsKey) ||
            (climate != null && (
                climate.powered != null ||
                    climate.currentTemperature != null ||
                    climate.targetTemperature != null ||
                    climate.mode != null ||
                    climate.fanSpeed != null
                ))

        return RoomUiState(
            lights = lights,
            blind = blind,
            temperatureCelsius = temperature,
            climate = climate,
            hasRealState = hasRealState
        )
    }

    fun roomName(roomType: RoomType): String = when (roomType) {
        RoomType.ENTRANCE -> "Entrada"
        RoomType.HALLWAY -> "Pasillo"
        RoomType.STORAGE -> "Trastero"
        RoomType.BATHROOM -> "Baño"
        RoomType.KITCHEN -> "Cocina"
        RoomType.BEDROOM_1 -> "Habitación 1"
        RoomType.DINING_ROOM -> "Comedor"
        RoomType.SUITE -> "Suite"
    }

    private val CLIMATE_ROOMS = setOf(
        RoomType.ENTRANCE,
        RoomType.BEDROOM_1,
        RoomType.DINING_ROOM,
        RoomType.SUITE
    )
}
