package com.onehouse.app.feature.more

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onehouse.app.design.AzulClaro
import com.onehouse.app.design.FondoInferior
import com.onehouse.app.design.FondoSuperior
import com.onehouse.app.design.OneHouseCard
import com.onehouse.app.design.TextoDesactivado
import com.onehouse.app.design.TextoPrincipal
import com.onehouse.app.design.TextoSecundario

@Composable
fun MoreScreen(
    onConfigurationSelected: () -> Unit,
    onKnxAddressesSelected: () -> Unit,
    onToolsSelected: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(FondoSuperior, FondoInferior)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Más",
                color = TextoPrincipal,
                style = MaterialTheme.typography.headlineLarge
            )
            Text(
                text = "Opciones y herramientas de OneHouse",
                color = TextoSecundario,
                style = MaterialTheme.typography.bodyMedium
            )

            MoreOptionCard(
                symbol = "⚙",
                title = "Configuración",
                subtitle = "Conexión KNX y preferencias de la aplicación",
                onClick = onConfigurationSelected
            )
            MoreOptionCard(
                symbol = "ⓘ",
                title = "Acerca de",
                subtitle = "Información de OneHouse",
                enabled = false
            )
            MoreOptionCard(
                symbol = "⌘",
                title = "Direcciones KNX",
                subtitle = "Editar las direcciones utilizadas por OneHouse",
                onClick = onKnxAddressesSelected
            )
            MoreOptionCard(
                symbol = "?",
                title = "Manual",
                subtitle = "Ayuda y documentación de la aplicación",
                enabled = false
            )
            MoreOptionCard(
                symbol = "⌘",
                title = "Herramientas",
                subtitle = "Utilidades avanzadas de OneHouse",
                onClick = onToolsSelected
            )

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun MoreOptionCard(
    symbol: String,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    OneHouseCard(onClick = if (enabled) onClick else null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = AzulClaro.copy(alpha = if (enabled) 0.12f else 0.06f),
                shape = CircleShape
            ) {
                Text(
                    text = symbol,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    color = if (enabled) AzulClaro else TextoDesactivado,
                    fontSize = 24.sp
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp)
            ) {
                Text(
                    text = title,
                    color = if (enabled) TextoPrincipal else TextoDesactivado,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                )
                Text(
                    text = subtitle,
                    color = if (enabled) TextoSecundario else TextoDesactivado,
                    fontSize = 13.sp
                )
                if (!enabled) {
                    Text(
                        text = "Próximamente",
                        color = AzulClaro.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Text(
                text = if (enabled) "›" else "",
                color = TextoSecundario,
                fontSize = 32.sp
            )
        }
    }
}
