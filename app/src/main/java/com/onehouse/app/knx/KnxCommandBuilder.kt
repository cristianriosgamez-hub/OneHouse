package com.onehouse.app.knx

import com.onehouse.app.device.ControlKind
import com.onehouse.app.device.ImportedKnxDevice

/** Genera la lista de acciones admitidas por cada dispositivo importado. */
object KnxCommandBuilder {
    fun build(device: ImportedKnxDevice): List<KnxCommand> {
        val commands = mutableListOf<KnxCommand>()
        val readDestination = device.primaryReadAddress ?: device.primaryWriteAddress
        val writeDestination = device.primaryWriteAddress

        if (readDestination != null) {
            commands += KnxCommand(
                type = KnxCommandType.READ,
                destination = readDestination,
                dpt = device.resolvedDpt
            )
        }

        if (writeDestination == null) return commands

        commands += when (device.controlKind) {
            ControlKind.BOOLEAN_SWITCH -> listOf(
                command(KnxCommandType.ON, writeDestination, device.resolvedDpt),
                command(KnxCommandType.OFF, writeDestination, device.resolvedDpt),
                command(KnxCommandType.TOGGLE, writeDestination, device.resolvedDpt)
            )
            ControlKind.BLIND -> listOf(
                command(KnxCommandType.UP, writeDestination, device.resolvedDpt),
                command(KnxCommandType.DOWN, writeDestination, device.resolvedDpt),
                command(KnxCommandType.STOP, writeDestination, "1.007"),
                command(
                    KnxCommandType.POSITION,
                    writeDestination,
                    "5.001",
                    requiresValue = true,
                    valueHint = "0–100 %"
                )
            )
            ControlKind.CLIMATE -> listOf(
                command(
                    KnxCommandType.SET_VALUE,
                    writeDestination,
                    device.resolvedDpt,
                    requiresValue = true,
                    valueHint = "Valor según DPT"
                )
            )
            ControlKind.SCENE -> listOf(
                command(
                    KnxCommandType.CALL_SCENE,
                    writeDestination,
                    device.resolvedDpt,
                    requiresValue = true,
                    valueHint = "Escena 0–63"
                )
            )
            ControlKind.ALARM,
            ControlKind.SENSOR,
            ControlKind.METER,
            ControlKind.TEMPERATURE,
            ControlKind.READ_ONLY,
            ControlKind.UNKNOWN -> emptyList()
        }

        return commands.distinctBy { Triple(it.type, it.destination, it.dpt) }
    }

    private fun command(
        type: KnxCommandType,
        destination: KnxGroupAddress,
        dpt: String,
        requiresValue: Boolean = false,
        valueHint: String? = null
    ) = KnxCommand(type, destination, dpt, requiresValue, valueHint)
}
