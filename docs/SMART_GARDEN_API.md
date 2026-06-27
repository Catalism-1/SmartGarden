# SmartGarden API Contract

Base URL lokal:

```text
http://localhost:3000
```

Semua response memakai format konsisten:

```json
{
  "success": true,
  "message": "Human readable message",
  "data": {}
}
```

## Endpoint

### GET /api/health

Mengecek status API dan koneksi database.

### POST /api/iot/telemetry

Dipakai ESP8266 untuk mengirim pembacaan sensor.

```json
{
  "deviceId": "smartgarden-01",
  "soilMoisturePercent": 38,
  "soilRaw": 730,
  "temperatureC": 28.5,
  "airHumidityPercent": 70,
  "pumpState": "off",
  "isConnected": true,
  "readAt": "2026-06-27T08:00:00Z"
}
```

`soilRaw` dan `readAt` opsional. Jika mode automatic aktif dan tanah kering, backend dapat membuat command penyiraman untuk diambil ESP lewat polling.

### GET /api/iot/config

Query:

```text
deviceId=smartgarden-01
```

Mengembalikan mode, threshold, max duration, cooldown, dan jadwal aktif/nonaktif.

### GET /api/iot/commands/next

Query:

```text
deviceId=smartgarden-01
```

ESP memanggil endpoint ini secara berkala. Backend mengembalikan command pending paling lama dan menandainya sebagai `sent`.

### POST /api/iot/commands/ack

ESP mengirim acknowledgement setelah command dijalankan.

```json
{
  "deviceId": "smartgarden-01",
  "commandId": 1,
  "status": "success",
  "durationSeconds": 10,
  "soilMoisturePercent": 38,
  "message": "Pump executed"
}
```

`status` dapat `success` atau `failed`. Command penyiraman akan dicatat ke `watering_logs`.

### GET /api/garden/dashboard

Query:

```text
deviceId=smartgarden-01
```

Mengembalikan device summary, latest reading, settings, dan jumlah command pending/sent.

### GET /api/garden/history/sensors

Query:

```text
deviceId=smartgarden-01&limit=100
```

Mengembalikan riwayat sensor untuk grafik.

### GET /api/garden/history/watering

Query:

```text
deviceId=smartgarden-01&limit=100
```

Mengembalikan log penyiraman.

### POST /api/garden/mode

```json
{
  "deviceId": "smartgarden-01",
  "mode": "automatic"
}
```

`mode` dapat `automatic` atau `manual`. Backend memperbarui setting dan membuat command `set_mode`.

### POST /api/garden/pump/manual

```json
{
  "deviceId": "smartgarden-01",
  "durationSeconds": 10
}
```

Backend membuat command `manual_watering`. Durasi dibatasi oleh `max_pump_duration_seconds` dari setting.

### POST /api/garden/settings/thresholds

```json
{
  "deviceId": "smartgarden-01",
  "startThreshold": 40,
  "stopThreshold": 55,
  "maxPumpDurationSeconds": 15,
  "cooldownSeconds": 60
}
```

Backend memperbarui setting dan membuat command `set_thresholds`.

### POST /api/garden/schedules

```json
{
  "deviceId": "smartgarden-01",
  "timeOfDay": "06:00",
  "durationSeconds": 10,
  "isActive": true
}
```

Membuat jadwal dan command `set_schedule`.

### GET /api/garden/schedules

Query:

```text
deviceId=smartgarden-01
```

Mengembalikan daftar jadwal.

### PATCH /api/garden/schedules/:id

```json
{
  "deviceId": "smartgarden-01",
  "timeOfDay": "06:30",
  "durationSeconds": 12,
  "isActive": false
}
```

Memperbarui jadwal dan membuat command `set_schedule`.
