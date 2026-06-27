package com.example.smartgarden.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.smartgarden.data.model.AppConnectionMode
import com.example.smartgarden.data.model.GardenMode
import com.example.smartgarden.data.model.GardenSettings

class SettingsStorage internal constructor(
    private val preferences: SharedPreferences,
) {
    constructor(context: Context) : this(
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    )

    fun load(): GardenSettings {
        val startThreshold = preferences.getInt(
            KEY_THRESHOLD,
            GardenSettings.DEFAULT_MOISTURE_THRESHOLD,
        ).coerceIn(
            GardenSettings.MIN_MOISTURE_THRESHOLD,
            GardenSettings.MAX_MOISTURE_THRESHOLD,
        )
        val stopThreshold = preferences.getInt(
            KEY_STOP_THRESHOLD,
            GardenSettings.DEFAULT_STOP_MOISTURE_THRESHOLD,
        ).coerceIn(
            startThreshold + 1,
            GardenSettings.MAX_STOP_MOISTURE_THRESHOLD,
        )
        return GardenSettings(
            mode = if (preferences.getBoolean(KEY_AUTOMATIC, true)) GardenMode.AUTO else GardenMode.MANUAL,
            connectionMode = if (preferences.getBoolean(KEY_LOCAL_SERVER_MODE, false)) {
                AppConnectionMode.LOCAL_SERVER
            } else {
                AppConnectionMode.DEMO
            },
            localServerIp = preferences.getString(KEY_LOCAL_SERVER_IP, "").orEmpty(),
            isScheduleEnabled = preferences.getBoolean(KEY_SCHEDULE_ENABLED, true),
            scheduleHour = preferences.getInt(KEY_SCHEDULE_HOUR, 6),
            scheduleMinute = preferences.getInt(KEY_SCHEDULE_MINUTE, 0),
            moistureThreshold = startThreshold,
            stopMoistureThreshold = stopThreshold,
            maxPumpDurationSeconds = preferences.getInt(
                KEY_MAX_PUMP_DURATION,
                GardenSettings.DEFAULT_MAX_PUMP_DURATION_SECONDS,
            ).coerceIn(1, MAX_DURATION_SECONDS),
            cooldownSeconds = preferences.getInt(
                KEY_COOLDOWN,
                GardenSettings.DEFAULT_COOLDOWN_SECONDS,
            ).coerceIn(0, MAX_COOLDOWN_SECONDS),
            areNotificationsEnabled = preferences.getBoolean(KEY_NOTIFICATIONS, true),
        )
    }

    fun setAutomaticMode(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_AUTOMATIC, enabled).apply()
    }

    fun setConnectionMode(mode: AppConnectionMode) {
        preferences.edit().putBoolean(KEY_LOCAL_SERVER_MODE, mode == AppConnectionMode.LOCAL_SERVER).apply()
    }

    fun setLocalServerIp(ipAddress: String) {
        preferences.edit().putString(KEY_LOCAL_SERVER_IP, ipAddress.trim()).apply()
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

    fun setStopMoistureThreshold(value: Int) {
        val safeValue = value.coerceIn(
            GardenSettings.DEFAULT_MOISTURE_THRESHOLD + 1,
            GardenSettings.MAX_STOP_MOISTURE_THRESHOLD,
        )
        preferences.edit().putInt(KEY_STOP_THRESHOLD, safeValue).apply()
    }

    fun setPumpSafety(maxDurationSeconds: Int, cooldownSeconds: Int) {
        preferences.edit()
            .putInt(KEY_MAX_PUMP_DURATION, maxDurationSeconds.coerceIn(1, MAX_DURATION_SECONDS))
            .putInt(KEY_COOLDOWN, cooldownSeconds.coerceIn(0, MAX_COOLDOWN_SECONDS))
            .apply()
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
        const val KEY_STOP_THRESHOLD = "stop_moisture_threshold"
        const val KEY_MAX_PUMP_DURATION = "max_pump_duration"
        const val KEY_COOLDOWN = "cooldown_seconds"
        const val KEY_NOTIFICATIONS = "notifications"
        const val KEY_LOCAL_SERVER_MODE = "local_server_mode"
        const val KEY_LOCAL_SERVER_IP = "local_server_ip"
        const val MAX_DURATION_SECONDS = 3600
        const val MAX_COOLDOWN_SECONDS = 86400
    }
}
