package com.onehouse.app.feature.security

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import java.util.concurrent.Executors

class HomeAssistantSecurityJobService : JobService() {
    private val executor = Executors.newSingleThreadExecutor()

    override fun onStartJob(params: JobParameters?): Boolean {
        val jobParams = params ?: return false
        executor.execute {
            val settings = HomeAssistantSettingsStore(applicationContext).read()
            if (
                settings.lastStatus == HomeAssistantConnectionStatus.CONNECTED &&
                settings.baseUrl.isNotBlank() &&
                settings.accessToken.isNotBlank()
            ) {
                when (val result = HomeAssistantSecurityClient.fetchBlocking(settings.baseUrl, settings.accessToken)) {
                    is HomeAssistantSecurityClient.Result.Success -> {
                        HomeAssistantSecuritySnapshotStore(applicationContext).write(result.snapshot)
                        val processed = HomeAssistantSecurityEventStore(applicationContext).process(result.snapshot)
                        val monitoringPreferences = HomeAssistantSecurityMonitoringPreferences(applicationContext)
                        HomeAssistantSecurityNotificationManager(applicationContext)
                            .notify(monitoringPreferences.filterNotificationEvents(processed.newEvents))
                    }
                    is HomeAssistantSecurityClient.Result.Failure -> Unit
                }
            }
            jobFinished(jobParams, false)
        }
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean = true
}

object HomeAssistantSecurityBackgroundScheduler {
    private const val JOB_ID = 1607
    private const val INTERVAL_MILLIS = 15L * 60L * 1000L

    fun sync(context: Context) {
        val notificationManager = HomeAssistantSecurityNotificationManager(context.applicationContext)
        setEnabled(context, notificationManager.enabled && notificationManager.hasPermission())
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        val scheduler = context.getSystemService(JobScheduler::class.java)
        if (!enabled) {
            scheduler.cancel(JOB_ID)
            return
        }

        val job = JobInfo.Builder(
            JOB_ID,
            ComponentName(context, HomeAssistantSecurityJobService::class.java)
        )
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setPersisted(true)
            .setPeriodic(INTERVAL_MILLIS)
            .build()

        scheduler.schedule(job)
    }
}
