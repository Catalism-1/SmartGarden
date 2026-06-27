# 01 - Persiapan

[Beranda](../README.md) |
[1 Persiapan](01_PERSIAPAN.md) |
[2 Server Lokal](02_INSTALASI_SERVER_LOKAL.md) |
[3 Android](03_SETUP_APLIKASI_ANDROID.md) |
[4 ESP8266](04_SETUP_ESP8266.md) |
[5 Wiring](05_WIRING_RANGKAIAN.md) |
[6 Penggunaan](06_CARA_PENGGUNAAN.md) |
[7 Troubleshooting](07_TROUBLESHOOTING.md) |
[8 Checklist](08_CHECKLIST_CLIENT.md)

Dokumen ini membantu kamu menyiapkan software dan hardware.

Targetnya sederhana:

- Laptop siap menjadi server lokal.
- HP Android siap menjalankan aplikasi.
- ESP8266 siap di-upload firmware.

## Istilah penting

- Server lokal: program yang berjalan di laptop sendiri.
- Docker: aplikasi untuk menjalankan database MySQL dengan mudah.
- Node.js: aplikasi untuk menjalankan backend SmartGarden.
- Backend: program yang menerima data dari ESP8266 dan aplikasi Android.
- IP address: alamat laptop di jaringan WiFi.
- Firmware: program yang di-upload ke ESP8266.

## Software yang perlu diinstall

### 1. Docker Desktop

Docker Desktop dipakai untuk menjalankan MySQL.

- [ ] Download Docker Desktop dari website resmi Docker.
- [ ] Install Docker Desktop.
- [ ] Restart laptop jika diminta.
- [ ] Buka Docker Desktop.
- [ ] Tunggu sampai statusnya berjalan.

Hasil yang diharapkan:

- Docker Desktop terbuka.
- Tidak ada pesan error.
- Docker menampilkan status berjalan.

> 📸 Screenshot yang perlu ditambahkan:
> - Tampilan Docker Desktop berjalan

### 2. Node.js

Node.js dipakai untuk menjalankan backend.

- [ ] Download Node.js versi LTS.
- [ ] Install Node.js.
- [ ] Buka PowerShell.
- [ ] Ketik command ini.

```powershell
node --version
```

Hasil yang diharapkan:

```text
v20.x.x
```

Versi boleh berbeda.

Lalu cek npm:

```powershell
npm --version
```

Hasil yang diharapkan:

```text
10.x.x
```

### 3. Arduino IDE

Arduino IDE dipakai untuk upload firmware ke ESP8266.

- [ ] Download Arduino IDE.
- [ ] Install Arduino IDE.
- [ ] Buka Arduino IDE.

Hasil yang diharapkan:

- Arduino IDE terbuka.
- Menu `Tools` bisa diklik.

### 4. Driver CH340

Beberapa NodeMCU ESP8266 memakai chip USB CH340.

Install driver ini jika ESP8266 tidak muncul di menu Port Arduino IDE.

- [ ] Sambungkan ESP8266 ke laptop.
- [ ] Buka Device Manager.
- [ ] Cek apakah muncul port baru.
- [ ] Jika tidak muncul, install driver CH340.

> [!TIP]
> Jika board sudah muncul sebagai COM port, driver tambahan mungkin tidak diperlukan.

### 5. Android APK

APK adalah file aplikasi Android.

APK debug bisa dibuat dari Android Studio atau Gradle.

Untuk client lapangan, developer biasanya memberikan file APK yang sudah siap install.

- [ ] Simpan APK di HP.
- [ ] Pastikan HP bisa install aplikasi dari file APK.

## Hardware yang dibutuhkan

- [ ] NodeMCU ESP8266.
- [ ] Soil Moisture Sensor.
- [ ] DHT11.
- [ ] Relay module.
- [ ] Pompa air DC.
- [ ] LCD I2C 16x2.
- [ ] Kabel jumper.
- [ ] Breadboard atau PCB.
- [ ] Power supply untuk pompa.
- [ ] Laptop Windows.
- [ ] HP Android.
- [ ] WiFi atau hotspot yang sama untuk laptop, HP, dan ESP8266.

## Cek instalasi berhasil

Jalankan PowerShell.

Cek Node.js:

```powershell
node --version
```

Cek npm:

```powershell
npm --version
```

Cek Docker:

```powershell
docker --version
```

Hasil yang diharapkan:

- Semua command menampilkan versi.
- Tidak muncul pesan `not recognized`.

## Checklist persiapan

- [ ] Docker Desktop sudah terinstall.
- [ ] Docker Desktop bisa dibuka.
- [ ] Node.js sudah terinstall.
- [ ] `node --version` berhasil.
- [ ] `npm --version` berhasil.
- [ ] Arduino IDE sudah terinstall.
- [ ] ESP8266 terdeteksi di laptop.
- [ ] Hardware sensor dan pompa tersedia.
- [ ] HP Android tersedia.
- [ ] WiFi atau hotspot tersedia.

## Lanjut

Jika semua sudah siap, lanjut ke:

[02 - Instalasi Server Lokal](02_INSTALASI_SERVER_LOKAL.md)

[Beranda](../README.md) |
[1 Persiapan](01_PERSIAPAN.md) |
[2 Server Lokal](02_INSTALASI_SERVER_LOKAL.md) |
[3 Android](03_SETUP_APLIKASI_ANDROID.md) |
[4 ESP8266](04_SETUP_ESP8266.md) |
[5 Wiring](05_WIRING_RANGKAIAN.md) |
[6 Penggunaan](06_CARA_PENGGUNAAN.md) |
[7 Troubleshooting](07_TROUBLESHOOTING.md) |
[8 Checklist](08_CHECKLIST_CLIENT.md)
