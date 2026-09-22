package dev.rideguard.core.settings

import dev.rideguard.core.model.WeeklySchedule
import dev.rideguard.core.model.WorkDaySchedule
import java.time.DayOfWeek
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class RideGuardSettingsTest {
    @Test
    fun `overnight shift uses the goals of its starting day`() {
        val schedule = WeeklySchedule(DayOfWeek.values().map { day ->
            if (day == DayOfWeek.WEDNESDAY) WorkDaySchedule(day, true, 22 * 60, 2 * 60)
            else WorkDaySchedule(day)
        })
        val settings = RideGuardSettings(schedule = schedule)

        val goals = settings.goalsFor(LocalDateTime.of(2026, 9, 24, 1, 0))

        assertEquals(15_000.0, goals?.targetArsPerHour ?: 0.0, 0.001)
        assertEquals(650.0, goals?.minimumArsPerKm ?: 0.0, 0.001)
    }
}
