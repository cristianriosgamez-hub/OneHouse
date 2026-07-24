package com.onehouse.app.knx

import android.content.Context
import com.onehouse.app.device.ImportedKnxDevice
import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Ejecuta lecturas de estado de forma secuencial para no saturar el túnel KNX/IP.
 * Cada dirección de grupo se consulta una sola vez aunque aparezca en varios objetos.
 */
class KnxBulkStateReader(context: Context) : Closeable {
    data class Progress(
        val completed: Int,
        val total: Int,
        val failures: Int,
        val currentAddress: String?
    ) {
        val finished: Boolean get() = completed >= total
    }

    private val appContext = context.applicationContext
    private val cancelled = AtomicBoolean(false)
    private var activeExecutor: KnxCommandExecutor? = null

    fun read(
        devices: List<ImportedKnxDevice>,
        onProgress: (Progress) -> Unit,
        onComplete: (Progress) -> Unit
    ) {
        cancel()
        cancelled.set(false)
        val commands = devices
            .flatMap { it.commands }
            .filter { it.type == KnxCommandType.READ }
            .distinctBy { it.destination.toString() }

        if (commands.isEmpty()) {
            val result = Progress(0, 0, 0, null)
            onProgress(result)
            onComplete(result)
            return
        }

        fun executeAt(index: Int, failures: Int) {
            if (cancelled.get()) return
            if (index >= commands.size) {
                val result = Progress(commands.size, commands.size, failures, null)
                onProgress(result)
                onComplete(result)
                return
            }

            val command = commands[index]
            onProgress(Progress(index, commands.size, failures, command.destination.toString()))
            val executor = KnxCommandExecutor(appContext)
            activeExecutor = executor
            executor.execute(command) { result ->
                executor.close()
                if (activeExecutor === executor) activeExecutor = null
                if (cancelled.get()) return@execute
                val nextFailures = failures + if (result is KnxCommandExecutor.Result.Failure) 1 else 0
                onProgress(
                    Progress(
                        completed = index + 1,
                        total = commands.size,
                        failures = nextFailures,
                        currentAddress = command.destination.toString()
                    )
                )
                executeAt(index + 1, nextFailures)
            }
        }

        executeAt(0, 0)
    }

    fun cancel() {
        cancelled.set(true)
        activeExecutor?.close()
        activeExecutor = null
    }

    override fun close() = cancel()
}
