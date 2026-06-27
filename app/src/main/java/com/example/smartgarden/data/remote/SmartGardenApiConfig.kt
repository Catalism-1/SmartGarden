package com.example.smartgarden.data.remote

data class SmartGardenApiConfig(
    val baseUrl: String = DEFAULT_BASE_URL,
    val deviceId: String = DEFAULT_DEVICE_ID,
) {
    companion object {
        const val DEFAULT_BASE_URL = "http://10.0.2.2:3000"
        const val DEFAULT_DEVICE_ID = "smartgarden-01"
    }
}
