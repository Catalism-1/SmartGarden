# SmartGarden ESP8266 Contract

Tahap ini belum membuat firmware final. Dokumen ini menjadi kontrak awal agar ESP8266 dan Android/backend bisa dikembangkan paralel.

## Hardware target

- NodeMCU ESP8266
- Soil Moisture Sensor V2.0
- DHT11
- Relay
- Pompa air
- LCD I2C
- WiFi/hotspot

## Device ID

Default device:

```text
smartgarden-01
```

Device ID dikirim di semua request ESP ke backend.

## Alur ESP

1. Boot.
2. Connect WiFi.
3. Load konfigurasi fallback lokal dari EEPROM/LittleFS.
4. GET `/api/iot/config`.
5. Simpan konfigurasi terbaru lokal bila request berhasil.
6. Baca sensor berkala.
7. POST `/api/iot/telemetry`.
8. GET `/api/iot/commands/next` berkala.
9. Eksekusi command.
10. POST `/api/iot/commands/ack`.
11. Bila backend offline, pakai konfigurasi lokal terakhir sebagai fallback dasar.

## Telemetry payload

```json
{
  "deviceId": "smartgarden-01",
  "soilMoisturePercent": 38,
  "soilRaw": 730,
  "temperatureC": 28.5,
  "airHumidityPercent": 70,
  "pumpState": "off",
  "isConnected": true
}
```

## Config response penting

ESP perlu menyimpan:

- `mode`
- `startThreshold`
- `stopThreshold`
- `maxPumpDurationSeconds`
- `cooldownSeconds`
- `schedules`

Default backend:

- mode `automatic`
- start threshold `40`
- stop threshold `55`
- max pump duration `15` detik
- cooldown `60` detik

## Command types

Command yang mungkin diterima ESP:

- `manual_watering`
- `pump_on`
- `pump_off`
- `set_mode`
- `set_thresholds`
- `set_schedule`

Contoh command:

```json
{
  "success": true,
  "message": "Command loaded",
  "data": {
    "id": 1,
    "commandType": "manual_watering",
    "status": "sent",
    "payload": {
      "source": "manual",
      "durationSeconds": 10
    }
  }
}
```

## Ack payload

```json
{
  "deviceId": "smartgarden-01",
  "commandId": 1,
  "status": "success",
  "durationSeconds": 10,
  "soilMoisturePercent": 38,
  "message": "Executed"
}
```

## Safety rules untuk firmware berikutnya

- Relay tidak boleh menyala lebih lama dari `maxPumpDurationSeconds`.
- Respect `cooldownSeconds` setelah penyiraman otomatis.
- Gunakan hysteresis:
  - ON jika soil moisture `< startThreshold`.
  - OFF jika soil moisture `>= stopThreshold`.
- Simpan konfigurasi terakhir secara lokal sebagai fallback.
- Jangan hardcode password WiFi/API token di repo.
