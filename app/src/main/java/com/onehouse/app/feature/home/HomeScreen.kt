package com.onehouse.app.feature.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onehouse.app.design.AmarilloEstado
import com.onehouse.app.design.AzulClaro
import com.onehouse.app.design.AzulOneHouse
import com.onehouse.app.design.BordeTarjeta
import com.onehouse.app.design.FondoChip
import com.onehouse.app.design.FondoInferior
import com.onehouse.app.design.FondoMedio
import com.onehouse.app.design.FondoSuperior
import com.onehouse.app.design.OneHouseCard
import com.onehouse.app.design.OneHouseHeader
import com.onehouse.app.design.OneHouseInfoCard
import com.onehouse.app.design.OneHouseSceneCard
import com.onehouse.app.design.OneHouseSectionTitle
import com.onehouse.app.design.OneHouseStatusItem
import com.onehouse.app.design.TextoDesactivado
import com.onehouse.app.design.TextoPrincipal
import com.onehouse.app.design.TextoSecundario
import com.onehouse.app.design.VerdeEstado
import com.onehouse.app.design.RojoEstado
import com.onehouse.app.feature.rooms.RoomItem
import com.onehouse.app.feature.scenes.SceneExecutionEngine
import com.onehouse.app.feature.scenes.SharedPreferencesSceneRepository
import com.onehouse.app.feature.scenes.SmartScene
import com.onehouse.app.data.knx.KnxConnectionStatus
import com.onehouse.app.data.knx.SettingsDataStore
import com.onehouse.app.feature.weather.WeatherUiState
import com.onehouse.app.feature.weather.rememberWeatherState
import com.onehouse.app.feature.home.state.HomeDashboardUiState
import com.onehouse.app.feature.home.state.HomeStateMapper
import com.onehouse.app.feature.security.HomeAssistantSecuritySnapshotStore
import com.onehouse.app.feature.security.SecuritySummary
import com.onehouse.app.knx.KnxHomeSnapshot
import com.onehouse.app.knx.KnxHomeStateRepository
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    favoriteRooms: List<RoomItem>,
    onFavoriteSelected: (String) -> Unit,
    onSecuritySelected: () -> Unit
) {
    val exteriorWeather = rememberWeatherState()
    val context = LocalContext.current
    val homeRepository = remember { KnxHomeStateRepository(context) }
    val homeState by homeRepository.stateFlow.collectAsState(initial = homeRepository.snapshot())
    val dashboardState = remember(homeState) { HomeStateMapper.dashboard(homeState) }
    val settingsDataStore = remember(context) { SettingsDataStore(context) }
    val securitySnapshotStore = remember(context) { HomeAssistantSecuritySnapshotStore(context) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var connectionStatus by remember {
        mutableStateOf(settingsDataStore.read().lastConnectionStatus)
    }
    var securitySummary by remember { mutableStateOf(securitySnapshotStore.readSummary()) }
    val sceneRepository = remember(context) { SharedPreferencesSceneRepository(context) }
    val sceneEngine = remember(context) { SceneExecutionEngine(context) }
    var favoriteScenes by remember { mutableStateOf(sceneRepository.load().scenes.filter { it.favorite }.take(4)) }
    var pendingScene by remember { mutableStateOf<SmartScene?>(null) }
    var sceneMessage by remember { mutableStateOf<String?>(null) }
    var executingSceneId by remember { mutableStateOf<Long?>(null) }

    DisposableEffect(sceneEngine) {
        onDispose { sceneEngine.close() }
    }

    DisposableEffect(lifecycleOwner, settingsDataStore, sceneRepository) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                connectionStatus = settingsDataStore.read().lastConnectionStatus
                securitySummary = securitySnapshotStore.readSummary()
                favoriteScenes = sceneRepository.load().scenes.filter { it.favorite }.take(4)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(FondoSuperior, FondoMedio, FondoInferior, Color.Black)
                )
            )
    ) {
        Box(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.TopEnd)
                .background(
                    Brush.radialGradient(
                        colors = listOf(AzulOneHouse.copy(alpha = 0.16f), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 18.dp, end = 18.dp, top = 24.dp, bottom = 34.dp)
        ) {
            OneHouseHeader(
                title = "OneHouse",
                subtitle = "Buenos días · Tu hogar está listo",
                badgeText = null
            )

            Spacer(modifier = Modifier.height(22.dp))
            ClimateHeroCard(dashboardState)

            Spacer(modifier = Modifier.height(16.dp))
            QuickStatusGrid(exteriorWeather, dashboardState, securitySummary, onSecuritySelected)

            Spacer(modifier = Modifier.height(26.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OneHouseSectionTitle(title = "Estancias favoritas", modifier = Modifier.weight(1f))
                Text(
                    text = "${favoriteRooms.size} guardadas",
                    color = TextoDesactivado,
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            FavoriteRooms(
                favoriteRooms = favoriteRooms,
                onFavoriteSelected = onFavoriteSelected
            )

            Spacer(modifier = Modifier.height(26.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OneHouseSectionTitle(title = "Escenas favoritas", modifier = Modifier.weight(1f))
                Text(
                    text = "${favoriteScenes.size}/4",
                    color = TextoDesactivado,
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            FavoriteScenes(
                scenes = favoriteScenes,
                executingSceneId = executingSceneId,
                onExecute = { scene ->
                    if (scene.requireConfirmation) {
                        pendingScene = scene
                    } else {
                        executeHomeScene(
                            scene = scene,
                            repository = sceneRepository,
                            engine = sceneEngine,
                            onExecuting = { executingSceneId = it },
                            onScenesChanged = { favoriteScenes = it },
                            onMessage = { sceneMessage = it }
                        )
                    }
                }
            )

            sceneMessage?.let {
                Spacer(modifier = Modifier.height(12.dp))
                OneHouseCard(modifier = Modifier.fillMaxWidth()) {
                    Text(it, modifier = Modifier.padding(16.dp), color = TextoPrincipal, style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(22.dp))
            ConnectionStatus(connectionStatus)
        }
    }

    pendingScene?.let { scene ->
        AlertDialog(
            onDismissRequest = { pendingScene = null },
            title = { Text("Ejecutar ${scene.name}") },
            text = { Text("Se enviarán ${scene.actions.size} acciones KNX en el orden configurado.") },
            confirmButton = {
                Button(onClick = {
                    pendingScene = null
                    executeHomeScene(
                        scene = scene,
                        repository = sceneRepository,
                        engine = sceneEngine,
                        onExecuting = { executingSceneId = it },
                        onScenesChanged = { favoriteScenes = it },
                        onMessage = { sceneMessage = it }
                    )
                }) { Text("Ejecutar") }
            },
            dismissButton = { TextButton(onClick = { pendingScene = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun ClimateHeroCard(homeState: HomeDashboardUiState) {
    OneHouseCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            AzulOneHouse.copy(alpha = 0.18f),
                            Color.Transparent,
                            Color.Transparent
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "CLIMATIZACIÓN CENTRAL",
                        color = AzulClaro,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = homeState.climate.currentTemperature?.let {
                                String.format(Locale("es", "ES"), "%.1f", it)
                            } ?: "--,-",
                            color = TextoPrincipal,
                            fontSize = 46.sp,
                            lineHeight = 48.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = " °C",
                            color = TextoSecundario,
                            fontSize = 21.sp,
                            modifier = Modifier.padding(bottom = 5.dp)
                        )
                    }
                    Text(
                        text = "Temperatura interior confortable",
                        color = TextoSecundario,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Surface(color = FondoChip, shape = RoundedCornerShape(50)) {
                    Text(
                        text = "❄  ${homeState.climate.mode ?: "Sin datos"}",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        color = AzulClaro,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = BordeTarjeta)
            Spacer(modifier = Modifier.height(17.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                OneHouseStatusItem(
                    title = "Estado",
                    value = when (homeState.climate.powered) {
                        true -> "Encendido"
                        false -> "Apagado"
                        null -> "Sin datos"
                    },
                    valueColor = if (homeState.climate.powered == true) VerdeEstado else TextoSecundario,
                    modifier = Modifier.weight(1f)
                )
                OneHouseStatusItem(
                    title = "Consigna",
                    value = homeState.climate.targetTemperature?.let {
                        String.format(Locale("es", "ES"), "%.1f °C", it)
                    } ?: "-- °C",
                    modifier = Modifier.weight(1f)
                )
                OneHouseStatusItem(
                    title = "Ventilador",
                    value = homeState.climate.fanSpeed ?: "Sin datos",
                    valueColor = AzulClaro,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun QuickStatusGrid(
    weather: WeatherUiState,
    homeState: HomeDashboardUiState,
    securitySummary: SecuritySummary,
    onSecuritySelected: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OneHouseInfoCard(
            title = "Persianas",
            value = if (homeState.blindsTotal > 0) {
                "${homeState.blindsOpen} de ${homeState.blindsTotal} abiertas"
            } else "Sin datos",
            detail = "Estado general",
            symbol = "▥",
            modifier = Modifier.weight(1f).height(146.dp)
        )
        OneHouseInfoCard(
            title = "Terraza de estancia",
            value = weather.temperatureC?.let {
                String.format(Locale("es", "ES"), "%.1f °C", it)
            } ?: "-- °C",
            detail = when {
                weather.isLoading -> "Actualizando temperatura exterior…"
                weather.temperatureC != null -> weather.condition
                weather.errorMessage != null -> "Último dato no disponible"
                else -> "Temperatura exterior"
            },
            symbol = weather.conditionSymbol.ifBlank { "◌" },
            modifier = Modifier.weight(1f).height(146.dp)
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OneHouseInfoCard(
            title = "Luces",
            value = if (homeState.lightsTotal > 0) {
                "${homeState.lightsOn} de ${homeState.lightsTotal} encendidas"
            } else "Sin datos",
            detail = "Estado general",
            symbol = "☀",
            symbolColor = AmarilloEstado,
            modifier = Modifier.weight(1f).height(146.dp)
        )
        val securityValue = when {
            securitySummary.updatedAt == 0L -> "Sin datos"
            securitySummary.openCount > 0 -> "${securitySummary.openCount} puerta(s) abierta(s)"
            securitySummary.motionCount > 0 -> "Movimiento detectado"
            else -> "Todo correcto"
        }
        val securityColor = when {
            securitySummary.updatedAt == 0L -> TextoDesactivado
            securitySummary.openCount > 0 || securitySummary.motionCount > 0 -> AmarilloEstado
            else -> VerdeEstado
        }
        OneHouseInfoCard(
            title = "Seguridad",
            value = securityValue,
            detail = if (securitySummary.updatedAt == 0L) "Sensores Xiaomi" else "${securitySummary.entityCount} sensores",
            symbol = "⌂",
            valueColor = securityColor,
            symbolColor = AzulClaro,
            modifier = Modifier.weight(1f).height(146.dp),
            onClick = onSecuritySelected
        )
    }
}

@Composable
private fun FavoriteRooms(
    favoriteRooms: List<RoomItem>,
    onFavoriteSelected: (String) -> Unit
) {
    if (favoriteRooms.isEmpty()) {
        OneHouseCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(text = "☆", color = AzulClaro, fontSize = 28.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Aún no tienes favoritas",
                    color = TextoPrincipal,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Marca una estancia con ☆ para acceder a ella desde aquí.",
                    color = TextoSecundario,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        return
    }

    favoriteRooms.take(4).chunked(2).forEachIndexed { index, rowItems ->
        if (index > 0) Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            rowItems.forEach { room ->
                OneHouseSceneCard(
                    name = room.name,
                    symbol = room.symbol,
                    modifier = Modifier.weight(1f),
                    onClick = { onFavoriteSelected(room.name) }
                )
            }
            if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun FavoriteScenes(
    scenes: List<SmartScene>,
    executingSceneId: Long?,
    onExecute: (SmartScene) -> Unit
) {
    if (scenes.isEmpty()) {
        OneHouseCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(text = "✦", color = AzulClaro, fontSize = 28.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Aún no tienes escenas favoritas", color = TextoPrincipal, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "En Más > Escenas inteligentes activa ‘Mostrar en Mi Hogar’.",
                    color = TextoSecundario,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        return
    }

    scenes.chunked(2).forEachIndexed { index, rowScenes ->
        if (index > 0) Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            rowScenes.forEach { scene ->
                OneHouseCard(
                    modifier = Modifier.weight(1f).height(126.dp),
                    onClick = if (executingSceneId == null && scene.enabled && scene.actions.isNotEmpty()) {
                        { onExecute(scene) }
                    } else null
                ) {
                    Column(modifier = Modifier.padding(15.dp)) {
                        Text("✦", color = AzulClaro, fontSize = 24.sp)
                        Spacer(modifier = Modifier.height(7.dp))
                        Text(scene.name, color = TextoPrincipal, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            when {
                                executingSceneId == scene.id -> "Ejecutando…"
                                !scene.enabled -> "Escena desactivada"
                                scene.actions.isEmpty() -> "Sin acciones"
                                else -> scene.lastExecutionMillis?.let {
                                    "Última: ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(it))}"
                                } ?: "${scene.actions.size} acciones"
                            },
                            color = if (executingSceneId == scene.id) AzulClaro else TextoDesactivado,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    }
                }
            }
            if (rowScenes.size == 1) Spacer(modifier = Modifier.weight(1f))
        }
    }
}

private fun executeHomeScene(
    scene: SmartScene,
    repository: SharedPreferencesSceneRepository,
    engine: SceneExecutionEngine,
    onExecuting: (Long?) -> Unit,
    onScenesChanged: (List<SmartScene>) -> Unit,
    onMessage: (String) -> Unit
) {
    onExecuting(scene.id)
    engine.execute(scene) { result ->
        val currentState = repository.load()
        val completedAll = result.successfulActions + result.failedActions == result.totalActions
        val shouldRegisterExecution = result.successfulActions > 0 || completedAll
        val now = System.currentTimeMillis()
        val updatedState = currentState.copy(scenes = currentState.scenes.map {
            if (it.id == scene.id && shouldRegisterExecution) {
                it.copy(lastExecutionMillis = now, executionCount = it.executionCount + 1)
            } else it
        })
        if (shouldRegisterExecution) repository.save(updatedState)
        onScenesChanged(updatedState.scenes.filter { it.favorite }.take(4))
        onExecuting(null)
        onMessage(
            when {
                result.errorMessage == null -> "${scene.name}: ${result.successfulActions}/${result.totalActions} acciones ejecutadas."
                !scene.stopOnError && completedAll -> "${scene.name}: ${result.successfulActions} correctas y ${result.failedActions} con error."
                else -> "${scene.name}: ${result.successfulActions}/${result.totalActions}. ${result.errorMessage}"
            }
        )
    }
}

@Composable
private fun ConnectionStatus(status: KnxConnectionStatus) {
    val label = when (status) {
        KnxConnectionStatus.NOT_TESTED -> "No comprobado"
        KnxConnectionStatus.TESTING -> "Comprobando…"
        KnxConnectionStatus.CONNECTED -> "Conectado"
        KnxConnectionStatus.FAILED -> "Sin conexión"
    }
    val statusColor = when (status) {
        KnxConnectionStatus.CONNECTED -> VerdeEstado
        KnxConnectionStatus.FAILED -> RojoEstado
        KnxConnectionStatus.TESTING -> AzulClaro
        KnxConnectionStatus.NOT_TESTED -> TextoDesactivado
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(statusColor)
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = label,
            color = statusColor,
            style = MaterialTheme.typography.labelLarge
        )
    }
}
