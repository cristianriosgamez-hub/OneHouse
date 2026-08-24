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
fun AutomationControlScreen(
    onBack: () -> Unit,
    onScenesSelected: () -> Unit,
    onAutomationsSelected: () -> Unit,
    onSolarSchedulesSelected: () -> Unit,
    onWeeklySchedulesSelected: () -> Unit,
    onKnxAddressesSelected: () -> Unit
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
                        text = "Automatización y control",
                        color = TextoPrincipal,
                        style = MaterialTheme.typography.headlineLarge
                    )
                    Text(
                        text = "Escenas, horarios y configuración avanzada",
                        color = TextoSecundario,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            AutomationOptionCard(
                symbol = "★",
                title = "Escenas inteligentes",
                subtitle = "Varias acciones KNX con una sola pulsación",
                onClick = onScenesSelected
            )
            AutomationOptionCard(
                symbol = "⚡",
                title = "Automatizaciones",
                subtitle = "Condiciones KNX y acciones automáticas",
                onClick = onAutomationsSelected
            )
            AutomationOptionCard(
                symbol = "☀",
                title = "Amanecer y atardecer",
                subtitle = "Acciones KNX según la luz solar",
                onClick = onSolarSchedulesSelected
            )
            AutomationOptionCard(
                symbol = "◷",
                title = "Horarios semanales",
                subtitle = "Luces, climatización y persianas",
                onClick = onWeeklySchedulesSelected
            )
            AutomationOptionCard(
                symbol = "⌁",
                title = "Direcciones KNX",
                subtitle = "Ver y editar mandos, estados y sensores X/X/X",
                onClick = onKnxAddressesSelected
            )

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun AutomationOptionCard(
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
