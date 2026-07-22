package com.onehouse.app.feature.settings

import com.onehouse.app.data.knx.KnxConnectionStatus
import com.onehouse.app.data.knx.KnxSettings
import com.onehouse.app.data.knx.KnxSettingsRepository
import com.onehouse.app.data.knx.KnxSettingsValidation

class SettingsViewModel(private val repository: KnxSettingsRepository) {
    private val observers = mutableSetOf<() -> Unit>()

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

        connectionStatus = KnxConnectionStatus.TESTING
        statusMessage = "Comprobando configuración…"
        notifyObservers()

        // v1.4.0 configura y valida los datos. La conexión KNX real llegará en v1.4.2.
        connectionStatus = KnxConnectionStatus.CONNECTED
        statusMessage = "Configuración válida"
        notifyObservers()
    }

    fun close() {
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
