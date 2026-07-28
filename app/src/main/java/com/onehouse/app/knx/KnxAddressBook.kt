package com.onehouse.app.knx

/** Catálogo de parámetros globales utilizados directamente por OneHouse. */
object KnxAddressBook {
    data class Entry(
        val key: String,
        val room: String,
        val name: String,
        val category: String,
        val dpt: String,
        val kind: String,
        val defaultAddress: String
    )

    val entries = listOf(
        Entry("climate_power", "Climatización", "Encendido / estado", "Climatización", "1.001", "Lectura y mando", "5/3/2"),
        Entry("climate_current_temperature", "Climatización", "Temperatura actual", "Climatización", "9.001", "Lectura", "5/3/3"),
        Entry("climate_mode", "Climatización", "Modo de funcionamiento", "Climatización", "20.102", "Lectura y mando", "5/3/4"),
        Entry("climate_fan", "Climatización", "Velocidad del ventilador", "Climatización", "5.010", "Lectura y mando", "5/3/5"),
        Entry("climate_target_primary", "Climatización", "Temperatura de consigna", "Climatización", "9.001", "Lectura y mando", "5/3/10"),
        Entry("climate_target_fallback", "Climatización", "Consigna alternativa", "Climatización", "9.001", "Lectura", "5/3/1"),
        Entry("dining_co2", "Comedor", "CO₂", "Sensor", "14.000", "Lectura", "5/1/1"),
        Entry("dining_humidity", "Comedor", "Humedad", "Sensor", "5.001", "Lectura", "5/1/2"),
        Entry("dining_temperature", "Comedor", "Temperatura interior", "Temperatura", "9.001", "Lectura", "5/1/3"),
        Entry("suite_temperature", "Suite", "Temperatura interior", "Temperatura", "9.001", "Lectura", "5/2/3"),
        Entry("entrance_pir_block", "Entrada", "Bloqueo PIR", "Interruptor", "1.001", "Lectura y mando", "5/5/1"),
        Entry("kitchen_flood", "Cocina", "Sensor de inundación", "Alarma", "1.005", "Lectura", "2/4/1"),
        Entry("bathroom_flood", "Baño", "Sensor de inundación", "Alarma", "1.005", "Lectura", "2/4/2"),
        Entry("terrace_luminosity", "Terraza", "Luminosidad", "Sensor", "14.000", "Lectura", "15/0/11"),
        Entry("terrace_excessive_wind", "Terraza", "Viento excesivo", "Alarma", "1.005", "Lectura", "15/0/13"),
        Entry("terrace_wind_speed", "Terraza", "Velocidad del viento", "Sensor", "9.005", "Lectura", "15/0/14"),
        Entry("terrace_raining", "Terraza", "Lluvia", "Sensor", "1.005", "Lectura", "15/0/21")
    )

    object Climate {
        @Volatile var POWER_STATE = "5/3/2"
        @Volatile var CURRENT_TEMPERATURE = "5/3/3"
        @Volatile var MODE_STATE = "5/3/4"
        @Volatile var FAN_SPEED_STATE = "5/3/5"
        @Volatile var TARGET_TEMPERATURE_PRIMARY = "5/3/10"
        @Volatile var TARGET_TEMPERATURE_FALLBACK = "5/3/1"
    }
    object Indoor {
        @Volatile var CO2_DINING = "5/1/1"
        @Volatile var HUMIDITY_DINING = "5/1/2"
        @Volatile var TEMPERATURE_DINING = "5/1/3"
        @Volatile var TEMPERATURE_SUITE = "5/2/3"
        @Volatile var PIR_BLOCK_ENTRANCE = "5/5/1"
        @Volatile var FLOOD_KITCHEN = "2/4/1"
        @Volatile var FLOOD_BATHROOM = "2/4/2"
    }
    object Terrace {
        @Volatile var LUMINOSITY = "15/0/11"
        @Volatile var EXCESSIVE_WIND = "15/0/13"
        @Volatile var WIND_SPEED = "15/0/14"
        @Volatile var RAINING = "15/0/21"
    }

    fun apply(repository: AppKnxConfigurationRepository) {
        fun value(key: String) = entries.first { it.key == key }.let { repository.globalAddress(it.key, it.defaultAddress) }
        Climate.POWER_STATE = value("climate_power")
        Climate.CURRENT_TEMPERATURE = value("climate_current_temperature")
        Climate.MODE_STATE = value("climate_mode")
        Climate.FAN_SPEED_STATE = value("climate_fan")
        Climate.TARGET_TEMPERATURE_PRIMARY = value("climate_target_primary")
        Climate.TARGET_TEMPERATURE_FALLBACK = value("climate_target_fallback")
        Indoor.CO2_DINING = value("dining_co2")
        Indoor.HUMIDITY_DINING = value("dining_humidity")
        Indoor.TEMPERATURE_DINING = value("dining_temperature")
        Indoor.TEMPERATURE_SUITE = value("suite_temperature")
        Indoor.PIR_BLOCK_ENTRANCE = value("entrance_pir_block")
        Indoor.FLOOD_KITCHEN = value("kitchen_flood")
        Indoor.FLOOD_BATHROOM = value("bathroom_flood")
        Terrace.LUMINOSITY = value("terrace_luminosity")
        Terrace.EXCESSIVE_WIND = value("terrace_excessive_wind")
        Terrace.WIND_SPEED = value("terrace_wind_speed")
        Terrace.RAINING = value("terrace_raining")
    }
}
