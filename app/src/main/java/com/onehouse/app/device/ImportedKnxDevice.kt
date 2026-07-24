package com.onehouse.app.device

import com.onehouse.app.importer.ImportedKnxCategory
import com.onehouse.app.importer.ImportedKnxObject
import com.onehouse.app.knx.KnxCommand
import com.onehouse.app.knx.KnxCommandType
import com.onehouse.app.knx.KnxGroupAddress

/** Representación normalizada de un objeto procedente de InsideControl. */
data class ImportedKnxDevice(
    val id: String,
    val roomName: String,
    val name: String,
    val category: ImportedKnxCategory,
    val controlKind: ControlKind,
    val writeAddresses: List<KnxGroupAddress>,
    val readAddresses: List<KnxGroupAddress>,
    val dataPointType: String?,
    val resolvedDpt: String,
    val unit: String?,
    val isFavourite: Boolean,
    val commands: List<KnxCommand>,
    val source: ImportedKnxObject
) {
    val primaryWriteAddress: KnxGroupAddress?
        get() = writeAddresses.firstOrNull()

    val primaryReadAddress: KnxGroupAddress?
        get() = readAddresses.firstOrNull()

    val canWrite: Boolean
        get() = commands.any { it.type != KnxCommandType.READ }

    val canRead: Boolean
        get() = commands.any { it.type == KnxCommandType.READ }

    val iconGlyph: String
        get() = when (controlKind) {
            ControlKind.BOOLEAN_SWITCH -> if (category == ImportedKnxCategory.LIGHT) "💡" else "🔌"
            ControlKind.BLIND -> "🪟"
            ControlKind.TEMPERATURE -> "🌡"
            ControlKind.CLIMATE -> "❄"
            ControlKind.SCENE -> "▶"
            ControlKind.ALARM -> "⚠"
            ControlKind.SENSOR -> "◉"
            ControlKind.METER -> "⚡"
            ControlKind.READ_ONLY -> "◌"
            ControlKind.UNKNOWN -> "◆"
        }
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
