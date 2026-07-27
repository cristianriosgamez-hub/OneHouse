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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
            GeneralSecurityStatusCard()

            Spacer(Modifier.height(22.dp))
            SectionTitle("Puertas y ventanas")
            Spacer(Modifier.height(10.dp))
            SecurityDeviceCard(
                symbol = "▣",
                title = "Sensores de apertura",
                status = "Sin configurar",
                detail = "Se mostrarán aquí las puertas y ventanas vinculadas."
            )

            Spacer(Modifier.height(22.dp))
            SectionTitle("Movimiento")
            Spacer(Modifier.height(10.dp))
            SecurityDeviceCard(
                symbol = "◉",
                title = "Sensores de movimiento",
                status = "Sin configurar",
                detail = "Se mostrarán aquí el estado y la última detección."
            )

            Spacer(Modifier.height(22.dp))
            SectionTitle("Cámaras")
            Spacer(Modifier.height(10.dp))
            SecurityDeviceCard(
                symbol = "▰",
                title = "Cámaras Xiaomi",
                status = "Sin configurar",
                detail = "La visualización dependerá de la compatibilidad del modelo."
            )

            Spacer(Modifier.height(26.dp))
            Text(
                text = "Esta pantalla no contiene estados simulados. Los dispositivos aparecerán cuando se configure la conexión con Home Assistant.",
                color = TextoDesactivado,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun GeneralSecurityStatusCard() {
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
                        .background(TextoDesactivado)
                )
                Spacer(Modifier.size(10.dp))
                Text(
                    text = "Sin configurar",
                    color = TextoPrincipal,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(7.dp))
            Text(
                text = "Configura Home Assistant para recibir los estados reales de Xiaomi Home.",
                color = TextoSecundario,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

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
