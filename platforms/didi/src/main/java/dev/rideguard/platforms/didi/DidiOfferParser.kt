package dev.rideguard.platforms.didi

import dev.rideguard.core.model.OfferParser
import dev.rideguard.core.model.OfferTextFields
import dev.rideguard.core.model.RawOffer
import dev.rideguard.core.model.RidePlatform

class DidiOfferParser : OfferParser {
    override val platform = RidePlatform.DIDI
    override val packageNames = setOf("com.didiglobal.driver")

    override fun parse(rawText: String): RawOffer? {
        val fare = OfferTextFields.fareArs(rawText) ?: return null
        val pickup = OfferTextFields.leg(
            rawText,
            listOf("Recoger", "Recogida", "Hasta el pasajero"),
        ) ?: return null
        val trip = OfferTextFields.leg(rawText, listOf("Viaje", "Destino", "Recorrido")) ?: return null
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
