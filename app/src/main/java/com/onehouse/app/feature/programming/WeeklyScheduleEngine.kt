package com.onehouse.app.feature.programming

import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

data class NextWeeklyExecution(
    val event: WeeklyScheduleEvent,
    val executionTime: LocalDateTime
)

object WeeklyScheduleEngine {
    fun nextExecution(
        state: WeeklyScheduleState,
        from: LocalDateTime = LocalDateTime.now()
    ): NextWeeklyExecution? {
        if (!state.globallyEnabled) return null
        return state.events.asSequence()
            .filter { it.enabled }
            .map { event ->
                var dateTime = LocalDateTime.of(
                    from.toLocalDate().with(TemporalAdjusters.nextOrSame(event.dayOfWeek)),
                    LocalTime.of(event.hour, event.minute)
                )
                if (!dateTime.isAfter(from)) dateTime = dateTime.plusWeeks(1)
                NextWeeklyExecution(event, dateTime)
            }
            .minByOrNull { it.executionTime }
    }
}
