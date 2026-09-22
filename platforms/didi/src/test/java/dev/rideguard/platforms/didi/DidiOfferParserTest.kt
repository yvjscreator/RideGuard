package dev.rideguard.platforms.didi

import org.junit.Assert.assertEquals
import org.junit.Test

class DidiOfferParserTest {
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
