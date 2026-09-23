package dev.rideguard.core.model

object OfferTextFields {
    data class Leg(val minutes: Int, val km: Double)

    private val moneyPattern = Regex("(?:ARS|\\$)\\s*([0-9][0-9.,]*)", RegexOption.IGNORE_CASE)
    private const val durationDistancePattern =
        "(?:(\\d+)\\s*h(?:oras?)?\\s*)?(\\d+)\\s*min(?:utos?)?[^0-9]{0,18}(\\d+(?:[.,]\\d+)?)\\s*(km|m)\\b"
    private val unlabeledLegPattern = Regex(durationDistancePattern, RegexOption.IGNORE_CASE)

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
            pattern = "(?:$labelPattern)\\s*:?\\s*$durationDistancePattern",
            option = RegexOption.IGNORE_CASE,
        )
        val match = pattern.find(text) ?: return null
        return parseLeg(match)
    }

    fun legsInOrder(text: String): List<Leg> = unlabeledLegPattern
        .findAll(text)
        .mapNotNull(::parseLeg)
        .toList()

    private fun parseLeg(match: MatchResult): Leg? {
        val hours = match.groupValues[1].toIntOrNull() ?: 0
        val minutes = match.groupValues[2].toIntOrNull() ?: return null
        val distance = LocaleNumbers.parseDistance(match.groupValues[3]) ?: return null
        return Leg(
            minutes = hours * 60 + minutes,
            km = if (match.groupValues[4].equals("m", ignoreCase = true)) distance / 1_000 else distance,
        )
    }
}
