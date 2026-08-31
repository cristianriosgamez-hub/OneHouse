package com.onehouse.app.knx

import com.onehouse.app.importer.ImportedKnxCategory
import com.onehouse.app.importer.ImportedKnxObject
import com.onehouse.app.importer.ImportedKnxProject

/**
 * Fuente única de verdad de los objetos importados que OneHouse utiliza realmente.
 *
 * InsideControl puede contener objetos históricos, sensores duplicados y controles
 * que ya no existen en la interfaz actual. Esos objetos no deben entrar en el
 * modelo de OneHouse, no deben leerse del bus y tampoco deben viajar en el backup.
 *
 * Los estados globales que OneHouse usa directamente (clima, sondas, alarmas y
 * sensores de terraza) están definidos en [KnxAddressBook], por lo que no se
 * conservan copias importadas de esos mismos objetos.
 */
object OneHouseKnxUsagePolicy {

    fun prune(project: ImportedKnxProject): ImportedKnxProject = project.copy(
        rooms = project.rooms.mapNotNull { room ->
            val devices = room.devices.filter(::isUsedImportedObject)
            if (devices.isEmpty()) null else room.copy(devices = devices)
        }
    )

    fun isUsedImportedObject(device: ImportedKnxObject): Boolean {
        val room = normalize(device.roomName)
        val name = normalize(device.name)

        return when (room) {
            "entrada" -> isLight(device) && matchesAny(name, "luz", "entrada")
            "pasillo" -> isLight(device) && matchesAny(name, "luz", "pasillo")
            "trastero" -> isLight(device) && matchesAny(name, "luz", "trastero")
            "bano" -> isLight(device) && matchesAny(name, "luz", "bano")

            "cocina" -> when {
                device.category == ImportedKnxCategory.BLIND -> true
                !isLight(device) -> false
                matchesAny(name, "vitro", "encimera") -> false
                else -> matchesAny(name, "luz", "fluorescente", "cocina")
            }

            "habitacion 1" -> when {
                device.category == ImportedKnxCategory.BLIND -> true
                !isLight(device) -> false
                name.contains("lampara") -> false
                else -> matchesAny(name, "luz", "mesita", "cabecero", "techo", "habitacion")
            }

            "comedor" -> when {
                device.category == ImportedKnxCategory.BLIND -> true
                !isLight(device) -> false
                else -> matchesAny(name, "luz", "salon", "comedor", "lampara")
            }

            "suite" -> when {
                device.category == ImportedKnxCategory.BLIND -> true
                !isLight(device) -> false
                else -> matchesAny(name, "luz", "mesita", "cabecero", "techo", "suite")
            }

            // En Terraza, KNX controla únicamente la luz exterior mediante el
            // objeto importado. La meteorología procede de Open-Meteo y los cuatro
            // sensores KNX visibles usan KnxAddressBook, sin duplicados importados.
            "terraza" -> isLight(device) && matchesAny(name, "luz", "exterior", "terraza")

            // Climatización usa exclusivamente KnxAddressBook. Mantenimiento ejecuta
            // acciones sobre los dispositivos ya conservados, no objetos propios de
            // una estancia histórica. Consumos y Meteo tampoco importan objetos KNX.
            else -> false
        }
    }

    private fun isLight(device: ImportedKnxObject): Boolean =
        device.category == ImportedKnxCategory.LIGHT

    private fun matchesAny(value: String, vararg tokens: String): Boolean =
        tokens.any(value::contains)

    private fun normalize(value: String): String = value
        .lowercase()
        .replace("á", "a")
        .replace("é", "e")
        .replace("í", "i")
        .replace("ó", "o")
        .replace("ú", "u")
        .replace("ñ", "n")
        .trim()
}
