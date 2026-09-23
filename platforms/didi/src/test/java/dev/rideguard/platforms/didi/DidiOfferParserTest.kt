package dev.rideguard.platforms.didi

import org.junit.Assert.assertEquals
import org.junit.Test

class DidiOfferParserTest {
    @Test
    fun `parses observed pickup in meters without labels`() {
        val offer = DidiOfferParser().parse(
            "$4.300\n$1.500 de tarifa base dinámica\n4 min (390 m)\n21 min (5,6 km)",
        )!!

        assertEquals(4_300.0, offer.fareArs, 0.001)
        assertEquals(4, offer.pickupMinutes)
        assertEquals(0.390, offer.pickupKm, 0.001)
        assertEquals(21, offer.tripMinutes)
        assertEquals(5.6, offer.tripKm, 0.001)
    }

    @Test
    fun `parses pickup and trip`() {
        val offer = DidiOfferParser().parse(
            "$ 4.850\nRecoger: 4 min (1,0 km)\nViaje: 15 min (5,5 km)",
        )!!

        assertEquals(4_850.0, offer.fareArs, 0.001)
        assertEquals(19, offer.totalMinutes)
        assertEquals(6.5, offer.totalKm, 0.001)
    }
}
