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

        /*
         * Algunos proyectos antiguos de InsideControl guardan luces e interruptores
         * binarios únicamente en DEVICE_READ_ADD, aunque la misma dirección de grupo
         * sea la que la aplicación original utilizaba también para escribir.
         *
         * Sin esta compatibilidad el dispositivo se clasificaba como "Solo lectura",
         * no se construían comandos ON/OFF y, por tanto, nunca podía generarse un
         * GroupValueWrite. Para controles inequívocamente escribibles reutilizamos la
         * primera dirección de lectura cuando no existe ninguna dirección de escritura.
         */
        val writeAddresses = importedWriteAddresses.ifEmpty {
            inferredLegacyWriteAddresses(source, readAddresses)
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
     * Compatibilidad con exportaciones antiguas de InsideControl.
     *
     * La inferencia se limita a categorías que representan controles y nunca se aplica
     * a sensores, temperaturas, contadores o alarmas. Se reutiliza una sola dirección
     * para evitar convertir accidentalmente una dirección auxiliar de estado en mando.
     */
    private fun inferredLegacyWriteAddresses(
        source: ImportedKnxObject,
        readAddresses: List<KnxGroupAddress>
    ): List<KnxGroupAddress> {
        if (readAddresses.isEmpty()) return emptyList()

        val categoryCanWrite = when (source.category) {
            ImportedKnxCategory.LIGHT,
            ImportedKnxCategory.SWITCH,
            ImportedKnxCategory.BLIND,
            ImportedKnxCategory.SCENE -> true

            ImportedKnxCategory.CLIMATE,
            ImportedKnxCategory.TEMPERATURE,
            ImportedKnxCategory.SENSOR,
            ImportedKnxCategory.ALARM,
            ImportedKnxCategory.METER,
            ImportedKnxCategory.UNKNOWN -> false
        }

        return if (categoryCanWrite) listOf(readAddresses.first()) else emptyList()
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
