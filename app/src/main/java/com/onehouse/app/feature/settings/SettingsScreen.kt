package com.onehouse.app.feature.settings

import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onehouse.app.data.knx.KnxConnectionStatus
import com.onehouse.app.data.knx.KnxSettingsRepository
import com.onehouse.app.data.knx.SettingsDataStore
import com.onehouse.app.design.AzulClaro
import com.onehouse.app.design.AzulOneHouse
import com.onehouse.app.design.BordeTarjeta
import com.onehouse.app.design.FondoInferior
import com.onehouse.app.design.FondoSuperior
import com.onehouse.app.design.FondoTarjetaSecundaria
import com.onehouse.app.design.RojoEstado
import com.onehouse.app.design.TextoDesactivado
import com.onehouse.app.design.TextoPrincipal
import com.onehouse.app.design.TextoSecundario
import com.onehouse.app.design.VerdeEstado
import com.onehouse.app.design.OneHouseCard
import com.onehouse.app.design.OneHouseTextField
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel = remember {
        SettingsViewModel(
            KnxSettingsRepository(SettingsDataStore(context.applicationContext))
        )
    }
    var revision by remember { mutableIntStateOf(0) }

    DisposableEffect(viewModel) {
        viewModel.observe { revision++ }
        onDispose { viewModel.close() }
    }

    // Fuerza la lectura de los datos tras cada modificación.
    @Suppress("UNUSED_VARIABLE") val currentRevision = revision
    val settings = viewModel.settings
    val validation = viewModel.validation

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(FondoSuperior, FondoInferior)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsHeader(onBack = onBack)
            Text(
                text = "Conexión y estado de OneHouse",
                color = TextoSecundario,
                style = MaterialTheme.typography.bodyMedium
            )

            SettingsCardHeader(
                symbol = "⌁",
                title = "Conexión KNX",
                subtitle = "Configura la conexión con tu instalación KNX"
            ) {
                OneHouseTextField(
                    value = settings.localIp,
                    onValueChange = { value -> viewModel.update { it.copy(localIp = value) } },
                    label = "Dirección IP (Local)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                FieldError(validation.localIpError)

                OneHouseTextField(
                    value = settings.localPort,
                    onValueChange = { value ->
                        viewModel.update { it.copy(localPort = value.filter(Char::isDigit).take(5)) }
                    },
                    label = "Puerto",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                FieldError(validation.localPortError)

                SettingsDivider()

                OneHouseTextField(
                    value = settings.remoteIp,
                    onValueChange = { value -> viewModel.update { it.copy(remoteIp = value) } },
                    label = "Dirección IP secundaria (Internet)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                FieldError(validation.remoteIpError)

                OneHouseTextField(
                    value = settings.remotePort,
                    onValueChange = { value ->
                        viewModel.update { it.copy(remotePort = value.filter(Char::isDigit).take(5)) }
                    },
                    label = "Puerto secundario",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                FieldError(validation.remotePortError)
            }

            SettingsCardHeader(
                symbol = "ⓘ",
                title = "Estado de la aplicación",
                subtitle = "Información general del sistema"
            ) {
                StatusRow(
                    label = "Estado",
                    value = connectionLabel(viewModel.connectionStatus),
                    valueColor = connectionColor(viewModel.connectionStatus)
                )
                SettingsDivider()
                StatusRow(label = "Versión de la aplicación", value = appVersionName(context))
                SettingsDivider()
                StatusRow(
                    label = "Última actualización",
                    value = formatDate(settings.lastUpdatedEpochMillis)
                )
            }

            SettingsCardHeader(
                symbol = "▣",
                title = "Información del dispositivo",
                subtitle = "Detalles de tu dispositivo"
            ) {
                StatusRow(label = "Dispositivo", value = "${Build.MANUFACTURER} ${Build.MODEL}")
                SettingsDivider()
                StatusRow(label = "Android", value = Build.VERSION.RELEASE)
                SettingsDivider()
                StatusRow(
                    label = "ID de dispositivo",
                    value = Settings.Secure.getString(
                        context.contentResolver,
                        Settings.Secure.ANDROID_ID
                    ).orEmpty().takeLast(12)
                )
            }

            OneHouseCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = AzulClaro.copy(alpha = 0.12f),
                        shape = CircleShape
                    ) {
                        Text(
                            text = "↻",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            color = AzulClaro,
                            fontSize = 27.sp
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 14.dp)
                    ) {
                        Text("Reconexión automática", color = TextoPrincipal, fontWeight = FontWeight.SemiBold)
                        Text("Reintentar si la conexión se pierde", color = TextoSecundario, fontSize = 13.sp)
                    }
                    Switch(
                        checked = settings.autoReconnect,
                        onCheckedChange = { checked ->
                            viewModel.update { it.copy(autoReconnect = checked) }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = AzulOneHouse,
                            uncheckedThumbColor = TextoSecundario,
                            uncheckedTrackColor = FondoTarjetaSecundaria
                        )
                    )
                }
            }

            OneHouseCard(
                onClick = {
                    if (viewModel.connectionStatus != KnxConnectionStatus.TESTING) {
                        viewModel.testConnection()
                    }
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (viewModel.connectionStatus == KnxConnectionStatus.TESTING) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(7.dp),
                            color = AzulClaro,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(text = "↻", color = AzulClaro, fontSize = 30.sp)
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 14.dp)
                    ) {
                        Text("Probar conexión", color = TextoPrincipal, fontWeight = FontWeight.SemiBold)
                        Text(viewModel.statusMessage, color = TextoSecundario, fontSize = 13.sp)
                    }
                    Text(text = "›", color = TextoSecundario, fontSize = 32.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun SettingsHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "‹",
            color = TextoPrincipal,
            fontSize = 40.sp,
            fontWeight = FontWeight.Light,
            modifier = Modifier
                .padding(end = 10.dp)
                .background(Color.Transparent)
                .clickable(onClick = onBack)
                .padding(horizontal = 8.dp, vertical = 2.dp)
        )
        Text(
            text = "Configuración",
            color = TextoPrincipal,
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SettingsCardHeader(
    symbol: String,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    OneHouseCard {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = AzulClaro.copy(alpha = 0.12f),
                    shape = CircleShape
                ) {
                    Text(
                        text = symbol,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        color = AzulClaro,
                        fontSize = 24.sp
                    )
                }
                Column(modifier = Modifier.padding(start = 14.dp)) {
                    Text(title, color = TextoPrincipal, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
                    Text(subtitle, color = TextoSecundario, fontSize = 13.sp)
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            content()
        }
    }
}

@Composable
private fun StatusRow(label: String, value: String, valueColor: Color = TextoSecundario) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextoSecundario, modifier = Modifier.weight(1f))
        Text(
            value,
            color = valueColor,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

@Composable
private fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(BordeTarjeta.copy(alpha = 0.65f))
    )
}

@Composable
private fun FieldError(message: String?) {
    if (message != null) {
        Text(
            text = message,
            color = RojoEstado,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

private fun connectionLabel(status: KnxConnectionStatus): String = when (status) {
    KnxConnectionStatus.NOT_TESTED -> "No comprobado"
    KnxConnectionStatus.TESTING -> "Comprobando…"
    KnxConnectionStatus.CONNECTED -> "Conectado"
    KnxConnectionStatus.FAILED -> "Sin conexión"
}

private fun connectionColor(status: KnxConnectionStatus): Color = when (status) {
    KnxConnectionStatus.CONNECTED -> VerdeEstado
    KnxConnectionStatus.FAILED -> RojoEstado
    KnxConnectionStatus.TESTING -> AzulClaro
    KnxConnectionStatus.NOT_TESTED -> TextoDesactivado
}

private fun formatDate(epochMillis: Long): String {
    if (epochMillis <= 0L) return "Sin cambios"
    return SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(epochMillis))
}


@Suppress("DEPRECATION")
private fun appVersionName(context: android.content.Context): String = runCatching {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName
}.getOrNull().orEmpty().ifBlank { "—" }
