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

    @Test
    fun `priority bonus before total fare does not suppress the offer`() {
        val offer = UberOfferParser().parse(
            "Uber Priority\n+ARS563.00 por inicio de viaje prioritario\n" +
                "ARS4,104\nARS1,140/km (estimado)\nA 4 min (1.0 km)\n" +
                "Viaje: 12 min (2.6 km)",
        )

        assertNotNull(offer)
        assertEquals(4_104.0, offer!!.fareArs, 0.001)
        assertEquals(3.6, offer.totalKm, 0.001)
    }

    @Test
    fun `second observed priority offer also picks total fare`() {
        val offer = UberOfferParser().parse(
            "+ARS846.00 por inicio de viaje prioritario\nUber Priority\n" +
                "ARS6,008\nARS733/km (estimado)\nA 1 min (0.2 km)\n" +
                "Viaje: 27 min (8.0 km)",
        )

        assertNotNull(offer)
        assertEquals(6_008.0, offer!!.fareArs, 0.001)
        assertEquals(28, offer.totalMinutes)
    }

    @Test
    fun `single accessibility line can contain bonus and total fare`() {
        val offer = UberOfferParser().parse(
            "Uber Priority +ARS563.00 por inicio de viaje prioritario " +
                "ARS4,104 ARS1,140/km (estimado) A 4 min (1.0 km) Viaje: 12 min (2.6 km)",
        )

        assertEquals(4_104.0, offer?.fareArs ?: 0.0, 0.001)
    }

    @Test
    fun `total fare before bonus on one line remains the total fare`() {
        val offer = UberOfferParser().parse(
            "ARS4,104 +ARS563.00 por inicio de viaje prioritario " +
                "ARS1,140/km (estimado) A 4 min (1.0 km) Viaje: 12 min (2.6 km)",
        )

        assertEquals(4_104.0, offer?.fareArs ?: 0.0, 0.001)
    }

    @Test
    fun `bonus alone is not mistaken for total fare`() {
        val offer = UberOfferParser().parse(
            "+ARS563.00 por inicio de viaje prioritario\nARS156/km (estimado)\n" +
                "A 4 min (1.0 km)\nViaje: 12 min (2.6 km)",
        )

        assertEquals(null, offer)
    }
}
