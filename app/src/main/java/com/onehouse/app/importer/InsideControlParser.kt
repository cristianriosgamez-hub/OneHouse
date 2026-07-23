package com.onehouse.app.importer

object InsideControlParser {
    fun parse(source: String): ImportedKnxProject {
        require(source.contains("{BUILDER_VERSION=")) {
            "El contenido no corresponde a un proyecto InsideControl compatible"
        }

        val projectName = topLevelValue(source, "PROJECT_NAME") ?: "Proyecto InsideControl"
        val builderVersion = topLevelValue(source, "BUILDER_VERSION").orEmpty()
        val minimumAppVersion = topLevelValue(source, "MIN_APP_VERSION").orEmpty()
        val macAddress = topLevelValue(source, "MAC_ADDRESS").orEmpty()
        val roomsBlock = topLevelValue(source, "ROOMS")
            ?: throw IllegalArgumentException("El proyecto no contiene habitaciones")

        val rooms = parseRooms(roomsBlock)
        val declaredRooms = topLevelValue(source, "NUM_OF_ROOMS")?.toIntOrNull()
        if (declaredRooms != null && declaredRooms != rooms.size) {
            throw IllegalArgumentException(
                "El proyecto declara $declaredRooms habitaciones, pero se han podido leer ${rooms.size}"
            )
        }

        return ImportedKnxProject(
            projectName = projectName,
            builderVersion = builderVersion,
            minimumAppVersion = minimumAppVersion,
            interfaceMacAddress = macAddress,
            rooms = rooms
        )
    }

    private fun parseRooms(block: String): List<ImportedKnxRoom> {
        return block
            .split("[ROOM_NAME=")
            .drop(1)
            .mapNotNull { segment -> parseRoom(segment) }
    }

    private fun parseRoom(segment: String): ImportedKnxRoom? {
        val roomName = segment.substringBefore(']').trim()
        if (roomName.isBlank()) return null

        val icon = bracketValue(segment, "ROOM_ICON").orEmpty()
        val devicesBlock = segment.substringAfter("[DEVICES=", missingDelimiterValue = "")
            .substringBeforeLast("/]", missingDelimiterValue = "")
        val devices = parseDevices(roomName, devicesBlock)

        val declaredDevices = bracketValue(segment, "NUM_OF_DEVICES")?.toIntOrNull()
        if (declaredDevices != null && declaredDevices != devices.size) {
            throw IllegalArgumentException(
                "La habitación '$roomName' declara $declaredDevices dispositivos, pero se han leído ${devices.size}"
            )
        }

        return ImportedKnxRoom(
            name = roomName,
            iconName = icon,
            devices = devices
        )
    }

    private fun parseDevices(roomName: String, block: String): List<ImportedKnxObject> {
        return block
            .split("/>")
            .map { value -> value.trim() }
            .filter { value -> value.isNotBlank() }
            .mapNotNull { parseDevice(roomName, it) }
    }

    private fun parseDevice(roomName: String, block: String): ImportedKnxObject? {
        val fields = FIELD_PATTERN.findAll(block).associate { match ->
            match.groupValues[1] to match.groupValues[2].trimEnd('/')
        }
        val name = fields["DEVICE_NAME"]?.trim().orEmpty()
        if (name.isBlank()) return null

        val type = fields["DEVICE_TYPE"]?.toIntOrNull() ?: -1
        val readAddresses = indexedValues(fields, "DEVICE_READ_ADD")
            .mapNotNull { address -> convertAddress(address) }
        val writeAddresses = indexedValues(fields, "DEVICE_WRITE_ADD")
            .mapNotNull { address -> convertAddress(address) }
        val values = indexedValues(fields, "DEVICE_VAL")
        val icons = indexedValues(fields, "DEVICE_ICON_")
        val explicitDpt = values.firstOrNull { DPT_PATTERN.matches(it) }
        val unit = values.dropWhile { it != explicitDpt }.drop(1).firstOrNull()
            ?: values.firstOrNull { it.any(Char::isLetter) || it.contains('º') }

        return ImportedKnxObject(
            roomName = roomName,
            name = name,
            insideControlType = type,
            category = categoryFor(type, name),
            isFavourite = fields["DEVICE_ISFAVOURITE"]?.toBooleanStrictOrNull() ?: false,
            readAddresses = readAddresses.distinct(),
            writeAddresses = writeAddresses.distinct(),
            dataPointType = explicitDpt ?: inferredDpt(type),
            unit = unit?.takeUnless { DPT_PATTERN.matches(it) },
            values = values,
            iconNames = icons
        )
    }

    private fun indexedValues(fields: Map<String, String>, prefix: String): List<String> {
        return fields.entries
            .filter { (key, value) -> key.startsWith(prefix) && value.isNotBlank() }
            .sortedBy { (key, _) -> key.removePrefix(prefix).toIntOrNull() ?: Int.MAX_VALUE }
            .map { it.value }
    }

    private fun convertAddress(raw: String): String? {
        val parts = raw.trim().split(':')
        if (parts.size != 2) return null
        val highByte = parts[0].toIntOrNull() ?: return null
        val subgroup = parts[1].toIntOrNull() ?: return null
        if (highByte !in 0..255 || subgroup !in 0..255) return null

        val mainGroup = highByte shr 3
        val middleGroup = highByte and 0x07
        return "$mainGroup/$middleGroup/$subgroup"
    }

    private fun categoryFor(type: Int, name: String): ImportedKnxCategory = when (type) {
        1 -> ImportedKnxCategory.LIGHT
        6 -> ImportedKnxCategory.BLIND
        8 -> ImportedKnxCategory.SCENE
        10, 16, 20, 48 -> ImportedKnxCategory.CLIMATE
        38, 39, 41 -> ImportedKnxCategory.ALARM
        61, 67 -> ImportedKnxCategory.SENSOR
        64 -> when {
            name.contains("temp", ignoreCase = true) -> ImportedKnxCategory.TEMPERATURE
            name.contains("kwh", ignoreCase = true) || name.contains("acs", ignoreCase = true) -> ImportedKnxCategory.METER
            else -> ImportedKnxCategory.SENSOR
        }
        7, 32 -> ImportedKnxCategory.SWITCH
        else -> ImportedKnxCategory.UNKNOWN
    }

    private fun inferredDpt(type: Int): String? = when (type) {
        1, 20, 32, 38, 39, 41 -> "1.001"
        6 -> "1.008 / 1.007"
        7 -> "1.001"
        8 -> "17.001"
        10 -> "9.001"
        else -> null
    }

    private fun topLevelValue(source: String, key: String): String? {
        val marker = "{$key="
        val start = source.indexOf(marker)
        if (start < 0) return null
        val valueStart = start + marker.length
        var depth = 0
        for (index in valueStart until source.length) {
            when (source[index]) {
                '{' -> depth++
                '}' -> if (depth == 0) return source.substring(valueStart, index) else depth--
            }
        }
        return null
    }

    private fun bracketValue(source: String, key: String): String? {
        val marker = "[$key="
        val start = source.indexOf(marker)
        if (start < 0) return null
        val valueStart = start + marker.length
        val end = source.indexOf(']', valueStart)
        return if (end >= 0) source.substring(valueStart, end) else null
    }

    private val FIELD_PATTERN = Regex("<([A-Z0-9_]+)=([^<>]*)>")
    private val DPT_PATTERN = Regex("\\d{1,3}\\.\\d{3}")
}
