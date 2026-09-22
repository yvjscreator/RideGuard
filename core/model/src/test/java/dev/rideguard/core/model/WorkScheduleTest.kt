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

    private fun withDay(day: DayOfWeek, start: Int, end: Int) = WeeklySchedule(
        DayOfWeek.values().map {
            if (it == day) WorkDaySchedule(it, true, start, end) else WorkDaySchedule(it)
        },
    )
}
