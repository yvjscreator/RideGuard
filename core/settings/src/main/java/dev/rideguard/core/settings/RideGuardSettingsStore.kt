package dev.rideguard.core.settings

import android.content.Context
import dev.rideguard.core.model.AvoidedStreet
import dev.rideguard.core.model.DriverGoals
import dev.rideguard.core.model.WeeklySchedule
import dev.rideguard.core.model.WorkDaySchedule
import java.time.DayOfWeek
import java.time.LocalDateTime
import org.json.JSONArray
import org.json.JSONObject

data class RideGuardSettings(
    val regularGoals: DriverGoals = DriverGoals(15_000.0, 650.0),
    val busyDaysGoals: DriverGoals = DriverGoals(18_000.0, 750.0),
    val usualWorkHours: Double = 6.0,
    val schedule: WeeklySchedule = WeeklySchedule(),
    val avoidedZones: List<String> = emptyList(),
    val avoidedStreets: List<AvoidedStreet> = emptyList(),
) {
    fun goalsFor(at: LocalDateTime): DriverGoals? = when (schedule.activeDay(at)) {
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY -> regularGoals
        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> busyDaysGoals
        null -> null
    }
}

class RideGuardSettingsStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "ride_guard_settings",
        Context.MODE_PRIVATE,
    )

    fun load(): RideGuardSettings {
        val defaults = RideGuardSettings()
        return RideGuardSettings(
            regularGoals = DriverGoals(
                preferences.getFloat(REGULAR_HOURLY, defaults.regularGoals.targetArsPerHour.toFloat()).toDouble(),
                preferences.getFloat(REGULAR_KM, defaults.regularGoals.minimumArsPerKm.toFloat()).toDouble(),
            ),
            busyDaysGoals = DriverGoals(
                preferences.getFloat(BUSY_HOURLY, defaults.busyDaysGoals.targetArsPerHour.toFloat()).toDouble(),
                preferences.getFloat(BUSY_KM, defaults.busyDaysGoals.minimumArsPerKm.toFloat()).toDouble(),
            ),
            usualWorkHours = preferences.getFloat(USUAL_HOURS, defaults.usualWorkHours.toFloat()).toDouble(),
            avoidedZones = readZones(preferences.getString(AVOIDED_ZONES, null)),
            avoidedStreets = readStreets(preferences.getString(AVOIDED_STREETS, null)),
            schedule = WeeklySchedule(
                DayOfWeek.values().map { day ->
                    val default = defaults.schedule.days.first { it.day == day }
                    val key = "day_${day.value}_"
                    val start = preferences.getInt(key + "start", default.startMinute).coerceIn(0, 1439)
                    val end = preferences.getInt(key + "end", default.endMinute).coerceIn(0, 1440)
                    WorkDaySchedule(
                        day = day,
                        enabled = preferences.getBoolean(key + "enabled", default.enabled),
                        startMinute = if (start == end) default.startMinute else start,
                        endMinute = if (start == end) default.endMinute else end,
                    )
                },
            ),
        )
    }

    fun save(settings: RideGuardSettings): Boolean {
        val editor = preferences.edit()
            .putFloat(REGULAR_HOURLY, settings.regularGoals.targetArsPerHour.toFloat())
            .putFloat(REGULAR_KM, settings.regularGoals.minimumArsPerKm.toFloat())
            .putFloat(BUSY_HOURLY, settings.busyDaysGoals.targetArsPerHour.toFloat())
            .putFloat(BUSY_KM, settings.busyDaysGoals.minimumArsPerKm.toFloat())
            .putFloat(USUAL_HOURS, settings.usualWorkHours.toFloat())
            .putString(AVOIDED_ZONES, JSONArray(settings.avoidedZones).toString())
            .putString(AVOIDED_STREETS, JSONArray().apply {
                settings.avoidedStreets.forEach { rule ->
                    put(JSONObject().put("name", rule.name).put("zone", rule.onlyInZone))
                }
            }.toString())
        settings.schedule.days.forEach { day ->
            val key = "day_${day.day.value}_"
            editor.putBoolean(key + "enabled", day.enabled)
            editor.putInt(key + "start", day.startMinute)
            editor.putInt(key + "end", day.endMinute)
        }
        return editor.commit()
    }

    private companion object {
        const val REGULAR_HOURLY = "regular_hourly"
        const val REGULAR_KM = "regular_km"
        const val BUSY_HOURLY = "busy_hourly"
        const val BUSY_KM = "busy_km"
        const val USUAL_HOURS = "usual_hours"
        const val AVOIDED_ZONES = "avoided_zones"
        const val AVOIDED_STREETS = "avoided_streets"
        const val MAX_RULES = 100
        const val MAX_RULE_LENGTH = 80
    }

    private fun readZones(raw: String?): List<String> = runCatching {
        val array = JSONArray(raw ?: "[]")
        (0 until minOf(array.length(), MAX_RULES)).mapNotNull { index ->
            array.optString(index).trim().take(MAX_RULE_LENGTH).takeIf(String::isNotEmpty)
        }
    }.getOrDefault(emptyList())

    private fun readStreets(raw: String?): List<AvoidedStreet> = runCatching {
        val array = JSONArray(raw ?: "[]")
        (0 until minOf(array.length(), MAX_RULES)).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            val name = item.optString("name").trim().take(MAX_RULE_LENGTH)
            if (name.isEmpty()) return@mapNotNull null
            AvoidedStreet(name, item.optString("zone").trim().take(MAX_RULE_LENGTH).ifEmpty { null })
        }
    }.getOrDefault(emptyList())

}
