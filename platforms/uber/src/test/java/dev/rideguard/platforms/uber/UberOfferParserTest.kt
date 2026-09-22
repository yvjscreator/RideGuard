package dev.rideguard.platforms.uber

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class UberOfferParserTest {
    @Test
    fun `parses observed Buenos Aires offer`() {
        val offer = UberOfferParser().parse(
            """
            ARS3,903
            ARS558/km (estimado)
            A 6 min (1.4 km)
            Viaje: 20 min (5.6 km)
            """.trimIndent(),
        )

        assertNotNull(offer)
        assertEquals(3_903.0, offer!!.fareArs, 0.001)
        assertEquals(6, offer.pickupMinutes)
        assertEquals(1.4, offer.pickupKm, 0.001)
        assertEquals(20, offer.tripMinutes)
        assertEquals(5.6, offer.tripKm, 0.001)
    }

    @Test
    fun `ignores incomplete screens`() {
        assertEquals(null, UberOfferParser().parse("Buscando solicitud de viaje\nARS 0.00"))
    }

    @Test
    fun `rejects values mixed during an offer transition`() {
        val offer = UberOfferParser().parse(
            "ARS4,101\nARS873/km (estimado)\nA 5 min (1.4 km)\nViaje: 18 min (4.2 km)",
        )

        assertEquals(null, offer)
    }
}
