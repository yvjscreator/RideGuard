package dev.rideguard.detection.accessibility

import dev.rideguard.core.model.OfferParser
import dev.rideguard.platforms.cabify.CabifyOfferParser
import dev.rideguard.platforms.didi.DidiOfferParser
import dev.rideguard.platforms.uber.UberOfferParser

internal class PlatformParserRegistry(
    parsers: List<OfferParser> = listOf(
        UberOfferParser(),
        CabifyOfferParser(),
        DidiOfferParser(),
    ),
) {
    private val parsersByPackage = parsers
        .flatMap { parser -> parser.packageNames.map { packageName -> packageName to parser } }
        .toMap()

    fun forPackage(packageName: String): OfferParser? = parsersByPackage[packageName]
}
