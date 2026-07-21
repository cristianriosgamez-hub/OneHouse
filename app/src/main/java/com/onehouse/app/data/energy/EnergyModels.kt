package com.onehouse.app.data.energy

import com.onehouse.app.data.local.EnergyReadingEntity

enum class MeterType(
    val storageValue: String,
    val title: String,
    val shortTitle: String,
    val unit: String,
    val symbol: String
) {
    ENDESA("ENDESA", "Electricidad ENDESA", "ENDESA", "kWh", "ϟ"),
    CLIMATIZATION("CLIMATIZATION", "Climatización", "Clima", "MWh", "♨"),
    ACS("ACS", "Agua caliente sanitaria", "ACS", "m³", "♨"),
    AGBAR("AGBAR", "Agua AGBAR", "AGBAR", "m³", "≈");

    companion object {
        fun fromStorage(value: String): MeterType =
            entries.firstOrNull { it.storageValue == value } ?: ENDESA
    }
}

enum class ReadingSource(val storageValue: String, val label: String) {
    MANUAL("MANUAL", "Manual"),
    KNX("KNX", "KNX"),
    EXCEL("EXCEL", "Importado del Excel")
}

enum class EnergyPeriod(val label: String) {
    MONTH("Mes"),
    YEAR("Año"),
    ALL("Histórico")
}

data class MeterSummary(
    val type: MeterType,
    val latestReading: EnergyReadingEntity? = null,
    val monthConsumption: Double = 0.0,
    val yearConsumption: Double = 0.0,
    val yearCost: Double = 0.0,
    val yearVariationPercent: Double? = null
)

data class EnergyStatistics(
    val average: Double? = null,
    val maximum: Double? = null,
    val minimum: Double? = null,
    val totalConsumption: Double = 0.0,
    val totalCost: Double = 0.0,
    val variationPercent: Double? = null
)

data class EnergyChartPoint(
    val timestamp: Long,
    val value: Double
)

data class EnergyDashboardState(
    val isLoading: Boolean = true,
    val summaries: List<MeterSummary> = MeterType.entries.map { MeterSummary(it) },
    val selectedType: MeterType? = null,
    val selectedPeriod: EnergyPeriod = EnergyPeriod.YEAR,
    val selectedReadings: List<EnergyReadingEntity> = emptyList(),
    val chartPoints: List<EnergyChartPoint> = emptyList(),
    val statistics: EnergyStatistics = EnergyStatistics(),
    val editorReading: EnergyReadingEntity? = null,
    val isEditorVisible: Boolean = false,
    val readingPendingDeletion: EnergyReadingEntity? = null,
    val message: String? = null
)
