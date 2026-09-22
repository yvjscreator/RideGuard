package dev.rideguard.core.settings

import android.content.Context
import dev.rideguard.core.model.DriverGoals
import dev.rideguard.core.model.TargetMode
import dev.rideguard.core.model.VisualThresholds

data class RideGuardSettings(
    val goals: DriverGoals = DriverGoals(),
    val thresholds: VisualThresholds = VisualThresholds(),
)

class RideGuardSettingsStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "ride_guard_settings",
        Context.MODE_PRIVATE,
    )

    fun load(): RideGuardSettings {
        val defaults = RideGuardSettings()
        return RideGuardSettings(
            goals = DriverGoals(
                targetMode = enumValueOrDefault(
                    preferences.getString(KEY_TARGET_MODE, null),
                    defaults.goals.targetMode,
                ),
                targetArsPerHour = preferences.getFloat(
                    KEY_TARGET_PER_HOUR,
                    defaults.goals.targetArsPerHour.toFloat(),
                ).toDouble(),
                minimumArsPerKm = preferences.getFloat(
                    KEY_MINIMUM_PER_KM,
                    defaults.goals.minimumArsPerKm.toFloat(),
                ).toDouble(),
                shiftDurationMinutes = preferences.getInt(
                    KEY_SHIFT_DURATION,
                    defaults.goals.shiftDurationMinutes,
                ),
                dailyTargetArs = preferences.getFloat(
                    KEY_DAILY_TARGET,
                    defaults.goals.dailyTargetArs.toFloat(),
                ).toDouble(),
                elapsedShiftMinutes = preferences.getInt(
                    KEY_ELAPSED_SHIFT,
                    defaults.goals.elapsedShiftMinutes,
                ),
                earningsSoFarArs = preferences.getFloat(
                    KEY_EARNINGS_SO_FAR,
                    defaults.goals.earningsSoFarArs.toFloat(),
                ).toDouble(),
            ),
            thresholds = VisualThresholds(
                goodMinimumRatio = preferences.getFloat(
                    KEY_GOOD_RATIO,
                    defaults.thresholds.goodMinimumRatio.toFloat(),
                ).toDouble(),
                warningMinimumRatio = preferences.getFloat(
                    KEY_WARNING_RATIO,
                    defaults.thresholds.warningMinimumRatio.toFloat(),
                ).toDouble(),
                goodColorArgb = preferences.getLong(
                    KEY_GOOD_COLOR,
                    defaults.thresholds.goodColorArgb,
                ),
                warningColorArgb = preferences.getLong(
                    KEY_WARNING_COLOR,
                    defaults.thresholds.warningColorArgb,
                ),
                badColorArgb = preferences.getLong(
                    KEY_BAD_COLOR,
                    defaults.thresholds.badColorArgb,
                ),
            ),
        )
    }

    fun save(settings: RideGuardSettings) {
        preferences.edit()
            .putString(KEY_TARGET_MODE, settings.goals.targetMode.name)
            .putFloat(KEY_TARGET_PER_HOUR, settings.goals.targetArsPerHour.toFloat())
            .putFloat(KEY_MINIMUM_PER_KM, settings.goals.minimumArsPerKm.toFloat())
            .putInt(KEY_SHIFT_DURATION, settings.goals.shiftDurationMinutes)
            .putFloat(KEY_DAILY_TARGET, settings.goals.dailyTargetArs.toFloat())
            .putInt(KEY_ELAPSED_SHIFT, settings.goals.elapsedShiftMinutes)
            .putFloat(KEY_EARNINGS_SO_FAR, settings.goals.earningsSoFarArs.toFloat())
            .putFloat(KEY_GOOD_RATIO, settings.thresholds.goodMinimumRatio.toFloat())
            .putFloat(KEY_WARNING_RATIO, settings.thresholds.warningMinimumRatio.toFloat())
            .putLong(KEY_GOOD_COLOR, settings.thresholds.goodColorArgb)
            .putLong(KEY_WARNING_COLOR, settings.thresholds.warningColorArgb)
            .putLong(KEY_BAD_COLOR, settings.thresholds.badColorArgb)
            .apply()
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(raw: String?, default: T): T =
        raw?.let { value -> enumValues<T>().firstOrNull { it.name == value } } ?: default

    private companion object {
        const val KEY_TARGET_MODE = "target_mode"
        const val KEY_TARGET_PER_HOUR = "target_per_hour"
        const val KEY_MINIMUM_PER_KM = "minimum_per_km"
        const val KEY_SHIFT_DURATION = "shift_duration"
        const val KEY_DAILY_TARGET = "daily_target"
        const val KEY_ELAPSED_SHIFT = "elapsed_shift"
        const val KEY_EARNINGS_SO_FAR = "earnings_so_far"
        const val KEY_GOOD_RATIO = "good_ratio"
        const val KEY_WARNING_RATIO = "warning_ratio"
        const val KEY_GOOD_COLOR = "good_color"
        const val KEY_WARNING_COLOR = "warning_color"
        const val KEY_BAD_COLOR = "bad_color"
    }
}
