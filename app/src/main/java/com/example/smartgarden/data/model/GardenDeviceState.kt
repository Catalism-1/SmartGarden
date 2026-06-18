package com.example.smartgarden.data.model

data class GardenDeviceState(
    val mode: GardenMode = GardenMode.AUTO,
    val soilMoisture: Int = 65,
    val temperature: Int = 24,
    val airHumidity: Int = 48,
    val pumpState: PumpState = PumpState.OFF,
    val isConnected: Boolean = true,
)
