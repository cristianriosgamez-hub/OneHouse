package com.onehouse.app.feature.security

data class HomeAssistantSettings(
    val baseUrl: String = "",
    val accessToken: String = "",
    val lastStatus: HomeAssistantConnectionStatus = HomeAssistantConnectionStatus.NOT_TESTED,
    val lastMessage: String = "Sin comprobar",
    val lastTestEpochMillis: Long = 0L
)

enum class HomeAssistantConnectionStatus {
    NOT_CONFIGURED,
    NOT_TESTED,
    TESTING,
    CONNECTED,
    FAILED
}
