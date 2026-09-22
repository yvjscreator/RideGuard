package dev.rideguard.detection.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import dev.rideguard.core.calculator.OfferCalculator
import dev.rideguard.core.settings.RideGuardSettings
import dev.rideguard.core.settings.RideGuardSettingsStore
import dev.rideguard.overlay.OfferOverlayController
import java.time.LocalDateTime

class OfferAccessibilityService : AccessibilityService() {
    private val registry = PlatformParserRegistry()
    private lateinit var settingsStore: RideGuardSettingsStore
    private lateinit var overlay: OfferOverlayController
    private lateinit var currentSettings: RideGuardSettings
    private val eventHandler = Handler(Looper.getMainLooper())
    private var pendingPackageName: String? = null
    private var scanScheduled = false
    private var receiverRegistered = false
    private var lastSignature: String? = null
    private var lastShownAtMs: Long = 0L
    private var lastScanAtMs: Long = 0L
    private val settingsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let(DetectorSettingsBroadcast::read)?.let {
                currentSettings = it
                lastSignature = null
                if (it.goalsFor(LocalDateTime.now()) == null && ::overlay.isInitialized) overlay.hide()
            }
        }
    }

    override fun onServiceConnected() {
        settingsStore = RideGuardSettingsStore(this)
        currentSettings = settingsStore.load()
        overlay = OfferOverlayController(this)
        if (!receiverRegistered) {
            ContextCompat.registerReceiver(
                this,
                settingsReceiver,
                IntentFilter(DetectorSettingsBroadcast.ACTION),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            receiverRegistered = true
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return
        registry.forPackage(packageName) ?: return
        if (currentSettings.goalsFor(LocalDateTime.now()) == null) {
            lastSignature = null
            if (::overlay.isInitialized) overlay.hide()
            return
        }
        pendingPackageName = packageName
        if (scanScheduled) return
        scanScheduled = true
        val elapsed = SystemClock.elapsedRealtime() - lastScanAtMs
        val delay = maxOf(UI_SETTLE_DELAY_MS, MIN_SCAN_INTERVAL_MS - elapsed)
        eventHandler.postDelayed({ scanActiveWindow() }, delay)
    }

    private fun scanActiveWindow() {
        scanScheduled = false
        val packageName = pendingPackageName ?: return
        val parser = registry.forPackage(packageName) ?: return
        lastScanAtMs = SystemClock.elapsedRealtime()
        val goals = currentSettings.goalsFor(LocalDateTime.now()) ?: run {
            lastSignature = null
            if (::overlay.isInitialized) overlay.hide()
            return
        }
        val root = rootInActiveWindow ?: return
        if (root.packageName?.toString() != packageName) return
        val screenText = AccessibilityTextReader.read(root)
        val offer = parser.parse(screenText)
        if (offer == null) {
            lastSignature = null
            if (::overlay.isInitialized) overlay.hide()
            return
        }
        val signature = listOf(
            offer.platform,
            offer.fareArs,
            offer.pickupMinutes,
            offer.pickupKm,
            offer.tripMinutes,
            offer.tripKm,
        ).joinToString("|")
        val now = SystemClock.elapsedRealtime()
        if (signature == lastSignature && now - lastShownAtMs < DUPLICATE_WINDOW_MS) return

        val evaluation = runCatching {
            OfferCalculator.evaluate(offer, goals)
        }.getOrNull() ?: return

        lastSignature = signature
        lastShownAtMs = now
        runCatching { overlay.show(evaluation) }
    }

    override fun onInterrupt() {
        if (::overlay.isInitialized) overlay.hide()
    }

    override fun onDestroy() {
        eventHandler.removeCallbacksAndMessages(null)
        if (::overlay.isInitialized) overlay.hide()
        if (receiverRegistered) runCatching { unregisterReceiver(settingsReceiver) }
        super.onDestroy()
    }

    private companion object {
        const val DUPLICATE_WINDOW_MS = 30_000L
        const val MIN_SCAN_INTERVAL_MS = 750L
        const val UI_SETTLE_DELAY_MS = 350L
    }
}
