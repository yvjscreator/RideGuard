package dev.rideguard.detection.accessibility

import android.content.Context
import android.content.Intent

/** The detector asks the app process to persist a confirmed destination zone. */
object QuickAvoidZoneBroadcast {
    const val ADD_ACTION = "dev.rideguard.action.ADD_AVOIDED_ZONE"
    const val RESULT_ACTION = "dev.rideguard.action.ADD_AVOIDED_ZONE_RESULT"
    const val ZONE_EXTRA = "zone"
    const val SAVED_EXTRA = "saved"

    fun request(context: Context, zone: String): Intent = Intent(ADD_ACTION)
        .setClassName(context.packageName, "dev.rideguard.app.AddAvoidedZoneReceiver")
        .putExtra(ZONE_EXTRA, zone)

    fun result(context: Context, zone: String, saved: Boolean): Intent = Intent(RESULT_ACTION)
        .setPackage(context.packageName)
        .putExtra(ZONE_EXTRA, zone)
        .putExtra(SAVED_EXTRA, saved)
}
