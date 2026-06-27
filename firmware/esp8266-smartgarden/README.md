# ESP8266 SmartGarden Firmware

Firmware ini untuk paket LOCAL-ONLY SmartGarden:

```text
ESP8266 -> WiFi/hotspot lokal -> Laptop Node.js API -> Docker MySQL
```

Tidak ada cloud, HTTPS, MQTT, Firebase, atau database langsung dari ESP.

## Library Arduino IDE

Install library berikut melalui Library Manager:

- `ArduinoJson`
- `DHT sensor library`
- `LiquidCrystal I2C`
- dependency `Adafruit Unified Sensor` bila diminta library DHT

Board:

- `ESP8266 Boards`
- Pilih `NodeMCU 1.0 (ESP-12E Module)`

## Konfigurasi lokal

1. Copy:

   ```text
   config.example.h
   ```

   menjadi:

   ```text
   config.h
   ```

2. Isi WiFi dan IP laptop:

   ```cpp
   #define WIFI_SSID "Nama WiFi"
   #define WIFI_PASSWORD "Password WiFi"
   #define API_BASE_URL "http://192.168.1.10:3000/api"
   #define DEVICE_ID "smartgarden-01"
   ```

3. Upload `esp8266-smartgarden.ino`.

`config.h` di-ignore oleh Git supaya SSID, password, dan IP laptop client tidak ikut ter-commit.

## Pin default

| Komponen | NodeMCU |
| --- | --- |
| Soil moisture analog | A0 |
| DHT11 data | D4 |
| Relay input | D5 |
| LCD I2C SDA | D1 |
| LCD I2C SCL | D2 |
| LED status opsional | D6 |
| Buzzer opsional | D7 |

## Fallback lokal

Firmware menyimpan konfigurasi terakhir ke EEPROM:

- mode automatic/manual
- start threshold
- stop threshold
- max pump duration
- cooldown
- satu jadwal aktif pertama dari backend

Jika backend tidak dapat diakses, ESP tetap menjalankan hysteresis lokal dengan konfigurasi terakhir.
