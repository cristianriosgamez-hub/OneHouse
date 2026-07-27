package com.onehouse.app.feature.security

import android.content.Context

class HomeAssistantSecuritySnapshotStore(context: Context) {
    private val preferences = context.getSharedPreferences("onehouse_security_snapshot", Context.MODE_PRIVATE)

    fun write(snapshot: HomeAssistantSecuritySnapshot) {
        preferences.edit()
            .putInt("open_count", snapshot.openings.count { it.available && it.active })
            .putInt("motion_count", snapshot.motions.count { it.available && it.active })
            .putInt("entity_count", snapshot.openings.size + snapshot.motions.size)
            .putInt("camera_count", snapshot.cameras.size)
            .putLong("updated_at", snapshot.fetchedAtEpochMillis)
            .apply()
    }

    fun readSummary(): SecuritySummary = SecuritySummary(
        openCount = preferences.getInt("open_count", 0),
        motionCount = preferences.getInt("motion_count", 0),
        entityCount = preferences.getInt("entity_count", 0),
        updatedAt = preferences.getLong("updated_at", 0L)
    )
}

data class SecuritySummary(
    val openCount: Int,
    val motionCount: Int,
    val entityCount: Int,
    val updatedAt: Long
)
