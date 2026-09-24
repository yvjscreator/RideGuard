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
import dev.rideguard.core.model.OfferGrade
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
                setStroke(dp(if (destinationAlert == null) 2 else 4),
                    if (destinationAlert == null) Color.WHITE else WARNING_YELLOW)
            }
            contentDescription = service.getString(R.string.overlay_close)
            addView(LinearLayout(service).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END or Gravity.CENTER_VERTICAL
                if (destinationAlert != null) {
                    addView(ImageView(service).apply {
                        setImageResource(R.drawable.ic_destination_warning)
                        contentDescription = when (destinationAlert.kind) {
                            DestinationAlert.Kind.ZONE -> "Zona a evitar: ${destinationAlert.matchedName}"
                            DestinationAlert.Kind.STREET -> "Calle a evitar: ${destinationAlert.matchedName}"
                        }
                    }, LinearLayout.LayoutParams(dp(26), dp(26)).apply { marginEnd = dp(8) })
                }
                addView(metricText("${money(evaluation.metrics.arsPerHour)}/h", 28f, Typeface.BOLD))
            })
            addView(metricText("${money(evaluation.metrics.arsPerKm)}/km", 21f, Typeface.BOLD))
            addView(metricText("${DurationDisplay.clock(evaluation.metrics.totalMinutes)} min - ${decimal(evaluation.metrics.totalKm)} km", 14f, Typeface.NORMAL))
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
        val WARNING_YELLOW = 0xFFFFD54F.toInt()
        val BRAND_AQUA = 0xFF5FE7E8.toInt()
        val BRAND_INK = 0xFF00363E.toInt()
    }
}
