package com.onehouse.app.feature.security

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onehouse.app.design.*

@Composable
fun SecurityScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { HomeAssistantSettingsStore(context.applicationContext) }
    val snapshotStore = remember { HomeAssistantSecuritySnapshotStore(context.applicationContext) }
    var settings by remember { mutableStateOf(store.read()) }
    var baseUrl by remember { mutableStateOf(settings.baseUrl) }
    var accessToken by remember { mutableStateOf(settings.accessToken) }
    var urlError by remember { mutableStateOf<String?>(null) }
    var snapshot by remember { mutableStateOf(HomeAssistantSecuritySnapshot()) }
    var loadingSensors by remember { mutableStateOf(false) }
    var sensorMessage by remember { mutableStateOf("Pulsa actualizar para leer los sensores") }

    fun persist(status: HomeAssistantConnectionStatus, message: String, tested: Boolean) {
        settings = HomeAssistantSettings(
            baseUrl = baseUrl.trim(), accessToken = accessToken.trim(), lastStatus = status,
            lastMessage = message,
            lastTestEpochMillis = if (tested) System.currentTimeMillis() else settings.lastTestEpochMillis
        )
        store.write(settings)
    }

    fun refreshSensors() {
        if (baseUrl.isBlank() || accessToken.isBlank()) {
            sensorMessage = "Configura primero Home Assistant"
            return
        }
        loadingSensors = true
        sensorMessage = "Leyendo sensores…"
        HomeAssistantSecurityClient.fetch(baseUrl, accessToken) { result ->
            loadingSensors = false
            when (result) {
                is HomeAssistantSecurityClient.Result.Success -> {
                    snapshot = result.snapshot
                    snapshotStore.write(result.snapshot)
                    sensorMessage = if (snapshot.openings.isEmpty() && snapshot.motions.isEmpty() && snapshot.cameras.isEmpty()) {
                        "No se encontraron sensores ni cámaras compatibles"
                    } else "Dispositivos actualizados correctamente"
                    persist(HomeAssistantConnectionStatus.CONNECTED, "Conectado correctamente con Home Assistant", true)
                }
                is HomeAssistantSecurityClient.Result.Failure -> {
                    sensorMessage = result.message
                    persist(HomeAssistantConnectionStatus.FAILED, result.message, true)
                }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(FondoSuperior, FondoMedio, FondoInferior, Color.Black))
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(onClick = onBack, shape = CircleShape, color = AzulOneHouse.copy(alpha = 0.16f)) {
                    Text("‹", color = TextoPrincipal, fontSize = 34.sp, modifier = Modifier.padding(horizontal = 15.dp, vertical = 4.dp))
                }
                Spacer(Modifier.size(14.dp))
                Column {
                    Text("Seguridad", color = TextoPrincipal, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Sensores Xiaomi mediante Home Assistant", color = TextoSecundario)
                }
            }

            Spacer(Modifier.height(24.dp))
            GeneralSecurityStatusCard(settings, snapshot, sensorMessage)

            Spacer(Modifier.height(22.dp))
            SectionTitle("Conexión con Home Assistant")
            Spacer(Modifier.height(10.dp))
            OneHouseCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text("Servidor local", color = TextoPrincipal, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text("Introduce la dirección y un token de larga duración.", color = TextoSecundario, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = {
                            baseUrl = it; urlError = null
                            persist(HomeAssistantConnectionStatus.NOT_TESTED, "Cambios pendientes de comprobar", false)
                        },
                        modifier = Modifier.fillMaxWidth(), label = { Text("Dirección Home Assistant") },
                        placeholder = { Text("http://192.168.1.20:8123") }, singleLine = true,
                        isError = urlError != null,
                        supportingText = urlError?.let { message -> { Text(message) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), colors = securityTextFieldColors()
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = accessToken,
                        onValueChange = {
                            accessToken = it
                            persist(HomeAssistantConnectionStatus.NOT_TESTED, "Cambios pendientes de comprobar", false)
                        },
                        modifier = Modifier.fillMaxWidth(), label = { Text("Token de acceso") },
                        placeholder = { Text("Token de larga duración") }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(), colors = securityTextFieldColors()
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val validation = HomeAssistantConnectionTester.validateBaseUrl(baseUrl)
                            urlError = validation
                            when {
                                validation != null -> persist(HomeAssistantConnectionStatus.FAILED, validation, true)
                                accessToken.isBlank() -> persist(HomeAssistantConnectionStatus.FAILED, "Introduce el token de acceso", true)
                                else -> {
                                    persist(HomeAssistantConnectionStatus.TESTING, "Comprobando conexión…", false)
                                    HomeAssistantConnectionTester.test(baseUrl, accessToken) { result ->
                                        when (result) {
                                            HomeAssistantConnectionTester.Result.Success -> {
                                                persist(HomeAssistantConnectionStatus.CONNECTED, "Conectado correctamente con Home Assistant", true)
                                                refreshSensors()
                                            }
                                            is HomeAssistantConnectionTester.Result.Failure -> persist(HomeAssistantConnectionStatus.FAILED, result.message, true)
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = settings.lastStatus != HomeAssistantConnectionStatus.TESTING,
                        colors = ButtonDefaults.buttonColors(containerColor = AzulOneHouse)
                    ) { Text(if (settings.lastStatus == HomeAssistantConnectionStatus.TESTING) "Comprobando…" else "Guardar y comprobar") }
                    Spacer(Modifier.height(10.dp))
                    Text("El token se guarda cifrado con Android Keystore.", color = TextoDesactivado, style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(Modifier.height(18.dp))
            Button(
                onClick = { refreshSensors() }, modifier = Modifier.fillMaxWidth(),
                enabled = !loadingSensors && settings.lastStatus == HomeAssistantConnectionStatus.CONNECTED,
                colors = ButtonDefaults.buttonColors(containerColor = AzulOneHouse.copy(alpha = 0.85f))
            ) { Text(if (loadingSensors) "Actualizando…" else "Actualizar sensores") }

            Spacer(Modifier.height(22.dp))
            SensorSection("Puertas y ventanas", "▣", snapshot.openings, SecurityEntityCategory.OPENING, settings)
            Spacer(Modifier.height(22.dp))
            SensorSection("Movimiento", "◉", snapshot.motions, SecurityEntityCategory.MOTION, settings)

            Spacer(Modifier.height(22.dp))
            SectionTitle("Cámaras")
            Spacer(Modifier.height(10.dp))
            CameraSection(snapshot.cameras, settings, baseUrl, accessToken)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun GeneralSecurityStatusCard(settings: HomeAssistantSettings, snapshot: HomeAssistantSecuritySnapshot, message: String) {
    val hasAlerts = snapshot.alertCount > 0
    val statusColor = when {
        settings.lastStatus == HomeAssistantConnectionStatus.FAILED -> Color(0xFFFF6B6B)
        hasAlerts -> Color(0xFFFFB74D)
        settings.lastStatus == HomeAssistantConnectionStatus.CONNECTED -> Color(0xFF61D88B)
        settings.lastStatus == HomeAssistantConnectionStatus.TESTING -> AzulClaro
        else -> TextoDesactivado
    }
    val title = when {
        settings.lastStatus == HomeAssistantConnectionStatus.FAILED -> "Sin conexión"
        hasAlerts -> "Atención: ${snapshot.alertCount} sensor(es) activo(s)"
        settings.lastStatus == HomeAssistantConnectionStatus.CONNECTED && snapshot.fetchedAtEpochMillis > 0 -> "Todo correcto"
        settings.lastStatus == HomeAssistantConnectionStatus.CONNECTED -> "Conectado"
        settings.lastStatus == HomeAssistantConnectionStatus.TESTING -> "Comprobando"
        settings.lastStatus == HomeAssistantConnectionStatus.NOT_TESTED -> "Sin comprobar"
        else -> "Sin configurar"
    }
    OneHouseCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text("ESTADO GENERAL", color = AzulClaro, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(statusColor))
                Spacer(Modifier.size(10.dp))
                Text(title, color = TextoPrincipal, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(7.dp))
            Text(message, color = TextoSecundario, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SensorSection(title: String, symbol: String, entities: List<SecurityEntity>, category: SecurityEntityCategory, settings: HomeAssistantSettings) {
    SectionTitle(title)
    Spacer(Modifier.height(10.dp))
    if (entities.isEmpty()) {
        val status = when (settings.lastStatus) {
            HomeAssistantConnectionStatus.CONNECTED -> "Sin sensores encontrados"
            HomeAssistantConnectionStatus.FAILED -> "Sin conexión"
            else -> "Sin comprobar"
        }
        SecurityDeviceCard(symbol, if (category == SecurityEntityCategory.OPENING) "Sensores de apertura" else "Sensores de movimiento", status, "Los dispositivos compatibles aparecerán aquí.", TextoDesactivado)
    } else {
        entities.forEachIndexed { index, entity ->
            val activeText = when {
                !entity.available -> "No disponible"
                category == SecurityEntityCategory.OPENING && entity.active -> "Abierto"
                category == SecurityEntityCategory.OPENING -> "Cerrado"
                entity.active -> "Movimiento detectado"
                else -> "Sin movimiento"
            }
            val color = when {
                !entity.available -> TextoDesactivado
                entity.active -> Color(0xFFFFB74D)
                else -> Color(0xFF61D88B)
            }
            SecurityDeviceCard(symbol, entity.name, activeText, "Último cambio: ${entity.lastChanged}", color)
            if (index != entities.lastIndex) Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun CameraSection(
    cameras: List<SecurityCameraEntity>,
    settings: HomeAssistantSettings,
    baseUrl: String,
    accessToken: String
) {
    if (cameras.isEmpty()) {
        val status = when (settings.lastStatus) {
            HomeAssistantConnectionStatus.CONNECTED -> "Sin cámaras encontradas"
            HomeAssistantConnectionStatus.FAILED -> "Sin conexión"
            else -> "Sin comprobar"
        }
        SecurityDeviceCard(
            "▰",
            "Cámaras Home Assistant",
            status,
            "Las cámaras compatibles aparecerán aquí.",
            TextoDesactivado
        )
        return
    }

    cameras.forEachIndexed { index, camera ->
        SecurityCameraCard(camera, baseUrl, accessToken)
        if (index != cameras.lastIndex) Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun SecurityCameraCard(camera: SecurityCameraEntity, baseUrl: String, accessToken: String) {
    var bitmap by remember(camera.entityId, baseUrl) { mutableStateOf<android.graphics.Bitmap?>(null) }
    var imageMessage by remember(camera.entityId, baseUrl) { mutableStateOf("Cargando imagen…") }
    var loading by remember(camera.entityId, baseUrl) { mutableStateOf(false) }

    fun loadImage() {
        if (!camera.available || baseUrl.isBlank() || accessToken.isBlank() || loading) return
        loading = true
        imageMessage = "Cargando imagen…"
        HomeAssistantCameraImageLoader.load(baseUrl, accessToken, camera.entityId) { result ->
            loading = false
            when (result) {
                is HomeAssistantCameraImageLoader.Result.Success -> {
                    bitmap = result.bitmap
                    imageMessage = "Imagen actual de Home Assistant"
                }
                is HomeAssistantCameraImageLoader.Result.Failure -> {
                    bitmap = null
                    imageMessage = result.message
                }
            }
        }
    }

    LaunchedEffect(camera.entityId, camera.available, baseUrl, accessToken) { loadImage() }

    OneHouseCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = AzulOneHouse.copy(alpha = 0.16f), shape = RoundedCornerShape(16.dp)) {
                    Text("▰", color = AzulClaro, fontSize = 25.sp, modifier = Modifier.padding(horizontal = 15.dp, vertical = 12.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(camera.name, color = TextoPrincipal, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        if (camera.available) "Disponible" else "No disponible",
                        color = if (camera.available) Color(0xFF61D88B) else TextoDesactivado,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = "Imagen de ${camera.name}",
                    modifier = Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(16.dp))
                        .background(AzulOneHouse.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(imageMessage, color = TextoSecundario, style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(imageMessage, color = TextoSecundario, style = MaterialTheme.typography.bodySmall)
            if (camera.available) {
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = { loadImage() }, modifier = Modifier.fillMaxWidth(), enabled = !loading) {
                    Text(if (loading) "Actualizando imagen…" else "Actualizar imagen")
                }
            }
        }
    }
}

@Composable
private fun securityTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextoPrincipal, unfocusedTextColor = TextoPrincipal,
    focusedBorderColor = AzulClaro, unfocusedBorderColor = AzulOneHouse.copy(alpha = 0.55f),
    focusedLabelColor = AzulClaro, unfocusedLabelColor = TextoSecundario, cursorColor = AzulClaro
)

@Composable private fun SectionTitle(text: String) {
    Text(text, color = TextoPrincipal, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun SecurityDeviceCard(symbol: String, title: String, status: String, detail: String, statusColor: Color) {
    OneHouseCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(color = AzulOneHouse.copy(alpha = 0.16f), shape = RoundedCornerShape(16.dp)) {
                Text(symbol, color = AzulClaro, fontSize = 25.sp, modifier = Modifier.padding(horizontal = 15.dp, vertical = 12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, color = TextoPrincipal, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(3.dp))
                Text(status, color = statusColor, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(5.dp))
                Text(detail, color = TextoSecundario, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
