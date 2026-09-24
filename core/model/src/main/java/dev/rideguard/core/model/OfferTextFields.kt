package dev.rideguard.core.model

import kotlin.math.abs

object OfferTextFields {
    data class Leg(val minutes: Int, val km: Double)

    private val moneyPattern = Regex("(?:ARS|\\$)\\s*([0-9][0-9.,]*)", RegexOption.IGNORE_CASE)
    private const val durationDistancePattern =
        "(?:(\\d+)\\s*h(?:oras?)?\\s*)?(\\d+)\\s*min(?:utos?)?[^0-9]{0,18}(\\d+(?:[.,]\\d+)?)\\s*(km|m)\\b"
    private val unlabeledLegPattern = Regex(durationDistancePattern, RegexOption.IGNORE_CASE)

    /** Finds the first total fare, ignoring a separately advertised priority bonus. */
    fun fareArs(text: String): Double? = moneyCandidates(text)
        .firstOrNull { !it.isSupplement }
        ?.amount

    /** A displayed ARS/km rate can disambiguate fare and bonus regardless of node order. */
    fun fareClosestTo(text: String, expectedArs: Double): Double? = moneyCandidates(text)
        .filterNot(MoneyCandidate::isSupplement)
        .minByOrNull { abs(it.amount - expectedArs) }
        ?.amount

    private fun moneyCandidates(text: String): List<MoneyCandidate> = text.lineSequence()
        .flatMap { line ->
            val matches = moneyPattern.findAll(line).toList()
            matches.asSequence().mapIndexedNotNull { index, match ->
                val suffix = line.substring(match.range.last + 1).trimStart()
                if (suffix.startsWith("/km", ignoreCase = true)) return@mapIndexedNotNull null
                val amount = LocaleNumbers.parseMoney(match.groupValues[1]) ?: return@mapIndexedNotNull null
                val prefix = line.substring(0, match.range.first).trimEnd()
                val nextMoneyOffset = matches.getOrNull(index + 1)?.range?.first ?: line.length
                val localSuffix = line.substring(match.range.last + 1, nextMoneyOffset)
                MoneyCandidate(
                    amount,
                    prefix.endsWith('+') || localSuffix.contains("por inicio", ignoreCase = true),
                )
            }
        }
        .toList()

    private data class MoneyCandidate(val amount: Double, val isSupplement: Boolean)

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
