package com.onehouse.app.feature.backup

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

class BackupManager(context: Context) {
    private val appContext = context.applicationContext

    fun createBackup(appVersion: String): String {
        val root = JSONObject()
            .put("format", FORMAT)
            .put("schemaVersion", SCHEMA_VERSION)
            .put("createdAtMillis", System.currentTimeMillis())
            .put("createdAtIso", Instant.now().toString())
            .put("appVersion", appVersion)

        val files = JSONObject()
        BACKUP_PREFERENCES.forEach { name ->
            files.put(name, encodePreferences(appContext.getSharedPreferences(name, Context.MODE_PRIVATE)))
        }
        root.put("preferences", files)
        root.put("summary", buildSummary(root, files))
        return root.toString(2)
    }

    fun preview(rawJson: String): BackupPreview {
        val root = JSONObject(rawJson)
        validateRoot(root)
        val files = root.getJSONObject("preferences")
        val summaryJson = root.optJSONObject("summary") ?: buildSummary(root, files)
        return BackupPreview(
            summary = BackupSummary(
                createdAtMillis = root.getLong("createdAtMillis"),
                appVersion = root.optString("appVersion", "Desconocida"),
                preferencesFiles = summaryJson.optInt("preferencesFiles", files.length()),
                scenes = summaryJson.optInt("scenes", countItems(files, "onehouse_smart_scenes", "scene_count", "scene_")),
                automations = summaryJson.optInt("automations", countSet(files, "onehouse_conditional_automations", "rules")),
                weeklySchedules = summaryJson.optInt("weeklySchedules", countSet(files, "onehouse_weekly_schedule", "events")),
                solarSchedules = summaryJson.optInt("solarSchedules", countSet(files, "onehouse_solar_schedule", "events")),
                knxEntries = summaryJson.optInt("knxEntries", countObjectEntries(files, "onehouse_knx_configuration"))
            ),
            rawJson = rawJson
        )
    }

    fun restore(rawJson: String): BackupOperationResult {
        return runCatching {
            val root = JSONObject(rawJson)
            validateRoot(root)
            val files = root.getJSONObject("preferences")

            val decoded = mutableMapOf<String, Map<String, Any>>()
            files.keys().forEach { name ->
                if (name in BACKUP_PREFERENCES) decoded[name] = decodePreferences(files.getJSONObject(name))
            }
            require(decoded.isNotEmpty()) { "La copia no contiene datos compatibles con OneHouse." }

            val previous = decoded.keys.associateWith { name ->
                appContext.getSharedPreferences(name, Context.MODE_PRIVATE).all.toMap()
            }

            try {
                decoded.forEach { (name, values) -> replacePreferences(name, values) }
            } catch (error: Throwable) {
                previous.forEach { (name, values) -> replacePreferences(name, values) }
                throw error
            }
            BackupOperationResult.Success("Restauración completada. Reinicia OneHouse para recargar toda la configuración.")
        }.getOrElse { error ->
            BackupOperationResult.Error(error.message ?: "No se pudo restaurar la copia de seguridad.")
        }
    }

    private fun validateRoot(root: JSONObject) {
        require(root.optString("format") == FORMAT) { "El archivo no es una copia válida de OneHouse." }
        val schema = root.optInt("schemaVersion", -1)
        require(schema in 1..SCHEMA_VERSION) { "Versión de copia no compatible: $schema." }
        require(root.has("createdAtMillis") && root.optJSONObject("preferences") != null) {
            "La copia está incompleta o dañada."
        }
    }

    private fun encodePreferences(preferences: SharedPreferences): JSONObject {
        val result = JSONObject()
        preferences.all.forEach { (key, value) ->
            val item = JSONObject()
            when (value) {
                is String -> item.put("type", "string").put("value", value)
                is Boolean -> item.put("type", "boolean").put("value", value)
                is Int -> item.put("type", "int").put("value", value)
                is Long -> item.put("type", "long").put("value", value.toString())
                is Float -> item.put("type", "float").put("value", value.toDouble())
                is Set<*> -> item.put("type", "stringSet").put("value", JSONArray(value.filterIsInstance<String>().sorted()))
                else -> return@forEach
            }
            result.put(key, item)
        }
        return result
    }

    private fun decodePreferences(json: JSONObject): Map<String, Any> {
        val result = linkedMapOf<String, Any>()
        json.keys().forEach { key ->
            val item = json.getJSONObject(key)
            result[key] = when (item.getString("type")) {
                "string" -> item.getString("value")
                "boolean" -> item.getBoolean("value")
                "int" -> item.getInt("value")
                "long" -> item.getString("value").toLong()
                "float" -> item.getDouble("value").toFloat()
                "stringSet" -> item.getJSONArray("value").let { array ->
                    buildSet { for (index in 0 until array.length()) add(array.getString(index)) }
                }
                else -> error("Tipo de dato desconocido para '$key'.")
            }
        }
        return result
    }

    private fun replacePreferences(name: String, values: Map<String, *>) {
        val editor = appContext.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear()
        values.forEach { (key, value) ->
            when (value) {
                is String -> editor.putString(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Float -> editor.putFloat(key, value)
                is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
            }
        }
        check(editor.commit()) { "No se pudo guardar '$name'." }
    }

    private fun buildSummary(root: JSONObject, files: JSONObject): JSONObject = JSONObject()
        .put("preferencesFiles", files.length())
        .put("scenes", countItems(files, "onehouse_smart_scenes", "scene_count", "scene_"))
        .put("automations", countSet(files, "onehouse_conditional_automations", "rules"))
        .put("weeklySchedules", countSet(files, "onehouse_weekly_schedule", "events"))
        .put("solarSchedules", countSet(files, "onehouse_solar_schedule", "events"))
        .put("knxEntries", countObjectEntries(files, "onehouse_knx_configuration"))

    private fun countItems(files: JSONObject, file: String, countKey: String, prefix: String): Int {
        val prefs = files.optJSONObject(file) ?: return 0
        val count = prefs.optJSONObject(countKey)?.optInt("value", -1) ?: -1
        if (count >= 0) return count
        var total = 0
        prefs.keys().forEach { if (it.startsWith(prefix)) total++ }
        return total
    }

    private fun countSet(files: JSONObject, file: String, key: String): Int =
        files.optJSONObject(file)?.optJSONObject(key)?.optJSONArray("value")?.length() ?: 0

    private fun countObjectEntries(files: JSONObject, file: String): Int =
        files.optJSONObject(file)?.length() ?: 0

    private companion object {
        const val FORMAT = "onehouse-backup"
        const val SCHEMA_VERSION = 1
        val BACKUP_PREFERENCES = listOf(
            "onehouse_knx_settings",
            "onehouse_knx_configuration",
            "insidecontrol_import",
            "import_project_ui",
            "onehouse_climate_schedule",
            "onehouse_weekly_schedule",
            "onehouse_solar_schedule",
            "onehouse_conditional_automations",
            "onehouse_smart_scenes",
            "onehouse_home_assistant",
            "onehouse_security_monitoring",
            "onehouse_security_notifications"
        )
    }
}
