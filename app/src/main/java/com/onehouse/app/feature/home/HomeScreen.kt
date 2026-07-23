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
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
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
import com.onehouse.app.data.knx.SettingsDataStore
import com.onehouse.app.device.LightDevice
import com.onehouse.app.feature.rooms.RoomItem
import com.onehouse.app.knx.KnxConnectionManager
import com.onehouse.app.knx.KnxEndpoint
import com.onehouse.app.knx.KnxGroupAddress
import com.onehouse.app.knx.NetworkConnectionDetector
import com.onehouse.app.feature.weather.WeatherUiState
import com.onehouse.app.feature.weather.rememberWeatherState
import java.util.Locale

@Composable
fun HomeScreen(
    favoriteRooms: List<RoomItem>,
    onFavoriteSelected: (String) -> Unit
) {
    val exteriorWeather = rememberWeatherState()

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
            ClimateHeroCard()

            Spacer(modifier = Modifier.height(16.dp))
            QuickStatusGrid(exteriorWeather)

            Spacer(modifier = Modifier.height(16.dp))
            KnxTestLightCard()

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

            Spacer(modifier = Modifier.height(22.dp))
            ConnectionStatus()
        }
    }
}

@Composable
private fun ClimateHeroCard() {
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
                            text = "22,5",
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
                        text = "❄  Frío",
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
                    value = "Encendido",
                    valueColor = VerdeEstado,
                    modifier = Modifier.weight(1f)
                )
                OneHouseStatusItem(
                    title = "Consigna",
                    value = "23 °C",
                    modifier = Modifier.weight(1f)
                )
                OneHouseStatusItem(
                    title = "Consumo ACS",
                    value = "4,8 kW",
                    valueColor = AmarilloEstado,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun QuickStatusGrid(weather: WeatherUiState) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OneHouseInfoCard(
            title = "Persianas",
            value = "2 de 4 abiertas",
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
            value = "3 de 12 encendidas",
            detail = "Estado general",
            symbol = "☀",
            symbolColor = AmarilloEstado,
            modifier = Modifier.weight(1f).height(146.dp)
        )
        OneHouseInfoCard(
            title = "Seguridad",
            value = "Todo correcto",
            detail = "Estado general",
            symbol = "✓",
            valueColor = VerdeEstado,
            symbolColor = VerdeEstado,
            modifier = Modifier.weight(1f).height(146.dp)
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
private fun ConnectionStatus() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(VerdeEstado)
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = "Sistema conectado",
            color = VerdeEstado,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun KnxTestLightCard() {
    val context = LocalContext.current
    val connectionManager = remember { KnxConnectionManager() }
    val light = remember {
        LightDevice(
            connectionManager = connectionManager,
            name = "Luz KNX de prueba",
            writeAddress = KnxGroupAddress.parse(TEST_LIGHT_GROUP_ADDRESS)
        )
    }
    var isOn by remember { mutableStateOf(false) }
    var isBusy by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Dirección $TEST_LIGHT_GROUP_ADDRESS · DPT 1.001") }

    DisposableEffect(connectionManager) {
        onDispose { connectionManager.close() }
    }

    OneHouseCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "PRIMER CONTROL KNX",
                color = AzulClaro,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = light.name,
                        color = TextoPrincipal,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = statusText,
                        color = TextoSecundario,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Button(
                    enabled = !isBusy,
                    onClick = {
                        val targetState = !isOn
                        val settings = SettingsDataStore(context).read()
                        val network = NetworkConnectionDetector(context).currentState()
                        val host = if (network.usesLocalRoute) settings.localIp else settings.remoteIp
                        val port = (if (network.usesLocalRoute) settings.localPort else settings.remotePort)
                            .toIntOrNull()

                        if (!network.isConnected) {
                            statusText = "Sin conexión de red"
                        } else if (host.isBlank() || port == null) {
                            statusText = "Configura la conexión KNX ${if (network.usesLocalRoute) "local" else "remota"}"
                        } else {
                            isBusy = true
                            statusText = "Conectando con $host:$port…"
                            connectionManager.connect(KnxEndpoint(host, port)) { connectResult ->
                                when (connectResult) {
                                    is KnxConnectionManager.ConnectResult.Success -> {
                                        light.setOn(targetState) { operation ->
                                            isBusy = false
                                            when (operation) {
                                                KnxConnectionManager.OperationResult.Success -> {
                                                    isOn = targetState
                                                    statusText = if (targetState) "Encendida · telegrama confirmado" else "Apagada · telegrama confirmado"
                                                }
                                                is KnxConnectionManager.OperationResult.Failure -> statusText = operation.detail
                                                is KnxConnectionManager.OperationResult.NotAvailable -> statusText = operation.detail
                                            }
                                        }
                                    }
                                    KnxConnectionManager.ConnectResult.Timeout -> {
                                        isBusy = false
                                        statusText = "Tiempo de espera agotado"
                                    }
                                    is KnxConnectionManager.ConnectResult.Rejected -> {
                                        isBusy = false
                                        statusText = "Conexión KNX rechazada (${connectResult.status})"
                                    }
                                    is KnxConnectionManager.ConnectResult.NetworkError -> {
                                        isBusy = false
                                        statusText = connectResult.detail
                                    }
                                    KnxConnectionManager.ConnectResult.InvalidResponse -> {
                                        isBusy = false
                                        statusText = "Respuesta KNX/IP no válida"
                                    }
                                    KnxConnectionManager.ConnectResult.Cancelled -> {
                                        isBusy = false
                                    }
                                }
                            }
                        }
                    }
                ) {
                    Text(if (isBusy) "Enviando…" else if (isOn) "Apagar" else "Encender")
                }
            }
        }
    }
}

private const val TEST_LIGHT_GROUP_ADDRESS = "1/0/1"

