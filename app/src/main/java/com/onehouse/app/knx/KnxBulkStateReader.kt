package com.onehouse.app.knx

import android.content.Context
import com.onehouse.app.data.knx.SettingsDataStore
import com.onehouse.app.device.ControlKind
import com.onehouse.app.device.ImportedKnxDevice
import android.os.Handler
import android.os.Looper
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
    companion object {
        // Una respuesta KNX/IP local normalmente llega en decenas de ms. Durante
        // la carga inicial no esperamos 4 s por cada GA silenciosa: avanzamos
        // rápido y dejamos que las siguientes rondas recuperen lo pendiente.
        private const val FAST_READ_TIMEOUT_MILLIS = 350
        private const val SELECTIVE_RETRY_TIMEOUT_MILLIS = 900
        private const val FINAL_RETRY_TIMEOUT_MILLIS = 1_500
        private const val SELECTIVE_RETRY_DELAY_MILLIS = 1_500L
        private const val FINAL_RETRY_DELAY_MILLIS = 5_000L
        private const val MAX_ROUNDS = 3
    }
    data class Progress(
        val completed: Int,
        val total: Int,
        val failures: Int,
        val currentAddress: String?
    )

    private val appContext = context.applicationContext
    private val cancelled = AtomicBoolean(false)
    private val handler = Handler(Looper.getMainLooper())
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
        manager.connect(endpoint, source = "BULK_STATE_READER") { connectResult ->
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

            fun timeoutForRound(round: Int): Int = when (round) {
                0 -> FAST_READ_TIMEOUT_MILLIS
                1 -> SELECTIVE_RETRY_TIMEOUT_MILLIS
                else -> FINAL_RETRY_TIMEOUT_MILLIS
            }

            fun delayForRound(round: Int): Long = when (round) {
                1 -> SELECTIVE_RETRY_DELAY_MILLIS
                else -> FINAL_RETRY_DELAY_MILLIS
            }

            fun finish(finalPending: List<String>) {
                manager.scheduleDisconnect()
                val result = Progress(
                    completed = addresses.size,
                    total = addresses.size,
                    failures = finalPending.size,
                    currentAddress = null
                )
                onProgress(result)
                onComplete(result)
            }

            fun runRound(round: Int, roundAddresses: List<String>) {
                if (cancelled.get()) return

                // Si una GA que falló en la pasada anterior ha llegado de forma
                // espontánea por el receptor pasivo, ya no hace falta volver a leerla.
                val pendingAtStart = if (round == 0) {
                    // La ronda principal siempre refresca todas las GAs. Esto mantiene
                    // vivo el refresco periódico de 60 s aunque ya exista caché.
                    roundAddresses
                } else {
                    roundAddresses.filter { address ->
                        stateRepository.get(address)?.let(StateFreshness::isTrusted) != true
                    }
                }

                if (pendingAtStart.isEmpty()) {
                    finish(emptyList())
                    return
                }

                val failedThisRound = mutableListOf<String>()

                fun readAt(index: Int) {
                    if (cancelled.get()) return
                    if (index >= pendingAtStart.size) {
                        val stillPending = failedThisRound.filter { address ->
                            stateRepository.get(address)?.let(StateFreshness::isTrusted) != true
                        }

                        if (stillPending.isEmpty() || round + 1 >= MAX_ROUNDS) {
                            finish(stillPending)
                            return
                        }

                        // Recuperación selectiva: solo se reintentan las GAs que no
                        // contestaron. La UI ya dispone del resto de estados y sigue
                        // siendo utilizable mientras esta recuperación ocurre.
                        handler.postDelayed(
                            {
                                if (!cancelled.get()) {
                                    runRound(round + 1, stillPending)
                                }
                            },
                            delayForRound(round + 1)
                        )
                        return
                    }

                    val address = pendingAtStart[index]
                    val completedBefore = addresses.size - pendingAtStart.size + index
                    onProgress(
                        Progress(
                            completed = completedBefore.coerceIn(0, addresses.size),
                            total = addresses.size,
                            failures = failedThisRound.size,
                            currentAddress = address
                        )
                    )

                    val groupAddress = runCatching { KnxGroupAddress.parse(address) }.getOrNull()
                    if (groupAddress == null) {
                        failedThisRound += address
                        readAt(index + 1)
                        return
                    }

                    manager.sendTelegram(
                        telegram = KnxTelegram.GroupValueRead(groupAddress),
                        timeoutOverrideMillis = timeoutForRound(round)
                    ) { result ->
                        if (cancelled.get()) return@sendTelegram
                        val success = result is KnxConnectionManager.OperationResult.Success &&
                            result.incoming != null
                        if (success) {
                            val incoming =
                                (result as KnxConnectionManager.OperationResult.Success).incoming!!
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
                        } else if (stateRepository.get(address)?.let(StateFreshness::isTrusted) != true) {
                            failedThisRound += address
                        }

                        readAt(index + 1)
                    }
                }

                readAt(0)
            }

            runRound(0, addresses)
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
