# ESP8266 Setup SmartGarden

Firmware berada di:

```text
firmware/esp8266-smartgarden
```

## Library Arduino IDE

Install:

- ESP8266 board package
- ArduinoJson
- DHT sensor library
- Adafruit Unified Sensor bila diminta
- LiquidCrystal I2C

Board:

```text
NodeMCU 1.0 (ESP-12E Module)
```

## Konfigurasi

Copy:

```text
firmware/esp8266-smartgarden/config.example.h
```

menjadi:

```text
firmware/esp8266-smartgarden/config.h
```

Isi:

```cpp
#define WIFI_SSID "YOUR_WIFI_NAME"
#define WIFI_PASSWORD "YOUR_WIFI_PASSWORD"
#define API_BASE_URL "http://YOUR_LAPTOP_IP:3000/api"
#define DEVICE_ID "smartgarden-01"
```

Jangan commit `config.h`.

## Upload

1. Hubungkan NodeMCU ke laptop.
2. Buka `esp8266-smartgarden.ino`.
3. Pilih board NodeMCU.
4. Pilih port.
5. Upload.

## Flow firmware

- Connect WiFi.
- Ambil config backend.
- Baca soil moisture dan DHT11.
- Kirim telemetry ke backend.
- Poll command dari backend.
- Eksekusi manual watering, pump on/off, mode, threshold, dan schedule.
- Ack command ke backend.
- Simpan config terakhir di EEPROM.
- Jika backend offline, pakai fallback hysteresis lokal.
