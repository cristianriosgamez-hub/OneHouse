package com.onehouse.app.feature.scenes

import android.content.Context
import com.onehouse.app.knx.KnxCommand
import com.onehouse.app.knx.KnxCommandExecutor
import com.onehouse.app.knx.KnxCommandType
import com.onehouse.app.knx.KnxGroupAddress
import java.io.Closeable

class SceneExecutionEngine(context: Context) : Closeable {
    private val executor = KnxCommandExecutor(context.applicationContext)

    data class ExecutionResult(
        val successfulActions: Int,
        val totalActions: Int,
        val errorMessage: String? = null
    )

    fun execute(scene: SmartScene, onComplete: (ExecutionResult) -> Unit) {
        if (!scene.enabled) {
            onComplete(ExecutionResult(0, scene.actions.size, "La escena está desactivada"))
            return
        }
        if (scene.actions.isEmpty()) {
            onComplete(ExecutionResult(0, 0, "La escena no contiene acciones"))
            return
        }
        executeNext(scene.actions, index = 0, successful = 0, onComplete = onComplete)
    }

    private fun executeNext(
        actions: List<SceneAction>,
        index: Int,
        successful: Int,
        onComplete: (ExecutionResult) -> Unit
    ) {
        if (index >= actions.size) {
            onComplete(ExecutionResult(successful, actions.size))
            return
        }

        val action = actions[index]
        val command = runCatching {
            KnxCommand(
                type = if (action.type == SceneActionType.ON) KnxCommandType.ON else KnxCommandType.OFF,
                destination = KnxGroupAddress.parse(action.groupAddress),
                dpt = action.dpt
            )
        }.getOrElse {
            onComplete(ExecutionResult(successful, actions.size, "Dirección no válida: ${action.groupAddress}"))
            return
        }

        executor.execute(command = command) { result ->
            when (result) {
                is KnxCommandExecutor.Result.Success -> executeNext(
                    actions = actions,
                    index = index + 1,
                    successful = successful + 1,
                    onComplete = onComplete
                )
                is KnxCommandExecutor.Result.Failure -> onComplete(
                    ExecutionResult(
                        successfulActions = successful,
                        totalActions = actions.size,
                        errorMessage = "${action.name}: ${result.message}"
                    )
                )
            }
        }
    }

    override fun close() {
        executor.close()
    }
}
