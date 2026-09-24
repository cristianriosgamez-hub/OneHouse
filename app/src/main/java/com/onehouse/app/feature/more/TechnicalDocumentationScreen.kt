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

        DocSection("Estado del documento", "Base técnica incorporada en v1.14.1 REV4, ampliada en REV5 con trazabilidad de la negociación KNX/IP, en REV6 con separación entre sesión visible e histórico y captura hexadecimal, en REV7 con HPAI Control/Data explícitos y traza de la última negociación, en REV8 con fallback remoto controlado NAT estándar → HPAI local únicamente tras timeout sin datagramas, en REV9 con instrumentación no invasiva del transporte UDP por fases (SOCKET_OK, REQUEST_PREPARADO, SEND_OK, RX_DATAGRAMA, RX_TIMEOUT e ICMP Port Unreachable), y en REV10 con una sonda remota final mediante socket UDP no conectado que acepta datagramas desde cualquier origen para detectar respuestas filtradas por IP/puerto de origen. Separa hechos verificados, configuración actual y elementos pendientes de validar para evitar convertir una prueba en una decisión definitiva.")
        DocSection("Arquitectura", "OneHouse es una aplicación Android con interfaz Jetpack Compose. El motor KNX centraliza conexión, lectura inicial, estados en tiempo real y envío de comandos. La UI consume repositorios de estado y no debe inventar valores KNX cuando no existe un dato real recibido.")
        DocSection("KNX/IP", "Transporte actual: KNXnet/IP Tunnelling sobre UDP. Puerto configurado: 3671. La aplicación negocia un canal de túnel antes de leer o escribir direcciones de grupo. La conexión lógica y la recepción real de telegramas se tratan como estados distintos.")
        DocSection("Ruta local", "Cuando el teléfono está en la red doméstica, OneHouse selecciona la IP KNX local configurada. En la configuración observada durante las pruebas se ha usado 192.168.0.16:3671. Esta dirección es configuración de instalación y puede cambiar; no forma parte fija del código.")
        DocSection("Ruta remota", "Cuando se usan datos móviles, OneHouse selecciona la IP pública configurada y UDP 3671. En REV3 se añadió KNXnet/IP NAT mode: el CONNECT_REQUEST y DISCONNECT_REQUEST anuncian HPAI 0.0.0.0:0. La prueba del 24/09/2026 selecciona correctamente la ruta remota, pero no recibe respuesta KNX/IP. Por tanto, la conexión remota continúa PENDIENTE DE RESOLVER.")
        DocSection("Red doméstica", "La instalación usa un router como salida a Internet y el equipo KNX está alcanzable desde la LAN a través de la infraestructura de red/extensión existente. Para la prueba remota se configuró una redirección UDP 3671 hacia la IP KNX. No se considera validada una solución remota hasta obtener CONNECT_RESPONSE y tráfico KNX real.")
        DocSection("Carga inicial", "La carga inicial solicita los estados KNX conocidos y contabiliza únicamente respuestas reales. Un 100% no debe alcanzarse por timeout ni por valores de respaldo. Tras la carga, el motor mantiene reconciliación en tiempo real de telegramas recibidos.")
        DocSection("Climatización", "Los estados de climatización deben proceder de KNX cuando existe integración: encendido, consigna, modo, ventilador y temperaturas. No se deben presentar valores dummy como si fueran estados reales. La ejecución autónoma de clima se mantiene separada de la prueba de conexión remota.")
        DocSection("Consumos — diseño previsto", "Endesa, Agbar y Climatización: al añadir lectura se introducirá el consumo periódico y OneHouse calculará internamente la lectura acumulada. ACS: KNX proporciona una lectura acumulada DPT9 en kW; la pantalla mostrará la lectura KNX actual y calculará el consumo del periodo respecto a la lectura anterior. Este cambio está DOCUMENTADO, pero todavía no se marca como implementado en REV4.")
        DocSection("Backup y configuración", "La configuración KNX y preferencias admitidas por la aplicación se gestionan mediante repositorios de configuración y BackupManager. Los cambios de estructura deben conservar compatibilidad con backups válidos y filtrar claves no soportadas.")
        DocSection("Diagnóstico de red", "Timeout / 'Sin respuesta KNX/IP': se envió la negociación y no llegó una respuesta válida dentro del tiempo esperado. REV5 registra en Diagnóstico KNX la preparación y envío de CONNECT_REQUEST, endpoint UDP local, NAT ON/OFF, espera de CONNECT_RESPONSE, cualquier datagrama UDP recibido y el error/timeout final. REV6 añade el HEX exacto del CONNECT_REQUEST y de cualquier datagrama recibido. REV7 muestra explícitamente HPAI Control y HPAI Data, servicio KNXnet/IP de cualquier respuesta, número de datagramas recibidos en timeout y una tarjeta con la última negociación. REV8 mantiene NAT estándar como primer intento remoto y, solo si recibe 0 datagramas, prueba una segunda variante de compatibilidad anunciando el endpoint UDP local; LAN no cambia. REV9 no modifica esa negociación: registra por separado creación/conexión del socket, preparación, envío y recepción/timeout, incluyendo endpoint local/remoto y detección explícita de ICMP Port Unreachable. REV10, solo después de fallar ambos intentos remotos y sin tocar LAN, ejecuta una sonda final con DatagramSocket no conectado: si llega UDP aquí, el socket conectado estaba filtrando una respuesta con origen distinto; si tampoco llega nada, el problema queda situado antes de la recepción Android (red móvil, NAT/port-forwarding, router o gateway). Si la sonda recibe un CONNECT_RESPONSE aceptado, envía DISCONNECT_REQUEST inmediatamente para no dejar un túnel huérfano. Los contadores diferenciales se etiquetan como desde abrir Diagnóstico para no confundirlos con el intento que pudo ocurrir antes de entrar en la pantalla. ICMP Port Unreachable: la red devolvió que el puerto de destino no era alcanzable. EBADF: el descriptor del socket dejó de ser válido; REV3 añadió protección para evitar que un cierre tardío cierre el socket/túnel compartido.")
        DocSection("Pruebas pendientes", "1) Resolver por qué la ruta remota no obtiene CONNECT_RESPONSE. 2) Validar de nuevo la ruta local cuando haya acceso a la vivienda. 3) Confirmar transición Wi‑Fi → datos → Wi‑Fi sin sockets residuales. 4) Solo después, validar lectura y mando KNX remoto. No se harán cambios de protocolo a ciegas sin nueva evidencia.")
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
