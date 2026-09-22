package dev.rideguard.core.calculator

import dev.rideguard.core.model.DriverGoals
import dev.rideguard.core.model.OfferGrade
import dev.rideguard.core.model.RawOffer
import dev.rideguard.core.model.RidePlatform
import dev.rideguard.core.model.TargetFailure
import org.junit.Assert.assertEquals
import org.junit.Test

class OfferCalculatorTest {
    private val observedUberOffer = RawOffer(
        platform = RidePlatform.UBER,
        fareArs = 3_903.0,
        pickupMinutes = 6,
        pickupKm = 1.4,
        tripMinutes = 20,
        tripKm = 5.6,
        sourceText = "observed",
    )

    @Test
    fun `includes pickup time and distance`() {
        val result = OfferCalculator.evaluate(
            observedUberOffer,
            DriverGoals(targetArsPerHour = 9_000.0, minimumArsPerKm = 500.0),
        )

        assertEquals(26, result.metrics.totalMinutes)
        assertEquals(7.0, result.metrics.totalKm, 0.001)
        assertEquals(557.57, result.metrics.arsPerKm, 0.01)
        assertEquals(9_006.92, result.metrics.arsPerHour, 0.01)
        assertEquals(OfferGrade.GOOD, result.grade)
    }

    @Test
    fun `hourly rate alone does not make an offer good`() {
        val result = OfferCalculator.evaluate(
            observedUberOffer,
            DriverGoals(targetArsPerHour = 9_000.0, minimumArsPerKm = 650.0),
        )

        assertEquals(true, result.grade != OfferGrade.GOOD)
        assertEquals(setOf(TargetFailure.PER_KM), result.unmetTargets)
    }

    @Test
    fun `per kilometer rate alone does not make an offer good`() {
        val result = OfferCalculator.evaluate(
            observedUberOffer,
            DriverGoals(targetArsPerHour = 20_000.0, minimumArsPerKm = 500.0),
        )

        assertEquals(OfferGrade.BAD, result.grade)
        assertEquals(setOf(TargetFailure.HOURLY_RATE), result.unmetTargets)
    }

    @Test
    fun `one peso below the minimum is near but not good`() {
        val offer = observedUberOffer.copy(
            fareArs = 749.0,
            pickupMinutes = 0,
            pickupKm = 0.0,
            tripMinutes = 3,
            tripKm = 1.0,
        )
        val goals = DriverGoals(targetArsPerHour = 15_000.0, minimumArsPerKm = 750.0)

        val result = OfferCalculator.evaluate(offer, goals)

        assertEquals(OfferGrade.NEAR, result.grade)
        assertEquals(setOf(TargetFailure.HOURLY_RATE, TargetFailure.PER_KM), result.unmetTargets)
        assertEquals(OfferGrade.GOOD, OfferCalculator.evaluate(offer.copy(fareArs = 750.0), goals).grade)
    }

    @Test
    fun `an offer further than five percent below either minimum is bad`() {
        val offer = observedUberOffer.copy(
            fareArs = 700.0,
            pickupMinutes = 0,
            pickupKm = 0.0,
            tripMinutes = 3,
            tripKm = 1.0,
        )

        val result = OfferCalculator.evaluate(
            offer,
            DriverGoals(targetArsPerHour = 15_000.0, minimumArsPerKm = 750.0),
        )

        assertEquals(OfferGrade.BAD, result.grade)
    }
}
