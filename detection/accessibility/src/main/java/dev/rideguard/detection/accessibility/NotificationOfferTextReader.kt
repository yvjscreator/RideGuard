package dev.rideguard.detection.accessibility

import android.app.Notification
import android.view.accessibility.AccessibilityEvent

/** Reads only text supplied with a notification event from a supported platform. */
internal object NotificationOfferTextReader {
    fun read(event: AccessibilityEvent): String {
        val values = ArrayList<CharSequence?>(12)
        values.addAll(event.text)
        values.add(event.contentDescription)

        val notification = event.parcelableData as? Notification
        if (notification != null) {
            values.add(notification.tickerText)
            val extras = notification.extras
            for (key in TEXT_KEYS) values.add(extras.getCharSequence(key))
            extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.let(values::addAll)
        }
        return OfferSignalText.join(values)
    }

    private val TEXT_KEYS = listOf(
        Notification.EXTRA_TITLE,
        Notification.EXTRA_TITLE_BIG,
        Notification.EXTRA_TEXT,
        Notification.EXTRA_BIG_TEXT,
        Notification.EXTRA_SUB_TEXT,
        Notification.EXTRA_SUMMARY_TEXT,
    )
}

internal object OfferSignalText {
    private const val MAX_VALUE_CHARS = 500
    private const val MAX_TOTAL_CHARS = 4_000

    fun join(values: Iterable<CharSequence?>): String {
        val unique = LinkedHashSet<String>()
        var totalChars = 0
        for (value in values) {
            val normalized = value?.toString()?.trim()?.take(MAX_VALUE_CHARS).orEmpty()
            if (normalized.isEmpty() || totalChars + normalized.length > MAX_TOTAL_CHARS) continue
            if (unique.add(normalized)) totalChars += normalized.length
        }
        return unique.joinToString("\n")
    }
}
