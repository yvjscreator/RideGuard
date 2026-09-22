package dev.rideguard.platforms.uber

import dev.rideguard.core.model.OfferParser
import dev.rideguard.core.model.OfferTextFields
import dev.rideguard.core.model.LocaleNumbers
import dev.rideguard.core.model.RawOffer
import dev.rideguard.core.model.RidePlatform
import kotlin.math.abs

class UberOfferParser : OfferParser {
    override val platform = RidePlatform.UBER
    override val packageNames = setOf("com.ubercab.driver")

    override fun parse(rawText: String): RawOffer? {
        val fare = OfferTextFields.fareArs(rawText) ?: return null
        val pickup = OfferTextFields.leg(rawText, listOf("A", "Recogida", "Retiro")) ?: return null
        val trip = OfferTextFields.leg(rawText, listOf("Viaje", "Trayecto")) ?: return null
        val totalKm = pickup.km + trip.km
        displayedRate(rawText)?.let { rate ->
            val calculatedRate = fare / totalKm
            val tolerance = maxOf(25.0, rate * 0.08)
            if (abs(calculatedRate - rate) > tolerance) return null
        }
        return RawOffer(
            platform = platform,
            fareArs = fare,
            pickupMinutes = pickup.minutes,
            pickupKm = pickup.km,
            tripMinutes = trip.minutes,
            tripKm = trip.km,
            sourceText = rawText,
        )
    }

    private fun displayedRate(text: String): Double? = PER_KM_PATTERN
        .find(text)
        ?.groupValues
        ?.get(1)
        ?.let(LocaleNumbers::parseMoney)

    private companion object {
        val PER_KM_PATTERN = Regex(
            "(?:ARS|\\$)\\s*([0-9][0-9.,]*)\\s*/\\s*km",
            RegexOption.IGNORE_CASE,
        )
    }
}
