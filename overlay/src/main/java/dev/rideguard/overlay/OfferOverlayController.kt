package dev.rideguard.overlay

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import dev.rideguard.core.model.OfferEvaluation
import dev.rideguard.core.model.OfferGrade
import java.text.NumberFormat
import java.util.Locale

class OfferOverlayController(
    private val service: AccessibilityService,
) {
    private val windowManager = service.getSystemService(WindowManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private var overlay: View? = null
    private var hideGeneration = 0

    fun show(evaluation: OfferEvaluation) {
        hide()
        val backgroundColor = when (evaluation.grade) {
            OfferGrade.GOOD -> 0xFF2E7D32.toInt()
            OfferGrade.NEAR -> 0xFFF9A825.toInt()
            OfferGrade.BAD -> 0xFFC62828.toInt()
        }

        val panel = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(14))
            background = GradientDrawable().apply {
                color = android.content.res.ColorStateList.valueOf(backgroundColor)
                cornerRadius = dp(18).toFloat()
                setStroke(dp(2), Color.WHITE)
            }
            contentDescription = service.getString(R.string.overlay_close)
            addView(metricText("${money(evaluation.metrics.arsPerHour)}/h", 28f, Typeface.BOLD))
            addView(metricText("${money(evaluation.metrics.arsPerKm)}/km", 21f, Typeface.BOLD))
            addView(metricText("${evaluation.metrics.totalMinutes} min - ${decimal(evaluation.metrics.totalKm)} km", 14f, Typeface.NORMAL))
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = dp(16)
            y = dp(90)
        }

        windowManager.addView(panel, params)
        overlay = panel
        val generation = ++hideGeneration
        handler.postDelayed({
            if (generation == hideGeneration) hide()
        }, DISPLAY_DURATION_MS)
    }

    fun hide() {
        hideGeneration++
        overlay?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        overlay = null
    }

    private fun metricText(text: String, sizeSp: Float, style: Int) = TextView(service).apply {
        this.text = text
        textSize = sizeSp
        setTextColor(Color.WHITE)
        setTypeface(typeface, style)
        gravity = Gravity.END
    }

    private fun money(value: Double): String = NumberFormat
        .getCurrencyInstance(Locale.forLanguageTag("es-AR"))
        .apply { maximumFractionDigits = 0 }
        .format(value)

    private fun decimal(value: Double): String = String.format(Locale.US, "%.1f", value)

    private fun dp(value: Int): Int = (value * service.resources.displayMetrics.density).toInt()

    private companion object {
        const val DISPLAY_DURATION_MS = 18_000L
    }
}
