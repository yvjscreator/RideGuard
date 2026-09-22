package dev.rideguard.core.model

enum class RidePlatform {
    UBER,
    CABIFY,
    DIDI,
    UNKNOWN,
}

enum class TargetMode {
    HOURLY,
    SHIFT,
}

enum class OfferGrade {
    GOOD,
    WARNING,
    BAD,
}

data class RawOffer(
    val platform: RidePlatform,
    val fareArs: Double,
    val pickupMinutes: Int,
    val pickupKm: Double,
    val tripMinutes: Int,
    val tripKm: Double,
    val sourceText: String,
) {
    val totalMinutes: Int get() = pickupMinutes + tripMinutes
    val totalKm: Double get() = pickupKm + tripKm
}

data class DriverGoals(
    val targetMode: TargetMode = TargetMode.HOURLY,
    val targetArsPerHour: Double = 20_000.0,
    val minimumArsPerKm: Double = 700.0,
    val shiftDurationMinutes: Int = 360,
    val dailyTargetArs: Double = 120_000.0,
    val elapsedShiftMinutes: Int = 0,
    val earningsSoFarArs: Double = 0.0,
) {
    fun requiredArsPerHour(): Double {
        if (targetMode == TargetMode.HOURLY) return targetArsPerHour.coerceAtLeast(0.0)

        val remainingMinutes = (shiftDurationMinutes - elapsedShiftMinutes).coerceAtLeast(1)
        val remainingTarget = (dailyTargetArs - earningsSoFarArs).coerceAtLeast(0.0)
        return remainingTarget * 60.0 / remainingMinutes
    }
}

data class VisualThresholds(
    val goodMinimumRatio: Double = 1.0,
    val warningMinimumRatio: Double = 0.8,
    val goodColorArgb: Long = 0xFF2E7D32,
    val warningColorArgb: Long = 0xFFF9A825,
    val badColorArgb: Long = 0xFFC62828,
)

data class OfferMetrics(
    val totalMinutes: Int,
    val totalKm: Double,
    val arsPerKm: Double,
    val arsPerMinute: Double,
    val arsPerHour: Double,
)

data class OfferEvaluation(
    val offer: RawOffer,
    val metrics: OfferMetrics,
    val requiredArsPerHour: Double,
    val scoreRatio: Double,
    val grade: OfferGrade,
    val unmetTargets: Set<TargetFailure>,
)

enum class TargetFailure {
    HOURLY_RATE,
    PER_KM,
}

interface OfferParser {
    val platform: RidePlatform
    val packageNames: Set<String>
    fun parse(rawText: String): RawOffer?
}
