package com.onehouse.app.feature.more

import com.onehouse.app.importer.ImportedKnxObject
import com.onehouse.app.knx.OneHouseKnxUsagePolicy

/** El editor/diagnóstico muestra exactamente los objetos importados que usa OneHouse. */
internal object AppKnxObjectFilter {
    fun isVisible(roomName: String, device: ImportedKnxObject): Boolean =
        roomName.trim().equals(device.roomName.trim(), ignoreCase = true) &&
            OneHouseKnxUsagePolicy.isUsedImportedObject(device)
}
