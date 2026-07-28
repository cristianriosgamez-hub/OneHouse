package com.onehouse.app.knx

import android.content.Context

/**
 * Punto único de acceso a los recursos compartidos del motor KNX de OneHouse.
 *
 * Todas las pantallas reutilizan la misma caché, monitor, estadísticas y gestor
 * de túnel. La cola global de [KnxTelegramQueue] continúa serializando las
 * operaciones para evitar que dos pantallas intenten utilizar el túnel a la vez.
 *
 * Esta primera fase mantiene el ciclo de conexión por operación para no alterar
 * el comportamiento ya validado en la v1.7.4. En versiones posteriores se podrá
 * conservar el túnel abierto sin cambiar la API pública de [KnxCommandExecutor].
 */
internal object KnxCentralEngine {

    class Resources internal constructor(context: Context) {
        private val appContext = context.applicationContext

        val stateRepository = KnxStateRepository(appContext)
        val connectionManager = KnxConnectionManager(stateRepository = stateRepository)
        val monitorRepository = KnxTelegramMonitorRepository(appContext)
        val statisticsRepository = KnxSessionStatisticsRepository(appContext)
    }

    @Volatile
    private var resources: Resources? = null

    fun get(context: Context): Resources =
        resources ?: synchronized(this) {
            resources ?: Resources(context.applicationContext).also { resources = it }
        }
}
