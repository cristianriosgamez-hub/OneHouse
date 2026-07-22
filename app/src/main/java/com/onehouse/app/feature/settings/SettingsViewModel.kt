package com.onehouse.app.feature.settings

import com.onehouse.app.data.knx.KnxConnectionStatus
import com.onehouse.app.data.knx.KnxSettings
import com.onehouse.app.data.knx.KnxSettingsRepository
import com.onehouse.app.data.knx.KnxSettingsValidation
import com.onehouse.app.knx.KnxConnectionTester
import java.io.Closeable

class SettingsViewModel(private val repository: KnxSettingsRepository) {
    private val observers = mutableSetOf<() -> Unit>()
    private var connectionTest: Closeable? = null

    var settings: KnxSettings = repository.load()
        private set

    var validation: KnxSettingsValidation = validate(settings)
        private set

    var connectionStatus: KnxConnectionStatus = KnxConnectionStatus.NOT_TESTED
        private set

    var statusMessage: String = "Sin comprobar"
        private set

    fun observe(observer: () -> Unit) {
        observers += observer
    }

    fun update(change: (KnxSettings) -> KnxSettings) {
        settings = change(settings).copy(lastUpdatedEpochMillis = System.currentTimeMillis())
        validation = validate(settings)
        connectionStatus = KnxConnectionStatus.NOT_TESTED
        statusMessage = "Cambios guardados. Conexión pendiente de comprobar"
        repository.save(settings)
        notifyObservers()
    }

    fun testConnection() {
        validation = validate(settings)
        if (!validation.isValid) {
            connectionStatus = KnxConnectionStatus.FAILED
            statusMessage = "Revisa los datos de conexión"
            notifyObservers()
            return
        }

        connectionTest?.close()
        connectionStatus = KnxConnectionStatus.TESTING
        statusMessage = "Buscando el interfaz KNX/IP…"
        notifyObservers()

        connectionTest = KnxConnectionTester.test(
            host = settings.localIp.trim(),
            port = settings.localPort.toInt()
        ) { result ->
            connectionTest = null
            when (result) {
                is KnxConnectionTester.Result.Success -> {
                    connectionStatus = KnxConnectionStatus.CONNECTED
                    statusMessage = "Respuesta KNX/IP recibida de ${result.deviceAddress}"
                }
                KnxConnectionTester.Result.Timeout -> {
                    connectionStatus = KnxConnectionStatus.FAILED
                    statusMessage = "Sin respuesta KNX/IP (tiempo agotado)"
                }
                KnxConnectionTester.Result.InvalidResponse -> {
                    connectionStatus = KnxConnectionStatus.FAILED
                    statusMessage = "El equipo respondió, pero no con KNXnet/IP"
                }
                is KnxConnectionTester.Result.NetworkError -> {
                    connectionStatus = KnxConnectionStatus.FAILED
                    statusMessage = "Error de red: ${result.detail}"
                }
            }
            notifyObservers()
        }
    }

    fun close() {
        connectionTest?.close()
        connectionTest = null
        observers.clear()
    }

    private fun notifyObservers() = observers.toList().forEach { it() }

    private fun validate(value: KnxSettings): KnxSettingsValidation = KnxSettingsValidation(
        localIpError = ipError(value.localIp, required = true),
        localPortError = portError(value.localPort),
        remoteIpError = ipError(value.remoteIp, required = false),
        remotePortError = portError(value.remotePort)
    )

    private fun ipError(value: String, required: Boolean): String? {
        if (value.isBlank()) return if (required) "Introduce una dirección IP" else null
        val parts = value.split('.')
        return if (parts.size == 4 && parts.all { it.toIntOrNull() in 0..255 }) null
        else "Dirección IP no válida"
    }

    private fun portError(value: String): String? {
        val port = value.toIntOrNull()
        return if (port != null && port in 1..65535) null else "Puerto no válido"
    }
}
