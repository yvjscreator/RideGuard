package dev.rideguard.core.model

object OfferTextFields {
    data class Leg(val minutes: Int, val km: Double)

    private val moneyPattern = Regex("(?:ARS|\\$)\\s*([0-9][0-9.,]*)", RegexOption.IGNORE_CASE)

    fun fareArs(text: String): Double? = text
        .lineSequence()
        .map(String::trim)
        .firstNotNullOfOrNull { line ->
            if (line.contains("/km", ignoreCase = true)) null
            else moneyPattern.find(line)?.groupValues?.get(1)?.let(LocaleNumbers::parseMoney)
        }

    fun leg(text: String, labels: List<String>): Leg? {
        val labelPattern = labels.joinToString("|") { Regex.escape(it) }
        val pattern = Regex(
            pattern = "(?:$labelPattern)\\s*:?\\s*(\\d+)\\s*min(?:utos?)?[^0-9]{0,18}(\\d+(?:[.,]\\d+)?)\\s*km",
            option = RegexOption.IGNORE_CASE,
        )
        val match = pattern.find(text) ?: return null
        return Leg(
            minutes = match.groupValues[1].toIntOrNull() ?: return null,
            km = LocaleNumbers.parseDistance(match.groupValues[2]) ?: return null,
        )
    }
}
