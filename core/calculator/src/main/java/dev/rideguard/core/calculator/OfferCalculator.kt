package dev.rideguard.core.calculator

import dev.rideguard.core.model.DriverGoals
import dev.rideguard.core.model.OfferEvaluation
import dev.rideguard.core.model.OfferGrade
import dev.rideguard.core.model.OfferMetrics
import dev.rideguard.core.model.RawOffer
import dev.rideguard.core.model.TargetFailure
import kotlin.math.min

object OfferCalculator {
    fun evaluate(
        offer: RawOffer,
        goals: DriverGoals,
    ): OfferEvaluation {
        require(offer.totalMinutes > 0) { "Total offer time must be positive" }
        require(offer.totalKm > 0.0) { "Total offer distance must be positive" }

        val metrics = OfferMetrics(
            totalMinutes = offer.totalMinutes,
            totalKm = offer.totalKm,
            arsPerKm = offer.fareArs / offer.totalKm,
            arsPerMinute = offer.fareArs / offer.totalMinutes,
            arsPerHour = offer.fareArs * 60.0 / offer.totalMinutes,
        )
        val requiredHourlyRate = goals.requiredArsPerHour()
        val hourlyRatio = ratio(metrics.arsPerHour, requiredHourlyRate)
        val kmRatio = ratio(metrics.arsPerKm, goals.minimumArsPerKm)
        val scoreRatio = min(hourlyRatio, kmRatio)
        val unmetTargets = buildSet {
            if (metrics.arsPerHour < requiredHourlyRate) add(TargetFailure.HOURLY_RATE)
            if (metrics.arsPerKm < goals.minimumArsPerKm) add(TargetFailure.PER_KM)
        }
        val grade = when {
            unmetTargets.isEmpty() -> OfferGrade.GOOD
            scoreRatio >= NEAR_MINIMUM_RATIO -> OfferGrade.NEAR
            else -> OfferGrade.BAD
        }

        return OfferEvaluation(
            offer = offer,
            metrics = metrics,
            requiredArsPerHour = requiredHourlyRate,
            scoreRatio = scoreRatio,
            grade = grade,
            unmetTargets = unmetTargets,
        )
    }

    private fun ratio(actual: Double, target: Double): Double =
        if (target <= 0.0) Double.POSITIVE_INFINITY else actual / target

    private const val NEAR_MINIMUM_RATIO = 0.95
}
