package com.onehouse.app.feature.security

import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors

data class SecurityEntity(
    val entityId: String,
    val name: String,
    val category: SecurityEntityCategory,
    val active: Boolean,
    val available: Boolean,
    val lastChanged: String
)

enum class SecurityEntityCategory { OPENING, MOTION }

data class HomeAssistantSecuritySnapshot(
    val openings: List<SecurityEntity> = emptyList(),
    val motions: List<SecurityEntity> = emptyList(),
    val fetchedAtEpochMillis: Long = 0L
) {
    val alertCount: Int get() = openings.count { it.available && it.active } + motions.count { it.available && it.active }
}

object HomeAssistantSecurityClient {
    sealed interface Result {
        data class Success(val snapshot: HomeAssistantSecuritySnapshot) : Result
        data class Failure(val message: String) : Result
    }

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun fetch(baseUrl: String, token: String, callback: (Result) -> Unit) {
        executor.execute {
            val result = runCatching { request(baseUrl, token) }
                .getOrElse { Result.Failure(it.message ?: "No se pudieron leer los sensores") }
            mainHandler.post { callback(result) }
        }
    }

    private fun request(baseUrl: String, token: String): Result {
        val endpoint = baseUrl.trim().trimEnd('/') + "/api/states"
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 7_000
            readTimeout = 10_000
            setRequestProperty("Authorization", "Bearer ${token.trim()}")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
        }
        return try {
            when (connection.responseCode) {
                200 -> parse(connection.inputStream.bufferedReader().use { it.readText() })
                401, 403 -> Result.Failure("Token no válido o sin permisos")
                else -> Result.Failure("Home Assistant respondió con código ${connection.responseCode}")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun parse(json: String): Result {
        val array = JSONArray(json)
        val openings = mutableListOf<SecurityEntity>()
        val motions = mutableListOf<SecurityEntity>()
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val entityId = item.optString("entity_id")
            if (!entityId.startsWith("binary_sensor.")) continue
            val attributes = item.optJSONObject("attributes")
            val deviceClass = attributes?.optString("device_class").orEmpty().lowercase()
            val category = when (deviceClass) {
                "door", "window", "opening", "garage_door" -> SecurityEntityCategory.OPENING
                "motion", "occupancy", "presence" -> SecurityEntityCategory.MOTION
                else -> continue
            }
            val rawState = item.optString("state").lowercase()
            val entity = SecurityEntity(
                entityId = entityId,
                name = attributes?.optString("friendly_name").orEmpty().ifBlank {
                    entityId.substringAfter('.').replace('_', ' ').replaceFirstChar { it.uppercase() }
                },
                category = category,
                active = rawState == "on" || rawState == "open" || rawState == "detected",
                available = rawState != "unavailable" && rawState != "unknown" && rawState.isNotBlank(),
                lastChanged = formatLastChanged(item.optString("last_changed"))
            )
            if (category == SecurityEntityCategory.OPENING) openings += entity else motions += entity
        }
        return Result.Success(
            HomeAssistantSecuritySnapshot(
                openings = openings.sortedBy { it.name.lowercase() },
                motions = motions.sortedBy { it.name.lowercase() },
                fetchedAtEpochMillis = System.currentTimeMillis()
            )
        )
    }

    private fun formatLastChanged(value: String): String = runCatching {
        OffsetDateTime.parse(value).format(DateTimeFormatter.ofPattern("dd/MM HH:mm"))
    }.getOrDefault("Sin fecha")
}
