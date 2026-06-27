#pragma once

#define WIFI_SSID "YOUR_WIFI_NAME"
#define WIFI_PASSWORD "YOUR_WIFI_PASSWORD"
#define API_BASE_URL "http://YOUR_LAPTOP_IP:3000/api"
#define DEVICE_ID "smartgarden-01"

// Pin mapping NodeMCU ESP8266.
#define SOIL_PIN A0
#define DHT_PIN D4
#define RELAY_PIN D5
#define LED_STATUS_PIN D6
#define BUZZER_PIN D7
#define LCD_SDA_PIN D1
#define LCD_SCL_PIN D2

// Sensor calibration. Adjust after testing dry/wet soil readings.
#define SOIL_RAW_DRY 850
#define SOIL_RAW_WET 350

// Most relay modules are active LOW. Set false if your relay is active HIGH.
#define RELAY_ACTIVE_LOW true

// Local intervals.
#define TELEMETRY_INTERVAL_MS 10000UL
#define CONFIG_INTERVAL_MS 30000UL
#define COMMAND_INTERVAL_MS 3000UL
