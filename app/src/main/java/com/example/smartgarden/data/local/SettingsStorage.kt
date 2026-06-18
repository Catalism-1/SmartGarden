package com.example.smartgarden.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.smartgarden.data.model.GardenMode
import com.example.smartgarden.data.model.GardenSettings

class SettingsStorage internal constructor(
    private val preferences: SharedPreferences,
) {
    constructor(context: Context) : this(
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    )

    fun load(): GardenSettings = GardenSettings(
        mode = if (preferences.getBoolean(KEY_AUTOMATIC, true)) GardenMode.AUTO else GardenMode.MANUAL,
        isScheduleEnabled = preferences.getBoolean(KEY_SCHEDULE_ENABLED, true),
        scheduleHour = preferences.getInt(KEY_SCHEDULE_HOUR, 6),
        scheduleMinute = preferences.getInt(KEY_SCHEDULE_MINUTE, 0),
        moistureThreshold = preferences.getInt(
            KEY_THRESHOLD,
            GardenSettings.DEFAULT_MOISTURE_THRESHOLD,
        ).coerceIn(
            GardenSettings.MIN_MOISTURE_THRESHOLD,
            GardenSettings.MAX_MOISTURE_THRESHOLD,
        ),
        areNotificationsEnabled = preferences.getBoolean(KEY_NOTIFICATIONS, true),
    )

    fun setAutomaticMode(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_AUTOMATIC, enabled).apply()
    }

    fun setScheduleEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_SCHEDULE_ENABLED, enabled).apply()
    }

    fun setScheduleTime(hour: Int, minute: Int) {
        preferences.edit()
            .putInt(KEY_SCHEDULE_HOUR, hour.coerceIn(0, 23))
            .putInt(KEY_SCHEDULE_MINUTE, minute.coerceIn(0, 59))
            .apply()
    }

    fun setMoistureThreshold(value: Int) {
        val safeValue = value.coerceIn(
            GardenSettings.MIN_MOISTURE_THRESHOLD,
            GardenSettings.MAX_MOISTURE_THRESHOLD,
        )
        preferences.edit().putInt(KEY_THRESHOLD, safeValue).apply()
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply()
    }

    fun reset(): GardenSettings {
        preferences.edit().clear().apply()
        return GardenSettings()
    }

    private companion object {
        const val PREFS_NAME = "garden_preferences"
        const val KEY_AUTOMATIC = "automatic_mode"
        const val KEY_SCHEDULE_ENABLED = "schedule_enabled"
        const val KEY_SCHEDULE_HOUR = "schedule_hour"
        const val KEY_SCHEDULE_MINUTE = "schedule_minute"
        const val KEY_THRESHOLD = "moisture_threshold"
        const val KEY_NOTIFICATIONS = "notifications"
    }
}
