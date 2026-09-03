package com.onehouse.app.feature.maintenance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Blinds
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onehouse.app.design.BordeTarjeta
import com.onehouse.app.design.FondoInferior
import com.onehouse.app.design.FondoSuperior
import com.onehouse.app.design.TextoPrincipal
import com.onehouse.app.design.TextoSecundario
import com.onehouse.app.feature.rooms.detail.RoomHeader
import com.onehouse.app.knx.KnxAddressBook
import com.onehouse.app.knx.KnxHomeStateRepository

private val MaintenanceCard = Color(0xFF0A1926)
private val Blue = Color(0xFF168EFF)
private val Purple = Color(0xFFBF5BFF)
private val Green = Color(0xFF21D991)
private val Orange = Color(0xFFFF981A)
private val Red = Color(0xFFFF4D45)

@Composable
fun MaintenanceScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember(context.applicationContext) {
        KnxHomeStateRepository(context.applicationContext)
    }
    val snapshot by repository.stateFlow.collectAsStateWithLifecycle(initialValue = repository.snapshot())

    val kitchenFlood = snapshot.booleanAt(KnxAddressBook.Indoor.FLOOD_KITCHEN)
    val bathroomFlood = snapshot.booleanAt(KnxAddressBook.Indoor.FLOOD_BATHROOM)
    val floodDetected: Boolean? = when {
        kitchenFlood == true || bathroomFlood == true -> true
        kitchenFlood == false && bathroomFlood == false -> false
        else -> null
    }
    val fireDetected = snapshot.booleanAt(KnxAddressBook.Indoor.FIRE_HALLWAY)

    var valveOpen by remember { mutableStateOf(true) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .background(Brush.verticalGradient(listOf(FondoSuperior, FondoInferior, Color.Black)))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(14.dp))
        RoomHeader("Mantenimiento", onBack)
        Spacer(Modifier.height(16.dp))
        ActionCard(Icons.Rounded.Lightbulb, "Apagado luces", "Apagar todas las luces de la vivienda", Blue)
        Spacer(Modifier.height(12.dp))
        ActionCard(Icons.Rounded.Home, "Apagado general", "Apagar todos los sistemas de la vivienda", Purple)
        Spacer(Modifier.height(12.dp))
        ActionCard(Icons.Rounded.AcUnit, "Apagado clima", "Apagar el sistema de climatización", Green)
        Spacer(Modifier.height(12.dp))
        ActionCard(Icons.Rounded.Blinds, "Cerrar todas las persianas", "Cerrar todas las persianas de la vivienda", Orange)
        Spacer(Modifier.height(12.dp))
        SensorCard(Icons.Rounded.WaterDrop, "Sensor inundación", floodDetected, Color(0xFF28DDE3))
        Spacer(Modifier.height(12.dp))
        SensorCard(Icons.Rounded.LocalFireDepartment, "Sensor incendio", fireDetected, Red)
        Spacer(Modifier.height(12.dp))
        ValveCard(open = valveOpen, onChange = { valveOpen = it })
        Spacer(Modifier.height(64.dp))
    }
}

@Composable
private fun ActionCard(icon: ImageVector, title: String, subtitle: String, accent: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().background(MaintenanceCard, RoundedCornerShape(22.dp)).border(1.dp, BordeTarjeta, RoundedCornerShape(22.dp)).padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBox(icon, accent)
        Spacer(Modifier.size(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextoPrincipal, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = TextoSecundario, fontSize = 12.sp)
        }
        Box(
            modifier = Modifier.border(1.dp, accent, RoundedCornerShape(13.dp)).clickable { }.padding(horizontal = 14.dp, vertical = 10.dp)
        ) { Text("⏻  Ejecutar", color = accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun SensorCard(icon: ImageVector, title: String, detected: Boolean?, accent: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().background(MaintenanceCard, RoundedCornerShape(22.dp)).border(1.dp, BordeTarjeta, RoundedCornerShape(22.dp)).padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBox(icon, accent)
        Spacer(Modifier.size(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextoPrincipal, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text("Estado actual", color = TextoSecundario, fontSize = 12.sp)
        }
        Text(
            when (detected) {
                true -> "⚠ Detección"
                false -> "✓ Sin detecciones"
                null -> "---"
            },
            color = when (detected) {
                true -> Red
                false -> Green
                null -> TextoSecundario
            },
            fontSize = 13.sp
        )
    }
}

@Composable
private fun ValveCard(open: Boolean, onChange: (Boolean) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().background(MaintenanceCard, RoundedCornerShape(22.dp)).border(1.dp, BordeTarjeta, RoundedCornerShape(22.dp)).padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBox(Icons.Rounded.Settings, Blue)
            Spacer(Modifier.size(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Electroválvula de agua", color = TextoPrincipal, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text("Control de la válvula principal", color = TextoSecundario, fontSize = 12.sp)
            }
            Row(modifier = Modifier.border(1.dp, BordeTarjeta, RoundedCornerShape(13.dp))) {
                ValveOption("ON", open) { onChange(true) }
                ValveOption("OFF", !open) { onChange(false) }
            }
        }
        Spacer(Modifier.height(14.dp))
        Box(
            modifier = Modifier.fillMaxWidth().background(Blue.copy(alpha = 0.05f), RoundedCornerShape(14.dp)).border(1.dp, BordeTarjeta, RoundedCornerShape(14.dp)).padding(14.dp)
        ) {
            Text(
                if (open) "La electroválvula está abierta.\nEl suministro de agua está habilitado."
                else "La electroválvula está cerrada.\nEl suministro de agua está interrumpido.",
                color = TextoSecundario,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun ValveOption(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.background(if (selected) Blue.copy(alpha = 0.25f) else Color.Transparent, RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 11.dp)
    ) { Text(text, color = if (selected) TextoPrincipal else TextoSecundario, fontSize = 13.sp) }
}

@Composable
private fun IconBox(icon: ImageVector, accent: Color) {
    Box(
        modifier = Modifier.size(58.dp).background(accent.copy(alpha = 0.11f), RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(30.dp)
        )
    }
}
