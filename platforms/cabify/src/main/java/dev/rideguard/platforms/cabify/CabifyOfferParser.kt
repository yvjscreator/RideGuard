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
        val labeledPickup = OfferTextFields.leg(
            rawText,
            listOf("Recogida", "Hasta recogida", "Pasajero"),
        )
        val labeledTrip = OfferTextFields.leg(rawText, listOf("Viaje", "Trayecto", "Destino"))
        val (pickup, trip) = if (labeledPickup != null && labeledTrip != null) {
            labeledPickup to labeledTrip
        } else {
            val legs = OfferTextFields.legsInOrder(rawText)
            if (legs.size != 2) return null
            legs[0] to legs[1]
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
}
