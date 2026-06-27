# Local Setup SmartGarden

Semua komponen berjalan di jaringan lokal. Laptop client menjadi server.

## Prasyarat

- Docker Desktop
- Node.js
- Git
- Android Studio atau ADB untuk install APK
- Arduino IDE untuk upload firmware ESP8266

## Setup backend dan database

Dari root repository:

```bat
copy backend-api\.env.example backend-api\.env
docker compose up -d
cd backend-api
npm install
npm start
```

Backend listen di:

```text
http://0.0.0.0:3000
```

Health check dari laptop:

```text
http://localhost:3000/api/health
```

Health check dari HP:

```text
http://IP_LAPTOP:3000/api/health
```

## Script helper Windows

Start MySQL dan backend:

```bat
scripts\start-local.bat
```

Stop service tanpa menghapus data database:

```bat
scripts\stop-local.bat
```

Lihat IP laptop:

```bat
scripts\show-ip.bat
```

## Database lokal

Default `.env.example`:

```text
DB_HOST=127.0.0.1
DB_PORT=3306
DB_NAME=smart_garden
DB_USER=smartgarden
DB_PASSWORD=smartgarden_dev_password
```

Ini credential development lokal, bukan secret production.

## Android

1. Install APK debug/release.
2. Buka halaman `System`.
3. Aktifkan `Gunakan Server Lokal`.
4. Isi IP laptop, contoh `192.168.1.10`.
5. Tekan `Simpan dan test server`.

Aplikasi akan membentuk URL:

```text
http://192.168.1.10:3000/api
```

Jika server belum hidup atau IP salah, aplikasi tetap aman dan bisa memakai Mode Demo.
