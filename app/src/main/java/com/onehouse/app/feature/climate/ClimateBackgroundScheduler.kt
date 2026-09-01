package com.onehouse.app.feature.climate

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.ZoneId

/**
 * Programa únicamente la siguiente orden. Cuando se ejecuta, el receiver
 * calcula y agenda la posterior. Así no se mantiene un servicio consumiendo
 * recursos continuamente.
 */
class ClimateBackgroundScheduler(
    private val context: Context,
    private val repository: ClimateScheduleRepository =
        SharedPreferencesClimateScheduleRepository(context)
) {
    private val alarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun reschedule() {
        cancel()

        val next = ClimateScheduleEngine.nextExecution(repository.load()) ?: return
        val triggerAtMillis = next.executionTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val pendingIntent = pendingIntent(next.event.id)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else {
            // Fallback seguro mientras el usuario concede el acceso especial.
            // Puede retrasarse por Doze, por eso la pantalla solicita permiso
            // al guardar/activar una programación.
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    fun cancel() {
        alarmManager.cancel(pendingIntent(0L))
    }

    private fun pendingIntent(eventId: Long): PendingIntent {
        val intent = Intent(context, ClimateScheduleAlarmReceiver::class.java)
            .putExtra(ClimateScheduleAlarmReceiver.EXTRA_EVENT_ID, eventId)

        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private companion object {
        const val REQUEST_CODE = 7040
    }
}
