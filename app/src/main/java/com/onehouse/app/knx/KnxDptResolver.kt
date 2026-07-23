package com.onehouse.app.knx

import com.onehouse.app.device.ControlKind
import com.onehouse.app.importer.ImportedKnxCategory
import com.onehouse.app.importer.ImportedKnxObject

/** Resuelve un DPT utilizable incluso cuando InsideControl no lo guardó explícitamente. */
object KnxDptResolver {
    fun resolve(source: ImportedKnxObject, controlKind: ControlKind): String {
        normalize(source.dataPointType)?.let { return it }

        return when (controlKind) {
            ControlKind.BOOLEAN_SWITCH -> "1.001"
            ControlKind.BLIND -> "1.008"
            ControlKind.TEMPERATURE -> "9.001"
            ControlKind.CLIMATE -> when (source.category) {
                ImportedKnxCategory.TEMPERATURE -> "9.001"
                else -> "20.102"
            }
            ControlKind.SCENE -> "17.001"
            ControlKind.ALARM,
            ControlKind.SENSOR -> "1.005"
            ControlKind.METER -> "14.000"
            ControlKind.READ_ONLY,
            ControlKind.UNKNOWN -> "desconocido"
        }
    }

    fun mainNumber(dpt: String): Int? = normalize(dpt)
        ?.substringBefore('.')
        ?.toIntOrNull()

    private fun normalize(value: String?): String? {
        val trimmed = value?.trim()?.removePrefix("DPT")?.trim()?.replace(',', '.')
        if (trimmed.isNullOrBlank()) return null

        val match = Regex("(\\d{1,3})(?:\\.(\\d{1,3}))?").find(trimmed) ?: return null
        val main = match.groupValues[1].toIntOrNull() ?: return null
        val sub = match.groupValues.getOrNull(2)?.takeIf { it.isNotBlank() }?.toIntOrNull()
        return if (sub == null) main.toString() else "%d.%03d".format(main, sub)
    }
}
