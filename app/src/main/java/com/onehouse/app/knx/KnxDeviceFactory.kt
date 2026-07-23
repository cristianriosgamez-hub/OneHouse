package com.onehouse.app.knx

import com.onehouse.app.device.ControlKind
import com.onehouse.app.device.ImportedKnxDevice
import com.onehouse.app.importer.ImportedKnxCategory
import com.onehouse.app.importer.ImportedKnxObject
import com.onehouse.app.importer.ImportedKnxProject

/** Convierte objetos importados en descriptores de dispositivo utilizables por OneHouse. */
object KnxDeviceFactory {
    fun create(project: ImportedKnxProject): List<ImportedKnxDevice> =
        project.devices.mapIndexed { index, source -> create(index, source) }

    fun create(index: Int, source: ImportedKnxObject): ImportedKnxDevice {
        val writeAddresses = source.writeAddresses.mapNotNull(::parseAddress)
        val readAddresses = source.readAddresses.mapNotNull(::parseAddress)
        val kind = controlKindFor(source, writeAddresses.isNotEmpty())
        val stableAddress = (source.writeAddresses + source.readAddresses).firstOrNull().orEmpty()

        return ImportedKnxDevice(
            id = listOf(source.roomName, source.name, source.insideControlType, stableAddress, index)
                .joinToString("|"),
            roomName = source.roomName,
            name = source.name,
            category = source.category,
            controlKind = kind,
            writeAddresses = writeAddresses,
            readAddresses = readAddresses,
            dataPointType = source.dataPointType,
            unit = source.unit,
            isFavourite = source.isFavourite,
            source = source
        )
    }

    private fun parseAddress(raw: String): KnxGroupAddress? =
        runCatching { KnxGroupAddress.parse(raw) }.getOrNull()

    private fun controlKindFor(source: ImportedKnxObject, hasWriteAddress: Boolean): ControlKind {
        if (!hasWriteAddress && source.readAddresses.isNotEmpty()) return when (source.category) {
            ImportedKnxCategory.TEMPERATURE -> ControlKind.TEMPERATURE
            ImportedKnxCategory.METER -> ControlKind.METER
            ImportedKnxCategory.ALARM -> ControlKind.ALARM
            else -> ControlKind.READ_ONLY
        }

        return when (source.category) {
            ImportedKnxCategory.LIGHT,
            ImportedKnxCategory.SWITCH -> ControlKind.BOOLEAN_SWITCH
            ImportedKnxCategory.BLIND -> ControlKind.BLIND
            ImportedKnxCategory.CLIMATE -> ControlKind.CLIMATE
            ImportedKnxCategory.TEMPERATURE -> ControlKind.TEMPERATURE
            ImportedKnxCategory.SCENE -> ControlKind.SCENE
            ImportedKnxCategory.ALARM -> ControlKind.ALARM
            ImportedKnxCategory.SENSOR -> ControlKind.SENSOR
            ImportedKnxCategory.METER -> ControlKind.METER
            ImportedKnxCategory.UNKNOWN -> ControlKind.UNKNOWN
        }
    }
}
