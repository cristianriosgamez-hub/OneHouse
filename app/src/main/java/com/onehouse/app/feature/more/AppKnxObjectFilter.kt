package com.onehouse.app.feature.more

import com.onehouse.app.importer.ImportedKnxCategory
import com.onehouse.app.importer.ImportedKnxObject

/**
 * Delimita los objetos KNX que realmente tienen representación en la interfaz OneHouse.
 * El importador puede contener escenas, objetos auxiliares y elementos de la app antigua
 * que no deben aparecer en el editor ni en el diagnóstico de OneHouse.
 */
internal object AppKnxObjectFilter {
    private val visibleRooms = setOf(
        "Entrada", "Pasillo", "Trastero", "Baño", "Cocina",
        "Habitación 1", "Comedor", "Suite", "Terraza", "Climatización",
        "Mantenimiento"
    )

    private val visibleCategories = setOf(
        ImportedKnxCategory.LIGHT,
        ImportedKnxCategory.BLIND,
        ImportedKnxCategory.CLIMATE,
        ImportedKnxCategory.TEMPERATURE,
        ImportedKnxCategory.SENSOR,
        ImportedKnxCategory.ALARM,
        ImportedKnxCategory.SWITCH,
        ImportedKnxCategory.METER
    )

    fun isVisible(roomName: String, device: ImportedKnxObject): Boolean {
        val normalizedRoom = roomName.trim()
        if (normalizedRoom !in visibleRooms) return false
        if (device.category !in visibleCategories) return false

        val text = "${device.name} ${device.unit.orEmpty()} ${device.dataPointType.orEmpty()}"
            .lowercase()

        // Estos sensores tienen una única fuente KNX definida en KnxAddressBook.
        // Las copias importadas de InsideControl y las tarjetas de Mantenimiento
        // serían duplicados del mismo estado y se ocultan del editor/diagnóstico.
        val duplicatedAlarm = when (normalizedRoom) {
            "Cocina", "Baño" -> device.category == ImportedKnxCategory.ALARM && text.contains("inund")
            "Pasillo" -> device.category == ImportedKnxCategory.ALARM && text.contains("incend")
            "Mantenimiento" -> device.category == ImportedKnxCategory.ALARM &&
                (text.contains("inund") || text.contains("incend"))
            else -> false
        }
        if (duplicatedAlarm) return false

        // Evita objetos auxiliares típicos de InsideControl que no se muestran en OneHouse.
        val excludedTokens = listOf(
            "icono", "texto", "etiqueta", "navigation", "navegación",
            "background", "fondo", "dummy", "test", "escena"
        )
        return excludedTokens.none(text::contains)
    }
}
