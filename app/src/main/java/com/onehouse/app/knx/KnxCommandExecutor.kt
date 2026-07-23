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
        data object Success : Result
        data class Failure(val message: String) : Result
    }

    private val appContext = context.applicationContext
    private val connectionManager = KnxConnectionManager()

    fun execute(
        command: KnxCommand,
        toggleValue: Boolean? = null,
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

        connectionManager.connect(endpoint) { connectResult ->
            when (connectResult) {
                is KnxConnectionManager.ConnectResult.Success -> {
                    connectionManager.sendTelegram(telegram) { operation ->
                        connectionManager.disconnect()
                        onResult(
                            when (operation) {
                                KnxConnectionManager.OperationResult.Success -> Result.Success
                                is KnxConnectionManager.OperationResult.Failure -> Result.Failure(operation.detail)
                                is KnxConnectionManager.OperationResult.NotAvailable -> Result.Failure(operation.detail)
                            }
                        )
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
        connectionManager.close()
    }
}
