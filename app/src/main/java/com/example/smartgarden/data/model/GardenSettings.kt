package com.example.smartgarden.data.model

data class GardenSettings(
    val mode: GardenMode = GardenMode.AUTO,
    val connectionMode: AppConnectionMode = AppConnectionMode.DEMO,
    val localServerIp: String = "",
    val isScheduleEnabled: Boolean = true,
    val scheduleHour: Int = 6,
    val scheduleMinute: Int = 0,
    val moistureThreshold: Int = DEFAULT_MOISTURE_THRESHOLD,
    val stopMoistureThreshold: Int = DEFAULT_STOP_MOISTURE_THRESHOLD,
    val maxPumpDurationSeconds: Int = DEFAULT_MAX_PUMP_DURATION_SECONDS,
    val cooldownSeconds: Int = DEFAULT_COOLDOWN_SECONDS,
    val areNotificationsEnabled: Boolean = true,
) {
    companion object {
        const val MIN_MOISTURE_THRESHOLD = 40
        const val MAX_MOISTURE_THRESHOLD = 90
        const val MAX_STOP_MOISTURE_THRESHOLD = 95
        const val DEFAULT_MOISTURE_THRESHOLD = MIN_MOISTURE_THRESHOLD
        const val DEFAULT_STOP_MOISTURE_THRESHOLD = 55
        const val DEFAULT_MAX_PUMP_DURATION_SECONDS = 15
        const val DEFAULT_COOLDOWN_SECONDS = 60
    }
}
