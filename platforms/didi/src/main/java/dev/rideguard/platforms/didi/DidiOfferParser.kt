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
        val labeledPickup = OfferTextFields.leg(
            rawText,
            listOf("Recoger", "Recogida", "Hasta el pasajero"),
        )
        val labeledTrip = OfferTextFields.leg(rawText, listOf("Viaje", "Destino", "Recorrido"))
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
