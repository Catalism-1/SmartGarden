package com.example.smartgarden.data.remote

import com.example.smartgarden.data.model.GardenMode
import com.example.smartgarden.data.model.PumpState
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import org.json.JSONArray
import org.json.JSONObject

class SmartGardenApiService(
    private val config: SmartGardenApiConfig = SmartGardenApiConfig(),
) {
    fun getDashboard(): ApiResponse<RemoteDashboard> {
        val json = request(
            path = "/api/garden/dashboard?deviceId=${config.deviceId.urlEncoded()}",
        )
        return ApiResponse(
            success = json.optBoolean("success"),
            message = json.optString("message"),
            data = json.optJSONObject("data")?.toDashboard(),
        )
    }

    fun createManualPumpCommand(durationSeconds: Int): ApiResponse<RemotePumpCommand> {
        val json = request(
            method = "POST",
            path = "/api/garden/pump/manual",
            body = JSONObject()
                .put("deviceId", config.deviceId)
                .put("durationSeconds", durationSeconds.coerceAtLeast(1)),
        )
        return ApiResponse(
            success = json.optBoolean("success"),
            message = json.optString("message"),
            data = json.optJSONObject("data")?.toPumpCommand(),
        )
    }

    fun updateMode(mode: GardenMode): ApiResponse<RemotePumpCommand> {
        val json = request(
            method = "POST",
            path = "/api/garden/mode",
            body = JSONObject()
                .put("deviceId", config.deviceId)
                .put("mode", mode.toApiValue()),
        )
        return ApiResponse(
            success = json.optBoolean("success"),
            message = json.optString("message"),
            data = json.optJSONObject("data")?.optJSONObject("command")?.toPumpCommand(),
        )
    }

    fun updateThresholds(
        startThreshold: Int,
        stopThreshold: Int,
        maxPumpDurationSeconds: Int,
        cooldownSeconds: Int,
    ): ApiResponse<RemoteGardenSettings> {
        val json = request(
            method = "POST",
            path = "/api/garden/settings/thresholds",
            body = JSONObject()
                .put("deviceId", config.deviceId)
                .put("startThreshold", startThreshold)
                .put("stopThreshold", stopThreshold)
                .put("maxPumpDurationSeconds", maxPumpDurationSeconds)
                .put("cooldownSeconds", cooldownSeconds),
        )
        return ApiResponse(
            success = json.optBoolean("success"),
            message = json.optString("message"),
            data = json.optJSONObject("data")?.toSettings(),
        )
    }

    fun getSensorHistory(limit: Int = 100): ApiResponse<List<RemoteSensorReading>> {
        val json = request(
            path = "/api/garden/history/sensors?deviceId=${config.deviceId.urlEncoded()}&limit=${limit.coerceIn(1, 500)}",
        )
        return ApiResponse(
            success = json.optBoolean("success"),
            message = json.optString("message"),
            data = json.optJSONArray("data").toSensorReadings(),
        )
    }

    fun getWateringHistory(limit: Int = 100): ApiResponse<List<RemoteWateringLog>> {
        val json = request(
            path = "/api/garden/history/watering?deviceId=${config.deviceId.urlEncoded()}&limit=${limit.coerceIn(1, 500)}",
        )
        return ApiResponse(
            success = json.optBoolean("success"),
            message = json.optString("message"),
            data = json.optJSONArray("data").toWateringLogs(),
        )
    }

    fun getSchedules(): ApiResponse<List<RemoteWateringSchedule>> {
        val json = request(
            path = "/api/garden/schedules?deviceId=${config.deviceId.urlEncoded()}",
        )
        return ApiResponse(
            success = json.optBoolean("success"),
            message = json.optString("message"),
            data = json.optJSONArray("data").toSchedules(),
        )
    }

    fun createSchedule(timeOfDay: String, durationSeconds: Int, isActive: Boolean): ApiResponse<Long> {
        val json = request(
            method = "POST",
            path = "/api/garden/schedules",
            body = JSONObject()
                .put("deviceId", config.deviceId)
                .put("timeOfDay", timeOfDay)
                .put("durationSeconds", durationSeconds)
                .put("isActive", isActive),
        )
        return ApiResponse(
            success = json.optBoolean("success"),
            message = json.optString("message"),
            data = json.optJSONObject("data")?.optLong("id"),
        )
    }

    private fun request(method: String = "GET", path: String, body: JSONObject? = null): JSONObject {
        val url = URL("${config.baseUrl.trimEnd('/')}$path")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("Accept", "application/json")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
        }

        if (body != null) {
            OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use { writer ->
                writer.write(body.toString())
            }
        }

        val stream = if (connection.responseCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream
        }
        val text = stream.bufferedReader().use(BufferedReader::readText)
        connection.disconnect()
        return JSONObject(text)
    }

    private fun JSONObject.toDashboard(): RemoteDashboard = RemoteDashboard(
        device = optJSONObject("device")?.toDeviceSummary(),
        latestReading = optJSONObject("latestReading")?.toSensorReading(),
        settings = optJSONObject("settings")?.toSettings(),
        pendingCommands = optInt("pendingCommands"),
    )

    private fun JSONObject.toDeviceSummary(): RemoteDeviceSummary = RemoteDeviceSummary(
        id = optString("id"),
        name = optNullableString("name"),
        isConnected = optBoolean("isConnected"),
        lastSeenAt = optNullableString("lastSeenAt"),
    )

    private fun JSONObject.toSettings(): RemoteGardenSettings = RemoteGardenSettings(
        mode = optString("mode").toGardenMode(),
        startThreshold = optInt("startThreshold", 40),
        stopThreshold = optInt("stopThreshold", 55),
        maxPumpDurationSeconds = optInt("maxPumpDurationSeconds", 15),
        cooldownSeconds = optInt("cooldownSeconds", 60),
    )

    private fun JSONObject.toSensorReading(): RemoteSensorReading = RemoteSensorReading(
        soilMoisturePercent = optDouble("soil_moisture_percent", optDouble("soilMoisturePercent")),
        soilRaw = if (isNull("soil_raw")) null else optInt("soil_raw"),
        temperatureC = optDouble("temperature_c", optDouble("temperatureC")),
        airHumidityPercent = optDouble("air_humidity_percent", optDouble("airHumidityPercent")),
        pumpState = optString("pump_state", optString("pumpState")).toPumpState(),
        readAt = optNullableString("read_at") ?: optNullableString("readAt"),
    )

    private fun JSONObject.toPumpCommand(): RemotePumpCommand = RemotePumpCommand(
        id = optLong("id"),
        commandType = optString("commandType"),
        status = optString("status"),
        payload = opt("payload")?.toString(),
    )

    private fun JSONObject.toSchedule(): RemoteWateringSchedule = RemoteWateringSchedule(
        id = optLong("id"),
        timeOfDay = optString("timeOfDay"),
        durationSeconds = optInt("durationSeconds"),
        isActive = optBoolean("isActive"),
    )

    private fun JSONObject.toWateringLog(): RemoteWateringLog = RemoteWateringLog(
        id = optLong("id"),
        source = optString("source"),
        durationSeconds = optInt("duration_seconds", optInt("durationSeconds")),
        resultStatus = optString("result_status", optString("resultStatus")),
        soilMoisturePercent = if (isNull("soil_moisture_percent")) null else optDouble("soil_moisture_percent"),
        startedAt = optNullableString("started_at") ?: optNullableString("startedAt"),
    )

    private fun JSONObject.optNullableString(name: String): String? {
        if (!has(name) || isNull(name)) return null
        return optString(name)
    }

    private fun JSONArray?.toSensorReadings(): List<RemoteSensorReading> {
        if (this == null) return emptyList()
        return List(length()) { index -> getJSONObject(index).toSensorReading() }
    }

    private fun JSONArray?.toSchedules(): List<RemoteWateringSchedule> {
        if (this == null) return emptyList()
        return List(length()) { index -> getJSONObject(index).toSchedule() }
    }

    private fun JSONArray?.toWateringLogs(): List<RemoteWateringLog> {
        if (this == null) return emptyList()
        return List(length()) { index -> getJSONObject(index).toWateringLog() }
    }

    private fun String.toGardenMode(): GardenMode = when (lowercase()) {
        "manual" -> GardenMode.MANUAL
        else -> GardenMode.AUTO
    }

    private fun String.toPumpState(): PumpState = when (lowercase()) {
        "on" -> PumpState.ON
        else -> PumpState.OFF
    }

    private fun GardenMode.toApiValue(): String = when (this) {
        GardenMode.AUTO -> "automatic"
        GardenMode.MANUAL -> "manual"
    }

    private fun String.urlEncoded(): String = URLEncoder.encode(this, StandardCharsets.UTF_8.name())

    private companion object {
        const val TIMEOUT_MS = 10_000
    }
}
