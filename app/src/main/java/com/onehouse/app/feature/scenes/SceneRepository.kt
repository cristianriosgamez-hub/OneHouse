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
                action.delayAfterMillis,
                action.numericValue ?: ""
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
            scene.favorite,
            scene.requireConfirmation,
            actions
        ).joinToString("|")
    }

    private fun decodeScene(raw: String): SmartScene? = runCatching {
        val parts = raw.split('|')
        if (parts.size < 7) return@runCatching null

        val hasStopOnError = parts.size >= 8
        val hasFavoriteFields = parts.size >= 10
        val stopOnError = if (hasStopOnError) parts[6].toBooleanStrictOrNull() ?: true else true
        val favorite = if (hasFavoriteFields) parts[7].toBooleanStrictOrNull() ?: false else false
        val requireConfirmation = if (hasFavoriteFields) parts[8].toBooleanStrictOrNull() ?: false else false
        val actionsStartIndex = when {
            hasFavoriteFields -> 9
            hasStopOnError -> 7
            else -> 6
        }
        val actionsRaw = parts.subList(actionsStartIndex, parts.size).joinToString("|")

        SmartScene(
            id = parts[0].toLong(),
            name = parts[1],
            description = parts[2],
            enabled = parts[3].toBoolean(),
            lastExecutionMillis = parts[4].toLongOrNull(),
            executionCount = parts[5].toInt(),
            stopOnError = stopOnError,
            favorite = favorite,
            requireConfirmation = requireConfirmation,
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
            delayAfterMillis = parts.getOrNull(5)?.toLongOrNull()?.coerceIn(0L, MAX_DELAY_MILLIS) ?: 0L,
            numericValue = parts.getOrNull(6)?.toDoubleOrNull()
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
