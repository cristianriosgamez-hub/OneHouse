package com.onehouse.app.feature.backup

data class BackupSummary(
    val createdAtMillis: Long,
    val appVersion: String,
    val preferencesFiles: Int,
    val knxEntries: Int,
    val knxActiveAddresses: Int
)

data class BackupValidation(
    val schemaVersion: Int,
    val compatiblePreferenceFiles: Int,
    val ignoredPreferenceFiles: Int,
    val ignoredKnxEntries: Int,
    val warnings: List<String>
) {
    val isClean: Boolean get() = warnings.isEmpty()
}

data class BackupPreview(
    val summary: BackupSummary,
    val validation: BackupValidation,
    val rawJson: String
)

sealed interface BackupOperationResult {
    data class Success(val message: String) : BackupOperationResult
    data class Error(val message: String) : BackupOperationResult
}
