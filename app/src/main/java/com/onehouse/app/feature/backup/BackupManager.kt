package com.onehouse.app.feature.backup

import android.content.Context
import android.content.SharedPreferences
import com.onehouse.app.core.storage.PreferenceFiles
import com.onehouse.app.device.ControlKind
import com.onehouse.app.knx.AppKnxConfigurationRepository
import com.onehouse.app.knx.KnxAddressBook
import com.onehouse.app.knx.KnxDeviceFactory
import com.onehouse.app.knx.KnxHomeStateRepository
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

/**
 * Creates and restores OneHouse backups using only the SharedPreferences files
 * explicitly supported by the application.
 *
 * Since v1.13.1 the generic backup follows the same KNX whitelist as the live app:
 * legacy InsideControl/importer preferences are not exported and KNX configuration
 * keys are filtered before writing or restoring a backup.
 */
class BackupManager(context: Context) {
    private val appContext = context.applicationContext

    fun createBackup(appVersion: String): String {
        // Forces the silent v1.13 pruning before taking the snapshot. This makes
        // app_project_copy itself clean even if the user has never opened the KNX editor.
        val knxRepository = AppKnxConfigurationRepository(appContext)
        knxRepository.loadProject()
        KnxAddressBook.apply(knxRepository)

        val root = JSONObject()
            .put("format", FORMAT)
            .put("schemaVersion", SCHEMA_VERSION)
            .put("createdAtMillis", System.currentTimeMillis())
            .put("createdAtIso", Instant.now().toString())
            .put("appVersion", appVersion)

        val files = JSONObject()
        BACKUP_PREFERENCES.forEach { name ->
            files.put(
                name,
                encodePreferences(
                    name = name,
                    preferences = appContext.getSharedPreferences(name, Context.MODE_PRIVATE)
                )
            )
        }
        root.put("preferences", files)
        root.put("summary", buildSummary(files, activeKnxAddressCount(knxRepository)))
        return root.toString(2)
    }

    fun preview(rawJson: String): BackupPreview {
        requireValidSize(rawJson)
        val root = JSONObject(rawJson)
        validateRoot(root)
        val files = root.getJSONObject("preferences")
        val summaryJson = root.optJSONObject("summary") ?: buildSummary(files, -1)

        return BackupPreview(
            summary = BackupSummary(
                createdAtMillis = root.getLong("createdAtMillis"),
                appVersion = root.optString("appVersion", "Desconocida"),
                preferencesFiles = summaryJson.optInt("preferencesFiles", files.length()),
                scenes = summaryJson.optInt("scenes", countItems(files, PreferenceFiles.SMART_SCENES, "scene_count", "scene_")),
                automations = summaryJson.optInt("automations", countSet(files, PreferenceFiles.CONDITIONAL_AUTOMATIONS, "rules")),
                weeklySchedules = summaryJson.optInt("weeklySchedules", countSet(files, PreferenceFiles.WEEKLY_SCHEDULE, "events")),
                solarSchedules = summaryJson.optInt("solarSchedules", countSet(files, PreferenceFiles.SOLAR_SCHEDULE, "events")),
                knxEntries = summaryJson.optInt("knxEntries", countObjectEntries(files, PreferenceFiles.KNX_CONFIGURATION)),
                knxActiveAddresses = summaryJson.optInt("knxActiveAddresses", -1)
            ),
            rawJson = rawJson
        )
    }

    fun restore(rawJson: String): BackupOperationResult = runCatching {
        requireValidSize(rawJson)
        val root = JSONObject(rawJson)
        validateRoot(root)
        val files = root.getJSONObject("preferences")

        val decoded = linkedMapOf<String, Map<String, Any>>()
        files.keys().forEach { name ->
            if (name in BACKUP_PREFERENCES) {
                val values = decodePreferences(files.getJSONObject(name))
                decoded[name] = if (name == PreferenceFiles.KNX_CONFIGURATION) {
                    // Old backups may contain automatic_backup or keys that belonged to
                    // historical objects. Never allow those values back into OneHouse.
                    values.filterKeys { AppKnxConfigurationRepository.isSupportedBackupPreferenceKey(it) }
                } else {
                    values
                }
            }
            // INSIDE_CONTROL_IMPORT / IMPORT_PROJECT_UI from schema 1 backups are
            // deliberately ignored. They are import sources, not active OneHouse data.
        }
        require(decoded.isNotEmpty()) { "La copia no contiene datos compatibles con OneHouse." }

        val previous: Map<String, Map<String, Any>> = decoded.keys.associateWith { name ->
            appContext.getSharedPreferences(name, Context.MODE_PRIVATE).all
                .mapNotNull { (key, value) -> value?.let { key to it } }
                .toMap()
        }
        val previousLegacy = PreferenceFiles.legacyImportFiles.associateWith { name ->
            appContext.getSharedPreferences(name, Context.MODE_PRIVATE).all
                .mapNotNull { (key, value) -> value?.let { key to it } }
                .toMap()
        }

        try {
            decoded.forEach { (name, values) -> replacePreferences(name, values) }

            // Once a OneHouse-native KNX configuration has been restored, remove the
            // historical importer source so it can never repopulate deleted objects.
            if (decoded.containsKey(PreferenceFiles.KNX_CONFIGURATION)) {
                PreferenceFiles.legacyImportFiles.forEach { name ->
                    check(appContext.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit()) {
                        "No se pudo limpiar '$name'."
                    }
                }

                val repository = AppKnxConfigurationRepository(appContext)
                repository.loadProject() // applies OneHouseKnxUsagePolicy.prune again
                KnxAddressBook.apply(repository)
            }
        } catch (restoreError: Throwable) {
            val rollbackError = runCatching {
                previous.forEach { (name, values) -> replacePreferences(name, values) }
                previousLegacy.forEach { (name, values) -> replacePreferences(name, values) }
            }.exceptionOrNull()
            if (rollbackError != null) restoreError.addSuppressed(rollbackError)
            throw restoreError
        }

        BackupOperationResult.Success(
            "Restauración completada y configuración KNX depurada. Reinicia OneHouse para recargar toda la configuración."
        )
    }.getOrElse { error ->
        BackupOperationResult.Error(error.message ?: "No se pudo restaurar la copia de seguridad.")
    }

    private fun requireValidSize(rawJson: String) {
        require(rawJson.toByteArray(Charsets.UTF_8).size <= MAX_BACKUP_BYTES) {
            "El archivo supera el tamaño máximo permitido de 5 MB."
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

    private fun encodePreferences(name: String, preferences: SharedPreferences): JSONObject {
        val result = JSONObject()
        preferences.all.forEach { (key, value) ->
            if (name == PreferenceFiles.KNX_CONFIGURATION &&
                !AppKnxConfigurationRepository.isSupportedBackupPreferenceKey(key)
            ) {
                return@forEach
            }

            val item = JSONObject()
            when (value) {
                is String -> item.put("type", "string").put("value", value)
                is Boolean -> item.put("type", "boolean").put("value", value)
                is Int -> item.put("type", "int").put("value", value)
                is Long -> item.put("type", "long").put("value", value.toString())
                is Float -> item.put("type", "float").put("value", value.toDouble())
                is Set<*> -> item.put(
                    "type",
                    "stringSet"
                ).put("value", JSONArray(value.filterIsInstance<String>().sorted()))
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

    private fun replacePreferences(name: String, values: Map<String, Any>) {
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

    private fun buildSummary(files: JSONObject, activeKnxAddresses: Int): JSONObject = JSONObject()
        .put("preferencesFiles", files.length())
        .put("scenes", countItems(files, PreferenceFiles.SMART_SCENES, "scene_count", "scene_"))
        .put("automations", countSet(files, PreferenceFiles.CONDITIONAL_AUTOMATIONS, "rules"))
        .put("weeklySchedules", countSet(files, PreferenceFiles.WEEKLY_SCHEDULE, "events"))
        .put("solarSchedules", countSet(files, PreferenceFiles.SOLAR_SCHEDULE, "events"))
        .put("knxEntries", countObjectEntries(files, PreferenceFiles.KNX_CONFIGURATION))
        .put("knxActiveAddresses", activeKnxAddresses)

    /** Counts exactly the state addresses used by the current initial-load engine. */
    private fun activeKnxAddressCount(repository: AppKnxConfigurationRepository): Int {
        val devices = repository.loadProject()
            ?.let(KnxDeviceFactory::create)
            .orEmpty()
            .filter { it.canRead }

        val importedAddresses = devices.flatMap { device ->
            val stateAddresses = device.readAddresses.map { it.toString() }
            if (stateAddresses.isNotEmpty()) {
                stateAddresses
            } else if (device.controlKind == ControlKind.BOOLEAN_SWITCH) {
                device.writeAddresses.take(1).map { it.toString() }
            } else {
                emptyList()
            }
        }

        return (KnxHomeStateRepository.explicitStateAddresses() + importedAddresses)
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
            .size
    }

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
        const val SCHEMA_VERSION = 2
        const val MAX_BACKUP_BYTES = 5 * 1024 * 1024

        val BACKUP_PREFERENCES = PreferenceFiles.backupFiles
    }
}
