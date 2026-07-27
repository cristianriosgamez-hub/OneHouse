package com.onehouse.app.feature.security

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onehouse.app.design.AzulClaro
import com.onehouse.app.design.AzulOneHouse
import com.onehouse.app.design.FondoInferior
import com.onehouse.app.design.FondoMedio
import com.onehouse.app.design.FondoSuperior
import com.onehouse.app.design.OneHouseCard
import com.onehouse.app.design.TextoDesactivado
import com.onehouse.app.design.TextoPrincipal
import com.onehouse.app.design.TextoSecundario

@Composable
fun SecurityScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { HomeAssistantSettingsStore(context.applicationContext) }
    var settings by remember { mutableStateOf(store.read()) }
    var baseUrl by remember { mutableStateOf(settings.baseUrl) }
    var accessToken by remember { mutableStateOf(settings.accessToken) }
    var urlError by remember { mutableStateOf<String?>(null) }

    fun persist(status: HomeAssistantConnectionStatus, message: String, tested: Boolean) {
        settings = HomeAssistantSettings(
            baseUrl = baseUrl.trim(),
            accessToken = accessToken.trim(),
            lastStatus = status,
            lastMessage = message,
            lastTestEpochMillis = if (tested) System.currentTimeMillis() else settings.lastTestEpochMillis
        )
        store.write(settings)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    onClick = onBack,
                    shape = CircleShape,
                    color = AzulOneHouse.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = "‹",
                        color = TextoPrincipal,
                        fontSize = 34.sp,
                        modifier = Modifier.padding(horizontal = 15.dp, vertical = 4.dp)
                    )
                }
                Spacer(Modifier.size(14.dp))
                Column {
                    Text(
                        text = "Seguridad",
                        color = TextoPrincipal,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Sensores y cámaras Xiaomi",
                        color = TextoSecundario,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            GeneralSecurityStatusCard(settings)

            Spacer(Modifier.height(22.dp))
            SectionTitle("Conexión con Home Assistant")
            Spacer(Modifier.height(10.dp))
            OneHouseCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Servidor local",
                        color = TextoPrincipal,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Introduce la dirección y un token de larga duración creado en Home Assistant.",
                        color = TextoSecundario,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = {
                            baseUrl = it
                            urlError = null
                            persist(
                                HomeAssistantConnectionStatus.NOT_TESTED,
                                "Cambios pendientes de comprobar",
                                tested = false
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Dirección Home Assistant") },
                        placeholder = { Text("http://192.168.1.20:8123") },
                        singleLine = true,
                        isError = urlError != null,
                        supportingText = urlError?.let { message -> { Text(message) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        colors = securityTextFieldColors()
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = accessToken,
                        onValueChange = {
                            accessToken = it
                            persist(
                                HomeAssistantConnectionStatus.NOT_TESTED,
                                "Cambios pendientes de comprobar",
                                tested = false
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Token de acceso") },
                        placeholder = { Text("Token de larga duración") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        colors = securityTextFieldColors()
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val validation = HomeAssistantConnectionTester.validateBaseUrl(baseUrl)
                            urlError = validation
                            when {
                                validation != null -> persist(
                                    HomeAssistantConnectionStatus.FAILED,
                                    validation,
                                    tested = true
                                )
                                accessToken.isBlank() -> persist(
                                    HomeAssistantConnectionStatus.FAILED,
                                    "Introduce el token de acceso",
                                    tested = true
                                )
                                else -> {
                                    persist(
                                        HomeAssistantConnectionStatus.TESTING,
                                        "Comprobando conexión…",
                                        tested = false
                                    )
                                    HomeAssistantConnectionTester.test(baseUrl, accessToken) { result ->
                                        when (result) {
                                            HomeAssistantConnectionTester.Result.Success -> persist(
                                                HomeAssistantConnectionStatus.CONNECTED,
                                                "Conectado correctamente con Home Assistant",
                                                tested = true
                                            )
                                            is HomeAssistantConnectionTester.Result.Failure -> persist(
                                                HomeAssistantConnectionStatus.FAILED,
                                                result.message,
                                                tested = true
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = settings.lastStatus != HomeAssistantConnectionStatus.TESTING,
                        colors = ButtonDefaults.buttonColors(containerColor = AzulOneHouse)
                    ) {
                        Text(
                            text = if (settings.lastStatus == HomeAssistantConnectionStatus.TESTING) {
                                "Comprobando…"
                            } else {
                                "Guardar y comprobar"
                            }
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "El token se guarda cifrado con Android Keystore y no se incluye en la interfaz.",
                        color = TextoDesactivado,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(Modifier.height(22.dp))
            SectionTitle("Puertas y ventanas")
            Spacer(Modifier.height(10.dp))
            SecurityDeviceCard(
                symbol = "▣",
                title = "Sensores de apertura",
                status = deviceStatus(settings),
                detail = "Se mostrarán aquí las puertas y ventanas vinculadas."
            )

            Spacer(Modifier.height(22.dp))
            SectionTitle("Movimiento")
            Spacer(Modifier.height(10.dp))
            SecurityDeviceCard(
                symbol = "◉",
                title = "Sensores de movimiento",
                status = deviceStatus(settings),
                detail = "Se mostrarán aquí el estado y la última detección."
            )

            Spacer(Modifier.height(22.dp))
            SectionTitle("Cámaras")
            Spacer(Modifier.height(10.dp))
            SecurityDeviceCard(
                symbol = "▰",
                title = "Cámaras Xiaomi",
                status = deviceStatus(settings),
                detail = "La visualización dependerá de la compatibilidad del modelo."
            )

            Spacer(Modifier.height(26.dp))
            Text(
                text = "Esta versión únicamente configura y comprueba Home Assistant. La lectura de entidades Xiaomi se añadirá en el siguiente paso.",
                color = TextoDesactivado,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun GeneralSecurityStatusCard(settings: HomeAssistantSettings) {
    val statusColor = when (settings.lastStatus) {
        HomeAssistantConnectionStatus.CONNECTED -> Color(0xFF61D88B)
        HomeAssistantConnectionStatus.FAILED -> Color(0xFFFF6B6B)
        HomeAssistantConnectionStatus.TESTING -> AzulClaro
        HomeAssistantConnectionStatus.NOT_CONFIGURED,
        HomeAssistantConnectionStatus.NOT_TESTED -> TextoDesactivado
    }
    val title = when (settings.lastStatus) {
        HomeAssistantConnectionStatus.CONNECTED -> "Conectado"
        HomeAssistantConnectionStatus.FAILED -> "Sin conexión"
        HomeAssistantConnectionStatus.TESTING -> "Comprobando"
        HomeAssistantConnectionStatus.NOT_TESTED -> "Sin comprobar"
        HomeAssistantConnectionStatus.NOT_CONFIGURED -> "Sin configurar"
    }

    OneHouseCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "ESTADO GENERAL",
                color = AzulClaro,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(Modifier.size(10.dp))
                Text(
                    text = title,
                    color = TextoPrincipal,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(7.dp))
            Text(
                text = settings.lastMessage,
                color = TextoSecundario,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun deviceStatus(settings: HomeAssistantSettings): String = when (settings.lastStatus) {
    HomeAssistantConnectionStatus.CONNECTED -> "Conexión preparada"
    HomeAssistantConnectionStatus.FAILED -> "Sin conexión"
    HomeAssistantConnectionStatus.TESTING -> "Comprobando"
    HomeAssistantConnectionStatus.NOT_TESTED -> "Sin comprobar"
    HomeAssistantConnectionStatus.NOT_CONFIGURED -> "Sin configurar"
}

@Composable
private fun securityTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextoPrincipal,
    unfocusedTextColor = TextoPrincipal,
    focusedBorderColor = AzulClaro,
    unfocusedBorderColor = AzulOneHouse.copy(alpha = 0.55f),
    focusedLabelColor = AzulClaro,
    unfocusedLabelColor = TextoSecundario,
    cursorColor = AzulClaro
)

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = TextoPrincipal,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun SecurityDeviceCard(
    symbol: String,
    title: String,
    status: String,
    detail: String
) {
    OneHouseCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                color = AzulOneHouse.copy(alpha = 0.16f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = symbol,
                    color = AzulClaro,
                    fontSize = 25.sp,
                    modifier = Modifier.padding(horizontal = 15.dp, vertical = 12.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TextoPrincipal,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = status,
                    color = TextoDesactivado,
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = detail,
                    color = TextoSecundario,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
