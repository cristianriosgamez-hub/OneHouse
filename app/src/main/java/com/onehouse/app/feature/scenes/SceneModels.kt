package com.onehouse.app.feature.scenes

enum class SceneActionType(val displayName: String) {
    ON("Encender / activar"),
    OFF("Apagar / desactivar")
}

data class SceneAction(
    val id: Long = System.nanoTime(),
    val name: String,
    val groupAddress: String,
    val dpt: String = "1.001",
    val type: SceneActionType
)

data class SmartScene(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val description: String = "",
    val enabled: Boolean = true,
    val actions: List<SceneAction> = emptyList(),
    val lastExecutionMillis: Long? = null,
    val executionCount: Int = 0
)

data class SceneState(
    val scenes: List<SmartScene> = emptyList()
)
