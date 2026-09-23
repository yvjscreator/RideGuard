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
    fun `parses a long offer shown over another app`() {
        val offer = UberOfferParser().parse(
            "ARS22,795\nARS465/km (estimado)\nA 1 min (0.2 km)\nViaje: 1 h 29 min (48.8 km)",
        )

        assertNotNull(offer)
        assertEquals(22_795.0, offer!!.fareArs, 0.001)
        assertEquals(1, offer.pickupMinutes)
        assertEquals(89, offer.tripMinutes)
        assertEquals(49.0, offer.pickupKm + offer.tripKm, 0.001)
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

    @Test
    fun `destination zone and street come after trip not pickup`() {
        val offer = UberOfferParser().parse(
            "ARS5,007\nA 4 min (0.9 km)\nPosadas, CABA - Recoleta\n" +
                "Viaje: 9 min (3.5 km)\nRepública Árabe Siria 3247, CABA - Palermo",
        )!!

        assertEquals("Palermo", offer.destination?.zone)
        assertEquals("República Árabe Siria 3247", offer.destination?.street)
    }
}
