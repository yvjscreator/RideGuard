package dev.rideguard.core.calculator

import dev.rideguard.core.model.DriverGoals
import dev.rideguard.core.model.OfferGrade
import dev.rideguard.core.model.RawOffer
import dev.rideguard.core.model.RidePlatform
import dev.rideguard.core.model.TargetMode
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
    fun `uses remaining shift target`() {
        val goals = DriverGoals(
            targetMode = TargetMode.SHIFT,
            shiftDurationMinutes = 360,
            dailyTargetArs = 120_000.0,
            elapsedShiftMinutes = 180,
            earningsSoFarArs = 70_000.0,
        )

        assertEquals(16_666.67, goals.requiredArsPerHour(), 0.01)
    }

    @Test
    fun `bad when hourly target is missed`() {
        val result = OfferCalculator.evaluate(
            observedUberOffer,
            DriverGoals(targetArsPerHour = 20_000.0, minimumArsPerKm = 500.0),
        )

        assertEquals(OfferGrade.BAD, result.grade)
    }
}
