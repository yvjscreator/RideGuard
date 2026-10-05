package dev.rideguard.overlay

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
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
        onQuickAdd: (String) -> Unit = {}) {
        hide()
        val hourlyColor = if (TargetFailure.HOURLY_RATE in evaluation.unmetTargets) METRIC_BAD else METRIC_GOOD
        val perKmColor = if (TargetFailure.PER_KM in evaluation.unmetTargets) METRIC_BAD else METRIC_GOOD

        val panel = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(14))
            background = GradientDrawable().apply {
                setColor(CARD_BACKGROUND)
                cornerRadius = dp(18).toFloat()
                setStroke(
                    dp(if (destinationAlert == null) 2 else 4),
                    if (destinationAlert == null) CARD_BORDER else WARNING_YELLOW,
                )
            }
            contentDescription = service.getString(R.string.overlay_close)

            addView(LinearLayout(service).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(
                    metricSection(
                        label = "POR HORA",
                        value = money(evaluation.metrics.arsPerHour),
                        valueColor = hourlyColor,
                        gravity = Gravity.START,
                    ),
                    LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
                )
                addView(View(service).apply { setBackgroundColor(DIVIDER_COLOR) },
                    LinearLayout.LayoutParams(dp(1), dp(50)).apply {
                        marginStart = dp(12)
                        marginEnd = dp(12)
                    })
                addView(
                    metricSection(
                        label = "POR KM",
                        value = money(evaluation.metrics.arsPerKm),
                        valueColor = perKmColor,
                        gravity = Gravity.END,
                    ),
                    LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
                )
            })

            addView(LinearLayout(service).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END or Gravity.CENTER_VERTICAL
                setPadding(0, dp(8), 0, 0)
                if (destinationAlert != null) {
                    addView(ImageView(service).apply {
                        setImageResource(R.drawable.ic_destination_warning)
                        contentDescription = when (destinationAlert.kind) {
                            DestinationAlert.Kind.ZONE -> "Zona a evitar: ${destinationAlert.matchedName}"
                            DestinationAlert.Kind.STREET -> "Calle a evitar: ${destinationAlert.matchedName}"
                        }
                    }, LinearLayout.LayoutParams(dp(22), dp(22)).apply { marginEnd = dp(8) })
                }
                addView(metricText(
                    "${DurationDisplay.clock(evaluation.metrics.totalMinutes)} min · ${decimal(evaluation.metrics.totalKm)} km",
                    14f,
                    Typeface.NORMAL,
                    STATS_TEXT,
                ))
            })
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
        if (quickAddZone != null) {
            quickZone = quickAddZone
            val button = Button(service).apply {
                text = "Evitar zona: $quickAddZone"
                textSize = 14f
                isAllCaps = false
                minHeight = dp(48)
                maxWidth = service.resources.displayMetrics.widthPixels - dp(32)
                maxLines = 2
                ellipsize = TextUtils.TruncateAt.END
                setTextColor(BRAND_INK)
                background = GradientDrawable().apply {
                    setColor(BRAND_AQUA)
                    cornerRadius = dp(14).toFloat()
                }
                setOnClickListener {
                    isEnabled = false
                    text = "Guardando zona…"
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
                    buttonParams.y += panel.height + dp(8)
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
            text = if (saved) "Zona añadida" else "No se pudo guardar · Reintentar"
            isEnabled = !saved
        }
    }

    private fun metricSection(label: String, value: String, valueColor: Int, gravity: Int) =
        LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            this.gravity = gravity
            addView(metricText(label, 11f, Typeface.BOLD, LABEL_TEXT).apply { this.gravity = gravity })
            addView(metricText(value, 25f, Typeface.BOLD, valueColor).apply { this.gravity = gravity })
        }

    private fun metricText(text: String, sizeSp: Float, style: Int, color: Int = Color.WHITE) = TextView(service).apply {
        this.text = text
        textSize = sizeSp
        setTextColor(color)
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
        val WARNING_YELLOW = 0xFFFFD54F.toInt()
        val CARD_BACKGROUND = 0xFF20242A.toInt()
        val CARD_BORDER = 0xFF59636E.toInt()
        val DIVIDER_COLOR = 0xFF434A52.toInt()
        val LABEL_TEXT = 0xFFB7C0C8.toInt()
        val STATS_TEXT = 0xFFE1E6EA.toInt()
        val METRIC_GOOD = 0xFF66D17A.toInt()
        val METRIC_BAD = 0xFFFF6B6B.toInt()
        val BRAND_AQUA = 0xFF5FE7E8.toInt()
        val BRAND_INK = 0xFF00363E.toInt()
    }
}
