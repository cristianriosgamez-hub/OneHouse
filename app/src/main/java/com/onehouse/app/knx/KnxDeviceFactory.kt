package com.onehouse.app.knx

import com.onehouse.app.device.ControlKind
import com.onehouse.app.device.ImportedKnxDevice
import com.onehouse.app.importer.ImportedKnxCategory
import com.onehouse.app.importer.ImportedKnxObject
import com.onehouse.app.importer.ImportedKnxProject

/** Convierte objetos importados en descriptores KNX listos para construir comandos. */
object KnxDeviceFactory {
    fun create(project: ImportedKnxProject): List<ImportedKnxDevice> =
        project.devices.mapIndexed { index, source -> create(index, source) }

    fun create(index: Int, source: ImportedKnxObject): ImportedKnxDevice {
        val readAddresses = source.readAddresses.mapNotNull(::parseAddress)
        val importedWriteAddresses = source.writeAddresses.mapNotNull(::parseAddress)
        val writeAddresses = importedWriteAddresses.ifEmpty {
            inferWriteAddresses(source, readAddresses)
        }
        val kind = controlKindFor(source, writeAddresses.isNotEmpty())
        val resolvedDpt = KnxDptResolver.resolve(source, kind)
        val stableAddress = (source.writeAddresses + source.readAddresses).firstOrNull().orEmpty()

        val provisional = ImportedKnxDevice(
            id = listOf(source.roomName, source.name, source.insideControlType, stableAddress, index)
                .joinToString("|"),
            roomName = source.roomName,
            name = source.name,
            category = source.category,
            controlKind = kind,
            writeAddresses = writeAddresses,
            readAddresses = readAddresses,
            dataPointType = source.dataPointType,
            resolvedDpt = resolvedDpt,
            unit = source.unit,
            isFavourite = source.isFavourite,
            commands = emptyList(),
            source = source
        )
        return provisional.copy(commands = KnxCommandBuilder.build(provisional))
    }

    private fun parseAddress(raw: String): KnxGroupAddress? =
        runCatching { KnxGroupAddress.parse(raw) }.getOrNull()

    /**
     * InsideControl puede exportar únicamente la dirección de estado de una luz.
     * En las instalaciones analizadas, el patrón ETS es:
     *
     *  - 1/1/x: mando ON/OFF
     *  - 1/2/x: estado ON/OFF
     *
     * Solo aplicamos esta inferencia a luces e interruptores sin ninguna dirección
     * de escritura importada. El resto de categorías conserva el comportamiento
     * de solo lectura para evitar escrituras sobre direcciones de estado.
     */
    private fun inferWriteAddresses(
        source: ImportedKnxObject,
        readAddresses: List<KnxGroupAddress>
    ): List<KnxGroupAddress> {
        if (source.category != ImportedKnxCategory.LIGHT &&
            source.category != ImportedKnxCategory.SWITCH
        ) {
            return emptyList()
        }

        return readAddresses.mapNotNull { readAddress ->
            when {
                readAddress.main == 1 && readAddress.middle == 2 ->
                    KnxGroupAddress.of(
                        main = readAddress.main,
                        middle = 1,
                        sub = readAddress.sub
                    )

                else -> null
            }
        }.distinct()
    }

    private fun controlKindFor(source: ImportedKnxObject, hasWriteAddress: Boolean): ControlKind {
        if (!hasWriteAddress && source.readAddresses.isNotEmpty()) return when (source.category) {
            ImportedKnxCategory.TEMPERATURE -> ControlKind.TEMPERATURE
            ImportedKnxCategory.METER -> ControlKind.METER
            ImportedKnxCategory.ALARM -> ControlKind.ALARM
            ImportedKnxCategory.SENSOR -> ControlKind.SENSOR
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
