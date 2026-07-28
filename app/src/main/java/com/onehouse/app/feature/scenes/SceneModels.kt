package com.onehouse.app.feature.scenes

enum class SceneActionType(val displayName: String) {
    ON("Encender / activar"),
    OFF("Apagar / desactivar"),
    BLIND_UP("Subir persiana"),
    BLIND_DOWN("Bajar persiana"),
    BLIND_POSITION("Posición de persiana"),
    CLIMATE_SETPOINT("Temperatura objetivo")
}

data class SceneAction(
    val id: Long = System.nanoTime(),
    val name: String,
    val groupAddress: String,
    val dpt: String = "1.001",
    val type: SceneActionType,
    val delayAfterMillis: Long = 0L,
    val numericValue: Double? = null
) {
    val valueDescription: String
        get() = when (type) {
            SceneActionType.BLIND_POSITION -> numericValue?.let { "${formatNumber(it)} %" } ?: "---"
            SceneActionType.CLIMATE_SETPOINT -> numericValue?.let { "${formatNumber(it)} °C" } ?: "---"
            else -> type.displayName
        }

    private fun formatNumber(value: Double): String =
        if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)
}

data class SmartScene(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val description: String = "",
    val enabled: Boolean = true,
    val stopOnError: Boolean = true,
    val actions: List<SceneAction> = emptyList(),
    val lastExecutionMillis: Long? = null,
    val executionCount: Int = 0
)

data class SceneState(
    val scenes: List<SmartScene> = emptyList()
)
