package com.example.smartgarden.data.model

data class GardenSettings(
    val mode: GardenMode = GardenMode.AUTO,
    val isScheduleEnabled: Boolean = true,
    val scheduleHour: Int = 6,
    val scheduleMinute: Int = 0,
    val moistureThreshold: Int = DEFAULT_MOISTURE_THRESHOLD,
    val areNotificationsEnabled: Boolean = true,
) {
    companion object {
        const val MIN_MOISTURE_THRESHOLD = 40
        const val MAX_MOISTURE_THRESHOLD = 90
        const val DEFAULT_MOISTURE_THRESHOLD = MIN_MOISTURE_THRESHOLD
    }
}
