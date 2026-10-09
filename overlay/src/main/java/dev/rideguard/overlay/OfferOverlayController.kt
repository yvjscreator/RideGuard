package dev.rideguard.overlay

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ImageView
import android.widget.Button
import android.widget.TextView
import dev.rideguard.core.model.DestinationAlert
import dev.rideguard.core.model.DurationDisplay
import dev.rideguard.core.model.OfferEvaluation
import dev.rideguard.core.model.TargetFailure
import dev.rideguard.core.settings.RateUnitStyle
import java.text.NumberFormat
import java.util.Locale

class OfferOverlayController(
    private val service: AccessibilityService,
) {
    private val windowManager = service.getSystemService(WindowManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private var overlay: View? = null
    private var quickButton: Button? = null
    private var quickZone: String? = null
    private var hideGeneration = 0

    fun isShowing(): Boolean = overlay != null

    fun show(evaluation: OfferEvaluation, destinationAlert: DestinationAlert?, quickAddZone: String? = null,
        rateUnitStyle: RateUnitStyle = RateUnitStyle.ICONS,
        onQuickAdd: (String) -> Unit = {}) {
        hide()
        val hourlyColor = if (TargetFailure.HOURLY_RATE in evaluation.unmetTargets) METRIC_BAD else METRIC_GOOD
        val perKmColor = if (TargetFailure.PER_KM in evaluation.unmetTargets) METRIC_BAD else METRIC_GOOD

        val panel = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = GradientDrawable().apply {
                setColor(CARD_BACKGROUND)
                if (quickAddZone == null) {
                    cornerRadius = dp(18).toFloat()
                } else {
                    val radius = dp(18).toFloat()
                    cornerRadii = floatArrayOf(radius, radius, radius, radius, 0f, 0f, 0f, 0f)
                }
                setStroke(
                    dp(if (destinationAlert == null) 2 else 4),
                    if (destinationAlert == null) CARD_BORDER else WARNING_YELLOW,
                )
            }
            contentDescription = service.getString(R.string.overlay_close)

            addView(metricRow(
                value = money(evaluation.metrics.arsPerHour),
                unit = "/h",
                valueColor = hourlyColor,
                rateUnitStyle = rateUnitStyle,
                destinationAlert = destinationAlert,
            ), LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ))
            addView(metricRow(
                value = money(evaluation.metrics.arsPerKm),
                unit = "/km",
                valueColor = perKmColor,
                rateUnitStyle = rateUnitStyle,
            ), LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(1) })

            addView(statRow(
                R.drawable.ic_offer_clock,
                "${DurationDisplay.clock(evaluation.metrics.totalMinutes)} min",
                "Tiempo total estimado",
            ), LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(5) })
            addView(statRow(
                R.drawable.ic_offer_finish,
                "${decimal(evaluation.metrics.totalKm)} km",
                "Distancia total estimada",
            ), LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(2) })
        }

        val maxPanelWidth = service.resources.displayMetrics.widthPixels - dp(32)
        panel.measure(
            View.MeasureSpec.makeMeasureSpec(maxPanelWidth, View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        )
        val params = WindowManager.LayoutParams(
            panel.measuredWidth,
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
        if (quickAddZone != null) {
            quickZone = quickAddZone
            val button = Button(service).apply {
                text = "Restringir zona"
                textSize = 13f
                isAllCaps = false
                minWidth = 0
                minHeight = dp(42)
                setPadding(dp(12), 0, dp(12), 0)
                setTextColor(STATS_TEXT)
                background = GradientDrawable().apply {
                    setColor(TAB_BACKGROUND)
                    val radius = dp(18).toFloat()
                    cornerRadii = floatArrayOf(0f, 0f, 0f, 0f, radius, radius, radius, radius)
                    setStroke(
                        dp(if (destinationAlert == null) 2 else 4),
                        if (destinationAlert == null) CARD_BORDER else WARNING_YELLOW,
                    )
                }
                setOnClickListener {
                    isEnabled = false
                    text = "Restringiendo…"
                    onQuickAdd(quickAddZone)
                }
            }
            val buttonParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                x = dp(16)
                y = dp(90)
            }
            panel.post {
                if (overlay === panel) {
                    buttonParams.width = panel.width
                    buttonParams.y += panel.height - dp(2)
                    runCatching { windowManager.addView(button, buttonParams) }
                        .onSuccess { quickButton = button }
                }
            }
        }
        val generation = ++hideGeneration
        handler.postDelayed({
            if (generation == hideGeneration) hide()
        }, DISPLAY_DURATION_MS)
    }

    fun hide() {
        hideGeneration++
        quickButton?.let { runCatching { windowManager.removeView(it) } }
        quickButton = null
        quickZone = null
        overlay?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        overlay = null
    }

    fun resolveQuickAdd(zone: String, saved: Boolean) {
        if (quickZone != zone) return
        quickButton?.apply {
            text = if (saved) "Zona restringida" else "No se pudo restringir · Reintentar"
            isEnabled = !saved
        }
    }

    private fun metricRow(
        value: String,
        unit: String,
        valueColor: Int,
        rateUnitStyle: RateUnitStyle,
        destinationAlert: DestinationAlert? = null,
    ) =
        LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            val text: CharSequence = if (rateUnitStyle == RateUnitStyle.TEXT) {
                SpannableStringBuilder(value).append(" ").append(unit).apply {
                    val unitStart = length - unit.length
                    setSpan(
                        ForegroundColorSpan(LABEL_TEXT), unitStart, length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
                    )
                    setSpan(
                        RelativeSizeSpan(0.58f), unitStart, length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
                    )
                }
            } else value
            addView(metricText(text, 25f, Typeface.BOLD, valueColor).apply {
                gravity = Gravity.START
                setSingleLine(true)
                maxWidth = service.resources.displayMetrics.widthPixels -
                    dp(56 + (if (destinationAlert == null) 0 else 22) +
                        (if (rateUnitStyle == RateUnitStyle.ICONS) 20 else 0))
                setAutoSizeTextTypeUniformWithConfiguration(18, 25, 1, TypedValue.COMPLEX_UNIT_SP)
                contentDescription = "${if (unit == "/h") "Por hora" else "Por kilómetro"}: $value"
            })
            if (rateUnitStyle == RateUnitStyle.ICONS) {
                addView(ImageView(service).apply {
                    setImageResource(if (unit == "/h") R.drawable.ic_offer_clock else R.drawable.ic_rate_road)
                    setColorFilter(LABEL_TEXT)
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                }, LinearLayout.LayoutParams(dp(16), dp(16)).apply { marginStart = dp(4) })
            }
            if (destinationAlert != null) {
                addView(ImageView(service).apply {
                    setImageResource(R.drawable.ic_destination_warning)
                    contentDescription = when (destinationAlert.kind) {
                        DestinationAlert.Kind.ZONE -> "Zona a evitar: ${destinationAlert.matchedName}"
                        DestinationAlert.Kind.STREET -> "Calle a evitar: ${destinationAlert.matchedName}"
                    }
                }, LinearLayout.LayoutParams(dp(16), dp(16)).apply { marginStart = dp(6) })
            }
        }

    private fun statRow(icon: Int, value: String, description: String) = LinearLayout(service).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.START or Gravity.CENTER_VERTICAL
        addView(ImageView(service).apply {
            setImageResource(icon)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, LinearLayout.LayoutParams(dp(16), dp(16)).apply { marginEnd = dp(6) })
        addView(metricText(value, 13f, Typeface.NORMAL, STATS_TEXT).apply {
            gravity = Gravity.START
            setSingleLine(true)
            contentDescription = "$description: $value"
        })
    }

    private fun metricText(text: CharSequence, sizeSp: Float, style: Int, color: Int = Color.WHITE) = TextView(service).apply {
        this.text = text
        textSize = sizeSp
        setTextColor(color)
        setTypeface(typeface, style)
        setIncludeFontPadding(false)
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
        val WARNING_YELLOW = 0xFFFFD54F.toInt()
        val CARD_BACKGROUND = 0xFF20242A.toInt()
        val CARD_BORDER = 0xFF59636E.toInt()
        val LABEL_TEXT = 0xFFB7C0C8.toInt()
        val STATS_TEXT = 0xFFE1E6EA.toInt()
        val TAB_BACKGROUND = 0xFF2B3037.toInt()
        val METRIC_GOOD = 0xFF66D17A.toInt()
        val METRIC_BAD = 0xFFFF6B6B.toInt()
    }
}
