package dev.rideguard.detection.accessibility

import android.content.Context
import android.content.Intent
import dev.rideguard.core.model.DriverGoals
import dev.rideguard.core.model.WeeklySchedule
import dev.rideguard.core.model.WorkDaySchedule
import dev.rideguard.core.settings.RideGuardSettings
import java.time.DayOfWeek

object DetectorSettingsBroadcast {
    const val ACTION = "dev.rideguard.action.SETTINGS_CHANGED"

    fun createIntent(context: Context, settings: RideGuardSettings): Intent {
        val days = settings.schedule.days.sortedBy { it.day.value }
        return Intent(ACTION)
            .setPackage(context.packageName)
            .putExtra(REGULAR_HOURLY, settings.regularGoals.targetArsPerHour)
            .putExtra(REGULAR_KM, settings.regularGoals.minimumArsPerKm)
            .putExtra(BUSY_HOURLY, settings.busyDaysGoals.targetArsPerHour)
            .putExtra(BUSY_KM, settings.busyDaysGoals.minimumArsPerKm)
            .putExtra(USUAL_HOURS, settings.usualWorkHours)
            .putExtra(ENABLED_DAYS, days.map { it.enabled }.toBooleanArray())
            .putExtra(START_MINUTES, days.map { it.startMinute }.toIntArray())
            .putExtra(END_MINUTES, days.map { it.endMinute }.toIntArray())
    }

    fun read(intent: Intent): RideGuardSettings? {
        if (intent.action != ACTION || !intent.hasExtra(REGULAR_HOURLY)) return null
        val defaults = RideGuardSettings()
        val enabled = intent.getBooleanArrayExtra(ENABLED_DAYS) ?: return null
        val starts = intent.getIntArrayExtra(START_MINUTES) ?: return null
        val ends = intent.getIntArrayExtra(END_MINUTES) ?: return null
        if (enabled.size != 7 || starts.size != 7 || ends.size != 7) return null
        return runCatching {
            RideGuardSettings(
                regularGoals = DriverGoals(
                    intent.getDoubleExtra(REGULAR_HOURLY, defaults.regularGoals.targetArsPerHour),
                    intent.getDoubleExtra(REGULAR_KM, defaults.regularGoals.minimumArsPerKm),
                ),
                busyDaysGoals = DriverGoals(
                    intent.getDoubleExtra(BUSY_HOURLY, defaults.busyDaysGoals.targetArsPerHour),
                    intent.getDoubleExtra(BUSY_KM, defaults.busyDaysGoals.minimumArsPerKm),
                ),
                usualWorkHours = intent.getDoubleExtra(USUAL_HOURS, defaults.usualWorkHours),
                schedule = WeeklySchedule(DayOfWeek.values().mapIndexed { index, day ->
                    WorkDaySchedule(day, enabled[index], starts[index], ends[index])
                }),
            )
        }.getOrNull()
    }

    private const val REGULAR_HOURLY = "regular_hourly"
    private const val REGULAR_KM = "regular_km"
    private const val BUSY_HOURLY = "busy_hourly"
    private const val BUSY_KM = "busy_km"
    private const val USUAL_HOURS = "usual_hours"
    private const val ENABLED_DAYS = "enabled_days"
    private const val START_MINUTES = "start_minutes"
    private const val END_MINUTES = "end_minutes"
}
