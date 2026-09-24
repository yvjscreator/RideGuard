package dev.rideguard.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.rideguard.core.settings.RideGuardSettingsStore
import dev.rideguard.detection.accessibility.DetectorSettingsBroadcast
import dev.rideguard.detection.accessibility.QuickAvoidZoneBroadcast

class AddAvoidedZoneReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != QuickAvoidZoneBroadcast.ADD_ACTION) return
        val zone = intent.getStringExtra(QuickAvoidZoneBroadcast.ZONE_EXTRA).orEmpty()
        val settings = RideGuardSettingsStore(context).addAvoidedZone(zone)
        if (settings != null) {
            context.sendBroadcast(DetectorSettingsBroadcast.createIntent(context, settings))
        }
        context.sendBroadcast(QuickAvoidZoneBroadcast.result(context, zone, settings != null))
    }
}
