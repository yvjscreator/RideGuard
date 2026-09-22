package dev.rideguard.platforms.cabify

import dev.rideguard.core.model.OfferParser
import dev.rideguard.core.model.OfferTextFields
import dev.rideguard.core.model.RawOffer
import dev.rideguard.core.model.RidePlatform

class CabifyOfferParser : OfferParser {
    override val platform = RidePlatform.CABIFY
    override val packageNames = setOf("com.cabify.driver")

    override fun parse(rawText: String): RawOffer? {
        val fare = OfferTextFields.fareArs(rawText) ?: return null
        val pickup = OfferTextFields.leg(
            rawText,
            listOf("Recogida", "Hasta recogida", "Pasajero"),
        ) ?: return null
        val trip = OfferTextFields.leg(rawText, listOf("Viaje", "Trayecto", "Destino")) ?: return null
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
}
