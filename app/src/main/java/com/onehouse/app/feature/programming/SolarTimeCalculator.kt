package com.onehouse.app.feature.programming

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.*

data class SolarTimes(
    val sunrise: LocalTime?,
    val sunset: LocalTime?
)

object SolarTimeCalculator {
    fun calculate(
        date: LocalDate,
        latitude: Double,
        longitude: Double,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): SolarTimes = SolarTimes(
        sunrise = calculateEvent(date, latitude, longitude, zoneId, true),
        sunset = calculateEvent(date, latitude, longitude, zoneId, false)
    )

    fun nextExecution(
        state: SolarScheduleState,
        from: LocalDateTime = LocalDateTime.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Pair<SolarScheduleEvent, LocalDateTime>? {
        if (!state.globallyEnabled) return null
        return state.events.asSequence()
            .filter { it.enabled }
            .flatMap { event ->
                (0..370).asSequence().mapNotNull { dayOffset ->
                    val date = from.toLocalDate().plusDays(dayOffset.toLong())
                    val times = calculate(date, state.latitude, state.longitude, zoneId)
                    val base = when (event.trigger) {
                        SolarTrigger.SUNRISE -> times.sunrise
                        SolarTrigger.SUNSET -> times.sunset
                    } ?: return@mapNotNull null
                    val execution = LocalDateTime.of(date, base).plusMinutes(event.offsetMinutes.toLong())
                    if (execution.isAfter(from)) event to execution else null
                }.take(1)
            }
            .minByOrNull { it.second }
    }

    private fun calculateEvent(
        date: LocalDate,
        latitude: Double,
        longitude: Double,
        zoneId: ZoneId,
        sunrise: Boolean
    ): LocalTime? {
        val dayOfYear = date.dayOfYear
        val lngHour = longitude / 15.0
        val approximateTime = dayOfYear + ((if (sunrise) 6.0 else 18.0) - lngHour) / 24.0

        val meanAnomaly = 0.9856 * approximateTime - 3.289
        var trueLongitude = meanAnomaly + 1.916 * sinDeg(meanAnomaly) +
            0.020 * sinDeg(2 * meanAnomaly) + 282.634
        trueLongitude = normalizeDegrees(trueLongitude)

        var rightAscension = radToDeg(atan(0.91764 * tan(degToRad(trueLongitude))))
        rightAscension = normalizeDegrees(rightAscension)
        rightAscension += floor(trueLongitude / 90.0) * 90.0 - floor(rightAscension / 90.0) * 90.0
        rightAscension /= 15.0

        val sinDeclination = 0.39782 * sinDeg(trueLongitude)
        val cosDeclination = cos(asin(sinDeclination))
        val cosHourAngle = (
            cos(degToRad(90.833)) - sinDeclination * sinDeg(latitude)
            ) / (cosDeclination * cosDeg(latitude))

        if (cosHourAngle > 1.0 || cosHourAngle < -1.0) return null

        var localHour = if (sunrise) 360.0 - radToDeg(acos(cosHourAngle)) else radToDeg(acos(cosHourAngle))
        localHour /= 15.0

        val localMeanTime = localHour + rightAscension - 0.06571 * approximateTime - 6.622
        val utcHours = normalizeHours(localMeanTime - lngHour)
        val utcSeconds = (utcHours * 3600.0).roundToInt()
        val utcDateTime = date.atStartOfDay(ZoneId.of("UTC")).plusSeconds(utcSeconds.toLong())
        return utcDateTime.withZoneSameInstant(zoneId).toLocalTime().withSecond(0).withNano(0)
    }

    private fun sinDeg(value: Double): Double = sin(degToRad(value))
    private fun cosDeg(value: Double): Double = cos(degToRad(value))
    private fun degToRad(value: Double): Double = Math.toRadians(value)
    private fun radToDeg(value: Double): Double = Math.toDegrees(value)
    private fun normalizeDegrees(value: Double): Double = ((value % 360.0) + 360.0) % 360.0
    private fun normalizeHours(value: Double): Double = ((value % 24.0) + 24.0) % 24.0
}
