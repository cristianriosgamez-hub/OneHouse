package com.onehouse.app.knx

import android.content.Context
import com.onehouse.app.data.knx.SettingsDataStore
import com.onehouse.app.device.ControlKind
import com.onehouse.app.device.ImportedKnxDevice
import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Lee un conjunto de direcciones usando un único túnel KNX/IP.
 *
 * Abrir/cerrar un túnel por dirección provocaba pérdidas, lentitud y rechazo de
 * conexiones en algunos interfaces KNX/IP. Esta implementación abre una sesión,
 * consulta todas las direcciones secuencialmente y cierra al terminar.
 */
class KnxBulkStateReader(context: Context) : Closeable {
    data class Progress(
        val completed: Int,
        val total: Int,
        val failures: Int,
        val currentAddress: String?
    )

    private val appContext = context.applicationContext
    private val cancelled = AtomicBoolean(false)
    private val centralResources = KnxCentralEngine.get(appContext)
    private val stateRepository = centralResources.stateRepository
    private val deviceStateRepository = KnxDeviceStateRepository(appContext)
    // v1.12.1.2: la carga masiva comparte exactamente el mismo gestor de túnel
    // que comandos, navegación y resto del motor. Abrir un manager propio aquí
    // era una de las principales fuentes de conexiones simultáneas/código 36.
    private val connectionManager = centralResources.connectionManager

    fun read(
        devices: List<ImportedKnxDevice>,
        extraReadAddresses: List<String> = emptyList(),
        onProgress: (Progress) -> Unit,
        onComplete: (Progress) -> Unit
    ) {
        cancel()
        cancelled.set(false)

        val importedAddresses = devices.flatMap { device ->
            buildList {
                addAll(device.readAddresses.map { it.toString() })
                // Algunos proyectos InsideControl no exportan una GA de estado
                // separada, o el actuador responde también en la GA de mando.
                // La lectura de la GA de mando es segura y permite recuperar el
                // estado real de todas las luces, no solo de unas pocas.
                if (device.controlKind == ControlKind.BOOLEAN_SWITCH ||
                    device.controlKind == ControlKind.CLIMATE
                ) {
                    addAll(device.writeAddresses.map { it.toString() })
                }
            }
        }

        val addresses = (extraReadAddresses + importedAddresses)
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()

        if (addresses.isEmpty()) {
            val result = Progress(0, 0, 0, null)
            onProgress(result)
            onComplete(result)
            return
        }

        val endpoint = resolveEndpoint().getOrElse {
            val result = Progress(0, addresses.size, addresses.size, null)
            onProgress(result)
            onComplete(result)
            return
        }

        // Timeout corto para la carga masiva. Una GA que no responde no debe
        // bloquear durante cuatro segundos al resto de sensores.
        val manager = connectionManager
        manager.connect(endpoint) { connectResult ->
            if (cancelled.get()) return@connect
            if (connectResult !is KnxConnectionManager.ConnectResult.Success) {
                // No cerramos el gestor central: puede estar siendo reutilizado por
                // otra pantalla. Si no hay sesión activa, esta llamada es inocua.
                manager.scheduleDisconnect()
                val result = Progress(0, addresses.size, addresses.size, null)
                onProgress(result)
                onComplete(result)
                return@connect
            }

            fun readAt(index: Int, failures: Int) {
                if (cancelled.get()) return
                if (index >= addresses.size) {
                    // Mantener la sesión unos segundos permite que al volver a Inicio
                    // o entrar en otra estancia se reutilice el mismo túnel.
                    manager.scheduleDisconnect()
                    val result = Progress(addresses.size, addresses.size, failures, null)
                    onProgress(result)
                    onComplete(result)
                    return
                }

                val address = addresses[index]
                onProgress(Progress(index, addresses.size, failures, address))
                val groupAddress = runCatching { KnxGroupAddress.parse(address) }.getOrNull()
                if (groupAddress == null) {
                    readAt(index + 1, failures + 1)
                    return
                }

                manager.sendTelegram(KnxTelegram.GroupValueRead(groupAddress)) { result ->
                    if (cancelled.get()) return@sendTelegram
                    val success = result is KnxConnectionManager.OperationResult.Success &&
                        result.incoming != null
                    if (success) {
                        val incoming = (result as KnxConnectionManager.OperationResult.Success).incoming!!
                        devices.filter { device ->
                            incoming.destination in device.readAddresses ||
                                incoming.destination in device.writeAddresses
                        }.forEach { device ->
                            val busValue = incoming.booleanValue?.let {
                                if (it) "Encendido" else "Apagado"
                            } ?: incoming.payload.joinToString("") { byte ->
                                "%02X".format(byte.toInt() and 0xFF)
                            }
                            deviceStateRepository.updateFromBus(device.id, busValue)
                        }
                    }
                    val nextFailures = failures + if (success) 0 else 1
                    onProgress(Progress(index + 1, addresses.size, nextFailures, address))
                    readAt(index + 1, nextFailures)
                }
            }

            readAt(0, 0)
        }
    }

    private fun resolveEndpoint(): Result<KnxEndpoint> = runCatching {
        val settings = SettingsDataStore(appContext).read()
        val detector = NetworkConnectionDetector(appContext)
        val network = try {
            detector.currentState()
        } finally {
            detector.close()
        }
        require(network.isConnected) { "El dispositivo no tiene conexión de red" }
        val local = network.usesLocalRoute
        val host = (if (local) settings.localIp else settings.remoteIp).trim()
        val port = (if (local) settings.localPort else settings.remotePort).trim().toIntOrNull()
        require(host.isNotBlank()) { "Dirección KNX/IP no configurada" }
        require(port != null && port in 1..65535) { "Puerto KNX/IP no válido" }
        KnxEndpoint(host, port)
    }

    fun cancel() {
        cancelled.set(true)
        // Antes, salir/cambiar de pantalla cerraba inmediatamente el túnel de la
        // carga masiva y la pantalla siguiente abría otro. Eso favorecía el código 36.
        // Ahora solo dejamos un cierre diferido del gestor compartido. Una nueva
        // operación cancelará automáticamente ese cierre y reutilizará la sesión.
        connectionManager.scheduleDisconnect()
    }

    override fun close() = cancel()
}
