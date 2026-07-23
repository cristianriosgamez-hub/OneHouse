package com.onehouse.app.importer

import android.content.Context
import android.util.Base64

class InsideControlProjectRepository(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun save(project: ImportedKnxProject) {
        val lines = buildList {
            add(listOf("PROJECT", project.projectName, project.builderVersion, project.minimumAppVersion, project.interfaceMacAddress).encode())
            project.rooms.forEach { room ->
                add(listOf("ROOM", room.name, room.iconName).encode())
                room.devices.forEach { device ->
                    add(
                        listOf(
                            "DEVICE",
                            room.name,
                            device.name,
                            device.insideControlType.toString(),
                            device.category.name,
                            device.isFavourite.toString(),
                            device.readAddresses.joinToString(","),
                            device.writeAddresses.joinToString(","),
                            device.dataPointType.orEmpty(),
                            device.unit.orEmpty(),
                            device.values.joinToString("\u001F"),
                            device.iconNames.joinToString("\u001F")
                        ).encode()
                    )
                }
            }
        }
        preferences.edit().putString(KEY_PROJECT, lines.joinToString("\n")).apply()
    }

    fun load(): ImportedKnxProject? {
        val stored = preferences.getString(KEY_PROJECT, null) ?: return null
        val decodedLines = stored.lineSequence().mapNotNull(::decode).toList()
        val projectLine = decodedLines.firstOrNull { it.firstOrNull() == "PROJECT" } ?: return null
        val roomLines = decodedLines.filter { it.firstOrNull() == "ROOM" }
        val deviceLines = decodedLines.filter { it.firstOrNull() == "DEVICE" }

        val rooms = roomLines.mapNotNull { room ->
            val name = room.getOrNull(1) ?: return@mapNotNull null
            ImportedKnxRoom(
                name = name,
                iconName = room.getOrNull(2).orEmpty(),
                devices = deviceLines.filter { it.getOrNull(1) == name }.mapNotNull(::decodeDevice)
            )
        }
        return ImportedKnxProject(
            projectName = projectLine.getOrNull(1).orEmpty(),
            builderVersion = projectLine.getOrNull(2).orEmpty(),
            minimumAppVersion = projectLine.getOrNull(3).orEmpty(),
            interfaceMacAddress = projectLine.getOrNull(4).orEmpty(),
            rooms = rooms
        )
    }

    fun clear() {
        preferences.edit().remove(KEY_PROJECT).apply()
    }

    private fun decodeDevice(values: List<String>): ImportedKnxObject? {
        val roomName = values.getOrNull(1) ?: return null
        val name = values.getOrNull(2) ?: return null
        return ImportedKnxObject(
            roomName = roomName,
            name = name,
            insideControlType = values.getOrNull(3)?.toIntOrNull() ?: -1,
            category = values.getOrNull(4)?.let { runCatching { ImportedKnxCategory.valueOf(it) }.getOrNull() }
                ?: ImportedKnxCategory.UNKNOWN,
            isFavourite = values.getOrNull(5).toBoolean(),
            readAddresses = values.getOrNull(6).orEmpty().split(',').filter(String::isNotBlank),
            writeAddresses = values.getOrNull(7).orEmpty().split(',').filter(String::isNotBlank),
            dataPointType = values.getOrNull(8)?.ifBlank { null },
            unit = values.getOrNull(9)?.ifBlank { null },
            values = values.getOrNull(10).orEmpty().split('\u001F').filter(String::isNotBlank),
            iconNames = values.getOrNull(11).orEmpty().split('\u001F').filter(String::isNotBlank)
        )
    }

    private fun List<String>.encode(): String = joinToString("|") { value ->
        Base64.encodeToString(value.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    }

    private fun decode(line: String): List<String>? = try {
        line.split('|').map { value ->
            Base64.decode(value, Base64.NO_WRAP).toString(Charsets.UTF_8)
        }
    } catch (_: IllegalArgumentException) {
        null
    }

    private companion object {
        const val PREFERENCES_NAME = "insidecontrol_import"
        const val KEY_PROJECT = "last_project"
    }
}
