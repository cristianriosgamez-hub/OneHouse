package com.onehouse.app.knx

import android.content.Context
import com.onehouse.app.data.knx.SettingsDataStore
import java.io.Closeable

/**
 * Ejecuta comandos normalizados sobre un túnel KNXnet/IP.
 *
 * En la Entrega 6 se habilitan lectura de grupo y escritura DPT 1.x
 * (encendido/apagado). Los DPT que requieren codificación adicional se
 * rechazan de forma explícita para evitar telegramas incorrectos.
 */
class KnxCommandExecutor(context: Context) : Closeable {
    sealed interface Result {
        data class Success(
            val busValue: String? = null,
            val sourceAddress: String? = null,
            val diagnostic: KnxConnectionManager.TelegramDiagnostic? = null
        ) : Result
        data class Failure(val message: String) : Result
    }

    private val appContext = context.applicationContext
    private val connectionManager = KnxConnectionManager()
    private val monitorRepository = KnxTelegramMonitorRepository(appContext)
    private var queuedOperation: Closeable? = null

    fun execute(
        command: KnxCommand,
        toggleValue: Boolean? = null,
        onResult: (Result) -> Unit
    ) {
        queuedOperation?.close()
        queuedOperation = KnxTelegramQueue.enqueue { done ->
            executeQueued(command, toggleValue) { result ->
                onResult(result)
                done()
            }
        }
    }

    private fun executeQueued(
        command: KnxCommand,
        toggleValue: Boolean?,
        onResult: (Result) -> Unit
    ) {
        val telegram = telegramFor(command, toggleValue)
        if (telegram == null) {
            onResult(Result.Failure("El comando ${command.type.displayName.lowercase()} todavía no admite este DPT"))
            return
        }

        val endpointResult = resolveEndpoint()
        val endpoint = endpointResult.getOrElse {
            onResult(Result.Failure(it.message ?: "No se pudo determinar la conexión KNX/IP"))
            return
        }

        val eventKind = if (command.type == KnxCommandType.READ) {
            KnxTelegramEvent.Kind.GROUP_VALUE_READ
        } else {
            KnxTelegramEvent.Kind.GROUP_VALUE_WRITE
        }
        val eventValue = when (command.type) {
            KnxCommandType.ON -> "1"
            KnxCommandType.OFF -> "0"
            KnxCommandType.TOGGLE -> if (toggleValue == true) "1" else "0"
            else -> null
        }

        fun attempt(number: Int) {
            monitorRepository.record(
                direction = KnxTelegramEvent.Direction.SYSTEM,
                kind = KnxTelegramEvent.Kind.CONNECT,
                groupAddress = command.destination.toString(),
                status = KnxTelegramEvent.Status.PENDING,
                detail = if (number == 1) {
                    "Conectando con ${endpoint.host}:${endpoint.port}"
                } else {
                    "Reintento de lectura ${number - 1}/1"
                }
            )

            connectionManager.connect(endpoint) { connectResult ->
                when (connectResult) {
                    is KnxConnectionManager.ConnectResult.Success -> {
                        monitorRepository.record(
                            direction = KnxTelegramEvent.Direction.SYSTEM,
                            kind = KnxTelegramEvent.Kind.CONNECT,
                            groupAddress = command.destination.toString(),
                            status = KnxTelegramEvent.Status.CONFIRMED,
                            detail = "Túnel KNX/IP conectado (canal ${connectResult.channelId})"
                        )
                        monitorRepository.record(
                            direction = KnxTelegramEvent.Direction.OUTGOING,
                            kind = eventKind,
                            groupAddress = command.destination.toString(),
                            value = eventValue,
                            status = KnxTelegramEvent.Status.PENDING
                        )
                        connectionManager.sendTelegram(telegram) { operation ->
                            connectionManager.disconnect()
                            if (command.type == KnxCommandType.READ &&
                                operation is KnxConnectionManager.OperationResult.Success &&
                                operation.incoming == null &&
                                number == 1
                            ) {
                                attempt(2)
                                return@sendTelegram
                            }

                            val result = operation.toExecutorResult()
                            monitorRepository.record(
                                direction = KnxTelegramEvent.Direction.OUTGOING,
                                kind = eventKind,
                                groupAddress = command.destination.toString(),
                                value = eventValue,
                                status = if (result is Result.Success) KnxTelegramEvent.Status.CONFIRMED else KnxTelegramEvent.Status.ERROR,
                                detail = when (result) {
                                    is Result.Success -> result.diagnostic?.let { diagnostic ->
                                        "ACK gateway · canal ${diagnostic.channelId} · secuencia ${diagnostic.sequence}\n" +
                                            "cEMI: ${diagnostic.cemiHex}\n" +
                                            "KNXnet/IP: ${diagnostic.knxNetIpHex}"
                                    }
                                    is Result.Failure -> result.message
                                }
                            )
                            onResult(result)
                        }
                    }
                    KnxConnectionManager.ConnectResult.Timeout -> onResult(Result.Failure("Tiempo de espera agotado al conectar con KNX/IP"))
                    is KnxConnectionManager.ConnectResult.Rejected -> onResult(Result.Failure("El interfaz KNX/IP rechazó el túnel (${connectResult.status})"))
                    is KnxConnectionManager.ConnectResult.NetworkError -> onResult(Result.Failure(connectResult.detail))
                    KnxConnectionManager.ConnectResult.InvalidResponse -> onResult(Result.Failure("Respuesta KNX/IP no válida"))
                    KnxConnectionManager.ConnectResult.Cancelled -> onResult(Result.Failure("Operación cancelada"))
                }
            }
        }

        attempt(1)
    }

    private fun KnxConnectionManager.OperationResult.toExecutorResult(): Result = when (this) {
        is KnxConnectionManager.OperationResult.Success -> {
            val incoming = incoming
            if (incoming != null) {
                val value = incoming.booleanValue?.let { if (it) "Encendido" else "Apagado" }
                monitorRepository.record(
                    direction = KnxTelegramEvent.Direction.INCOMING,
                    kind = if (incoming.kind == KnxConnectionManager.IncomingGroupTelegram.Kind.RESPONSE) {
                        KnxTelegramEvent.Kind.GROUP_VALUE_RESPONSE
                    } else {
                        KnxTelegramEvent.Kind.GROUP_VALUE_WRITE
                    },
                    groupAddress = incoming.destination.toString(),
                    value = incoming.booleanValue?.let { if (it) "1" else "0" },
                    status = KnxTelegramEvent.Status.RECEIVED,
                    detail = "Origen ${incoming.sourceAddress}"
                )
                Result.Success(
                    busValue = value,
                    sourceAddress = incoming.sourceAddress,
                    diagnostic = diagnostic
                )
            } else {
                Result.Success(diagnostic = diagnostic)
            }
        }
        is KnxConnectionManager.OperationResult.Failure -> Result.Failure(detail)
        is KnxConnectionManager.OperationResult.NotAvailable -> Result.Failure(detail)
    }

    private fun telegramFor(command: KnxCommand, toggleValue: Boolean?): KnxTelegram? = when (command.type) {
        KnxCommandType.READ -> KnxTelegram.GroupValueRead(command.destination)
        KnxCommandType.ON -> KnxTelegram.GroupValueWriteBoolean(command.destination, true)
        KnxCommandType.OFF -> KnxTelegram.GroupValueWriteBoolean(command.destination, false)
        KnxCommandType.TOGGLE -> toggleValue?.let {
            KnxTelegram.GroupValueWriteBoolean(command.destination, it)
        }
        else -> null
    }

    private fun resolveEndpoint(): kotlin.Result<KnxEndpoint> = runCatching {
        val settings = SettingsDataStore(appContext).read()
        val detector = NetworkConnectionDetector(appContext)
        val network = try {
            detector.currentState()
        } finally {
            detector.close()
        }

        require(network.isConnected) { "El dispositivo no tiene conexión de red" }
        val useLocal = network.usesLocalRoute
        val host = (if (useLocal) settings.localIp else settings.remoteIp).trim()
        val portText = (if (useLocal) settings.localPort else settings.remotePort).trim()
        val route = if (useLocal) "local" else "remota"
        require(host.isNotBlank()) { "Configura la dirección KNX/IP $route" }
        val port = portText.toIntOrNull()
        require(port != null && port in 1..65535) { "Configura un puerto KNX/IP válido para la ruta $route" }
        KnxEndpoint(host, port)
    }

    override fun close() {
        queuedOperation?.close()
        queuedOperation = null
        connectionManager.close()
    }
}
