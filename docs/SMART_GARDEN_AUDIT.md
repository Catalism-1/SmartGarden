# SmartGarden Project Audit

Tanggal audit: 2026-06-27

## Kondisi project saat ini

Project lokal di `D:\ANDROID STUDIO\SmartGarden` adalah Android Kotlin XML dengan ViewBinding, bukan Jetpack Compose. UI utama masih berada di `activity_main.xml` dan dikendalikan oleh `MainActivity.kt`.

Status terakhir sebelum fondasi backend:

- Android app sudah memiliki package `com.example.smartgarden`.
- Module Android valid melalui `include(":app")`.
- UI masih prototype lokal dengan data simulasi.
- Mode automatic/manual, status pompa, status koneksi, jadwal sederhana, threshold, dan insight lokal sudah ada.
- Belum ada integrasi hardware langsung.
- Belum ada backend/API/database.
- Belum ada MQTT, Room, Retrofit, Firebase, atau Compose.

## File penting Android

- `settings.gradle.kts`: root Gradle project dan include module `:app`.
- `build.gradle.kts`: root build configuration.
- `gradle/libs.versions.toml`: version catalog dependency Android.
- `app/build.gradle.kts`: konfigurasi Android application, namespace, applicationId, SDK, dependency, ViewBinding.
- `app/src/main/AndroidManifest.xml`: manifest aplikasi, launcher activity, permission internet/network state.
- `app/src/main/java/com/example/smartgarden/MainActivity.kt`: UI controller lokal untuk dashboard, jadwal, insight, status sistem, mode, dan pompa manual.
- `app/src/main/java/com/example/smartgarden/GardenController.kt`: business logic status kebun dan hysteresis pompa.
- `app/src/main/java/com/example/smartgarden/data/model/`: model lokal seperti `GardenDeviceState`, `GardenMode`, `PumpState`, `SensorSnapshot`, dan `GardenSettings`.
- `app/src/main/java/com/example/smartgarden/data/local/SettingsStorage.kt`: wrapper SharedPreferences.
- `app/src/main/res/layout/activity_main.xml`: layout XML utama.

## Mock data dan state saat ini

Mock data lokal ada di `MainActivity.kt` pada `SIMULATED_READINGS`. Data ini dipakai untuk refresh insight tanpa ESP/backend.

State utama masih berada di memory `MainActivity`:

- mode automatic/manual
- soil moisture
- temperature
- air humidity
- pump state
- connection state
- schedule hour/minute/enabled
- notification enabled
- threshold lokal

Sebagian setting disimpan lewat `SettingsStorage`.

## Gap terhadap kebutuhan client

1. Backend belum tersedia.
2. Database belum tersedia untuk sensor history, watering log, command queue, schedule persistency, dan device registry.
3. ESP8266 belum punya kontrak API final untuk telemetry/config/command polling.
4. UI Android belum membaca API; mode demo lokal perlu dipertahankan sambil disiapkan repository API.
5. Belum ada audit trail untuk penyiraman manual/automatic/schedule.
6. Hysteresis client terbaru adalah `40/55`; versi prototype sebelumnya memakai batas off lebih rendah.
7. Belum ada konfigurasi backend untuk max pump duration dan cooldown.
8. Belum ada dokumentasi setup lokal.

## Implementasi tahap ini

Tahap ini menambahkan fondasi tanpa mengubah desain UI besar:

- Backend lokal `backend-api` memakai Node.js, Express, dan MySQL2.
- MySQL lokal via `docker-compose.yml`.
- Schema minimal: `devices`, `sensor_readings`, `garden_settings`, `pump_commands`, `watering_logs`, dan `watering_schedules`.
- Default device `smartgarden-01`.
- Default konfigurasi: mode `automatic`, start threshold `40`, stop threshold `55`, max pump duration `15` detik, cooldown `60` detik.
- Endpoint telemetry, config, command polling, dashboard, history, mode, manual pump, thresholds, dan schedules.
- Android ditambah layer `data/remote` dan `data/repository` agar siap integrasi API, tetapi UI demo lokal tidak dihapus.

## Rencana implementasi bertahap berikutnya

1. Tambahkan layar/opsi konfigurasi base URL backend di Android, tetap dengan default mode demo.
2. Sambungkan dashboard Android ke `GET /api/garden/dashboard` dengan fallback data lokal saat backend offline.
3. Tampilkan sensor history dan watering history dari API.
4. Tambahkan firmware ESP8266 untuk telemetry, config polling, command polling, command ack, dan fallback config lokal.
5. Tambahkan scheduler backend atau ESP-side schedule executor sesuai keputusan deployment.
6. Tambahkan autentikasi ringan/API token hanya setelah alur dasar stabil.
