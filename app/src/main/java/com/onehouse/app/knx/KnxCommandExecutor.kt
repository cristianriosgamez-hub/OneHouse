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
    private val statisticsRepository = KnxSessionStatisticsRepository(appContext)
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
                        statisticsRepository.recordConnectionSuccess()
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
                            when (operation) {
                                is KnxConnectionManager.OperationResult.Success -> operation.diagnostic?.let {
                                    statisticsRepository.recordOperation(
                                        diagnostic = it,
                                        receivedFromBus = operation.incoming != null
                                    )
                                }
                                is KnxConnectionManager.OperationResult.Failure,
                                is KnxConnectionManager.OperationResult.NotAvailable -> statisticsRepository.recordOperationError()
                            }
                            monitorRepository.record(
                                direction = KnxTelegramEvent.Direction.OUTGOING,
                                kind = eventKind,
                                groupAddress = command.destination.toString(),
                                value = eventValue,
                                status = if (result is Result.Success) KnxTelegramEvent.Status.CONFIRMED else KnxTelegramEvent.Status.ERROR,
                                detail = when (result) {
                                    is Result.Success -> result.diagnostic?.let { diagnostic ->
                                        buildString {
                                            append("TX validado · canal ${diagnostic.channelId} · secuencia ${diagnostic.sequence}\n")
                                            append("cEMI TX: ${diagnostic.cemiHex}\n")
                                            append("KNXnet/IP TX: ${diagnostic.knxNetIpHex}")
                                            diagnostic.gatewayAckHex?.let { ackHex ->
                                                append("\nACK gateway: $ackHex")
                                            }
                                            diagnostic.gatewayRoundTripMillis?.let { elapsed ->
                                                append("\nTiempo TX→ACK: ${elapsed} ms")
                                            }
                                            diagnostic.incomingKnxNetIpHex?.let { incomingHex ->
                                                append("\nTelegrama bus RX: $incomingHex")
                                            }
                                            diagnostic.incomingCemiHex?.let { incomingCemi ->
                                                append("\ncEMI RX: $incomingCemi")
                                            }
                                            diagnostic.incomingMessageCode?.let { messageCode ->
                                                append("\nMensaje cEMI RX: 0x%02X".format(messageCode))
                                            }
                                            diagnostic.incomingApci?.let { apci ->
                                                append(" · APCI $apci")
                                            }
                                            if (diagnostic.ignoredAckCount > 0) {
                                                append("\nACK ajenos ignorados: ${diagnostic.ignoredAckCount}")
                                            }
                                            if (diagnostic.invalidPacketCount > 0) {
                                                append("\nPaquetes no válidos ignorados: ${diagnostic.invalidPacketCount}")
                                            }
                                            if (diagnostic.duplicateIncomingCount > 0) {
                                                append("\nTelegramas entrantes duplicados: ${diagnostic.duplicateIncomingCount}")
                                            }
                                        }
                                    }
                                    is Result.Failure -> result.message
                                }
                            )
                            onResult(result)
                        }
                    }
                    KnxConnectionManager.ConnectResult.Timeout -> reportConnectionFailure(
                        command,
                        "Tiempo de espera agotado al conectar con KNX/IP",
                        onResult
                    )
                    is KnxConnectionManager.ConnectResult.Rejected -> reportConnectionFailure(
                        command,
                        "El interfaz KNX/IP rechazó el túnel (${connectResult.status})",
                        onResult
                    )
                    is KnxConnectionManager.ConnectResult.NetworkError -> reportConnectionFailure(
                        command,
                        connectResult.detail,
                        onResult
                    )
                    KnxConnectionManager.ConnectResult.InvalidResponse -> reportConnectionFailure(
                        command,
                        "Respuesta KNX/IP no válida",
                        onResult
                    )
                    KnxConnectionManager.ConnectResult.Cancelled -> onResult(Result.Failure("Operación cancelada"))
                }
            }
        }

        attempt(1)
    }


    private fun reportConnectionFailure(
        command: KnxCommand,
        message: String,
        onResult: (Result) -> Unit
    ) {
        statisticsRepository.recordConnectionError()
        monitorRepository.record(
            direction = KnxTelegramEvent.Direction.SYSTEM,
            kind = KnxTelegramEvent.Kind.CONNECT,
            groupAddress = command.destination.toString(),
            status = KnxTelegramEvent.Status.ERROR,
            detail = message
        )
        onResult(Result.Failure(message))
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
                    detail = buildString {
                        append("Origen ${incoming.sourceAddress}")
                        append(" · ${incoming.apci}")
                        append(" · cEMI 0x%02X".format(incoming.messageCode))
                        append("\n${incoming.cemiHex}")
                    }
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
