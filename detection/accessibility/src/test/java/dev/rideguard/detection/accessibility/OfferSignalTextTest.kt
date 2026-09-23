package dev.rideguard.detection.accessibility

import dev.rideguard.core.calculator.OfferCalculator
import dev.rideguard.core.model.DriverGoals
import dev.rideguard.platforms.uber.UberOfferParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class OfferSignalTextTest {
    @Test
    fun `complete notification fields can be parsed and use pickup wait in hourly rate`() {
        val text = OfferSignalText.join(listOf(
            "Nueva solicitud",
            "ARS3,903",
            "A 6 min (1.4 km)",
            "Viaje: 20 min (5.6 km)",
            "ARS3,903",
        ))
        val offer = UberOfferParser().parse(text)

        assertNotNull(offer)
        assertEquals(4, text.lines().size)
        val evaluation = OfferCalculator.evaluate(offer!!, DriverGoals())
        assertEquals(27.28, evaluation.metrics.totalMinutes, 0.001)
        assertEquals(8_584.31, evaluation.metrics.arsPerHour, 0.01)
    }

    @Test
    fun `notification without trip details is not classified`() {
        val text = OfferSignalText.join(listOf("Nueva solicitud de viaje", "Abrir Uber Driver"))

        assertEquals(null, UberOfferParser().parse(text))
    }
}
