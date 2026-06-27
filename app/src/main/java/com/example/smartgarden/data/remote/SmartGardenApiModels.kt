package com.example.smartgarden.data.remote

import com.example.smartgarden.data.model.GardenMode
import com.example.smartgarden.data.model.PumpState

data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T?,
)

data class RemoteGardenSettings(
    val mode: GardenMode,
    val startThreshold: Int,
    val stopThreshold: Int,
    val maxPumpDurationSeconds: Int,
    val cooldownSeconds: Int,
)

data class RemoteSensorReading(
    val soilMoisturePercent: Double,
    val soilRaw: Int?,
    val temperatureC: Double,
    val airHumidityPercent: Double,
    val pumpState: PumpState,
    val readAt: String?,
)

data class RemoteDeviceSummary(
    val id: String,
    val name: String?,
    val isConnected: Boolean,
    val lastSeenAt: String?,
)

data class RemoteDashboard(
    val device: RemoteDeviceSummary?,
    val latestReading: RemoteSensorReading?,
    val settings: RemoteGardenSettings?,
    val pendingCommands: Int,
)

data class RemotePumpCommand(
    val id: Long,
    val commandType: String,
    val status: String,
    val payload: String?,
)

data class RemoteWateringSchedule(
    val id: Long,
    val timeOfDay: String,
    val durationSeconds: Int,
    val isActive: Boolean,
)

data class RemoteWateringLog(
    val id: Long,
    val source: String,
    val durationSeconds: Int,
    val resultStatus: String,
    val soilMoisturePercent: Double?,
    val startedAt: String?,
)
