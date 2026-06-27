# 03 - Setup Aplikasi Android

[Beranda](../README.md) |
[1 Persiapan](01_PERSIAPAN.md) |
[2 Server Lokal](02_INSTALASI_SERVER_LOKAL.md) |
[3 Android](03_SETUP_APLIKASI_ANDROID.md) |
[4 ESP8266](04_SETUP_ESP8266.md) |
[5 Wiring](05_WIRING_RANGKAIAN.md) |
[6 Penggunaan](06_CARA_PENGGUNAAN.md) |
[7 Troubleshooting](07_TROUBLESHOOTING.md) |
[8 Checklist](08_CHECKLIST_CLIENT.md)

Aplikasi Android dipakai untuk melihat data dan memberi perintah pompa.

Aplikasi punya dua mode:

- Mode Demo.
- Server Lokal.

Mode Demo bisa dipakai tanpa backend.

Server Lokal dipakai saat backend laptop sudah berjalan.

## 1. Install APK

- [ ] Pindahkan file APK ke HP Android.
- [ ] Buka file APK.
- [ ] Tekan Install.

Jika muncul peringatan sumber tidak dikenal:

- [ ] Tekan Settings.
- [ ] Aktifkan izin install dari sumber ini.
- [ ] Kembali ke file APK.
- [ ] Install lagi.

> [!WARNING]
> Install APK hanya dari file yang diberikan developer project ini.

## 2. Pastikan HP satu jaringan dengan laptop

- [ ] Sambungkan HP ke WiFi yang sama dengan laptop.

Atau:

- [ ] Nyalakan hotspot HP.
- [ ] Sambungkan laptop ke hotspot HP.
- [ ] Sambungkan ESP8266 ke hotspot yang sama.

> [!IMPORTANT]
> HP dan laptop harus berada di jaringan yang sama.
> Jika berbeda jaringan, aplikasi tidak bisa menghubungi server lokal.

## 3. Buka halaman System

- [ ] Buka aplikasi SmartGarden.
- [ ] Tekan tab `System`.
- [ ] Cari bagian `Gunakan Server Lokal`.

> 📸 Screenshot yang perlu ditambahkan:
> - Pengaturan Server Lokal pada APK

## 4. Aktifkan Server Lokal

- [ ] Aktifkan switch `Gunakan Server Lokal`.
- [ ] Isi IP laptop.

Contoh:

```text
192.168.1.10
```

> [!IMPORTANT]
> Isi IP saja.
> Jangan isi URL penuh.
>
> Benar: `192.168.1.10`
>
> Salah: `http://192.168.1.10:3000/api`

Aplikasi akan otomatis membuat URL:

```text
http://192.168.1.10:3000/api
```

## 5. Test koneksi

- [ ] Tekan tombol `Simpan dan test server`.

Hasil yang diharapkan:

- Status menjadi terhubung.
- Muncul pesan server lokal terhubung.
- Dashboard bisa mengambil data dari backend.

Jika gagal:

- Cek backend masih berjalan.
- Cek IP laptop benar.
- Cek HP dan laptop satu WiFi.
- Cek firewall Windows.

## 6. Cara membaca status Online atau Offline

Status koneksi ada di dashboard dan halaman system.

Arti status:

| Status | Arti |
| --- | --- |
| Terhubung | Aplikasi bisa mengakses server lokal |
| Terputus | Aplikasi tidak bisa mengakses server lokal |

Jika status Offline:

- [ ] Tekan `Hubungkan ulang`.
- [ ] Test `/api/health` dari browser HP.
- [ ] Cek IP laptop lagi.

## 7. Menggunakan Mode Demo

Mode Demo berguna jika server belum siap.

Cara memakai:

- [ ] Buka tab `System`.
- [ ] Matikan switch `Gunakan Server Lokal`.
- [ ] Buka tab `Insights`.
- [ ] Tekan `Perbarui data`.

Mode Demo memakai data simulasi.

> [!TIP]
> Gunakan Mode Demo untuk presentasi UI.
> Gunakan Server Lokal untuk test sensor dan pompa sungguhan.

## Checklist Android

- [ ] APK berhasil diinstall.
- [ ] HP satu jaringan dengan laptop.
- [ ] IP laptop sudah diketahui.
- [ ] Server Lokal berhasil diaktifkan.
- [ ] Test koneksi berhasil.
- [ ] Mode Demo tetap bisa digunakan.

## Lanjut

Jika aplikasi sudah bisa test server, lanjut ke:

[04 - Setup ESP8266](04_SETUP_ESP8266.md)

[Beranda](../README.md) |
[1 Persiapan](01_PERSIAPAN.md) |
[2 Server Lokal](02_INSTALASI_SERVER_LOKAL.md) |
[3 Android](03_SETUP_APLIKASI_ANDROID.md) |
[4 ESP8266](04_SETUP_ESP8266.md) |
[5 Wiring](05_WIRING_RANGKAIAN.md) |
[6 Penggunaan](06_CARA_PENGGUNAAN.md) |
[7 Troubleshooting](07_TROUBLESHOOTING.md) |
[8 Checklist](08_CHECKLIST_CLIENT.md)
