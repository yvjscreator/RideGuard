package dev.rideguard.platforms.cabify

import org.junit.Assert.assertEquals
import org.junit.Test

class CabifyOfferParserTest {
    @Test
    fun `parses observed unlabeled Buenos Aires offer`() {
        val offer = CabifyOfferParser().parse(
            "CABIFY PRIORITARIO\n$ 4.000 en app\n$ 1.169/km\n6 min · 1.1 km\n14 min · 3.4 km",
        )!!

        assertEquals(4_000.0, offer.fareArs, 0.001)
        assertEquals(6, offer.pickupMinutes)
        assertEquals(1.1, offer.pickupKm, 0.001)
        assertEquals(14, offer.tripMinutes)
        assertEquals(3.4, offer.tripKm, 0.001)
    }

    @Test
    fun `parses localized decimal commas`() {
        val offer = CabifyOfferParser().parse(
            "ARS 5.200\nRecogida 5 min · 1,2 km\nTrayecto 18 min · 6,4 km",
        )!!

        assertEquals(5_200.0, offer.fareArs, 0.001)
        assertEquals(1.2, offer.pickupKm, 0.001)
        assertEquals(6.4, offer.tripKm, 0.001)
    }

    @Test
    fun `destination address comes after second leg`() {
        val offer = CabifyOfferParser().parse(
            "$ 6.305 en app\n6 min · 1 km\nPalermo - Calle Silvio L. Ruggieri, 2767\n" +
                "30 min · 8.6 km\nColonia Express - Avenida Elvira Rawson de Dellepiane, 155",
        )!!

        assertEquals("Avenida Elvira Rawson de Dellepiane, 155", offer.destination?.street)
    }

    @Test
    fun `reads cabify zone before street when present`() {
        val offer = CabifyOfferParser().parse(
            "$ 4.000 en app\n6 min · 1.1 km\nRecoleta - Calle Posadas, 1000\n" +
                "14 min · 3.4 km\nPalermo - Av. Rafael Obligado, 1234",
        )!!

        assertEquals("Palermo", offer.destination?.zone)
        assertEquals("Av. Rafael Obligado, 1234", offer.destination?.street)
    }
}
