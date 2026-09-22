package dev.rideguard.core.model

object LocaleNumbers {
    fun parseMoney(raw: String): Double? {
        val cleaned = raw
            .replace("ARS", "", ignoreCase = true)
            .replace("$", "")
            .replace("\u00A0", "")
            .replace(" ", "")
            .filter { it.isDigit() || it == ',' || it == '.' || it == '-' }

        if (cleaned.isBlank()) return null

        val comma = cleaned.lastIndexOf(',')
        val dot = cleaned.lastIndexOf('.')
        val separator = maxOf(comma, dot)
        if (separator < 0) return cleaned.toDoubleOrNull()

        val decimalDigits = cleaned.length - separator - 1
        val hasBoth = comma >= 0 && dot >= 0
        val normalized = when {
            hasBoth -> {
                val decimalSeparator = if (comma > dot) ',' else '.'
                cleaned
                    .replace(if (decimalSeparator == ',') "." else ",", "")
                    .replace(decimalSeparator, '.')
            }
            decimalDigits == 3 -> cleaned.replace(",", "").replace(".", "")
            decimalDigits in 1..2 -> cleaned.replace(',', '.')
            else -> cleaned.replace(",", "").replace(".", "")
        }
        return normalized.toDoubleOrNull()
    }

    fun parseDistance(raw: String): Double? = raw
        .replace("\u00A0", "")
        .replace(',', '.')
        .filter { it.isDigit() || it == '.' }
        .toDoubleOrNull()
}
