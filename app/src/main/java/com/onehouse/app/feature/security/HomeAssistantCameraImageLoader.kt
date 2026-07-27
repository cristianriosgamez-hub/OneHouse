package com.onehouse.app.feature.security

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/** Loads a single authenticated still image from a Home Assistant camera entity. */
object HomeAssistantCameraImageLoader {
    sealed interface Result {
        data class Success(val bitmap: Bitmap) : Result
        data class Failure(val message: String) : Result
    }

    private val executor = Executors.newFixedThreadPool(2)
    private val mainHandler = Handler(Looper.getMainLooper())

    fun load(baseUrl: String, token: String, entityId: String, callback: (Result) -> Unit) {
        executor.execute {
            val result = runCatching { request(baseUrl, token, entityId) }
                .getOrElse { Result.Failure(it.message ?: "No se pudo obtener la imagen") }
            mainHandler.post { callback(result) }
        }
    }

    private fun request(baseUrl: String, token: String, entityId: String): Result {
        val endpoint = baseUrl.trim().trimEnd('/') + "/api/camera_proxy/" + entityId
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 7_000
            readTimeout = 12_000
            setRequestProperty("Authorization", "Bearer ${token.trim()}")
            setRequestProperty("Accept", "image/*")
        }
        return try {
            when (connection.responseCode) {
                200 -> BitmapFactory.decodeStream(connection.inputStream)?.let { Result.Success(it) }
                    ?: Result.Failure("Home Assistant no devolvió una imagen válida")
                401, 403 -> Result.Failure("Token sin permisos para acceder a la cámara")
                404 -> Result.Failure("La cámara no ofrece una imagen compatible")
                else -> Result.Failure("La cámara respondió con código ${connection.responseCode}")
            }
        } finally {
            connection.disconnect()
        }
    }
}
