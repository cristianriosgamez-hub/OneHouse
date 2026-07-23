package com.onehouse.app.feature.settings

import android.os.Handler
import android.os.Looper
import com.onehouse.app.data.knx.KnxConnectionStatus
import com.onehouse.app.data.knx.KnxSettings
import com.onehouse.app.data.knx.KnxSettingsRepository
import com.onehouse.app.data.knx.KnxSettingsValidation
import com.onehouse.app.knx.KnxConnectionTester
import com.onehouse.app.knx.NetworkConnectionDetector
import java.io.Closeable

class SettingsViewModel(
    private val repository: KnxSettingsRepository,
    private val networkDetector: NetworkConnectionDetector
) {
    enum class ConnectionRoute {
        LOCAL,
        REMOTE,
        NONE
    }

    private val observers = mutableSetOf<() -> Unit>()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var connectionTest: Closeable? = null
    private var networkObservation: Closeable? = null

    var settings: KnxSettings = repository.load()
        private set

    var validation: KnxSettingsValidation = validate(settings)
        private set

    var connectionStatus: KnxConnectionStatus = KnxConnectionStatus.NOT_TESTED
        private set

    var statusMessage: String = "Sin comprobar"
        private set

    var networkState: NetworkConnectionDetector.State = networkDetector.currentState()
        private set

    val selectedRoute: ConnectionRoute
        get() = if (settings.autoReconnect) {
            routeFor(networkState)
        } else if (networkState.isConnected) {
            ConnectionRoute.LOCAL
        } else {
            ConnectionRoute.NONE
        }

    val selectedEndpoint: String
        get() = when (selectedRoute) {
            ConnectionRoute.LOCAL -> endpointLabel(settings.localIp, settings.localPort)
            ConnectionRoute.REMOTE -> endpointLabel(settings.remoteIp, settings.remotePort)
            ConnectionRoute.NONE -> "No disponible"
        }

    var lastTestEpochMillis: Long = 0L
        private set

    init {
        networkObservation = networkDetector.observe { newState ->
            mainHandler.post {
                val previousRoute = selectedRoute
                networkState = newState
                if (previousRoute != selectedRoute && connectionStatus != KnxConnectionStatus.TESTING) {
                    connectionStatus = KnxConnectionStatus.NOT_TESTED
                    statusMessage = "La red ha cambiado. Conexión pendiente de comprobar"
                }
                notifyObservers()
            }
        }
    }

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
        networkState = networkDetector.currentState()
        validation = validate(settings)

        val route = selectedRoute
        if (route == ConnectionRoute.NONE || !networkState.isConnected) {
            failTest("El dispositivo no tiene conexión de red")
            return
        }

        val host = when (route) {
            ConnectionRoute.LOCAL -> settings.localIp.trim()
            ConnectionRoute.REMOTE -> settings.remoteIp.trim()
            ConnectionRoute.NONE -> ""
        }
        val portText = when (route) {
            ConnectionRoute.LOCAL -> settings.localPort
            ConnectionRoute.REMOTE -> settings.remotePort
            ConnectionRoute.NONE -> ""
        }

        val routeError = when (route) {
            ConnectionRoute.LOCAL -> validation.localIpError ?: validation.localPortError
            ConnectionRoute.REMOTE -> {
                if (host.isBlank()) "Introduce la dirección IP remota" else {
                    ipError(host, required = true) ?: validation.remotePortError
                }
            }
            ConnectionRoute.NONE -> "No hay una ruta disponible"
        }

        val port = portText.toIntOrNull()
        if (routeError != null || port == null) {
            failTest(routeError ?: "Revisa los datos de conexión")
            return
        }

        connectionTest?.close()
        connectionStatus = KnxConnectionStatus.TESTING
        statusMessage = "Abriendo túnel KNX/IP por la ruta ${routeLabel(route).lowercase()}…"
        notifyObservers()

        connectionTest = KnxConnectionTester.test(host = host, port = port) { result ->
            connectionTest = null
            lastTestEpochMillis = System.currentTimeMillis()
            when (result) {
                is KnxConnectionTester.Result.Success -> {
                    connectionStatus = KnxConnectionStatus.CONNECTED
                    statusMessage = "Túnel KNX/IP correcto por ${routeLabel(route).lowercase()} (${result.deviceAddress})"
                }
                KnxConnectionTester.Result.Timeout -> {
                    connectionStatus = KnxConnectionStatus.FAILED
                    statusMessage = "Sin respuesta KNX/IP en ${endpointLabel(host, portText)}"
                }
                is KnxConnectionTester.Result.Rejected -> {
                    connectionStatus = KnxConnectionStatus.FAILED
                    statusMessage = "El interfaz KNX/IP rechazó el túnel (código ${result.status})"
                }
                KnxConnectionTester.Result.InvalidResponse -> {
                    connectionStatus = KnxConnectionStatus.FAILED
                    statusMessage = "El equipo respondió, pero no aceptó KNXnet/IP Tunnelling"
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
        networkObservation?.close()
        networkObservation = null
        networkDetector.close()
        observers.clear()
    }

    private fun failTest(message: String) {
        connectionStatus = KnxConnectionStatus.FAILED
        statusMessage = message
        lastTestEpochMillis = System.currentTimeMillis()
        notifyObservers()
    }

    private fun routeFor(state: NetworkConnectionDetector.State): ConnectionRoute = when {
        !state.isConnected -> ConnectionRoute.NONE
        state.usesLocalRoute -> ConnectionRoute.LOCAL
        state.usesRemoteRoute -> ConnectionRoute.REMOTE
        else -> ConnectionRoute.NONE
    }

    private fun routeLabel(route: ConnectionRoute): String = when (route) {
        ConnectionRoute.LOCAL -> "Local"
        ConnectionRoute.REMOTE -> "Remota"
        ConnectionRoute.NONE -> "Sin ruta"
    }

    private fun endpointLabel(host: String, port: String): String =
        if (host.isBlank()) "Sin configurar" else "$host:$port"

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
