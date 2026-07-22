package com.onehouse.app.feature.consumption

import com.onehouse.app.data.energy.EnergyDashboardState
import com.onehouse.app.data.energy.EnergyPeriod
import com.onehouse.app.data.energy.EnergyRepository
import com.onehouse.app.data.energy.MeterType
import com.onehouse.app.data.local.EnergyReadingEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

data class EnergyAnalyticsPoint(
    val timestamp: Long,
    val value: Double
)

class EnergyViewModel(
    private val repository: EnergyRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var readingsJob: Job? = null
    private var allReadings: List<EnergyReadingEntity> = emptyList()

    private val _state = MutableStateFlow(EnergyDashboardState())
    val state: StateFlow<EnergyDashboardState> = _state.asStateFlow()

    private val _analyticsPoints = MutableStateFlow<List<EnergyAnalyticsPoint>>(emptyList())
    val analyticsPoints: StateFlow<List<EnergyAnalyticsPoint>> = _analyticsPoints.asStateFlow()

    init {
        observeReadings()
    }

    fun openMeter(type: MeterType) {
        _state.value = _state.value.copy(selectedType = type)
        rebuildSelectedMeter()
    }

    fun closeMeter() {
        _state.value = _state.value.copy(
            selectedType = null,
            editorReading = null,
            isEditorVisible = false,
            readingPendingDeletion = null
        )
    }

    fun selectPeriod(period: EnergyPeriod) {
        _state.value = _state.value.copy(selectedPeriod = period)
        rebuildSelectedMeter()
    }

    fun addReading() {
        _state.value = _state.value.copy(editorReading = null, isEditorVisible = true)
    }

    fun editReading(reading: EnergyReadingEntity) {
        _state.value = _state.value.copy(editorReading = reading, isEditorVisible = true)
    }

    fun dismissEditor() {
        _state.value = _state.value.copy(editorReading = null, isEditorVisible = false)
    }

    fun requestDelete(reading: EnergyReadingEntity) {
        _state.value = _state.value.copy(readingPendingDeletion = reading)
    }

    fun dismissDelete() {
        _state.value = _state.value.copy(readingPendingDeletion = null)
    }

    fun saveReading(reading: EnergyReadingEntity) {
        scope.launch {
            runCatching { repository.save(reading) }
                .onSuccess {
                    _state.value = _state.value.copy(
                        editorReading = null,
                        isEditorVisible = false,
                        message = "Lectura guardada"
                    )
                }
                .onFailure {
                    _state.value = _state.value.copy(message = "No se pudo guardar la lectura")
                }
        }
    }

    fun confirmDelete() {
        val reading = _state.value.readingPendingDeletion ?: return
        scope.launch {
            runCatching { repository.delete(reading) }
                .onSuccess {
                    _state.value = _state.value.copy(
                        readingPendingDeletion = null,
                        message = "Lectura eliminada"
                    )
                }
                .onFailure {
                    _state.value = _state.value.copy(
                        readingPendingDeletion = null,
                        message = "No se pudo eliminar la lectura"
                    )
                }
        }
    }

    fun consumeMessage() {
        _state.value = _state.value.copy(message = null)
    }

    fun close() {
        readingsJob?.cancel()
        scope.cancel()
    }

    private fun observeReadings() {
        readingsJob?.cancel()
        readingsJob = scope.launch {
            repository.observeAll().collect { readings ->
                allReadings = readings
                val summaries = repository.buildSummaries(readings)
                _analyticsPoints.value = buildElectricityAnalytics(readings)
                _state.value = _state.value.copy(
                    isLoading = false,
                    summaries = summaries,
                    overview = repository.buildOverview(readings, summaries)
                )
                rebuildSelectedMeter()
            }
        }
    }

    private fun buildElectricityAnalytics(
        readings: List<EnergyReadingEntity>,
        now: Long = System.currentTimeMillis()
    ): List<EnergyAnalyticsPoint> {
        val monthStarts = (11 downTo 0).map { offset ->
            Calendar.getInstance().apply {
                timeInMillis = now
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.MONTH, -offset)
            }.timeInMillis
        }

        return monthStarts.map { monthStart ->
            val month = Calendar.getInstance().apply { timeInMillis = monthStart }
            val yearValue = month.get(Calendar.YEAR)
            val monthValue = month.get(Calendar.MONTH)
            val totalKwh = readings.asSequence()
                .filter { reading ->
                    val calendar = Calendar.getInstance().apply { timeInMillis = reading.timestamp }
                    calendar.get(Calendar.YEAR) == yearValue &&
                        calendar.get(Calendar.MONTH) == monthValue
                }
                .sumOf { reading ->
                    when (MeterType.fromStorage(reading.meterType)) {
                        MeterType.ENDESA -> reading.consumption ?: 0.0
                        MeterType.CLIMATIZATION -> (reading.consumption ?: 0.0) * 1_000.0
                        else -> 0.0
                    }
                }
            EnergyAnalyticsPoint(monthStart, totalKwh)
        }
    }

    private fun rebuildSelectedMeter() {
        val currentState = _state.value
        val type = currentState.selectedType ?: return
        val filtered = repository.readingsForPeriod(
            readings = allReadings,
            type = type,
            period = currentState.selectedPeriod
        )
        _state.value = currentState.copy(
            selectedReadings = filtered,
            chartPoints = repository.buildChartPoints(filtered),
            statistics = repository.buildStatistics(
                allReadings = allReadings,
                type = type,
                periodReadings = filtered
            )
        )
    }
}
