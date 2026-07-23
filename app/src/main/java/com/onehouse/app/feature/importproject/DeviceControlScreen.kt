package com.onehouse.app.feature.importproject

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onehouse.app.design.AzulClaro
import com.onehouse.app.design.AzulOneHouse
import com.onehouse.app.design.FondoInferior
import com.onehouse.app.design.FondoSuperior
import com.onehouse.app.design.FondoTarjeta
import com.onehouse.app.design.TextoPrincipal
import com.onehouse.app.design.TextoSecundario
import com.onehouse.app.device.ImportedKnxDevice
import com.onehouse.app.knx.KnxCommand
import com.onehouse.app.knx.KnxCommandExecutor
import com.onehouse.app.knx.KnxCommandType

@Composable
fun DeviceControlScreen(device: ImportedKnxDevice, onBack: () -> Unit) {
    val context = LocalContext.current
    val executor = remember(context.applicationContext) { KnxCommandExecutor(context.applicationContext) }
    var isBusy by remember { mutableStateOf(false) }
    var assumedOn by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Preparado para enviar al bus KNX") }

    DisposableEffect(executor) {
        onDispose { executor.close() }
    }

    fun run(command: KnxCommand, toggleValue: Boolean? = null) {
        if (isBusy) return
        isBusy = true
        status = "Conectando y enviando ${command.type.displayName.lowercase()}…"
        executor.execute(command, toggleValue) { result ->
            isBusy = false
            when (result) {
                KnxCommandExecutor.Result.Success -> {
                    when (command.type) {
                        KnxCommandType.ON -> assumedOn = true
                        KnxCommandType.OFF -> assumedOn = false
                        KnxCommandType.TOGGLE -> assumedOn = toggleValue ?: assumedOn
                        else -> Unit
                    }
                    status = when (command.type) {
                        KnxCommandType.READ -> "Solicitud de lectura confirmada por KNX/IP"
                        else -> "Telegrama ${command.type.displayName.lowercase()} confirmado"
                    }
                }
                is KnxCommandExecutor.Result.Failure -> status = result.message
            }
        }
    }

    val onCommand = device.commands.firstOrNull { it.type == KnxCommandType.ON }
    val offCommand = device.commands.firstOrNull { it.type == KnxCommandType.OFF }
    val toggleCommand = device.commands.firstOrNull { it.type == KnxCommandType.TOGGLE }
    val readCommand = device.commands.firstOrNull { it.type == KnxCommandType.READ }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(FondoSuperior, FondoInferior)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "‹",
                    color = AzulClaro,
                    fontSize = 40.sp,
                    modifier = Modifier.clickable(onClick = onBack).padding(end = 12.dp)
                )
                Column {
                    Text(device.name, color = TextoPrincipal, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(device.roomName, color = TextoSecundario, fontSize = 13.sp)
                }
            }

            Surface(color = FondoTarjeta, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Control KNX", color = AzulClaro, fontWeight = FontWeight.Bold)
                    ControlDetail("Tipo", device.controlKind.displayName)
                    ControlDetail("Dirección de escritura", device.primaryWriteAddress?.toString() ?: "—")
                    ControlDetail("Dirección de lectura", device.primaryReadAddress?.toString() ?: "—")
                    ControlDetail("DPT resuelto", device.resolvedDpt)
                }
            }

            if (onCommand != null && offCommand != null) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { run(onCommand) },
                        enabled = !isBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = AzulOneHouse),
                        modifier = Modifier.weight(1f)
                    ) { Text("Encender") }
                    OutlinedButton(onClick = { run(offCommand) }, enabled = !isBusy, modifier = Modifier.weight(1f)) {
                        Text("Apagar")
                    }
                }
            } else if (toggleCommand != null) {
                Button(
                    onClick = { run(toggleCommand, !assumedOn) },
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AzulOneHouse)
                ) { Text(if (assumedOn) "Apagar" else "Encender") }
            }

            readCommand?.let { command ->
                OutlinedButton(onClick = { run(command) }, enabled = !isBusy, modifier = Modifier.fillMaxWidth()) {
                    Text("Solicitar lectura de estado")
                }
            }

            Surface(color = FondoTarjeta, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (isBusy) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 12.dp), color = AzulClaro)
                    }
                    Text(status, color = if (isBusy) AzulClaro else TextoSecundario, style = MaterialTheme.typography.bodyMedium)
                }
            }

            if (onCommand == null && offCommand == null && toggleCommand == null) {
                Text(
                    "En esta entrega el envío real está habilitado para luces e interruptores DPT 1.x. El resto de controles se incorporará con su codificación DPT específica.",
                    color = TextoSecundario,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ControlDetail(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, color = TextoSecundario, modifier = Modifier.weight(1f), fontSize = 12.sp)
        Text(value, color = TextoPrincipal, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    }
}
