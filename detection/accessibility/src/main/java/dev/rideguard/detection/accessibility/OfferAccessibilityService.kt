package dev.rideguard.detection.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import dev.rideguard.core.calculator.OfferCalculator
import dev.rideguard.core.model.DriverGoals
import dev.rideguard.core.model.DestinationAlerts
import dev.rideguard.core.model.OfferTextFields
import dev.rideguard.core.model.RawOffer
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
    private var lastDiagnosticAtMs: Long = 0L
    private var watchedPackageName: String? = null
    private var windowWatchScheduled = false
    private var missingWindowChecks = 0
    private val settingsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == QuickAvoidZoneBroadcast.RESULT_ACTION) {
                overlay.resolveQuickAdd(
                    intent.getStringExtra(QuickAvoidZoneBroadcast.ZONE_EXTRA).orEmpty(),
                    intent.getBooleanExtra(QuickAvoidZoneBroadcast.SAVED_EXTRA, false),
                )
                return
            }
            intent?.let(DetectorSettingsBroadcast::read)?.let {
                currentSettings = it
                lastSignature = null
                if (it.goalsFor(LocalDateTime.now()) == null) hideCurrentOffer()
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
                IntentFilter().apply {
                    addAction(DetectorSettingsBroadcast.ACTION)
                    addAction(QuickAvoidZoneBroadcast.RESULT_ACTION)
                },
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            receiverRegistered = true
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return
        val parser = registry.forPackage(packageName) ?: return
        val goals = currentSettings.goalsFor(LocalDateTime.now())
        if (goals == null) {
            hideCurrentOffer()
            return
        }
        if (event.eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            val text = runCatching { NotificationOfferTextReader.read(event) }.getOrNull() ?: return
            val offer = parser.parse(text) ?: run {
                debugRejected("notification", text)
                return
            }
            showOffer(offer, goals)
            return
        }
        pendingPackageName = packageName
        if (scanScheduled) return
        scanScheduled = true
        val elapsed = SystemClock.elapsedRealtime() - lastScanAtMs
        val delay = maxOf(UI_SETTLE_DELAY_MS, MIN_SCAN_INTERVAL_MS - elapsed)
        eventHandler.postDelayed({ scanVisibleWindows() }, delay)
    }

    private fun scanVisibleWindows() {
        scanScheduled = false
        val packageName = pendingPackageName ?: return
        val parser = registry.forPackage(packageName) ?: return
        lastScanAtMs = SystemClock.elapsedRealtime()
        val goals = currentSettings.goalsFor(LocalDateTime.now()) ?: run {
            hideCurrentOffer()
            return
        }

        var activePlatformWindow = false
        var platformWindowFound = false
        val visibleWindows = windows
        for (window in visibleWindows.take(MAX_WINDOWS_TO_INSPECT)) {
            val root = window.root ?: continue
            val isPlatformWindow = root.packageName?.toString() == packageName
            if (isPlatformWindow) {
                platformWindowFound = true
                activePlatformWindow = activePlatformWindow || window.isActive
            }
            val screenText = if (isPlatformWindow) runCatching { AccessibilityTextReader.read(root) }.getOrNull() else null
            recycleRoot(root)
            val offer = screenText?.let(parser::parse)
            if (offer == null) {
                debugRejected("window", screenText)
                continue
            }
            showOffer(offer, goals, packageName)
            return
        }
        // Some Android builds expose the active root but return no interactive windows.
        if (!platformWindowFound) {
            val root = rootInActiveWindow ?: return
            val isPlatformWindow = root.packageName?.toString() == packageName
            val screenText = if (isPlatformWindow) runCatching { AccessibilityTextReader.read(root) }.getOrNull() else null
            recycleRoot(root)
            if (!isPlatformWindow) return
            val offer = screenText?.let(parser::parse)
            if (offer != null) {
                showOffer(offer, goals, packageName)
                return
            }
            debugRejected("active-window", screenText)
            activePlatformWindow = true
        }

        if (activePlatformWindow) {
            hideCurrentOffer()
        }
    }

    private fun showOffer(offer: RawOffer, goals: DriverGoals, windowPackageName: String? = null) {
        val signature = listOf(
            offer.platform,
            offer.fareArs,
            offer.pickupMinutes,
            offer.pickupKm,
            offer.tripMinutes,
            offer.tripKm,
            offer.destination?.zone,
            offer.destination?.street,
        ).joinToString("|")
        val now = SystemClock.elapsedRealtime()
        if (signature == lastSignature && now - lastShownAtMs < DUPLICATE_WINDOW_MS) {
            if (windowPackageName != null && overlay.isShowing()) startWindowWatch(windowPackageName)
            return
        }

        val evaluation = runCatching {
            OfferCalculator.evaluate(offer, goals)
        }.getOrNull() ?: return

        lastSignature = signature
        lastShownAtMs = now
        val alert = DestinationAlerts.find(
            offer.destination,
            currentSettings.avoidedZones,
            currentSettings.avoidedStreets,
        )
        val quickZone = offer.destination?.takeIf { it.zoneConfirmed }?.zone
            ?.takeIf { !DestinationAlerts.isZoneAvoided(it, currentSettings.avoidedZones) }
        val result = runCatching {
            overlay.show(evaluation, alert, quickZone) { zone ->
                sendBroadcast(QuickAvoidZoneBroadcast.request(this, zone))
            }
        }
        if (result.isSuccess && windowPackageName != null) startWindowWatch(windowPackageName)
        if (isDebugBuild) {
            if (result.isSuccess) Log.d(LOG_TAG, "shown source=${if (windowPackageName == null) "notification" else "window"}")
            else Log.d(LOG_TAG, "overlay failed", result.exceptionOrNull())
        }
    }

    private fun startWindowWatch(packageName: String) {
        watchedPackageName = packageName
        missingWindowChecks = 0
        scheduleWindowWatch()
    }

    private fun scheduleWindowWatch() {
        if (windowWatchScheduled) return
        windowWatchScheduled = true
        eventHandler.postDelayed({
            windowWatchScheduled = false
            val packageName = watchedPackageName ?: return@postDelayed
            if (!overlay.isShowing()) {
                watchedPackageName = null
                return@postDelayed
            }
            if (hasVisibleOffer(packageName)) {
                missingWindowChecks = 0
            } else if (++missingWindowChecks >= MISSING_WINDOW_LIMIT) {
                hideCurrentOffer()
                return@postDelayed
            }
            scheduleWindowWatch()
        }, WINDOW_WATCH_INTERVAL_MS)
    }

    private fun hasVisibleOffer(packageName: String): Boolean {
        val parser = registry.forPackage(packageName) ?: return false
        for (window in windows.take(MAX_WINDOWS_TO_INSPECT)) {
            val root = window.root ?: continue
            val isPlatformWindow = root.packageName?.toString() == packageName
            val text = if (isPlatformWindow) runCatching { AccessibilityTextReader.read(root) }.getOrNull() else null
            recycleRoot(root)
            if (text != null && parser.parse(text) != null) return true
        }
        return false
    }

    private fun hideCurrentOffer() {
        lastSignature = null
        watchedPackageName = null
        missingWindowChecks = 0
        if (::overlay.isInitialized) overlay.hide()
    }

    private fun debugRejected(source: String, text: String?) {
        if (!isDebugBuild || text.isNullOrBlank()) return
        if (!text.contains("ARS", true) && !text.contains("Viaje", true) &&
            !text.contains('$') && !text.contains("min", true)) return
        val now = SystemClock.elapsedRealtime()
        if (now - lastDiagnosticAtMs < DIAGNOSTIC_INTERVAL_MS) return
        lastDiagnosticAtMs = now
        Log.d(
            LOG_TAG,
            "unparsed source=$source chars=${text.length} " +
                "fare=${OfferTextFields.fareArs(text) != null} " +
                "pickup=${OfferTextFields.leg(text, listOf("A", "Recogida", "Retiro")) != null} " +
                "trip=${OfferTextFields.leg(text, listOf("Viaje", "Trayecto")) != null} " +
                "legs=${OfferTextFields.legsInOrder(text).size}",
        )
    }

    private val isDebugBuild: Boolean
        get() = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

    @Suppress("DEPRECATION")
    private fun recycleRoot(root: android.view.accessibility.AccessibilityNodeInfo) {
        root.recycle()
    }

    override fun onInterrupt() {
        hideCurrentOffer()
    }

    override fun onDestroy() {
        eventHandler.removeCallbacksAndMessages(null)
        hideCurrentOffer()
        if (receiverRegistered) runCatching { unregisterReceiver(settingsReceiver) }
        super.onDestroy()
    }

    private companion object {
        const val DUPLICATE_WINDOW_MS = 30_000L
        const val MIN_SCAN_INTERVAL_MS = 750L
        const val UI_SETTLE_DELAY_MS = 350L
        const val MAX_WINDOWS_TO_INSPECT = 8
        const val DIAGNOSTIC_INTERVAL_MS = 3_000L
        const val WINDOW_WATCH_INTERVAL_MS = 1_500L
        const val MISSING_WINDOW_LIMIT = 2
        const val LOG_TAG = "RideGuardDetector"
    }
}
