# SmartGarden Backend API

Backend lokal ini menyiapkan fondasi koneksi bertahap:

```text
Android App -> Node.js/Express API -> MySQL
ESP8266 -> POST telemetry ke API
ESP8266 -> GET command/config dari API secara berkala
```

Backend listen di `0.0.0.0:3000`.

Default database lokal:

```text
host: localhost
port: 3306
database: smart_garden
user/password: lihat backend-api/.env
```

## Menjalankan lokal

1. Jalankan MySQL:

   ```powershell
   docker compose up -d mysql
   ```

2. Install dependency backend:

   ```powershell
   cd backend-api
   npm install
   copy .env.example .env
   npm start
   ```

3. Test:

   ```powershell
   curl http://localhost:3000/api/health
   ```

File `.env` hanya untuk lokal dan tidak boleh di-commit.
