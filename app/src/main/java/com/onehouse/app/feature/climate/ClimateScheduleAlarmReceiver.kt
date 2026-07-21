package com.onehouse.app.feature.climate

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Punto de entrada de segundo plano.
 *
 * ClimateKnxCommandGateway es deliberadamente una interfaz: al conectar KNX,
 * la implementación real enviará aquí los telegramas correspondientes.
 */
class ClimateScheduleAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val repository = SharedPreferencesClimateScheduleRepository(context)
        val state = repository.load()
        val eventId = intent.getLongExtra(EXTRA_EVENT_ID, -1L)

        val event = state.events.firstOrNull {
            it.id == eventId && it.enabled
        }

        if (state.globallyEnabled && event != null) {
            ClimateKnxCommandGatewayProvider.gateway.execute(event)
        }

        ClimateBackgroundScheduler(
            context = context,
            repository = repository
        ).reschedule()
    }

    companion object {
        const val EXTRA_EVENT_ID = "climate_schedule_event_id"
    }
}

interface ClimateKnxCommandGateway {
    fun execute(event: ClimateScheduleEvent)
}

/**
 * Implementación provisional. Sustituir por el gateway KNX real.
 */
object ClimateKnxCommandGatewayProvider {
    var gateway: ClimateKnxCommandGateway = object : ClimateKnxCommandGateway {
        override fun execute(event: ClimateScheduleEvent) {
            // Pendiente: enviar ON/OFF, modo, consigna y ventilador a KNX.
        }
    }
}
