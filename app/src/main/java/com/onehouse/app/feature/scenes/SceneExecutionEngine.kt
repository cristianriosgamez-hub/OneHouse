package com.onehouse.app.feature.scenes

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.onehouse.app.knx.KnxCommand
import com.onehouse.app.knx.KnxCommandExecutor
import com.onehouse.app.knx.KnxCommandType
import com.onehouse.app.knx.KnxGroupAddress
import java.io.Closeable

class SceneExecutionEngine(context: Context) : Closeable {
    private val executor = KnxCommandExecutor(context.applicationContext)
    private val handler = Handler(Looper.getMainLooper())
    private var closed = false

    data class ExecutionResult(
        val successfulActions: Int,
        val totalActions: Int,
        val failedActions: Int = 0,
        val errorMessage: String? = null
    )

    fun execute(scene: SmartScene, onComplete: (ExecutionResult) -> Unit) {
        if (!scene.enabled) {
            onComplete(ExecutionResult(0, scene.actions.size, errorMessage = "La escena está desactivada"))
            return
        }
        if (scene.actions.isEmpty()) {
            onComplete(ExecutionResult(0, 0, errorMessage = "La escena no contiene acciones"))
            return
        }
        closed = false
        executeNext(
            scene = scene,
            index = 0,
            successful = 0,
            failed = 0,
            firstError = null,
            onComplete = onComplete
        )
    }

    private fun executeNext(
        scene: SmartScene,
        index: Int,
        successful: Int,
        failed: Int,
        firstError: String?,
        onComplete: (ExecutionResult) -> Unit
    ) {
        if (closed) return
        if (index >= scene.actions.size) {
            onComplete(
                ExecutionResult(
                    successfulActions = successful,
                    totalActions = scene.actions.size,
                    failedActions = failed,
                    errorMessage = firstError
                )
            )
            return
        }

        val action = scene.actions[index]
        val command = runCatching {
            KnxCommand(
                type = if (action.type == SceneActionType.ON) KnxCommandType.ON else KnxCommandType.OFF,
                destination = KnxGroupAddress.parse(action.groupAddress),
                dpt = action.dpt
            )
        }.getOrElse {
            handleFailure(
                scene = scene,
                index = index,
                successful = successful,
                failed = failed,
                firstError = firstError,
                message = "Dirección no válida: ${action.groupAddress}",
                delayAfterMillis = action.delayAfterMillis,
                onComplete = onComplete
            )
            return
        }

        executor.execute(command = command) { result ->
            when (result) {
                is KnxCommandExecutor.Result.Success -> scheduleNext(action.delayAfterMillis) {
                    executeNext(
                        scene = scene,
                        index = index + 1,
                        successful = successful + 1,
                        failed = failed,
                        firstError = firstError,
                        onComplete = onComplete
                    )
                }
                is KnxCommandExecutor.Result.Failure -> handleFailure(
                    scene = scene,
                    index = index,
                    successful = successful,
                    failed = failed,
                    firstError = firstError,
                    message = "${action.name}: ${result.message}",
                    delayAfterMillis = action.delayAfterMillis,
                    onComplete = onComplete
                )
            }
        }
    }

    private fun handleFailure(
        scene: SmartScene,
        index: Int,
        successful: Int,
        failed: Int,
        firstError: String?,
        message: String,
        delayAfterMillis: Long,
        onComplete: (ExecutionResult) -> Unit
    ) {
        val updatedError = firstError ?: message
        if (scene.stopOnError) {
            onComplete(
                ExecutionResult(
                    successfulActions = successful,
                    totalActions = scene.actions.size,
                    failedActions = failed + 1,
                    errorMessage = updatedError
                )
            )
            return
        }

        scheduleNext(delayAfterMillis) {
            executeNext(
                scene = scene,
                index = index + 1,
                successful = successful,
                failed = failed + 1,
                firstError = updatedError,
                onComplete = onComplete
            )
        }
    }

    private fun scheduleNext(delayMillis: Long, action: () -> Unit) {
        val safeDelay = delayMillis.coerceIn(0L, 60_000L)
        if (safeDelay == 0L) action() else handler.postDelayed({ if (!closed) action() }, safeDelay)
    }

    override fun close() {
        closed = true
        handler.removeCallbacksAndMessages(null)
        executor.close()
    }
}
