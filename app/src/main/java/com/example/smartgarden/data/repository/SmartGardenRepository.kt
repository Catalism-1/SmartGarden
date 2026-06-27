package com.example.smartgarden.data.repository

import com.example.smartgarden.data.model.GardenMode
import com.example.smartgarden.data.remote.ApiResponse
import com.example.smartgarden.data.remote.RemoteDashboard
import com.example.smartgarden.data.remote.RemoteGardenSettings
import com.example.smartgarden.data.remote.RemotePumpCommand
import com.example.smartgarden.data.remote.RemoteSensorReading
import com.example.smartgarden.data.remote.RemoteWateringLog
import com.example.smartgarden.data.remote.RemoteWateringSchedule
import com.example.smartgarden.data.remote.SmartGardenApiService
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class SmartGardenRepository(
    private val apiService: SmartGardenApiService = SmartGardenApiService(),
    private val executor: ExecutorService = Executors.newSingleThreadExecutor(),
) {
    fun loadDashboard(callback: RepositoryCallback<RemoteDashboard>) {
        execute(callback) { apiService.getDashboard() }
    }

    fun createManualWatering(durationSeconds: Int, callback: RepositoryCallback<RemotePumpCommand>) {
        execute(callback) { apiService.createManualPumpCommand(durationSeconds) }
    }

    fun updateMode(mode: GardenMode, callback: RepositoryCallback<RemotePumpCommand>) {
        execute(callback) { apiService.updateMode(mode) }
    }

    fun updateThresholds(
        startThreshold: Int,
        stopThreshold: Int,
        maxPumpDurationSeconds: Int,
        cooldownSeconds: Int,
        callback: RepositoryCallback<RemoteGardenSettings>,
    ) {
        execute(callback) {
            apiService.updateThresholds(
                startThreshold = startThreshold,
                stopThreshold = stopThreshold,
                maxPumpDurationSeconds = maxPumpDurationSeconds,
                cooldownSeconds = cooldownSeconds,
            )
        }
    }

    fun loadSensorHistory(limit: Int, callback: RepositoryCallback<List<RemoteSensorReading>>) {
        execute(callback) { apiService.getSensorHistory(limit) }
    }

    fun loadWateringHistory(limit: Int, callback: RepositoryCallback<List<RemoteWateringLog>>) {
        execute(callback) { apiService.getWateringHistory(limit) }
    }

    fun loadSchedules(callback: RepositoryCallback<List<RemoteWateringSchedule>>) {
        execute(callback) { apiService.getSchedules() }
    }

    fun createSchedule(
        timeOfDay: String,
        durationSeconds: Int,
        isActive: Boolean,
        callback: RepositoryCallback<Long>,
    ) {
        execute(callback) { apiService.createSchedule(timeOfDay, durationSeconds, isActive) }
    }

    private fun <T> execute(
        callback: RepositoryCallback<T>,
        request: () -> ApiResponse<T>,
    ) {
        executor.execute {
            try {
                val response = request()
                if (response.success && response.data != null) {
                    callback.onSuccess(response.data)
                } else {
                    callback.onError(IllegalStateException(response.message))
                }
            } catch (error: Exception) {
                callback.onError(error)
            }
        }
    }
}

interface RepositoryCallback<T> {
    fun onSuccess(data: T)
    fun onError(error: Throwable)
}
