package com.onehouse.app.feature.scenes

import android.content.Context

interface SceneRepository {
    fun load(): SceneState
    fun save(state: SceneState)
}

class SharedPreferencesSceneRepository(context: Context) : SceneRepository {
    private val preferences = context.applicationContext.getSharedPreferences(
        "onehouse_smart_scenes",
        Context.MODE_PRIVATE
    )

    override fun load(): SceneState {
        val count = preferences.getInt(KEY_COUNT, 0)
        val scenes = (0 until count).mapNotNull { index ->
            preferences.getString("scene_$index", null)?.let(::decodeScene)
        }
        return SceneState(scenes)
    }

    override fun save(state: SceneState) {
        val editor = preferences.edit().clear().putInt(KEY_COUNT, state.scenes.size)
        state.scenes.forEachIndexed { index, scene ->
            editor.putString("scene_$index", encodeScene(scene))
        }
        editor.apply()
    }

    private fun encodeScene(scene: SmartScene): String {
        val actions = scene.actions.joinToString("~") { action ->
            listOf(
                action.id,
                clean(action.name),
                action.groupAddress,
                action.dpt,
                action.type.name,
                action.delayAfterMillis
            ).joinToString("^")
        }
        return listOf(
            scene.id,
            clean(scene.name),
            clean(scene.description),
            scene.enabled,
            scene.lastExecutionMillis ?: "",
            scene.executionCount,
            scene.stopOnError,
            actions
        ).joinToString("|")
    }

    private fun decodeScene(raw: String): SmartScene? = runCatching {
        val parts = raw.split('|')
        if (parts.size < 7) return@runCatching null

        val isNewFormat = parts.size >= 8
        val stopOnError = if (isNewFormat) parts[6].toBooleanStrictOrNull() ?: true else true
        val actionsRaw = if (isNewFormat) parts.subList(7, parts.size).joinToString("|") else parts[6]

        SmartScene(
            id = parts[0].toLong(),
            name = parts[1],
            description = parts[2],
            enabled = parts[3].toBoolean(),
            lastExecutionMillis = parts[4].toLongOrNull(),
            executionCount = parts[5].toInt(),
            stopOnError = stopOnError,
            actions = if (actionsRaw.isBlank()) emptyList() else actionsRaw.split('~').mapNotNull(::decodeAction)
        )
    }.getOrNull()

    private fun decodeAction(raw: String): SceneAction? = runCatching {
        val parts = raw.split('^')
        if (parts.size < 5) return@runCatching null
        SceneAction(
            id = parts[0].toLong(),
            name = parts[1],
            groupAddress = parts[2],
            dpt = parts[3],
            type = SceneActionType.valueOf(parts[4]),
            delayAfterMillis = parts.getOrNull(5)?.toLongOrNull()?.coerceIn(0L, MAX_DELAY_MILLIS) ?: 0L
        )
    }.getOrNull()

    private fun clean(value: String): String = value
        .replace("|", " ")
        .replace("~", " ")
        .replace("^", " ")

    private companion object {
        const val KEY_COUNT = "scene_count"
        const val MAX_DELAY_MILLIS = 60_000L
    }
}
