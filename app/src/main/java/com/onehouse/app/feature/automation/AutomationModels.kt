package com.onehouse.app.feature.automation

enum class AutomationOperator(val displayName: String) {
    EQUALS("Igual a"),
    NOT_EQUALS("Distinto de"),
    GREATER_THAN("Mayor que"),
    LESS_THAN("Menor que")
}

enum class AutomationAction(val displayName: String) {
    ON("Encender / activar"),
    OFF("Apagar / desactivar")
}

data class AutomationRule(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val conditionAddress: String,
    val conditionDpt: String,
    val operator: AutomationOperator,
    val conditionValue: String,
    val actionAddress: String,
    val actionDpt: String = "1.001",
    val action: AutomationAction,
    val enabled: Boolean = true,
    val cooldownMinutes: Int = 5,
    val lastExecutionMillis: Long? = null,
    val executionCount: Int = 0
)

data class AutomationState(
    val globallyEnabled: Boolean = false,
    val rules: List<AutomationRule> = emptyList()
)

data class AutomationEvaluation(
    val matches: Boolean,
    val currentValue: String?,
    val message: String
)
