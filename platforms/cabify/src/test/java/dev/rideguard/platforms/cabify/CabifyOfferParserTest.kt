package dev.rideguard.platforms.cabify

import org.junit.Assert.assertEquals
import org.junit.Test

class CabifyOfferParserTest {
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
