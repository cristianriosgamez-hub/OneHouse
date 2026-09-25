package com.onehouse.app.feature.more

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.onehouse.app.design.AzulClaro
import com.onehouse.app.design.FondoInferior
import com.onehouse.app.design.FondoSuperior
import com.onehouse.app.design.OneHouseCard
import com.onehouse.app.design.TextoPrincipal
import com.onehouse.app.design.TextoSecundario

@Composable
fun TechnicalDocumentationScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(FondoSuperior, FondoInferior)))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Volver", tint = TextoPrincipal)
            }
            Column {
                Text("Documentación técnica", color = TextoPrincipal, style = MaterialTheme.typography.headlineSmall)
                Text("Arquitectura y decisiones de OneHouse", color = TextoSecundario)
            }
        }

        DocSection("Estado del documento", "REV16 es la base estable validada. REV17 corrige únicamente la presentación del Diagnóstico KNX. REV16 realizó una limpieza conservadora de la instrumentación temporal usada durante la investigación remota: se retira la sonda UDP experimental y las trazas HEX/de laboratorio, manteniendo el transporte KNX/IP, NAT mode, fallback de compatibilidad, métricas útiles y diagnóstico de errores. La documentación conserva el historial técnico sin ejecutar sondas de investigación en producción.")
        DocSection("Arquitectura", "OneHouse es una aplicación Android con interfaz Jetpack Compose. El motor KNX centraliza conexión, lectura inicial, estados en tiempo real y envío de comandos. La UI consume repositorios de estado y no debe inventar valores KNX cuando no existe un dato real recibido.")
        DocSection("KNX/IP", "Transporte actual: KNXnet/IP Tunnelling sobre UDP. Puerto configurado: 3671. La aplicación negocia un canal de túnel antes de leer o escribir direcciones de grupo. La conexión lógica y la recepción real de telegramas se tratan como estados distintos.")
        DocSection("Ruta local", "Cuando el teléfono está en la red doméstica, OneHouse selecciona la IP KNX local configurada. La instalación validada usa actualmente 192.168.0.12:3671 para el KNX/IP, con reserva DHCP. Esta dirección sigue siendo configuración de instalación y no forma parte fija del código.")
        DocSection("Ruta remota", "Cuando se usan datos móviles, OneHouse selecciona la IP pública configurada y UDP 3671. En REV3 se añadió KNXnet/IP NAT mode: el CONNECT_REQUEST y DISCONNECT_REQUEST anuncian HPAI 0.0.0.0:0. La conexión remota quedó VALIDADA el 24/09/2026 por datos móviles contra la IP pública configurada, después de corregir la redirección UDP 3671 hacia 192.168.0.12.")
        DocSection("Red doméstica", "Topología validada: Siemens KNX/IP por Ethernet a TP-Link en modo repetidor; TP-Link enlaza por Wi-Fi con el router Lowi. El KNX/IP queda reservado en 192.168.0.12 con MAC 6A:FF:7A:14:47:9E. Para remoto, el router redirige UDP 3671 a 192.168.0.12:3671. Wi-Fi local y datos móviles están validados.")
        DocSection("Carga inicial", "La carga inicial solicita los estados KNX conocidos y contabiliza únicamente respuestas reales. Un 100% no debe alcanzarse por timeout ni por valores de respaldo. Tras la carga, el motor mantiene reconciliación en tiempo real de telegramas recibidos.")
        DocSection("Climatización", "Los estados de climatización deben proceder de KNX cuando existe integración: encendido, consigna, modo, ventilador y temperaturas. No se deben presentar valores dummy como si fueran estados reales. La ejecución autónoma de clima se mantiene separada de la prueba de conexión remota.")
        DocSection("Consumos — diseño previsto", "Endesa, Agbar y Climatización: al añadir lectura se introducirá el consumo periódico y OneHouse calculará internamente la lectura acumulada. ACS: KNX proporciona una lectura acumulada DPT9 en kW; la pantalla mostrará la lectura KNX actual y calculará el consumo del periodo respecto a la lectura anterior. REV14 IMPLEMENTA este diseño. Endesa, Agbar y Climatización solicitan solo consumo del periodo y calculan el acumulado internamente. ACS usa la lectura acumulada KNX 15/5/67 (DPT9, kW), la muestra con fecha actual y calcula el consumo respecto a la lectura acumulada anterior. 15/5/67 se incorpora además a la carga inicial KNX.")
        DocSection("Backup y configuración", "La configuración KNX y preferencias admitidas por la aplicación se gestionan mediante repositorios de configuración y BackupManager. Los cambios de estructura deben conservar compatibilidad con backups válidos y filtrar claves no soportadas.")
        DocSection("Diagnóstico de red", "Diagnóstico permanente: timeout al abrir túnel, rechazo KNX/IP, ICMP Port Unreachable, errores de socket y origen de la apertura (CONNECTION_TEST / BULK_STATE_READER). Las sondas UDP, volcados HEX y trazas detalladas REV7-REV12 fueron instrumentación temporal de investigación y se retiran del camino productivo en REV16. Se mantienen las métricas y estadísticas útiles para mantenimiento. La protección de ciclo de vida del socket introducida tras el EBADF se conserva.")
        DocSection("Estado validado y siguientes pruebas", "REV16 es la referencia estable. REV17 distingue mando/escritura, estado/lectura y Lluvia por evento (GA configurada y 15/0/21). Sin valor, mando y evento muestran avisos neutros; los estados conservan SIN RESPUESTA. VALOR RECIBIDO no implica respuesta a GroupValueRead: puede ser un telegrama espontáneo. No se eximen otros sensores sin confirmación. Motor, red, túnel, carga inicial y contadores permanecen intactos. Validar diagnóstico, recepción del evento Lluvia y regresión Wi-Fi/5G antes de declarar REV17 estable.")
    }
}

@Composable
private fun DocSection(title: String, body: String) {
    OneHouseCard {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, color = AzulClaro, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
            Text(body, color = TextoSecundario, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
