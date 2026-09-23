package dev.rideguard.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DurationDisplayTest {
    @Test
    fun `decimal minutes are converted to minutes and seconds`() {
        assertEquals("1:17", DurationDisplay.clock(1.28))
        assertEquals("14:17", DurationDisplay.clock(13.0 + 1.28))
        assertEquals(77, DurationDisplay.roundedSeconds(1.28))
    }

    @Test
    fun `seconds carry into the next minute`() {
        assertEquals("15:00", DurationDisplay.clock(14.999))
        assertEquals("14:30", DurationDisplay.clock(14.5))
    }
}
