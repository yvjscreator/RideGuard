package dev.rideguard.detection.accessibility

import android.view.accessibility.AccessibilityNodeInfo
import java.util.ArrayDeque

internal object AccessibilityTextReader {
    private const val MAX_NODES = 350
    private const val MAX_VALUE_CHARS = 180
    private const val MAX_TOTAL_CHARS = 16_000

    @Suppress("DEPRECATION")
    fun read(root: AccessibilityNodeInfo): String {
        val pending = ArrayDeque<AccessibilityNodeInfo>()
        val values = LinkedHashSet<String>()
        pending.add(root)
        var visited = 0
        var totalChars = 0

        fun collect(value: CharSequence?) {
            val normalized = value?.toString()?.trim()?.take(MAX_VALUE_CHARS).orEmpty()
            if (normalized.isEmpty() || totalChars + normalized.length > MAX_TOTAL_CHARS) return
            if (values.add(normalized)) totalChars += normalized.length
        }

        while (pending.isNotEmpty() && visited < MAX_NODES) {
            val node = pending.removeFirst()
            visited++
            collect(node.text)
            collect(node.contentDescription)
            for (index in 0 until node.childCount) {
                node.getChild(index)?.let(pending::addLast)
            }
            if (node !== root) node.recycle()
        }
        return values.joinToString("\n")
    }
}
