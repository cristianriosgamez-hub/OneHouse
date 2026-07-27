package com.onehouse.app.feature.security

import android.os.Handler
import android.os.Looper
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.concurrent.Executors

object HomeAssistantConnectionTester {
    sealed interface Result {
        data object Success : Result
        data class Failure(val message: String) : Result
    }

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun test(baseUrl: String, accessToken: String, callback: (Result) -> Unit) {
        executor.execute {
            val result = runCatching {
                val normalized = normalizeBaseUrl(baseUrl)
                val connection = URL("$normalized/api/").openConnection() as HttpURLConnection
                try {
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 5_000
                    connection.readTimeout = 5_000
                    connection.setRequestProperty("Authorization", "Bearer ${accessToken.trim()}")
                    connection.setRequestProperty("Accept", "application/json")
                    when (connection.responseCode) {
                        in 200..299 -> Result.Success
                        401 -> Result.Failure("Token no válido o sin permisos")
                        404 -> Result.Failure("La dirección no corresponde a Home Assistant")
                        else -> Result.Failure("Home Assistant respondió con código ${connection.responseCode}")
                    }
                } finally {
                    connection.disconnect()
                }
            }.getOrElse { error ->
                Result.Failure(error.message?.takeIf { it.isNotBlank() } ?: "No se pudo establecer la conexión")
            }
            mainHandler.post { callback(result) }
        }
    }

    fun validateBaseUrl(value: String): String? {
        if (value.isBlank()) return "Introduce la dirección de Home Assistant"
        return runCatching {
            val uri = URI(normalizeBaseUrl(value))
            require(uri.scheme == "http" || uri.scheme == "https")
            require(!uri.host.isNullOrBlank())
        }.fold(
            onSuccess = { null },
            onFailure = { "Utiliza una dirección como http://192.168.1.20:8123" }
        )
    }

    private fun normalizeBaseUrl(value: String): String = value.trim().trimEnd('/')
}
