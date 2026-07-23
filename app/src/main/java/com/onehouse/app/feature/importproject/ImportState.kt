package com.onehouse.app.feature.importproject

import com.onehouse.app.importer.ImportedKnxProject

sealed interface ImportState {
    data object Idle : ImportState
    data object SelectingFile : ImportState
    data class Importing(val fileName: String) : ImportState
    data class Success(
        val project: ImportedKnxProject,
        val fileName: String,
        val message: String
    ) : ImportState
    data class Error(val message: String) : ImportState
}
