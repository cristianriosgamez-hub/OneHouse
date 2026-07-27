package com.onehouse.app.feature.security

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.ui.window.Dialog
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
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SecurityScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { HomeAssistantSettingsStore(context.applicationContext) }
    val snapshotStore = remember { HomeAssistantSecuritySnapshotStore(context.applicationContext) }
    val eventStore = remember { HomeAssistantSecurityEventStore(context.applicationContext) }
    val notificationManager = remember { HomeAssistantSecurityNotificationManager(context.applicationContext) }
    val monitoringPreferences = remember { HomeAssistantSecurityMonitoringPreferences(context.applicationContext) }
    var settings by remember { mutableStateOf(store.read()) }
    var baseUrl by remember { mutableStateOf(settings.baseUrl) }
    var accessToken by remember { mutableStateOf(settings.accessToken) }
    var urlError by remember { mutableStateOf<String?>(null) }
    var snapshot by remember { mutableStateOf(HomeAssistantSecuritySnapshot()) }
    var loadingSensors by remember { mutableStateOf(false) }
    var sensorMessage by remember { mutableStateOf("Pulsa actualizar para leer los sensores") }
    var securityEvents by remember { mutableStateOf(eventStore.readEvents()) }
    var notificationsEnabled by remember { mutableStateOf(notificationManager.enabled) }
    var notificationPermissionGranted by remember { mutableStateOf(notificationManager.hasPermission()) }
    var monitoringOptions by remember { mutableStateOf(monitoringPreferences.read()) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationPermissionGranted = granted
        notificationsEnabled = granted
        notificationManager.enabled = granted
        HomeAssistantSecurityBackgroundScheduler.setEnabled(context.applicationContext, granted)
    }

    fun persist(status: HomeAssistantConnectionStatus, message: String, tested: Boolean) {
        settings = HomeAssistantSettings(
            baseUrl = baseUrl.trim(), accessToken = accessToken.trim(), lastStatus = status,
            lastMessage = message,
            lastTestEpochMillis = if (tested) System.currentTimeMillis() else settings.lastTestEpochMillis
        )
        store.write(settings)
    }

    fun refreshSensors(silent: Boolean = false) {
        if (baseUrl.isBlank() || accessToken.isBlank()) {
            if (!silent) sensorMessage = "Configura primero Home Assistant"
            return
        }
        if (loadingSensors) return
        loadingSensors = true
        if (!silent) sensorMessage = "Leyendo sensores…"
        HomeAssistantSecurityClient.fetch(baseUrl, accessToken) { result ->
            loadingSensors = false
            when (result) {
                is HomeAssistantSecurityClient.Result.Success -> {
                    snapshot = result.snapshot
                    snapshotStore.write(result.snapshot)
                    val processed = eventStore.process(result.snapshot)
                    securityEvents = processed.events
                    notificationManager.notify(monitoringPreferences.filterNotificationEvents(processed.newEvents))
                    if (!silent) {
                        sensorMessage = if (snapshot.openings.isEmpty() && snapshot.motions.isEmpty() && snapshot.cameras.isEmpty()) {
                            "No se encontraron sensores ni cámaras compatibles"
                        } else "Dispositivos actualizados correctamente"
                    }
                    persist(HomeAssistantConnectionStatus.CONNECTED, "Conectado correctamente con Home Assistant", true)
                }
                is HomeAssistantSecurityClient.Result.Failure -> {
                    if (!silent) sensorMessage = result.message
                    persist(HomeAssistantConnectionStatus.FAILED, result.message, true)
                }
            }
        }
    }


    LaunchedEffect(settings.lastStatus, baseUrl, accessToken) {
        if (settings.lastStatus == HomeAssistantConnectionStatus.CONNECTED && baseUrl.isNotBlank() && accessToken.isNotBlank()) {
            refreshSensors(silent = true)
            while (true) {
                delay(30_000)
                refreshSensors(silent = true)
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
                                                refreshSensors(silent = false)
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
                onClick = { refreshSensors(silent = false) }, modifier = Modifier.fillMaxWidth(),
                enabled = !loadingSensors && settings.lastStatus == HomeAssistantConnectionStatus.CONNECTED,
                colors = ButtonDefaults.buttonColors(containerColor = AzulOneHouse.copy(alpha = 0.85f))
            ) { Text(if (loadingSensors) "Actualizando…" else "Actualizar sensores") }


            Spacer(Modifier.height(22.dp))
            SecurityNotificationsCard(
                enabled = notificationsEnabled,
                permissionGranted = notificationPermissionGranted,
                monitoringOptions = monitoringOptions,
                onMonitoringOptionsChange = { updated ->
                    monitoringOptions = updated
                    monitoringPreferences.write(updated)
                },
                onEnabledChange = { enabled ->
                    if (!enabled) {
                        notificationsEnabled = false
                        notificationManager.enabled = false
                        HomeAssistantSecurityBackgroundScheduler.setEnabled(context.applicationContext, false)
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationManager.hasPermission()) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        notificationsEnabled = true
                        notificationPermissionGranted = true
                        notificationManager.enabled = true
                        HomeAssistantSecurityBackgroundScheduler.setEnabled(context.applicationContext, true)
                    }
                }
            )

            Spacer(Modifier.height(22.dp))
            SensorSection("Puertas y ventanas", "▣", snapshot.openings, SecurityEntityCategory.OPENING, settings)
            Spacer(Modifier.height(22.dp))
            SensorSection("Movimiento", "◉", snapshot.motions, SecurityEntityCategory.MOTION, settings)

            Spacer(Modifier.height(22.dp))
            SectionTitle("Cámaras")
            Spacer(Modifier.height(10.dp))
            CameraSection(
                cameras = snapshot.cameras,
                settings = settings,
                baseUrl = baseUrl,
                accessToken = accessToken,
                autoRefreshEnabled = monitoringOptions.cameraAutoRefreshEnabled,
                refreshToken = snapshot.fetchedAtEpochMillis
            )

            Spacer(Modifier.height(22.dp))
            SecurityEventHistory(
                events = securityEvents,
                onClear = {
                    eventStore.clear()
                    securityEvents = emptyList()
                }
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SecurityNotificationsCard(
    enabled: Boolean,
    permissionGranted: Boolean,
    monitoringOptions: HomeAssistantSecurityMonitoringOptions,
    onMonitoringOptionsChange: (HomeAssistantSecurityMonitoringOptions) -> Unit,
    onEnabledChange: (Boolean) -> Unit
) {
    SectionTitle("Avisos de seguridad")
    Spacer(Modifier.height(10.dp))
    OneHouseCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Notificaciones en el móvil",
                        color = TextoPrincipal,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        when {
                            enabled -> "Vigilancia activa. Con la pantalla abierta se comprueba cada 30 s y en segundo plano aproximadamente cada 15 min."
                            !permissionGranted -> "Activa el permiso para recibir alertas de seguridad."
                            else -> "Los avisos están desactivados."
                        },
                        color = TextoSecundario,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(checked = enabled, onCheckedChange = onEnabledChange)
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 14.dp),
                color = TextoDesactivado.copy(alpha = 0.25f)
            )
            Text(
                "ELEMENTOS SUPERVISADOS",
                color = AzulClaro,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
            Spacer(Modifier.height(8.dp))
            MonitoringOptionRow(
                title = "Puertas y ventanas",
                detail = "Generar avisos cuando se detecte una apertura.",
                checked = monitoringOptions.openingsEnabled,
                onCheckedChange = { onMonitoringOptionsChange(monitoringOptions.copy(openingsEnabled = it)) }
            )
            MonitoringOptionRow(
                title = "Movimiento",
                detail = "Generar avisos cuando un sensor detecte presencia.",
                checked = monitoringOptions.motionEnabled,
                onCheckedChange = { onMonitoringOptionsChange(monitoringOptions.copy(motionEnabled = it)) }
            )
            MonitoringOptionRow(
                title = "Actualizar cámaras automáticamente",
                detail = "Renovar las imágenes durante cada actualización de Seguridad.",
                checked = monitoringOptions.cameraAutoRefreshEnabled,
                onCheckedChange = { onMonitoringOptionsChange(monitoringOptions.copy(cameraAutoRefreshEnabled = it)) },
                showDivider = false
            )
        }
    }
}

@Composable
private fun MonitoringOptionRow(
    title: String,
    detail: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    showDivider: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = TextoPrincipal, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(3.dp))
            Text(detail, color = TextoSecundario, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
    if (showDivider) {
        HorizontalDivider(color = TextoDesactivado.copy(alpha = 0.16f))
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
private fun SecurityEventHistory(events: List<SecurityEvent>, onClear: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SectionTitle("Últimos eventos")
        Spacer(Modifier.weight(1f))
        if (events.isNotEmpty()) {
            TextButton(onClick = onClear) { Text("Borrar") }
        }
    }
    Spacer(Modifier.height(10.dp))

    if (events.isEmpty()) {
        SecurityDeviceCard(
            symbol = "◷",
            title = "Historial de seguridad",
            status = "Sin eventos registrados",
            detail = "Aquí aparecerán los cambios detectados en puertas y sensores de movimiento.",
            statusColor = TextoDesactivado
        )
        return
    }

    events.take(12).forEachIndexed { index, event ->
        val isOpening = event.category == SecurityEntityCategory.OPENING
        val status = when {
            isOpening && event.active -> "Puerta o ventana abierta"
            isOpening -> "Puerta o ventana cerrada"
            event.active -> "Movimiento detectado"
            else -> "Movimiento finalizado"
        }
        val color = if (event.active) Color(0xFFFFB74D) else Color(0xFF61D88B)
        SecurityDeviceCard(
            symbol = if (isOpening) "▣" else "◉",
            title = event.name,
            status = status,
            detail = formatSecurityEventTime(event.occurredAtEpochMillis),
            statusColor = color
        )
        if (index != events.take(12).lastIndex) Spacer(Modifier.height(10.dp))
    }
}

private fun formatSecurityEventTime(epochMillis: Long): String {
    if (epochMillis <= 0L) return "Sin fecha"
    return SimpleDateFormat("dd/MM/yyyy · HH:mm:ss", Locale.getDefault()).format(Date(epochMillis))
}

@Composable
private fun CameraSection(
    cameras: List<SecurityCameraEntity>,
    settings: HomeAssistantSettings,
    baseUrl: String,
    accessToken: String,
    autoRefreshEnabled: Boolean,
    refreshToken: Long
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
        SecurityCameraCard(camera, baseUrl, accessToken, autoRefreshEnabled, refreshToken)
        if (index != cameras.lastIndex) Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun SecurityCameraCard(
    camera: SecurityCameraEntity,
    baseUrl: String,
    accessToken: String,
    autoRefreshEnabled: Boolean,
    refreshToken: Long
) {
    var bitmap by remember(camera.entityId, baseUrl) { mutableStateOf<android.graphics.Bitmap?>(null) }
    var imageMessage by remember(camera.entityId, baseUrl) { mutableStateOf("Cargando imagen…") }
    var loading by remember(camera.entityId, baseUrl) { mutableStateOf(false) }
    var showExpanded by remember(camera.entityId) { mutableStateOf(false) }

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

    LaunchedEffect(camera.entityId, camera.available, baseUrl, accessToken, autoRefreshEnabled, refreshToken) {
        if (bitmap == null || autoRefreshEnabled) loadImage()
    }

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
                    modifier = Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(16.dp))
                        .clickable { showExpanded = true },
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
            if (bitmap != null) {
                Spacer(Modifier.height(6.dp))
                Text("Pulsa la imagen para ampliarla", color = TextoDesactivado, style = MaterialTheme.typography.bodySmall)
            }
            if (camera.available) {
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = { loadImage() }, modifier = Modifier.fillMaxWidth(), enabled = !loading) {
                    Text(if (loading) "Actualizando imagen…" else "Actualizar imagen")
                }
            }
        }
    }

    if (showExpanded && bitmap != null) {
        Dialog(onDismissRequest = { showExpanded = false }) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color.Black
            ) {
                Column {
                    Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = "Vista ampliada de ${camera.name}",
                        modifier = Modifier.fillMaxWidth().heightIn(min = 260.dp, max = 620.dp),
                        contentScale = ContentScale.Fit
                    )
                    TextButton(
                        onClick = { showExpanded = false },
                        modifier = Modifier.align(Alignment.End).padding(8.dp)
                    ) { Text("Cerrar") }
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
