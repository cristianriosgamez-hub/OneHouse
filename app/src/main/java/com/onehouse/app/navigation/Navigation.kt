package com.onehouse.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.onehouse.app.feature.climate.ClimateScreen
import com.onehouse.app.feature.consumption.ConsumptionScreen
import com.onehouse.app.feature.home.HomeScreen
import com.onehouse.app.feature.importproject.ImportProjectScreen
import com.onehouse.app.feature.maintenance.MaintenanceScreen
import com.onehouse.app.feature.more.MoreScreen
import com.onehouse.app.feature.more.ToolsScreen
import com.onehouse.app.feature.more.AutomationControlScreen
import com.onehouse.app.feature.more.KnxAddressEditorScreen
import com.onehouse.app.feature.more.KnxDiagnosticsScreen
import com.onehouse.app.feature.rooms.RoomsScreen
import com.onehouse.app.feature.settings.SettingsScreen
import com.onehouse.app.feature.security.SecurityScreen
import com.onehouse.app.feature.rooms.detail.RoomDetailScreen
import com.onehouse.app.feature.rooms.detail.RoomType
import com.onehouse.app.feature.rooms.roomItemForName
import com.onehouse.app.feature.terrace.TerraceScreen
import com.onehouse.app.feature.weather.WeatherScreen
import com.onehouse.app.feature.programming.WeeklyScheduleScreen
import com.onehouse.app.feature.programming.SolarScheduleScreen
import com.onehouse.app.feature.automation.AutomationScreen
import com.onehouse.app.feature.scenes.ScenesScreen
import com.onehouse.app.feature.backup.BackupRestoreScreen
import com.onehouse.app.feature.consumption.transfer.ConsumptionTransferScreen
import com.onehouse.app.design.FondoSuperior

private enum class InternalScreen {
    MAIN,
    CLIMATE,
    ENTRANCE,
    HALLWAY,
    STORAGE,
    BATHROOM,
    KITCHEN,
    BEDROOM_1,
    DINING_ROOM,
    SUITE,
    TERRACE,
    CONSUMPTION,
    MAINTENANCE,
    SETTINGS,
    IMPORT_PROJECT,
    SECURITY,
    KNX_ADDRESS_EDITOR,
    KNX_DIAGNOSTICS,
    WEEKLY_SCHEDULES,
    SOLAR_SCHEDULES,
    AUTOMATIONS,
    SCENES,
    BACKUP_RESTORE,
    CONSUMPTION_TRANSFER,
    TOOLS,
    AUTOMATION_CONTROL
}

@Composable
fun OneHouseNavigation() {
    var selectedSection by rememberSaveable { mutableStateOf(OneHouseSection.HOME) }
    var internalScreen by rememberSaveable { mutableStateOf(InternalScreen.MAIN) }
    var favoriteRoomNames by rememberSaveable {
        mutableStateOf(listOf("Climatización", "Suite", "Baño", "Terraza"))
    }

    val openRoomByName: (String) -> Unit = { roomName ->
        internalScreen = when (roomName) {
            "Climatización" -> InternalScreen.CLIMATE
            "Entrada" -> InternalScreen.ENTRANCE
            "Pasillo" -> InternalScreen.HALLWAY
            "Trastero" -> InternalScreen.STORAGE
            "Baño" -> InternalScreen.BATHROOM
            "Cocina" -> InternalScreen.KITCHEN
            "Habitación 1" -> InternalScreen.BEDROOM_1
            "Comedor" -> InternalScreen.DINING_ROOM
            "Suite" -> InternalScreen.SUITE
            "Terraza" -> InternalScreen.TERRACE
            "Mantenimiento" -> InternalScreen.MAINTENANCE
            else -> InternalScreen.MAIN
        }
    }

    when (internalScreen) {
        InternalScreen.CLIMATE -> {
            ClimateScreen(
                onBack = {
                    internalScreen = InternalScreen.MAIN
                    selectedSection = OneHouseSection.ROOMS
                }
            )
            return
        }

        InternalScreen.TERRACE -> {
            TerraceScreen(
                onBack = {
                    internalScreen = InternalScreen.MAIN
                    selectedSection = OneHouseSection.ROOMS
                }
            )
            return
        }

        InternalScreen.CONSUMPTION -> {
            ConsumptionScreen(
                onBack = {
                    internalScreen = InternalScreen.MAIN
                    selectedSection = OneHouseSection.ROOMS
                }
            )
            return
        }

        InternalScreen.MAINTENANCE -> {
            MaintenanceScreen(
                onBack = {
                    internalScreen = InternalScreen.MAIN
                    selectedSection = OneHouseSection.ROOMS
                }
            )
            return
        }

        InternalScreen.SETTINGS -> {
            SettingsScreen(
                onBack = {
                    internalScreen = InternalScreen.MAIN
                    selectedSection = OneHouseSection.MORE
                }
            )
            return
        }

        InternalScreen.TOOLS -> {
            ToolsScreen(
                onBack = {
                    internalScreen = InternalScreen.MAIN
                    selectedSection = OneHouseSection.MORE
                },
                onImportProjectSelected = { internalScreen = InternalScreen.IMPORT_PROJECT },
                onKnxDiagnosticsSelected = { internalScreen = InternalScreen.KNX_DIAGNOSTICS },
                onBackupRestoreSelected = { internalScreen = InternalScreen.BACKUP_RESTORE },
                onConsumptionTransferSelected = { internalScreen = InternalScreen.CONSUMPTION_TRANSFER }
            )
            return
        }

        InternalScreen.AUTOMATION_CONTROL -> {
            AutomationControlScreen(
                onBack = {
                    internalScreen = InternalScreen.MAIN
                    selectedSection = OneHouseSection.MORE
                },
                onScenesSelected = { internalScreen = InternalScreen.SCENES },
                onAutomationsSelected = { internalScreen = InternalScreen.AUTOMATIONS },
                onSolarSchedulesSelected = { internalScreen = InternalScreen.SOLAR_SCHEDULES },
                onWeeklySchedulesSelected = { internalScreen = InternalScreen.WEEKLY_SCHEDULES },
                onKnxAddressesSelected = { internalScreen = InternalScreen.KNX_ADDRESS_EDITOR }
            )
            return
        }

        InternalScreen.IMPORT_PROJECT -> {
            ImportProjectScreen(
                onBack = {
                    internalScreen = InternalScreen.TOOLS
                }
            )
            return
        }

        InternalScreen.SECURITY -> {
            SecurityScreen(
                onBack = {
                    internalScreen = InternalScreen.MAIN
                    selectedSection = OneHouseSection.HOME
                }
            )
            return
        }

        InternalScreen.KNX_ADDRESS_EDITOR -> {
            KnxAddressEditorScreen(
                onBack = {
                    internalScreen = InternalScreen.AUTOMATION_CONTROL
                }
            )
            return
        }

        InternalScreen.KNX_DIAGNOSTICS -> {
            KnxDiagnosticsScreen(
                onBack = {
                    internalScreen = InternalScreen.TOOLS
                }
            )
            return
        }

        InternalScreen.WEEKLY_SCHEDULES -> {
            WeeklyScheduleScreen(
                onBack = {
                    internalScreen = InternalScreen.AUTOMATION_CONTROL
                }
            )
            return
        }

        InternalScreen.SOLAR_SCHEDULES -> {
            SolarScheduleScreen(
                onBack = {
                    internalScreen = InternalScreen.AUTOMATION_CONTROL
                }
            )
            return
        }

        InternalScreen.AUTOMATIONS -> {
            AutomationScreen(
                onBack = {
                    internalScreen = InternalScreen.AUTOMATION_CONTROL
                }
            )
            return
        }

        InternalScreen.SCENES -> {
            ScenesScreen(
                onBack = {
                    internalScreen = InternalScreen.AUTOMATION_CONTROL
                }
            )
            return
        }

        InternalScreen.BACKUP_RESTORE -> {
            BackupRestoreScreen(
                onBack = {
                    internalScreen = InternalScreen.TOOLS
                }
            )
            return
        }

        InternalScreen.CONSUMPTION_TRANSFER -> {
            ConsumptionTransferScreen(
                onBack = {
                    internalScreen = InternalScreen.TOOLS
                }
            )
            return
        }

        InternalScreen.ENTRANCE,
        InternalScreen.HALLWAY,
        InternalScreen.STORAGE,
        InternalScreen.BATHROOM,
        InternalScreen.KITCHEN,
        InternalScreen.BEDROOM_1,
        InternalScreen.DINING_ROOM,
        InternalScreen.SUITE -> {
            val roomType = when (internalScreen) {
                InternalScreen.ENTRANCE -> RoomType.ENTRANCE
                InternalScreen.HALLWAY -> RoomType.HALLWAY
                InternalScreen.STORAGE -> RoomType.STORAGE
                InternalScreen.BATHROOM -> RoomType.BATHROOM
                InternalScreen.KITCHEN -> RoomType.KITCHEN
                InternalScreen.BEDROOM_1 -> RoomType.BEDROOM_1
                InternalScreen.DINING_ROOM -> RoomType.DINING_ROOM
                InternalScreen.SUITE -> RoomType.SUITE
                else -> error("Pantalla de estancia no válida")
            }
            RoomDetailScreen(
                roomType = roomType,
                onBack = {
                    internalScreen = InternalScreen.MAIN
                    selectedSection = OneHouseSection.ROOMS
                }
            )
            return
        }

        InternalScreen.MAIN -> Unit
    }

    Scaffold(
        containerColor = FondoSuperior,
        bottomBar = {
            OneHouseNavigationBar(
                selectedSection = selectedSection,
                onSectionSelected = {
                    internalScreen = InternalScreen.MAIN
                    selectedSection = it
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (selectedSection) {
                OneHouseSection.HOME -> HomeScreen(
                    favoriteRooms = favoriteRoomNames.mapNotNull(::roomItemForName).take(4),
                    onFavoriteSelected = openRoomByName,
                    onSecuritySelected = { internalScreen = InternalScreen.SECURITY }
                )

                OneHouseSection.ROOMS -> RoomsScreen(
                    favoriteRoomNames = favoriteRoomNames,
                    onFavoriteToggle = { roomName ->
                        favoriteRoomNames = if (roomName in favoriteRoomNames) {
                            favoriteRoomNames - roomName
                        } else {
                            (favoriteRoomNames + roomName).takeLast(4)
                        }
                    },
                    onClimateSelected = { internalScreen = InternalScreen.CLIMATE },
                    onEntranceSelected = { internalScreen = InternalScreen.ENTRANCE },
                    onHallwaySelected = { internalScreen = InternalScreen.HALLWAY },
                    onStorageSelected = { internalScreen = InternalScreen.STORAGE },
                    onBathroomSelected = { internalScreen = InternalScreen.BATHROOM },
                    onKitchenSelected = { internalScreen = InternalScreen.KITCHEN },
                    onBedroom1Selected = { internalScreen = InternalScreen.BEDROOM_1 },
                    onDiningRoomSelected = { internalScreen = InternalScreen.DINING_ROOM },
                    onSuiteSelected = { internalScreen = InternalScreen.SUITE },
                    onTerraceSelected = { internalScreen = InternalScreen.TERRACE },
                    onConsumptionSelected = { internalScreen = InternalScreen.CONSUMPTION },
                    onMaintenanceSelected = { internalScreen = InternalScreen.MAINTENANCE }
                )

                OneHouseSection.WEATHER -> WeatherScreen()
                OneHouseSection.CONSUMPTION -> ConsumptionScreen()
                OneHouseSection.MORE -> MoreScreen(
                    onConfigurationSelected = { internalScreen = InternalScreen.SETTINGS },
                    onAutomationControlSelected = { internalScreen = InternalScreen.AUTOMATION_CONTROL },
                    onToolsSelected = { internalScreen = InternalScreen.TOOLS }
                )
            }
        }
    }
}
