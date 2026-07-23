package com.onehouse.app.device

import com.onehouse.app.importer.ImportedKnxCategory
import com.onehouse.app.importer.ImportedKnxObject
import com.onehouse.app.knx.KnxGroupAddress

/**
 * Representación normalizada de un objeto procedente de InsideControl.
 *
 * Esta clase no abre una conexión KNX: describe qué control podrá construir
 * OneHouse y deja las direcciones ya validadas para las siguientes entregas.
 */
data class ImportedKnxDevice(
    val id: String,
    val roomName: String,
    val name: String,
    val category: ImportedKnxCategory,
    val controlKind: ControlKind,
    val writeAddresses: List<KnxGroupAddress>,
    val readAddresses: List<KnxGroupAddress>,
    val dataPointType: String?,
    val unit: String?,
    val isFavourite: Boolean,
    val source: ImportedKnxObject
) {
    val primaryWriteAddress: KnxGroupAddress?
        get() = writeAddresses.firstOrNull()

    val primaryReadAddress: KnxGroupAddress?
        get() = readAddresses.firstOrNull()

    val canWrite: Boolean
        get() = writeAddresses.isNotEmpty()

    val canRead: Boolean
        get() = readAddresses.isNotEmpty()
}

enum class ControlKind(val displayName: String) {
    BOOLEAN_SWITCH("Interruptor"),
    BLIND("Persiana"),
    TEMPERATURE("Temperatura"),
    CLIMATE("Climatización"),
    SCENE("Escena"),
    ALARM("Alarma"),
    SENSOR("Sensor"),
    METER("Medición"),
    READ_ONLY("Solo lectura"),
    UNKNOWN("Sin determinar")
}
