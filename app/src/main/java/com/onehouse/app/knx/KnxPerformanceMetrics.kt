package com.onehouse.app.knx

import java.util.concurrent.atomic.AtomicLong

/**
 * Métricas ligeras de la ruta de recepción KNX.
 *
 * Solo observa tiempos y contadores; no modifica conexiones, telegramas,
 * caché, prioridades ni flujos de estado.
 */
object KnxPerformanceMetrics {

    data class Snapshot(
        val acceptedStateUpdates: Long,
        val duplicateStateUpdates: Long,
        val lastCacheUpdateMicros: Long?,
        val averageCacheUpdateMicros: Long?,
        val maxCacheUpdateMicros: Long?,
        val lastObserverDispatchMicros: Long?,
        val averageObserverDispatchMicros: Long?,
        val maxObserverDispatchMicros: Long?,
        val parallelEventsReceived: Long,
        val lastParallelDispatchMicros: Long?,
        val averageParallelDispatchMicros: Long?,
        val maxParallelDispatchMicros: Long?,
        val tunnelOpenAttempts: Long,
        val tunnelReuses: Long,
        val tunnelConnectSuccesses: Long,
        val tunnelRejected36: Long,
        val tunnelDisconnects: Long
    )

    private val accepted = AtomicLong(0)
    private val duplicates = AtomicLong(0)

    private val cacheSamples = AtomicLong(0)
    private val cacheTotalNanos = AtomicLong(0)
    private val cacheLastNanos = AtomicLong(-1)
    private val cacheMaxNanos = AtomicLong(-1)

    private val observerSamples = AtomicLong(0)
    private val observerTotalNanos = AtomicLong(0)
    private val observerLastNanos = AtomicLong(-1)
    private val observerMaxNanos = AtomicLong(-1)

    private val parallelEvents = AtomicLong(0)
    private val parallelSamples = AtomicLong(0)
    private val parallelTotalNanos = AtomicLong(0)
    private val parallelLastNanos = AtomicLong(-1)
    private val parallelMaxNanos = AtomicLong(-1)

    private val tunnelOpenAttemptsCounter = AtomicLong(0)
    private val tunnelReusesCounter = AtomicLong(0)
    private val tunnelConnectSuccessesCounter = AtomicLong(0)
    private val tunnelRejected36Counter = AtomicLong(0)
    private val tunnelDisconnectsCounter = AtomicLong(0)

    fun recordDuplicate() {
        duplicates.incrementAndGet()
    }

    fun recordCacheUpdate(elapsedNanos: Long) {
        accepted.incrementAndGet()
        cacheSamples.incrementAndGet()
        cacheTotalNanos.addAndGet(elapsedNanos.coerceAtLeast(0))
        cacheLastNanos.set(elapsedNanos.coerceAtLeast(0))
        updateMax(cacheMaxNanos, elapsedNanos)
    }

    fun recordObserverDispatch(elapsedNanos: Long) {
        observerSamples.incrementAndGet()
        observerTotalNanos.addAndGet(elapsedNanos.coerceAtLeast(0))
        observerLastNanos.set(elapsedNanos.coerceAtLeast(0))
        updateMax(observerMaxNanos, elapsedNanos)
    }

    fun recordTunnelOpenAttempt() { tunnelOpenAttemptsCounter.incrementAndGet() }

    fun recordTunnelReuse() { tunnelReusesCounter.incrementAndGet() }

    fun recordTunnelConnectSuccess() { tunnelConnectSuccessesCounter.incrementAndGet() }

    fun recordTunnelRejected(status: Int) {
        if ((status and 0xFF) == 36) tunnelRejected36Counter.incrementAndGet()
    }

    fun recordTunnelDisconnect() { tunnelDisconnectsCounter.incrementAndGet() }

    fun recordParallelDispatch(elapsedNanos: Long) {
        val safe = elapsedNanos.coerceAtLeast(0)
        parallelEvents.incrementAndGet()
        parallelSamples.incrementAndGet()
        parallelTotalNanos.addAndGet(safe)
        parallelLastNanos.set(safe)
        updateMax(parallelMaxNanos, safe)
    }

    fun snapshot(): Snapshot {
        val cacheCount = cacheSamples.get()
        val observerCount = observerSamples.get()
        val parallelCount = parallelSamples.get()
        return Snapshot(
            acceptedStateUpdates = accepted.get(),
            duplicateStateUpdates = duplicates.get(),
            lastCacheUpdateMicros = cacheLastNanos.get().takeIf { it >= 0 }?.div(1_000),
            averageCacheUpdateMicros = if (cacheCount > 0) cacheTotalNanos.get().div(cacheCount).div(1_000) else null,
            maxCacheUpdateMicros = cacheMaxNanos.get().takeIf { it >= 0 }?.div(1_000),
            lastObserverDispatchMicros = observerLastNanos.get().takeIf { it >= 0 }?.div(1_000),
            averageObserverDispatchMicros = if (observerCount > 0) observerTotalNanos.get().div(observerCount).div(1_000) else null,
            maxObserverDispatchMicros = observerMaxNanos.get().takeIf { it >= 0 }?.div(1_000),
            parallelEventsReceived = parallelEvents.get(),
            lastParallelDispatchMicros = parallelLastNanos.get().takeIf { it >= 0 }?.div(1_000),
            averageParallelDispatchMicros = if (parallelCount > 0) parallelTotalNanos.get().div(parallelCount).div(1_000) else null,
            maxParallelDispatchMicros = parallelMaxNanos.get().takeIf { it >= 0 }?.div(1_000),
            tunnelOpenAttempts = tunnelOpenAttemptsCounter.get(),
            tunnelReuses = tunnelReusesCounter.get(),
            tunnelConnectSuccesses = tunnelConnectSuccessesCounter.get(),
            tunnelRejected36 = tunnelRejected36Counter.get(),
            tunnelDisconnects = tunnelDisconnectsCounter.get()
        )
    }

    fun clear() {
        accepted.set(0)
        duplicates.set(0)
        cacheSamples.set(0)
        cacheTotalNanos.set(0)
        cacheLastNanos.set(-1)
        cacheMaxNanos.set(-1)
        observerSamples.set(0)
        observerTotalNanos.set(0)
        observerLastNanos.set(-1)
        observerMaxNanos.set(-1)
        parallelEvents.set(0)
        parallelSamples.set(0)
        parallelTotalNanos.set(0)
        parallelLastNanos.set(-1)
        parallelMaxNanos.set(-1)
        tunnelOpenAttemptsCounter.set(0)
        tunnelReusesCounter.set(0)
        tunnelConnectSuccessesCounter.set(0)
        tunnelRejected36Counter.set(0)
        tunnelDisconnectsCounter.set(0)
    }

    private fun updateMax(target: AtomicLong, value: Long) {
        val safeValue = value.coerceAtLeast(0)
        while (true) {
            val current = target.get()
            if (safeValue <= current || target.compareAndSet(current, safeValue)) return
        }
    }
}
