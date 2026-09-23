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
}
