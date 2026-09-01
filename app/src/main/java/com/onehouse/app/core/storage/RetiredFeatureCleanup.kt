package com.onehouse.app.core.storage

import android.app.job.JobScheduler
import android.content.Context

/**
 * Removes data and persisted jobs belonging to features retired in v1.13.3.
 * Safe to execute on every startup.
 */
object RetiredFeatureCleanup {
    private const val LEGACY_HOME_ASSISTANT_SECURITY_JOB_ID = 1607

    fun run(context: Context) {
        val appContext = context.applicationContext
        runCatching {
            appContext.getSystemService(JobScheduler::class.java)
                ?.cancel(LEGACY_HOME_ASSISTANT_SECURITY_JOB_ID)
        }
        PreferenceFiles.retiredFiles.forEach { name ->
            runCatching { appContext.deleteSharedPreferences(name) }
        }
    }
}
