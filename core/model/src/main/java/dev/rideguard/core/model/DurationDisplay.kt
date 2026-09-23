package dev.rideguard.core.model

import kotlin.math.roundToInt

/** Formats decimal minutes for display; calculations keep the unrounded value. */
object DurationDisplay {
    fun roundedSeconds(decimalMinutes: Double): Int {
        require(decimalMinutes.isFinite() && decimalMinutes >= 0.0)
        return (decimalMinutes * 60.0).roundToInt()
    }

    fun clock(decimalMinutes: Double): String {
        val seconds = roundedSeconds(decimalMinutes)
        return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
    }
}
