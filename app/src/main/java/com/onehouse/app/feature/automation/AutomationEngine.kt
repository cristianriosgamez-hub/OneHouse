package com.onehouse.app.feature.automation

import android.content.Context
import com.onehouse.app.knx.KnxCommand
import com.onehouse.app.knx.KnxCommandExecutor
import com.onehouse.app.knx.KnxCommandType
import com.onehouse.app.knx.KnxGroupAddress
import com.onehouse.app.knx.KnxStateRepository
import java.io.Closeable

class AutomationEngine(context: Context) : Closeable {
    private val executor = KnxCommandExecutor(context.applicationContext)

    fun evaluate(rule: AutomationRule): AutomationEvaluation {
        val state = executor.getState(rule.conditionAddress)
            ?: return AutomationEvaluation(false, null, "Sin valor KNX real para ${rule.conditionAddress}")

        val currentValue = state.displayValue()
        val matches = compare(state, rule)
        return AutomationEvaluation(
            matches = matches,
            currentValue = currentValue,
            message = if (matches) "Condición cumplida" else "Condición no cumplida"
        )
    }

    fun execute(rule: AutomationRule, onResult: (KnxCommandExecutor.Result) -> Unit) {
        val command = KnxCommand(
            type = if (rule.action == AutomationAction.ON) KnxCommandType.ON else KnxCommandType.OFF,
            destination = KnxGroupAddress.parse(rule.actionAddress),
            dpt = rule.actionDpt
        )
        executor.execute(command = command, onResult = onResult)
    }

    fun canExecute(rule: AutomationRule, nowMillis: Long = System.currentTimeMillis()): Boolean {
        val last = rule.lastExecutionMillis ?: return true
        return nowMillis - last >= rule.cooldownMinutes.coerceAtLeast(0) * 60_000L
    }

    private fun compare(state: KnxStateRepository.State, rule: AutomationRule): Boolean {
        val expectedBoolean = parseBoolean(rule.conditionValue)
        val actualBoolean = state.booleanValue
        if (actualBoolean != null && expectedBoolean != null) {
            return when (rule.operator) {
                AutomationOperator.EQUALS -> actualBoolean == expectedBoolean
                AutomationOperator.NOT_EQUALS -> actualBoolean != expectedBoolean
                AutomationOperator.GREATER_THAN -> actualBoolean && !expectedBoolean
                AutomationOperator.LESS_THAN -> !actualBoolean && expectedBoolean
            }
        }

        val actualNumber = state.rawValue?.replace(',', '.')?.toDoubleOrNull()
        val expectedNumber = rule.conditionValue.replace(',', '.').toDoubleOrNull()
        if (actualNumber != null && expectedNumber != null) {
            return when (rule.operator) {
                AutomationOperator.EQUALS -> actualNumber == expectedNumber
                AutomationOperator.NOT_EQUALS -> actualNumber != expectedNumber
                AutomationOperator.GREATER_THAN -> actualNumber > expectedNumber
                AutomationOperator.LESS_THAN -> actualNumber < expectedNumber
            }
        }

        val actualText = state.rawValue.orEmpty().trim()
        val expectedText = rule.conditionValue.trim()
        return when (rule.operator) {
            AutomationOperator.EQUALS -> actualText.equals(expectedText, ignoreCase = true)
            AutomationOperator.NOT_EQUALS -> !actualText.equals(expectedText, ignoreCase = true)
            AutomationOperator.GREATER_THAN -> actualText > expectedText
            AutomationOperator.LESS_THAN -> actualText < expectedText
        }
    }

    private fun parseBoolean(value: String): Boolean? = when (value.trim().lowercase()) {
        "1", "true", "on", "encendido", "activo", "sí", "si" -> true
        "0", "false", "off", "apagado", "inactivo", "no" -> false
        else -> null
    }

    private fun KnxStateRepository.State.displayValue(): String =
        booleanValue?.let { if (it) "1 (activo)" else "0 (inactivo)" }
            ?: rawValue
            ?: "---"

    override fun close() {
        executor.close()
    }
}
