# SmartGarden Local Setup

## Prasyarat

- Node.js 20+ atau versi LTS terbaru.
- Docker Desktop.
- Android Studio untuk menjalankan aplikasi Android.

## Menjalankan MySQL lokal

Dari root project:

```powershell
docker compose up -d mysql
```

MySQL akan membaca schema dari:

```text
backend-api/schema.sql
```

Database lokal:

```text
host: 127.0.0.1
port: 3306
database: smart_garden
user: smartgarden
password: smartgarden_dev_password
```

Password ini hanya contoh lokal development. Jangan commit `.env` berisi credential asli.

## Menjalankan backend

```powershell
cd backend-api
npm install
copy .env.example .env
npm start
```

Backend listen di:

```text
0.0.0.0:3000
```

Health check:

```powershell
curl http://localhost:3000/api/health
```

## Test manual cepat

Kirim telemetry:

```powershell
curl -X POST http://localhost:3000/api/iot/telemetry `
  -H "Content-Type: application/json" `
  -d "{\"deviceId\":\"smartgarden-01\",\"soilMoisturePercent\":38,\"soilRaw\":730,\"temperatureC\":28.5,\"airHumidityPercent\":70,\"pumpState\":\"off\"}"
```

Ambil dashboard:

```powershell
curl http://localhost:3000/api/garden/dashboard?deviceId=smartgarden-01
```

Buat command manual:

```powershell
curl -X POST http://localhost:3000/api/garden/pump/manual `
  -H "Content-Type: application/json" `
  -d "{\"deviceId\":\"smartgarden-01\",\"durationSeconds\":10}"
```

ESP mengambil command:

```powershell
curl http://localhost:3000/api/iot/commands/next?deviceId=smartgarden-01
```

Ack command:

```powershell
curl -X POST http://localhost:3000/api/iot/commands/ack `
  -H "Content-Type: application/json" `
  -d "{\"deviceId\":\"smartgarden-01\",\"commandId\":1,\"status\":\"success\",\"durationSeconds\":10,\"soilMoisturePercent\":38,\"message\":\"Executed\"}"
```

Watering log:

```powershell
curl http://localhost:3000/api/garden/history/watering?deviceId=smartgarden-01
```

## Android

Android masih mempertahankan mode demo lokal. Layer API awal tersedia di:

```text
app/src/main/java/com/example/smartgarden/data/remote
app/src/main/java/com/example/smartgarden/data/repository
```

Default base URL emulator Android:

```text
http://10.0.2.2:3000
```

Untuk device fisik, gunakan IP komputer di jaringan yang sama pada tahap integrasi UI berikutnya.
