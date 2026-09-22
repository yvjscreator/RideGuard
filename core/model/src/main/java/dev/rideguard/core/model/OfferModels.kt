package dev.rideguard.core.model

enum class RidePlatform {
    UBER,
    CABIFY,
    DIDI,
    UNKNOWN,
}

enum class OfferGrade {
    GOOD,
    NEAR,
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
    val targetArsPerHour: Double = 15_000.0,
    val minimumArsPerKm: Double = 650.0,
) {
    fun requiredArsPerHour(): Double = targetArsPerHour.coerceAtLeast(0.0)
}

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
