package com.onehouse.app.feature.climate

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager

/**
 * Punto de entrada de las alarmas exactas de climatización.
 *
 * La ejecución real se delega a un foreground service de corta duración. De
 * este modo el envío KNX no queda limitado por la ventana de ejecución de un
 * BroadcastReceiver cuando Android/HyperOS lleva tiempo con OneHouse dormida.
 */
class ClimateScheduleAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        val eventId = intent.getLongExtra(EXTRA_EVENT_ID, -1L)
        if (eventId <= 0L) {
            ClimateBackgroundScheduler(appContext).reschedule()
            return
        }

        val serviceIntent = Intent(appContext, ClimateScheduleExecutionService::class.java)
            .putExtra(ClimateScheduleExecutionService.EXTRA_EVENT_ID, eventId)

        val serviceStarted = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                appContext.startForegroundService(serviceIntent)
            } else {
                appContext.startService(serviceIntent)
            }
        }.isSuccess

        // Fallback defensivo para dispositivos que impidan iniciar el FGS desde
        // segundo plano pese a venir de una alarma exacta.
        if (!serviceStarted) {
            executeInsideReceiver(appContext, eventId)
        }
    }

    private fun executeInsideReceiver(context: Context, eventId: Long) {
        val pendingResult = goAsync()
        val wakeLock = runCatching {
            context.getSystemService(PowerManager::class.java)
                .newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "OneHouse:ClimateScheduleFallback"
                )
                .apply { acquire(FALLBACK_WAKE_LOCK_TIMEOUT_MILLIS) }
        }.getOrNull()

        val finishOnce = object {
            private var finished = false

            @Synchronized
            fun finish() {
                if (finished) return
                finished = true
                runCatching {
                    if (wakeLock?.isHeld == true) wakeLock.release()
                }
                pendingResult.finish()
            }
        }

        runCatching {
            ClimateScheduleKnxCommandRunner(context).execute(eventId) {
                finishOnce.finish()
            }
        }.onFailure {
            ClimateBackgroundScheduler(context).reschedule()
            finishOnce.finish()
        }
    }

    companion object {
        const val EXTRA_EVENT_ID = "climate_schedule_event_id"
        private const val FALLBACK_WAKE_LOCK_TIMEOUT_MILLIS = 60_000L
    }
}
