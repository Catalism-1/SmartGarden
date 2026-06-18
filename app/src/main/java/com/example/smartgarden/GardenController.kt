package com.example.smartgarden

import com.example.smartgarden.data.model.GardenDeviceState
import com.example.smartgarden.data.model.GardenMode
import com.example.smartgarden.data.model.GardenSettings
import com.example.smartgarden.data.model.GardenStatus
import com.example.smartgarden.data.model.PumpState
import com.example.smartgarden.data.model.SensorSnapshot

object GardenController {
    const val AUTO_PUMP_ON_THRESHOLD = 40
    const val AUTO_PUMP_OFF_THRESHOLD = 45

    private const val MIN_SAFE_TEMPERATURE = 10
    private const val MAX_SAFE_TEMPERATURE = 38
    private const val MIN_SAFE_AIR_HUMIDITY = 25
    private const val MAX_SAFE_AIR_HUMIDITY = 85

    fun clampThreshold(value: Int): Int = value.coerceIn(
        GardenSettings.MIN_MOISTURE_THRESHOLD,
        GardenSettings.MAX_MOISTURE_THRESHOLD,
    )

    fun evaluateAutoPumpState(
        mode: GardenMode,
        soilMoisture: Int,
        currentPumpState: PumpState,
    ): PumpState {
        if (mode == GardenMode.MANUAL) return currentPumpState

        return when {
            soilMoisture < AUTO_PUMP_ON_THRESHOLD -> PumpState.ON
            soilMoisture >= AUTO_PUMP_OFF_THRESHOLD -> PumpState.OFF
            else -> currentPumpState
        }
    }

    fun evaluateGardenStatus(state: GardenDeviceState): GardenStatus = when {
        !state.isConnected -> GardenStatus.WARNING
        state.temperature !in MIN_SAFE_TEMPERATURE..MAX_SAFE_TEMPERATURE -> GardenStatus.WARNING
        state.airHumidity !in MIN_SAFE_AIR_HUMIDITY..MAX_SAFE_AIR_HUMIDITY -> GardenStatus.WARNING
        state.soilMoisture < AUTO_PUMP_ON_THRESHOLD -> GardenStatus.NEEDS_WATERING
        else -> GardenStatus.HEALTHY
    }

    // Kept for the existing configurable insight threshold flow.
    fun evaluate(snapshot: SensorSnapshot, moistureThreshold: Int): GardenStatus {
        val threshold = clampThreshold(moistureThreshold)
        return when {
            snapshot.temperature !in MIN_SAFE_TEMPERATURE..MAX_SAFE_TEMPERATURE -> GardenStatus.WARNING
            snapshot.humidity !in MIN_SAFE_AIR_HUMIDITY..MAX_SAFE_AIR_HUMIDITY -> GardenStatus.WARNING
            snapshot.moisture < threshold -> GardenStatus.NEEDS_WATERING
            else -> GardenStatus.HEALTHY
        }
    }
}
