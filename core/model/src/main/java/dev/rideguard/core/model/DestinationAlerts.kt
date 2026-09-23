package dev.rideguard.core.model

import java.text.Normalizer
import java.util.Locale

/** Only fields identified in the destination of an offer, never map labels or pickup text. */
data class OfferDestination(
    val zone: String? = null,
    val street: String? = null,
)

data class AvoidedStreet(
    val name: String,
    /** Null means that the street is avoided in every zone. */
    val onlyInZone: String? = null,
)

data class DestinationAlert(
    val kind: Kind,
    val matchedName: String,
) {
    enum class Kind { ZONE, STREET }
}

object DestinationAlerts {
    fun find(
        destination: OfferDestination?,
        avoidedZones: List<String>,
        avoidedStreets: List<AvoidedStreet>,
    ): DestinationAlert? {
        if (destination == null) return null
        val zone = key(destination.zone)
        val street = streetKey(destination.street)

        // A street restriction overrides an otherwise allowed zone.
        val streetRule = avoidedStreets.firstOrNull { rule ->
            val requiredZone = key(rule.onlyInZone)
            street.isNotEmpty() && streetMatches(street, streetKey(rule.name)) &&
                (requiredZone.isEmpty() || requiredZone == zone)
        }
        if (streetRule != null) return DestinationAlert(DestinationAlert.Kind.STREET, streetRule.name)

        val zoneRule = avoidedZones.firstOrNull { zone.isNotEmpty() && key(it) == zone }
        return zoneRule?.let { DestinationAlert(DestinationAlert.Kind.ZONE, it) }
    }

    private fun streetMatches(destination: String, rule: String): Boolean =
        rule.isNotEmpty() && (destination == rule ||
            (destination.startsWith("$rule ") && destination.removePrefix("$rule ").matches(HOUSE_NUMBER)))

    private fun streetKey(value: String?): String = key(value)
        .replace(Regex("^av(?:da)? "), "avenida ")
        .removePrefix("calle ")

    private fun key(value: String?): String {
        if (value.isNullOrBlank()) return ""
        val unaccented = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase(Locale.ROOT)
        return unaccented.replace(Regex("[^a-z0-9]+"), " ").trim().replace(Regex("\\s+"), " ")
    }

    private val HOUSE_NUMBER = Regex("\\d{1,6}[a-z]?")
}
