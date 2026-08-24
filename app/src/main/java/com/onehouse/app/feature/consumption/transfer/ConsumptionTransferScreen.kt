package com.onehouse.app.feature.consumption.transfer

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onehouse.app.design.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ConsumptionTransferScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val manager = remember { ConsumptionExcelManager(context.applicationContext) }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            busy = true
            message = runCatching { manager.export(uri) }.fold({ "$it lecturas exportadas correctamente." }, { "Error al exportar: ${it.message}" })
            busy = false
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            busy = true
            message = runCatching { manager.import(uri) }.fold(
                { "Importación terminada: ${it.newReadings} nuevas, ${it.duplicates} ya existentes, ${it.invalid} incorrectas." },
                { "Error al importar: ${it.message}" }
            )
            busy = false
        }
    }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(FondoSuperior, FondoInferior)))) {
        Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            TextButton(onClick = onBack) { Text("‹ Volver", color = AzulClaro) }
            Text("Importar / Exportar consumos", color = TextoPrincipal, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Gestiona en un único Excel el histórico de ENDESA, AGBAR, Climatización y ACS.", color = TextoSecundario)

            OneHouseCard {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Exportar a Excel", color = TextoPrincipal, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Text("Crea un .xlsx con cuatro hojas y todo el histórico disponible, incluidas las lecturas desde 2014.", color = TextoSecundario)
                    Button(enabled = !busy, onClick = {
                        val stamp = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        exportLauncher.launch("OneHouse_Consumos_$stamp.xlsx")
                    }, modifier = Modifier.fillMaxWidth()) { Text("Exportar consumos") }
                }
            }
            OneHouseCard {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Importar desde Excel", color = TextoPrincipal, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Text("Importa un Excel exportado por OneHouse. Las lecturas con la misma fecha y contador no se duplican ni sobrescriben.", color = TextoSecundario)
                    OutlinedButton(enabled = !busy, onClick = { importLauncher.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) }, modifier = Modifier.fillMaxWidth()) { Text("Seleccionar Excel") }
                }
            }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            message?.let { Surface(color = FondoTarjeta, shape = RoundedCornerShape(16.dp)) { Text(it, Modifier.padding(16.dp), color = TextoPrincipal) } }
            Text("Formato: Fecha · Lectura · Consumo · Coste · Unidad · Origen · Nota", color = TextoSecundario, fontSize = 12.sp)
        }
    }
}
