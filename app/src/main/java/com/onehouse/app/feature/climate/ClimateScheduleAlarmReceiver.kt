package com.onehouse.app.feature.climate

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager

/**
 * Punto de entrada de las programaciones de climatización.
 *
 * La alarma despierta el proceso incluso con OneHouse cerrada. goAsync() evita
 * abandonar el receiver mientras siguen los telegramas KNX y el WakeLock
 * temporal mantiene la CPU activa con la pantalla apagada.
 */
class ClimateScheduleAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        val pendingResult = goAsync()
        val wakeLock = runCatching {
            appContext.getSystemService(PowerManager::class.java)
                .newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "OneHouse:ClimateSchedule"
                )
                .apply { acquire(WAKE_LOCK_TIMEOUT_MILLIS) }
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
            val eventId = intent.getLongExtra(EXTRA_EVENT_ID, -1L)
            ClimateScheduleKnxCommandRunner(appContext).execute(eventId) {
                finishOnce.finish()
            }
        }.onFailure {
            ClimateBackgroundScheduler(appContext).reschedule()
            finishOnce.finish()
        }
    }

    companion object {
        const val EXTRA_EVENT_ID = "climate_schedule_event_id"
        private const val WAKE_LOCK_TIMEOUT_MILLIS = 60_000L
    }
}
