package com.onehouse.app.knx

/**
 * Catálogo único de direcciones KNX usadas directamente por la interfaz OneHouse.
 *
 * Las direcciones importadas desde InsideControl continúan perteneciendo a cada
 * [com.onehouse.app.device.ImportedKnxDevice]. Este objeto solo centraliza los
 * objetos globales/sensores que todavía no llegan identificados por estancia.
 * Centralizarlas evita que una corrección visual cambie accidentalmente una GA.
 */
object KnxAddressBook {
    object Climate {
        const val POWER_STATE = "5/3/2"
        const val CURRENT_TEMPERATURE = "5/3/3"
        const val MODE_STATE = "5/3/4"
        const val FAN_SPEED_STATE = "5/3/5"
        const val TARGET_TEMPERATURE_PRIMARY = "5/3/10"
        const val TARGET_TEMPERATURE_FALLBACK = "5/3/1"
    }

    object Indoor {
        const val CO2_DINING = "5/1/1"
        const val HUMIDITY_DINING = "5/1/2"
        const val TEMPERATURE_DINING = "5/1/3"
        const val TEMPERATURE_SUITE = "5/1/4"
        const val PIR_BLOCK_ENTRANCE = "5/5/1"
        const val FLOOD_KITCHEN = "2/4/1"
        const val FLOOD_BATHROOM = "2/4/2"
    }

    object Terrace {
        const val LUMINOSITY = "15/0/11"
        const val EXCESSIVE_WIND = "15/0/13"
        const val WIND_SPEED = "15/0/14"
        const val RAINING = "15/0/21"
    }
}
