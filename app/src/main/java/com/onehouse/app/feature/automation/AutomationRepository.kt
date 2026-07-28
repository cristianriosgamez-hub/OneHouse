package com.onehouse.app.feature.automation

import android.content.Context

interface AutomationRepository {
    fun load(): AutomationState
    fun save(state: AutomationState)
}

class SharedPreferencesAutomationRepository(context: Context) : AutomationRepository {
    private val preferences = context.applicationContext.getSharedPreferences(
        "onehouse_conditional_automations",
        Context.MODE_PRIVATE
    )

    override fun load(): AutomationState {
        val rules = preferences.getStringSet("rules", emptySet()).orEmpty()
            .mapNotNull(::decode)
            .sortedBy { it.name.lowercase() }
        return AutomationState(
            globallyEnabled = preferences.getBoolean("enabled", false),
            rules = rules
        )
    }

    override fun save(state: AutomationState) {
        preferences.edit()
            .putBoolean("enabled", state.globallyEnabled)
            .putStringSet("rules", state.rules.map(::encode).toSet())
            .apply()
    }

    private fun encode(rule: AutomationRule): String = listOf(
        rule.id,
        rule.name,
        rule.conditionAddress,
        rule.conditionDpt,
        rule.operator.name,
        rule.conditionValue,
        rule.actionAddress,
        rule.actionDpt,
        rule.action.name,
        rule.enabled,
        rule.cooldownMinutes,
        rule.lastExecutionMillis ?: "",
        rule.executionCount
    ).joinToString("|") { it.toString().replace("|", " ") }

    private fun decode(raw: String): AutomationRule? = runCatching {
        val p = raw.split('|')
        AutomationRule(
            id = p[0].toLong(),
            name = p[1],
            conditionAddress = p[2],
            conditionDpt = p[3],
            operator = AutomationOperator.valueOf(p[4]),
            conditionValue = p[5],
            actionAddress = p[6],
            actionDpt = p[7],
            action = AutomationAction.valueOf(p[8]),
            enabled = p[9].toBoolean(),
            cooldownMinutes = p[10].toInt(),
            lastExecutionMillis = p[11].toLongOrNull(),
            executionCount = p[12].toInt()
        )
    }.getOrNull()
}
