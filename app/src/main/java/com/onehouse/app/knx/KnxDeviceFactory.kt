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
     * Reconstruye las direcciones de mando que InsideControl puede omitir.
     *
     * Patrones comprobados en proyectos ETS reales:
     *
     *  - Luces:       1/1/n mando, 1/2/n estado.
     *  - Persianas:   2/1/n mando, 2/2/n estado de posición.
     *  - Clima:       5/2/n mando, 5/3/n estado.
     *
     * La inferencia solo se usa cuando el proyecto importado no incluye ninguna
     * dirección de escritura. Las direcciones explícitas siempre tienen prioridad.
     */
    private fun inferWriteAddresses(
        source: ImportedKnxObject,
        readAddresses: List<KnxGroupAddress>
    ): List<KnxGroupAddress> = when (source.category) {
        ImportedKnxCategory.LIGHT,
        ImportedKnxCategory.SWITCH -> inferParallelMiddleGroup(
            readAddresses = readAddresses,
            expectedMain = 1,
            readMiddle = 2,
            writeMiddle = 1
        )

        ImportedKnxCategory.BLIND -> inferBlindWriteAddresses(readAddresses)

        ImportedKnxCategory.CLIMATE -> inferParallelMiddleGroup(
            readAddresses = readAddresses,
            expectedMain = 5,
            readMiddle = 3,
            writeMiddle = 2
        )

        else -> emptyList()
    }

    private fun inferParallelMiddleGroup(
        readAddresses: List<KnxGroupAddress>,
        expectedMain: Int,
        readMiddle: Int,
        writeMiddle: Int
    ): List<KnxGroupAddress> = readAddresses.mapNotNull { readAddress ->
        if (readAddress.main == expectedMain && readAddress.middle == readMiddle) {
            KnxGroupAddress.of(
                main = expectedMain,
                middle = writeMiddle,
                sub = readAddress.sub
            )
        } else {
            null
        }
    }.distinct()

    /**
     * En el esquema de persianas, la dirección de estado de altura mantiene el
     * mismo subgrupo que la escritura de posición. Los dos subgrupos anteriores
     * corresponden a movimiento Up/Down y Stop, respectivamente.
     *
     * Ejemplo: estado 2/2/9 -> movimiento 2/1/7, stop 2/1/8, posición 2/1/9.
     */
    private fun inferBlindWriteAddresses(
        readAddresses: List<KnxGroupAddress>
    ): List<KnxGroupAddress> {
        val positionState = readAddresses.firstOrNull { address ->
            address.main == 2 && address.middle == 2 && address.sub >= 3
        } ?: return emptyList()

        return listOf(
            KnxGroupAddress.of(2, 1, positionState.sub - 2),
            KnxGroupAddress.of(2, 1, positionState.sub - 1),
            KnxGroupAddress.of(2, 1, positionState.sub)
        )
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
