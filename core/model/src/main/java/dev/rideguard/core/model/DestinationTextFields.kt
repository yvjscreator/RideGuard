package dev.rideguard.core.model

/** Reads the address immediately after the trip leg; an ambiguous address stays partly unknown. */
object DestinationTextFields {
    fun afterLabeledTrip(text: String): OfferDestination? {
        val lines = text.lines().map(String::trim).filter(String::isNotEmpty)
        val tripIndex = lines.indexOfFirst { line ->
            line.startsWith("Viaje:", ignoreCase = true) ||
                line.startsWith("Trayecto:", ignoreCase = true)
        }
        return lines.getOrNull(tripIndex + 1)?.takeIf { tripIndex >= 0 }?.let(::parseAddress)
    }

    fun afterSecondLeg(text: String): OfferDestination? {
        val lines = text.lines().map(String::trim).filter(String::isNotEmpty)
        val legIndices = lines.indices.filter { OfferTextFields.legsInOrder(lines[it]).isNotEmpty() }
        if (legIndices.size != 2) return null
        return lines.getOrNull(legIndices[1] + 1)?.let(::parseAddress)
    }

    fun parseAddress(raw: String): OfferDestination? {
        val address = raw.trim().take(MAX_ADDRESS_CHARS)
        if (address.isBlank() || OfferTextFields.legsInOrder(address).isNotEmpty()) return null

        CABA_SUFFIX.matchEntire(address)?.let { match ->
            return OfferDestination(zone = match.groupValues[2].trim(), street = match.groupValues[1].trim(), zoneConfirmed = true)
        }

        val parts = address.split(SPACED_DASH, limit = 2).map(String::trim)
        if (parts.size == 2 && parts.all(String::isNotBlank)) {
            val (first, second) = parts
            return when {
                looksLikeStreet(first) -> OfferDestination(zone = second, street = first)
                looksLikeStreet(second) && looksLikeZone(first) -> OfferDestination(zone = first, street = second)
                looksLikeStreet(second) -> OfferDestination(street = second)
                else -> null
            }
        }

        val comma = address.lastIndexOf(',')
        if (comma >= 0) {
            val first = address.substring(0, comma).trim()
            val last = address.substring(comma + 1).trim()
            if (first.isNotBlank() && looksLikeZone(last)) {
                return OfferDestination(zone = last, street = first)
            }
        }
        return OfferDestination(street = address)
    }

    private fun looksLikeStreet(value: String): Boolean =
        STREET_PREFIX.containsMatchIn(value) || HOUSE_NUMBER.containsMatchIn(value)

    private fun looksLikeZone(value: String): Boolean =
        COMUNA.matches(value) ||
            (value.length <= 45 && !looksLikeStreet(value) && !value.contains(',') && value.none(Char::isDigit))

    private const val MAX_ADDRESS_CHARS = 180
    private val CABA_SUFFIX = Regex("(.+),\\s*(?:CABA|Ciudad Autónoma de Buenos Aires)\\s*[-–]\\s*([^,]+)", RegexOption.IGNORE_CASE)
    private val SPACED_DASH = Regex("\\s+[-–]\\s+")
    private val STREET_PREFIX = Regex("^(?:avenida|av\\.?|avda\\.?|calle|pasaje|pje\\.?|boulevard|blvd\\.?|ruta)(?=\\s|$)", RegexOption.IGNORE_CASE)
    private val HOUSE_NUMBER = Regex("(?:,\\s*|\\s+)\\d{1,6}(?:\\s|$)")
    private val COMUNA = Regex("comuna\\s+\\d{1,2}", RegexOption.IGNORE_CASE)
}
