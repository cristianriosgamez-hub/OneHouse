package com.onehouse.app.feature.backup

data class BackupSummary(
    val createdAtMillis: Long,
    val appVersion: String,
    val preferencesFiles: Int,
    val scenes: Int,
    val automations: Int,
    val weeklySchedules: Int,
    val solarSchedules: Int,
    val knxEntries: Int,
    val knxActiveAddresses: Int
)

data class BackupPreview(
    val summary: BackupSummary,
    val rawJson: String
)

sealed interface BackupOperationResult {
    data class Success(val message: String) : BackupOperationResult
    data class Error(val message: String) : BackupOperationResult
}
