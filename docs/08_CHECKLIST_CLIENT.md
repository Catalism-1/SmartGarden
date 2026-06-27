# 08 - Checklist Client

[Beranda](../README.md) |
[1 Persiapan](01_PERSIAPAN.md) |
[2 Server Lokal](02_INSTALASI_SERVER_LOKAL.md) |
[3 Android](03_SETUP_APLIKASI_ANDROID.md) |
[4 ESP8266](04_SETUP_ESP8266.md) |
[5 Wiring](05_WIRING_RANGKAIAN.md) |
[6 Penggunaan](06_CARA_PENGGUNAAN.md) |
[7 Troubleshooting](07_TROUBLESHOOTING.md) |
[8 Checklist](08_CHECKLIST_CLIENT.md)

Gunakan checklist ini saat implementasi lapangan.

Centang satu per satu.

## Sebelum mulai

- [ ] Semua komponen tersedia.
- [ ] Laptop Windows tersedia.
- [ ] HP Android tersedia.
- [ ] NodeMCU ESP8266 tersedia.
- [ ] Sensor soil moisture tersedia.
- [ ] DHT11 tersedia.
- [ ] Relay tersedia.
- [ ] Pompa DC tersedia.
- [ ] LCD I2C tersedia.
- [ ] Kabel jumper tersedia.
- [ ] Laptop, HP, dan ESP terhubung jaringan yang sama.

## Software

- [ ] Docker Desktop sudah terinstall.
- [ ] Docker Desktop berjalan.
- [ ] Node.js sudah terinstall.
- [ ] Arduino IDE sudah terinstall.
- [ ] Driver CH340 sudah terinstall jika diperlukan.
- [ ] Repository SmartGarden sudah ada di laptop.

## Backend

- [ ] File `backend-api\.env` sudah dibuat dari `.env.example`.
- [ ] Docker MySQL hidup.
- [ ] Backend aktif di port 3000.
- [ ] Endpoint health bisa dibuka dari laptop.
- [ ] Endpoint health bisa dibuka dari HP.
- [ ] IP laptop sudah dicatat.

## Aplikasi

- [ ] APK berhasil terinstal.
- [ ] Aplikasi bisa dibuka.
- [ ] Server Lokal berhasil diuji.
- [ ] Status koneksi aplikasi Online.
- [ ] Mode Demo tetap bisa digunakan.

## ESP8266

- [ ] Board ESP8266 sudah terinstall di Arduino IDE.
- [ ] Library Arduino sudah terinstall.
- [ ] File `config.h` sudah dibuat dari `config.example.h`.
- [ ] WiFi sudah diisi di `config.h`.
- [ ] `API_BASE_URL` memakai IP laptop.
- [ ] Firmware berhasil diupload.
- [ ] ESP8266 berhasil connect WiFi.
- [ ] Telemetry masuk.
- [ ] LCD menampilkan data.

## Wiring

- [ ] Soil sensor AO ke A0.
- [ ] DHT11 DATA ke D4.
- [ ] Relay IN ke D5.
- [ ] LCD SDA ke D1.
- [ ] LCD SCL ke D2.
- [ ] Semua GND common.
- [ ] Power supply pompa sesuai spesifikasi.

## Pompa

- [ ] Relay teruji.
- [ ] Pompa manual berjalan.
- [ ] Pompa manual dengan durasi berjalan.
- [ ] Mode otomatis berjalan.
- [ ] Pompa ON saat soil moisture `< 40%`.
- [ ] Pompa OFF saat soil moisture `>= 55%`.
- [ ] Cooldown berjalan.
- [ ] Jadwal berjalan.

## Histori

- [ ] Sensor history tampil.
- [ ] Watering log tampil.
- [ ] Source log manual tampil.
- [ ] Source log automatic tampil jika otomatis berjalan.
- [ ] Source log schedule tampil jika jadwal berjalan.

## Serah terima

- [ ] Client tahu cara menyalakan server lokal.
- [ ] Client tahu cara mencari IP laptop.
- [ ] Client tahu cara mengubah IP di aplikasi.
- [ ] Client tahu cara menghentikan server.
- [ ] Client tahu file `config.h` tidak boleh diupload ke GitHub.
- [ ] Client tahu laptop harus menyala saat sistem dipakai.

## Selesai

Jika semua checklist utama sudah tercentang, sistem siap digunakan.

[Beranda](../README.md) |
[1 Persiapan](01_PERSIAPAN.md) |
[2 Server Lokal](02_INSTALASI_SERVER_LOKAL.md) |
[3 Android](03_SETUP_APLIKASI_ANDROID.md) |
[4 ESP8266](04_SETUP_ESP8266.md) |
[5 Wiring](05_WIRING_RANGKAIAN.md) |
[6 Penggunaan](06_CARA_PENGGUNAAN.md) |
[7 Troubleshooting](07_TROUBLESHOOTING.md) |
[8 Checklist](08_CHECKLIST_CLIENT.md)
