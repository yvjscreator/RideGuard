package dev.rideguard.core.model

import java.time.DayOfWeek
import java.time.LocalDateTime

data class WorkDaySchedule(
    val day: DayOfWeek,
    val enabled: Boolean = false,
    val startMinute: Int = 8 * 60,
    val endMinute: Int = 20 * 60,
) {
    init {
        require(startMinute in 0..1439)
        require(endMinute in 0..1440)
        require(startMinute != endMinute)
    }
}

data class WeeklySchedule(
    val days: List<WorkDaySchedule> = DayOfWeek.values().map(::WorkDaySchedule),
) {
    init {
        require(days.size == 7 && days.map { it.day }.toSet().size == 7)
    }

    fun activeDay(at: LocalDateTime): DayOfWeek? {
        val current = days.first { it.day == at.dayOfWeek }
        val minute = at.hour * 60 + at.minute
        if (current.enabled) {
            if (current.startMinute < current.endMinute && minute in current.startMinute until current.endMinute) {
                return current.day
            }
            if (current.startMinute > current.endMinute && minute >= current.startMinute) {
                return current.day
            }
        }

        val previousDay = at.dayOfWeek.minus(1)
        val previous = days.first { it.day == previousDay }
        if (previous.enabled && previous.endMinute < previous.startMinute && minute < previous.endMinute) {
            return previousDay
        }
        return null
    }
}
