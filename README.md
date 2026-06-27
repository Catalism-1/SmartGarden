# SmartGarden LOCAL-ONLY

SmartGarden adalah paket lokal untuk sistem penyiraman otomatis/manual berbasis NodeMCU ESP8266, Android APK, Node.js API, dan MySQL lokal.

Tidak memakai Vercel, Aiven, Firebase, cloud database, MQTT, atau layanan hosting cloud.

Arsitektur:

```text
Android APK ─┐
             ├─ WiFi/hotspot yang sama ─→ Laptop Node.js API ─→ Docker MySQL
ESP8266 ─────┘
```

Laptop client menjadi server lokal. Android dan ESP8266 tidak mengakses MySQL langsung.

## Quick start Windows

```bat
git clone https://github.com/Catalism-1/SmartGarden.git
cd SmartGarden
copy backend-api\.env.example backend-api\.env
scripts\start-local.bat
```

Cari IP laptop:

```bat
scripts\show-ip.bat
```

Health check:

```text
http://IP_LAPTOP:3000/api/health
```

## Folder penting

```text
app/                         Android Kotlin XML app
backend-api/                 Node.js Express API
firmware/esp8266-smartgarden ESP8266 firmware
docs/                        Dokumentasi client-ready
scripts/                     Helper Windows
docker-compose.yml           MySQL lokal
```

## Dokumentasi

- `docs/CLIENT_HANDOVER.md`
- `docs/LOCAL_SETUP.md`
- `docs/ESP8266_SETUP.md`
- `docs/WIRING_GUIDE.md`
- `docs/API_REFERENCE.md`
- `docs/PROJECT_STATUS.md`

Mulai dari `docs/CLIENT_HANDOVER.md` untuk implementasi lapangan.
