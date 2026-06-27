# Project Status SmartGarden LOCAL-ONLY

Tanggal: 2026-06-27

## Kondisi project saat ini

SmartGarden adalah paket lokal untuk sistem penyiraman tanaman berbasis:

```text
Android APK ─┐
             ├─ WiFi/hotspot lokal ─→ Laptop Node.js API ─→ Docker MySQL
ESP8266 ─────┘
```

Project Android lokal saat ini menggunakan Kotlin XML + ViewBinding. Beberapa brief sebelumnya menyebut Compose, tetapi struktur repository yang aktif adalah XML, jadi UI lama dipertahankan dan hanya ditambah kontrol kecil untuk server lokal.

## Fitur yang sudah ada sebelum tahap ini

- Dashboard lokal dengan soil moisture, suhu, kelembapan udara, status pompa, dan koneksi.
- Mode automatic/manual.
- Kontrol pompa manual di mode manual.
- Jadwal lokal sederhana.
- Insight lokal dengan data simulasi.
- `GardenController` untuk hysteresis dan status kebun.
- `SettingsStorage` untuk SharedPreferences.

## Fitur yang ditambahkan

- Backend lokal `backend-api` berbasis Node.js, Express, dan MySQL2.
- Docker Compose untuk MySQL lokal.
- Database schema non-destruktif dengan `CREATE TABLE IF NOT EXISTS`.
- Endpoint API untuk telemetry, config, command queue, dashboard, history, mode, pump, thresholds, dan schedules.
- Firmware ESP8266 siap upload di `firmware/esp8266-smartgarden`.
- Android mode:
  - `Mode Demo`
  - `Server Lokal`
- Input IP laptop di aplikasi Android.
- Repository Android untuk membaca/menulis API lokal tanpa library berat baru.
- Dokumentasi client-ready.
- Script helper Windows untuk start/stop local stack dan melihat IP laptop.

## Cara implementasi client secara ringkas

1. Clone repository.
2. Copy `backend-api/.env.example` menjadi `backend-api/.env`.
3. Jalankan `scripts/start-local.bat`.
4. Cari IP laptop dengan `scripts/show-ip.bat`.
5. Install APK Android.
6. Di Android, aktifkan `Server Lokal` dan isi IP laptop.
7. Copy firmware `config.example.h` menjadi `config.h`.
8. Isi WiFi dan `API_BASE_URL` pada `config.h`.
9. Upload firmware ke NodeMCU ESP8266.
10. Test telemetry, manual watering, automatic watering, schedule, dan history.

## Batasan tahap ini

- Tidak memakai cloud hosting, Vercel, Aiven, Firebase, MQTT, atau database cloud.
- ESP8266 dan Android tidak mengakses MySQL langsung.
- Android masih mempertahankan mode demo agar aman bila server belum hidup.
- Firmware memakai library Arduino yang harus diinstall lewat Arduino IDE.
