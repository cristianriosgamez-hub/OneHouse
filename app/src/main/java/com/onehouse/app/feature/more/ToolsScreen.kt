package com.onehouse.app.feature.more

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
import com.onehouse.app.design.TextoPrincipal
import com.onehouse.app.design.TextoSecundario

@Composable
fun ToolsScreen(
    onBack: () -> Unit,
    onKnxDiagnosticsSelected: () -> Unit,
    onBackupRestoreSelected: () -> Unit,
    onConsumptionTransferSelected: () -> Unit
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "‹",
                    color = TextoPrincipal,
                    fontSize = 44.sp,
                    modifier = Modifier
                        .clickable(onClick = onBack)
                        .padding(end = 14.dp)
                )
                Column {
                    Text(
                        text = "Herramientas",
                        color = TextoPrincipal,
                        style = MaterialTheme.typography.headlineLarge
                    )
                    Text(
                        text = "Utilidades avanzadas de OneHouse",
                        color = TextoSecundario,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            ToolOptionCard(
                symbol = "◫",
                title = "Diagnóstico KNX",
                subtitle = "Valores reales, tiempos y prueba de lectura",
                onClick = onKnxDiagnosticsSelected
            )
            ToolOptionCard(
                symbol = "⇅",
                title = "Backup y restauración",
                subtitle = "Exportar y recuperar la configuración",
                onClick = onBackupRestoreSelected
            )
            ToolOptionCard(
                symbol = "▦",
                title = "Importar / Exportar consumos",
                subtitle = "Excel con ENDESA, AGBAR, Climatización y ACS",
                onClick = onConsumptionTransferSelected
            )

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun ToolOptionCard(
    symbol: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    OneHouseCard(onClick = onClick) {
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
                    text = symbol,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    color = AzulClaro,
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
                    color = TextoPrincipal,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                )
                Text(
                    text = subtitle,
                    color = TextoSecundario,
                    fontSize = 13.sp
                )
            }
            Text(
                text = "›",
                color = TextoSecundario,
                fontSize = 32.sp
            )
        }
    }
}
