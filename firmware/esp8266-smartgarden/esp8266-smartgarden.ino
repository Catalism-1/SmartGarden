#include <Arduino.h>
#include <ArduinoJson.h>
#include <DHT.h>
#include <EEPROM.h>
#include <ESP8266HTTPClient.h>
#include <ESP8266WiFi.h>
#include <LiquidCrystal_I2C.h>
#include <Wire.h>
#include <time.h>

#include "config.h"

#define DHT_TYPE DHT11
#define EEPROM_SIZE 128
#define SETTINGS_MAGIC 0x5347

DHT dht(DHT_PIN, DHT_TYPE);
LiquidCrystal_I2C lcd(0x27, 16, 2);
WiFiClient wifiClient;

enum GardenMode {
  MODE_AUTOMATIC,
  MODE_MANUAL
};

struct GardenSettings {
  uint16_t magic;
  uint8_t mode;
  int startThreshold;
  int stopThreshold;
  int maxPumpDurationSeconds;
  int cooldownSeconds;
  bool scheduleActive;
  int scheduleHour;
  int scheduleMinute;
  int scheduleDurationSeconds;
};

struct SensorState {
  int soilRaw;
  int soilPercent;
  float temperatureC;
  float airHumidityPercent;
};

GardenSettings settings;
SensorState sensors;

bool pumpOn = false;
bool backendReachable = false;
unsigned long lastTelemetryAt = 0;
unsigned long lastConfigAt = 0;
unsigned long lastCommandAt = 0;
unsigned long pumpStartedAt = 0;
unsigned long lastPumpStoppedAt = 0;
int lastScheduleDay = -1;

void saveSettings();
void loadSettings();
void connectWiFi();
void readSensors();
void renderLcd();
void sendTelemetry();
void fetchConfig();
void fetchCommand();
void ackCommand(long commandId, const char *status, int durationSeconds, const char *message);
void executeCommand(long commandId, const char *commandType, JsonObject payload);
void applyAutomationFallback();
void applyScheduleFallback();
void setPump(bool enabled);
String apiUrl(const String &path);
String pumpStateText();
String modeText();

void setup() {
  pinMode(RELAY_PIN, OUTPUT);
  pinMode(LED_STATUS_PIN, OUTPUT);
  pinMode(BUZZER_PIN, OUTPUT);
  setPump(false);

  Serial.begin(115200);
  EEPROM.begin(EEPROM_SIZE);
  loadSettings();

  Wire.begin(LCD_SDA_PIN, LCD_SCL_PIN);
  lcd.init();
  lcd.backlight();
  lcd.clear();
  lcd.print("SmartGarden");
  lcd.setCursor(0, 1);
  lcd.print("Local Only");

  dht.begin();
  connectWiFi();
  fetchConfig();
}

void loop() {
  const unsigned long now = millis();

  if (WiFi.status() != WL_CONNECTED) {
    backendReachable = false;
    connectWiFi();
  }

  readSensors();
  renderLcd();

  if (now - lastConfigAt >= CONFIG_INTERVAL_MS) {
    fetchConfig();
  }

  if (now - lastTelemetryAt >= TELEMETRY_INTERVAL_MS) {
    sendTelemetry();
  }

  if (now - lastCommandAt >= COMMAND_INTERVAL_MS) {
    fetchCommand();
  }

  if (!backendReachable) {
    applyAutomationFallback();
    applyScheduleFallback();
  }

  if (pumpOn) {
    const unsigned long maxDurationMs = (unsigned long)settings.maxPumpDurationSeconds * 1000UL;
    if (sensors.soilPercent >= settings.stopThreshold || now - pumpStartedAt >= maxDurationMs) {
      setPump(false);
      lastPumpStoppedAt = now;
    }
  }

  delay(500);
}

void connectWiFi() {
  if (WiFi.status() == WL_CONNECTED) return;

  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  lcd.clear();
  lcd.print("WiFi connect...");

  unsigned long start = millis();
  while (WiFi.status() != WL_CONNECTED && millis() - start < 15000UL) {
    digitalWrite(LED_STATUS_PIN, !digitalRead(LED_STATUS_PIN));
    delay(400);
  }

  digitalWrite(LED_STATUS_PIN, WiFi.status() == WL_CONNECTED ? HIGH : LOW);
  lcd.clear();
  lcd.print(WiFi.status() == WL_CONNECTED ? "WiFi OK" : "WiFi failed");
  lcd.setCursor(0, 1);
  lcd.print(WiFi.localIP());
}

void loadSettings() {
  EEPROM.get(0, settings);
  if (settings.magic != SETTINGS_MAGIC) {
    settings.magic = SETTINGS_MAGIC;
    settings.mode = MODE_AUTOMATIC;
    settings.startThreshold = 40;
    settings.stopThreshold = 55;
    settings.maxPumpDurationSeconds = 15;
    settings.cooldownSeconds = 60;
    settings.scheduleActive = false;
    settings.scheduleHour = 6;
    settings.scheduleMinute = 0;
    settings.scheduleDurationSeconds = 10;
    saveSettings();
  }
}

void saveSettings() {
  settings.magic = SETTINGS_MAGIC;
  EEPROM.put(0, settings);
  EEPROM.commit();
}

void readSensors() {
  sensors.soilRaw = analogRead(SOIL_PIN);
  sensors.soilPercent = map(sensors.soilRaw, SOIL_RAW_DRY, SOIL_RAW_WET, 0, 100);
  sensors.soilPercent = constrain(sensors.soilPercent, 0, 100);

  const float humidity = dht.readHumidity();
  const float temperature = dht.readTemperature();
  if (!isnan(humidity)) sensors.airHumidityPercent = humidity;
  if (!isnan(temperature)) sensors.temperatureC = temperature;
}

void renderLcd() {
  static unsigned long lastRenderAt = 0;
  if (millis() - lastRenderAt < 1500UL) return;
  lastRenderAt = millis();

  lcd.clear();
  lcd.print("Soil:");
  lcd.print(sensors.soilPercent);
  lcd.print("% ");
  lcd.print(pumpStateText());
  lcd.setCursor(0, 1);
  lcd.print((int)sensors.temperatureC);
  lcd.print("C ");
  lcd.print((int)sensors.airHumidityPercent);
  lcd.print("% ");
  lcd.print(modeText());
}

void sendTelemetry() {
  lastTelemetryAt = millis();
  if (WiFi.status() != WL_CONNECTED) {
    backendReachable = false;
    return;
  }

  HTTPClient http;
  http.begin(wifiClient, apiUrl("/iot/telemetry"));
  http.addHeader("Content-Type", "application/json");

  StaticJsonDocument<384> body;
  body["deviceId"] = DEVICE_ID;
  body["soilMoisturePercent"] = sensors.soilPercent;
  body["soilRaw"] = sensors.soilRaw;
  body["temperatureC"] = sensors.temperatureC;
  body["airHumidityPercent"] = sensors.airHumidityPercent;
  body["pumpState"] = pumpOn ? "on" : "off";
  body["isConnected"] = true;

  String payload;
  serializeJson(body, payload);
  const int code = http.POST(payload);
  backendReachable = code >= 200 && code < 300;
  http.end();
}

void fetchConfig() {
  lastConfigAt = millis();
  if (WiFi.status() != WL_CONNECTED) {
    backendReachable = false;
    return;
  }

  HTTPClient http;
  http.begin(wifiClient, apiUrl("/iot/config?deviceId=" + String(DEVICE_ID)));
  const int code = http.GET();
  if (code < 200 || code >= 300) {
    backendReachable = false;
    http.end();
    return;
  }

  StaticJsonDocument<1024> doc;
  DeserializationError error = deserializeJson(doc, http.getString());
  http.end();
  if (error || !doc["success"]) {
    backendReachable = false;
    return;
  }

  JsonObject data = doc["data"];
  settings.mode = String((const char *)data["mode"]) == "manual" ? MODE_MANUAL : MODE_AUTOMATIC;
  settings.startThreshold = data["startThreshold"] | settings.startThreshold;
  settings.stopThreshold = data["stopThreshold"] | settings.stopThreshold;
  settings.maxPumpDurationSeconds = data["maxPumpDurationSeconds"] | settings.maxPumpDurationSeconds;
  settings.cooldownSeconds = data["cooldownSeconds"] | settings.cooldownSeconds;

  JsonArray schedules = data["schedules"].as<JsonArray>();
  if (!schedules.isNull() && schedules.size() > 0) {
    JsonObject schedule = schedules[0];
    String timeValue = schedule["timeOfDay"] | "06:00:00";
    settings.scheduleHour = timeValue.substring(0, 2).toInt();
    settings.scheduleMinute = timeValue.substring(3, 5).toInt();
    settings.scheduleDurationSeconds = schedule["durationSeconds"] | 10;
    settings.scheduleActive = schedule["isActive"] | false;
  }

  backendReachable = true;
  saveSettings();
}

void fetchCommand() {
  lastCommandAt = millis();
  if (WiFi.status() != WL_CONNECTED) {
    backendReachable = false;
    return;
  }

  HTTPClient http;
  http.begin(wifiClient, apiUrl("/iot/commands/next?deviceId=" + String(DEVICE_ID)));
  const int code = http.GET();
  if (code < 200 || code >= 300) {
    backendReachable = false;
    http.end();
    return;
  }

  StaticJsonDocument<1024> doc;
  DeserializationError error = deserializeJson(doc, http.getString());
  http.end();
  if (error || !doc["success"]) {
    backendReachable = false;
    return;
  }

  backendReachable = true;
  if (doc["data"].isNull()) return;

  JsonObject data = doc["data"];
  long commandId = data["id"] | 0;
  const char *commandType = data["commandType"] | "";
  JsonObject payload = data["payload"].as<JsonObject>();
  executeCommand(commandId, commandType, payload);
}

void executeCommand(long commandId, const char *commandType, JsonObject payload) {
  String type = String(commandType);
  int durationSeconds = payload["durationSeconds"] | settings.maxPumpDurationSeconds;

  if (type == "manual_watering") {
    setPump(true);
    delay((unsigned long)durationSeconds * 1000UL);
    setPump(false);
    lastPumpStoppedAt = millis();
    ackCommand(commandId, "success", durationSeconds, "Watering executed");
  } else if (type == "pump_on") {
    setPump(true);
    ackCommand(commandId, "success", 0, "Pump on");
  } else if (type == "pump_off") {
    setPump(false);
    lastPumpStoppedAt = millis();
    ackCommand(commandId, "success", 0, "Pump off");
  } else if (type == "set_mode") {
    settings.mode = String((const char *)payload["mode"]) == "manual" ? MODE_MANUAL : MODE_AUTOMATIC;
    saveSettings();
    ackCommand(commandId, "success", 0, "Mode updated");
  } else if (type == "set_thresholds") {
    settings.startThreshold = payload["startThreshold"] | settings.startThreshold;
    settings.stopThreshold = payload["stopThreshold"] | settings.stopThreshold;
    settings.maxPumpDurationSeconds = payload["maxPumpDurationSeconds"] | settings.maxPumpDurationSeconds;
    settings.cooldownSeconds = payload["cooldownSeconds"] | settings.cooldownSeconds;
    saveSettings();
    ackCommand(commandId, "success", 0, "Thresholds updated");
  } else if (type == "set_schedule") {
    String timeValue = payload["timeOfDay"] | "06:00";
    settings.scheduleHour = timeValue.substring(0, 2).toInt();
    settings.scheduleMinute = timeValue.substring(3, 5).toInt();
    settings.scheduleDurationSeconds = payload["durationSeconds"] | settings.scheduleDurationSeconds;
    settings.scheduleActive = payload["isActive"] | settings.scheduleActive;
    saveSettings();
    ackCommand(commandId, "success", 0, "Schedule updated");
  } else {
    ackCommand(commandId, "failed", 0, "Unknown command");
  }
}

void ackCommand(long commandId, const char *status, int durationSeconds, const char *message) {
  if (commandId <= 0 || WiFi.status() != WL_CONNECTED) return;

  HTTPClient http;
  http.begin(wifiClient, apiUrl("/iot/commands/ack"));
  http.addHeader("Content-Type", "application/json");

  StaticJsonDocument<384> body;
  body["deviceId"] = DEVICE_ID;
  body["commandId"] = commandId;
  body["status"] = status;
  body["durationSeconds"] = durationSeconds;
  body["soilMoisturePercent"] = sensors.soilPercent;
  body["message"] = message;

  String payload;
  serializeJson(body, payload);
  http.POST(payload);
  http.end();
}

void applyAutomationFallback() {
  if (settings.mode != MODE_AUTOMATIC) return;

  const unsigned long cooldownMs = (unsigned long)settings.cooldownSeconds * 1000UL;
  if (!pumpOn && sensors.soilPercent < settings.startThreshold && millis() - lastPumpStoppedAt >= cooldownMs) {
    setPump(true);
  }

  if (pumpOn && sensors.soilPercent >= settings.stopThreshold) {
    setPump(false);
    lastPumpStoppedAt = millis();
  }
}

void applyScheduleFallback() {
  if (!settings.scheduleActive || settings.mode != MODE_AUTOMATIC) return;

  time_t now = time(nullptr);
  if (now < 100000) return;
  struct tm *timeInfo = localtime(&now);
  if (!timeInfo) return;

  if (timeInfo->tm_hour == settings.scheduleHour &&
      timeInfo->tm_min == settings.scheduleMinute &&
      lastScheduleDay != timeInfo->tm_yday) {
    lastScheduleDay = timeInfo->tm_yday;
    setPump(true);
    delay((unsigned long)settings.scheduleDurationSeconds * 1000UL);
    setPump(false);
    lastPumpStoppedAt = millis();
  }
}

void setPump(bool enabled) {
  pumpOn = enabled;
  digitalWrite(RELAY_PIN, RELAY_ACTIVE_LOW ? !enabled : enabled);
  digitalWrite(LED_STATUS_PIN, enabled ? HIGH : LOW);
  if (enabled) {
    pumpStartedAt = millis();
    tone(BUZZER_PIN, 1800, 120);
  }
}

String apiUrl(const String &path) {
  String base = API_BASE_URL;
  if (base.endsWith("/")) base.remove(base.length() - 1);
  return base + path;
}

String pumpStateText() {
  return pumpOn ? "ON" : "OFF";
}

String modeText() {
  return settings.mode == MODE_MANUAL ? "MAN" : "AUTO";
}
