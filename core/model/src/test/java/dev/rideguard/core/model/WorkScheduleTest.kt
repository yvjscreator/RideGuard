package dev.rideguard.core.model

import java.time.DayOfWeek
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkScheduleTest {
    @Test
    fun `disabled days are inactive`() {
        assertEquals(null, WeeklySchedule().activeDay(LocalDateTime.of(2026, 9, 21, 12, 0)))
    }

    @Test
    fun `regular window includes start and excludes end`() {
        val schedule = withDay(DayOfWeek.MONDAY, 8 * 60, 17 * 60)
        assertEquals(DayOfWeek.MONDAY, schedule.activeDay(LocalDateTime.of(2026, 9, 21, 8, 0)))
        assertEquals(null, schedule.activeDay(LocalDateTime.of(2026, 9, 21, 17, 0)))
    }

    @Test
    fun `overnight window keeps originating day after midnight`() {
        val schedule = withDay(DayOfWeek.MONDAY, 22 * 60, 2 * 60)
        assertEquals(DayOfWeek.MONDAY, schedule.activeDay(LocalDateTime.of(2026, 9, 22, 1, 59)))
        assertEquals(null, schedule.activeDay(LocalDateTime.of(2026, 9, 22, 2, 0)))
    }

    @Test
    fun `monday schedule can be copied to every day and customized afterward`() {
        val copied = withDay(DayOfWeek.MONDAY, 22 * 60, 2 * 60).copyMondayToAllDays()

        assertEquals(7, copied.days.count { it.enabled })
        assertEquals(true, copied.days.all { it.startMinute == 22 * 60 && it.endMinute == 2 * 60 })
        assertEquals(DayOfWeek.MONDAY, copied.activeDay(LocalDateTime.of(2026, 9, 22, 1, 0)))

        val customized = WeeklySchedule(copied.days.map { day ->
            if (day.day == DayOfWeek.TUESDAY) day.copy(startMinute = 9 * 60, endMinute = 17 * 60) else day
        })
        assertEquals(22 * 60, customized.days.first { it.day == DayOfWeek.MONDAY }.startMinute)
        assertEquals(9 * 60, customized.days.first { it.day == DayOfWeek.TUESDAY }.startMinute)
    }

    private fun withDay(day: DayOfWeek, start: Int, end: Int) = WeeklySchedule(
        DayOfWeek.values().map {
            if (it == day) WorkDaySchedule(it, true, start, end) else WorkDaySchedule(it)
        },
    )
}
