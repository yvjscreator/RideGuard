package dev.rideguard.detection.accessibility

import android.content.Context
import android.content.Intent
import dev.rideguard.core.model.DriverGoals
import dev.rideguard.core.model.TargetMode
import dev.rideguard.core.model.VisualThresholds
import dev.rideguard.core.settings.RideGuardSettings

object DetectorSettingsBroadcast {
    const val ACTION = "dev.rideguard.action.SETTINGS_CHANGED"

    fun createIntent(context: Context, settings: RideGuardSettings): Intent = Intent(ACTION)
        .setPackage(context.packageName)
        .putExtra(TARGET_MODE, settings.goals.targetMode.name)
        .putExtra(TARGET_PER_HOUR, settings.goals.targetArsPerHour)
        .putExtra(MINIMUM_PER_KM, settings.goals.minimumArsPerKm)
        .putExtra(SHIFT_DURATION, settings.goals.shiftDurationMinutes)
        .putExtra(DAILY_TARGET, settings.goals.dailyTargetArs)
        .putExtra(ELAPSED_SHIFT, settings.goals.elapsedShiftMinutes)
        .putExtra(EARNINGS_SO_FAR, settings.goals.earningsSoFarArs)
        .putExtra(GOOD_RATIO, settings.thresholds.goodMinimumRatio)
        .putExtra(WARNING_RATIO, settings.thresholds.warningMinimumRatio)
        .putExtra(GOOD_COLOR, settings.thresholds.goodColorArgb)
        .putExtra(WARNING_COLOR, settings.thresholds.warningColorArgb)
        .putExtra(BAD_COLOR, settings.thresholds.badColorArgb)

    fun read(intent: Intent): RideGuardSettings? {
        if (intent.action != ACTION || !intent.hasExtra(TARGET_MODE)) return null
        val mode = intent.getStringExtra(TARGET_MODE)
            ?.let { raw -> TargetMode.entries.firstOrNull { it.name == raw } }
            ?: TargetMode.HOURLY
        return RideGuardSettings(
            goals = DriverGoals(
                targetMode = mode,
                targetArsPerHour = intent.getDoubleExtra(TARGET_PER_HOUR, 20_000.0),
                minimumArsPerKm = intent.getDoubleExtra(MINIMUM_PER_KM, 700.0),
                shiftDurationMinutes = intent.getIntExtra(SHIFT_DURATION, 360),
                dailyTargetArs = intent.getDoubleExtra(DAILY_TARGET, 120_000.0),
                elapsedShiftMinutes = intent.getIntExtra(ELAPSED_SHIFT, 0),
                earningsSoFarArs = intent.getDoubleExtra(EARNINGS_SO_FAR, 0.0),
            ),
            thresholds = VisualThresholds(
                goodMinimumRatio = intent.getDoubleExtra(GOOD_RATIO, 1.0),
                warningMinimumRatio = intent.getDoubleExtra(WARNING_RATIO, 0.8),
                goodColorArgb = intent.getLongExtra(GOOD_COLOR, 0xFF2E7D32),
                warningColorArgb = intent.getLongExtra(WARNING_COLOR, 0xFFF9A825),
                badColorArgb = intent.getLongExtra(BAD_COLOR, 0xFFC62828),
            ),
        )
    }

    private const val TARGET_MODE = "target_mode"
    private const val TARGET_PER_HOUR = "target_per_hour"
    private const val MINIMUM_PER_KM = "minimum_per_km"
    private const val SHIFT_DURATION = "shift_duration"
    private const val DAILY_TARGET = "daily_target"
    private const val ELAPSED_SHIFT = "elapsed_shift"
    private const val EARNINGS_SO_FAR = "earnings_so_far"
    private const val GOOD_RATIO = "good_ratio"
    private const val WARNING_RATIO = "warning_ratio"
    private const val GOOD_COLOR = "good_color"
    private const val WARNING_COLOR = "warning_color"
    private const val BAD_COLOR = "bad_color"
}
