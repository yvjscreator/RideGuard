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

class OfferAccessibilityService : AccessibilityService() {
    private val registry = PlatformParserRegistry()
    private lateinit var settingsStore: RideGuardSettingsStore
    private lateinit var overlay: OfferOverlayController
    private lateinit var currentSettings: RideGuardSettings
    private val eventHandler = Handler(Looper.getMainLooper())
    private var pendingPackageName: String? = null
    private var scanScheduled = false
    private var lastSignature: String? = null
    private var lastShownAtMs: Long = 0L
    private var lastScanAtMs: Long = 0L
    private val settingsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let(DetectorSettingsBroadcast::read)?.let { currentSettings = it }
        }
    }

    override fun onServiceConnected() {
        settingsStore = RideGuardSettingsStore(this)
        currentSettings = settingsStore.load()
        overlay = OfferOverlayController(this)
        ContextCompat.registerReceiver(
            this,
            settingsReceiver,
            IntentFilter(DetectorSettingsBroadcast.ACTION),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return
        registry.forPackage(packageName) ?: return
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
        val root = rootInActiveWindow ?: return
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
            OfferCalculator.evaluate(offer, currentSettings.goals, currentSettings.thresholds)
        }.getOrNull() ?: return

        lastSignature = signature
        lastShownAtMs = now
        overlay.show(evaluation, currentSettings.thresholds)
    }

    override fun onInterrupt() {
        if (::overlay.isInitialized) overlay.hide()
    }

    override fun onDestroy() {
        eventHandler.removeCallbacksAndMessages(null)
        if (::overlay.isInitialized) overlay.hide()
        runCatching { unregisterReceiver(settingsReceiver) }
        super.onDestroy()
    }

    private companion object {
        const val DUPLICATE_WINDOW_MS = 30_000L
        const val MIN_SCAN_INTERVAL_MS = 750L
        const val UI_SETTLE_DELAY_MS = 350L
    }
}
