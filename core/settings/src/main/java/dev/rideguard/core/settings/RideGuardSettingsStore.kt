package dev.rideguard.core.settings

import android.content.Context
import dev.rideguard.core.model.DriverGoals
import dev.rideguard.core.model.WeeklySchedule
import dev.rideguard.core.model.WorkDaySchedule
import java.time.DayOfWeek
import java.time.LocalDateTime

data class RideGuardSettings(
    val regularGoals: DriverGoals = DriverGoals(15_000.0, 650.0),
    val busyDaysGoals: DriverGoals = DriverGoals(18_000.0, 750.0),
    val usualWorkHours: Double = 6.0,
    val schedule: WeeklySchedule = WeeklySchedule(),
) {
    fun goalsFor(at: LocalDateTime): DriverGoals? = when (schedule.activeDay(at)) {
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY -> regularGoals
        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> busyDaysGoals
        null -> null
    }
}

class RideGuardSettingsStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "ride_guard_settings",
        Context.MODE_PRIVATE,
    )

    fun load(): RideGuardSettings {
        val defaults = RideGuardSettings()
        return RideGuardSettings(
            regularGoals = DriverGoals(
                preferences.getFloat(REGULAR_HOURLY, defaults.regularGoals.targetArsPerHour.toFloat()).toDouble(),
                preferences.getFloat(REGULAR_KM, defaults.regularGoals.minimumArsPerKm.toFloat()).toDouble(),
            ),
            busyDaysGoals = DriverGoals(
                preferences.getFloat(BUSY_HOURLY, defaults.busyDaysGoals.targetArsPerHour.toFloat()).toDouble(),
                preferences.getFloat(BUSY_KM, defaults.busyDaysGoals.minimumArsPerKm.toFloat()).toDouble(),
            ),
            usualWorkHours = preferences.getFloat(USUAL_HOURS, defaults.usualWorkHours.toFloat()).toDouble(),
            schedule = WeeklySchedule(
                DayOfWeek.values().map { day ->
                    val default = defaults.schedule.days.first { it.day == day }
                    val key = "day_${day.value}_"
                    val start = preferences.getInt(key + "start", default.startMinute).coerceIn(0, 1439)
                    val end = preferences.getInt(key + "end", default.endMinute).coerceIn(0, 1440)
                    WorkDaySchedule(
                        day = day,
                        enabled = preferences.getBoolean(key + "enabled", default.enabled),
                        startMinute = if (start == end) default.startMinute else start,
                        endMinute = if (start == end) default.endMinute else end,
                    )
                },
            ),
        )
    }

    fun save(settings: RideGuardSettings): Boolean {
        val editor = preferences.edit()
            .putFloat(REGULAR_HOURLY, settings.regularGoals.targetArsPerHour.toFloat())
            .putFloat(REGULAR_KM, settings.regularGoals.minimumArsPerKm.toFloat())
            .putFloat(BUSY_HOURLY, settings.busyDaysGoals.targetArsPerHour.toFloat())
            .putFloat(BUSY_KM, settings.busyDaysGoals.minimumArsPerKm.toFloat())
            .putFloat(USUAL_HOURS, settings.usualWorkHours.toFloat())
        settings.schedule.days.forEach { day ->
            val key = "day_${day.day.value}_"
            editor.putBoolean(key + "enabled", day.enabled)
            editor.putInt(key + "start", day.startMinute)
            editor.putInt(key + "end", day.endMinute)
        }
        return editor.commit()
    }

    private companion object {
        const val REGULAR_HOURLY = "regular_hourly"
        const val REGULAR_KM = "regular_km"
        const val BUSY_HOURLY = "busy_hourly"
        const val BUSY_KM = "busy_km"
        const val USUAL_HOURS = "usual_hours"
    }
}
