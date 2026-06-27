# Wiring Guide SmartGarden ESP8266

## Pin default firmware

| Komponen | NodeMCU ESP8266 | Catatan |
| --- | --- | --- |
| Soil Moisture Sensor V2.0 AO | A0 | Gunakan output analog |
| Soil Moisture VCC | 3V3 | Jangan lebih dari batas input sensor/board |
| Soil Moisture GND | GND | Ground bersama |
| DHT11 DATA | D4 | Tambahkan pull-up bila modul tidak punya |
| DHT11 VCC | 3V3 | Sesuai modul |
| DHT11 GND | GND | Ground bersama |
| Relay IN | D5 | Default firmware active LOW |
| Relay VCC | 5V/VIN | Sesuaikan modul relay |
| Relay GND | GND | Ground bersama |
| LCD I2C SDA | D1 | I2C |
| LCD I2C SCL | D2 | I2C |
| LED status opsional | D6 | Dengan resistor |
| Buzzer opsional | D7 | Opsional |

## Pompa DC

Gunakan relay sebagai saklar untuk jalur pompa. Jangan memberi beban pompa langsung dari pin ESP8266.

Contoh jalur:

```text
Power + pompa -> COM relay
NO relay -> Pompa +
Pompa - -> Power -
GND power -> GND ESP bila memakai modul relay yang butuh common ground
```

## Catatan relay aktif terbalik

Jika pompa menyala saat seharusnya mati, ubah di `config.h`:

```cpp
#define RELAY_ACTIVE_LOW false
```

## Kalibrasi soil sensor

Default:

```cpp
#define SOIL_RAW_DRY 850
#define SOIL_RAW_WET 350
```

Kalibrasi:

1. Baca nilai sensor di udara/tanah kering sebagai `SOIL_RAW_DRY`.
2. Baca nilai sensor di tanah basah sebagai `SOIL_RAW_WET`.
3. Update `config.h`.
