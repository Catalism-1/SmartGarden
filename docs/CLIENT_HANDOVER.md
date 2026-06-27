# Client Handover SmartGarden LOCAL-ONLY

Ikuti langkah ini dari awal sampai akhir.

## 1. Install tools

Install:

1. Docker Desktop
2. Node.js
3. Git
4. Android Studio
5. Arduino IDE

## 2. Clone repository

```bat
git clone https://github.com/Catalism-1/SmartGarden.git
cd SmartGarden
```

## 3. Copy environment backend

```bat
copy backend-api\.env.example backend-api\.env
```

Jika perlu, edit `backend-api\.env`. Untuk lokal testing, default sudah cukup.

## 4. Jalankan Docker MySQL

```bat
docker compose up -d
```

## 5. Jalankan backend

```bat
cd backend-api
npm install
npm start
```

Backend berjalan di:

```text
http://0.0.0.0:3000
```

## 6. Cari IP laptop

Buka terminal baru dari root project:

```bat
scripts\show-ip.bat
```

Pakai IPv4 WiFi, contoh:

```text
192.168.1.10
```

## 7. Test health dari HP

Pastikan HP dan laptop berada di WiFi/hotspot yang sama.

Buka browser HP:

```text
http://IP_LAPTOP:3000/api/health
```

Contoh:

```text
http://192.168.1.10:3000/api/health
```

## 8. Install APK Android

Install APK ke HP. Di aplikasi:

1. Buka tab `System`.
2. Aktifkan `Gunakan Server Lokal`.
3. Isi IP laptop, contoh `192.168.1.10`.
4. Tekan `Simpan dan test server`.

## 9. Setup firmware ESP8266

Copy:

```text
firmware\esp8266-smartgarden\config.example.h
```

menjadi:

```text
firmware\esp8266-smartgarden\config.h
```

Isi:

```cpp
#define WIFI_SSID "Nama WiFi"
#define WIFI_PASSWORD "Password WiFi"
#define API_BASE_URL "http://192.168.1.10:3000/api"
#define DEVICE_ID "smartgarden-01"
```

Upload `esp8266-smartgarden.ino` melalui Arduino IDE.

## 10. Test lapangan

Checklist:

- Health endpoint bisa dibuka dari HP.
- Android `Server Lokal` berhasil connect.
- ESP8266 connect WiFi.
- LCD menampilkan soil, suhu, humidity, mode, dan pompa.
- Sensor telemetry muncul di dashboard Android.
- Tombol manual watering membuat pompa menyala sesuai durasi.
- Mode automatic menyiram saat tanah `< 40%`.
- Pompa mati saat soil moisture `>= 55%` atau max duration tercapai.
- Cooldown mencegah pompa hidup-mati cepat.
- Jadwal tersimpan dan dapat dipoll ESP.
- Sensor history terisi.
- Watering history terisi setelah command di-ack ESP.

## Troubleshooting

### HP tidak bisa akses backend

- Pastikan HP dan laptop berada di WiFi/hotspot yang sama.
- Test dari HP: `http://IP_LAPTOP:3000/api/health`.
- Pastikan backend masih running.
- Jangan pakai `localhost` di HP; gunakan IP laptop.

### Port 3000 diblokir firewall

- Buka Windows Security.
- Allow Node.js di private network.
- Pastikan WiFi profile adalah Private, bukan Public.
- Coba restart backend setelah rule firewall diizinkan.

### ESP tidak connect WiFi

- Cek SSID/password di `config.h`.
- Pastikan WiFi 2.4GHz aktif; ESP8266 tidak mendukung 5GHz-only.
- Dekatkan ESP ke router/hotspot.

### Sensor soil belum terkalibrasi

- Baca nilai kering dan basah dari Serial Monitor.
- Update:

```cpp
#define SOIL_RAW_DRY 850
#define SOIL_RAW_WET 350
```

### Relay aktif terbalik

Jika pompa menyala saat status OFF, ubah:

```cpp
#define RELAY_ACTIVE_LOW false
```

### Server belum hidup

Jalankan:

```bat
scripts\start-local.bat
```

### IP laptop berubah

- Jalankan ulang `scripts\show-ip.bat`.
- Update IP di Android.
- Update `API_BASE_URL` di firmware `config.h`, lalu upload ulang bila perlu.

## Stop service lokal

```bat
scripts\stop-local.bat
```

Script ini tidak menghapus data database.
