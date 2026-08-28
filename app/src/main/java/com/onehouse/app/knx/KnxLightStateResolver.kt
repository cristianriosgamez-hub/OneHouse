package com.onehouse.app.knx

import com.onehouse.app.device.ImportedKnxDevice

/**
 * Resolución central del estado real de luces KNX (v1.12.2).
 *
 * Las pantallas no deben deducir el estado a partir del último mando enviado.
 * Si existe una GA de lectura se usa exclusivamente esa respuesta confirmada;
 * la GA de escritura solo se usa como estado cuando el proyecto no dispone de
 * una dirección de lectura independiente.
 */
internal object KnxLightStateResolver {

    fun stateAddresses(device: ImportedKnxDevice): List<String> {
        val read = device.readAddresses.map { it.toString() }.distinct()
        if (read.isNotEmpty()) return read
        return device.writeAddresses.map { it.toString() }.distinct()
    }

    fun confirmedBoolean(
        device: ImportedKnxDevice,
        states: Map<String, KnxStateRepository.State>
    ): Boolean? = stateAddresses(device)
        .asSequence()
        .mapNotNull { address -> states[address]?.takeIf(StateFreshness::isTrusted) }
        .mapNotNull { state -> state.booleanValue }
        .firstOrNull()
}
