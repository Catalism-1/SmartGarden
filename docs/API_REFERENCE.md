# API Reference SmartGarden Local

Base URL:

```text
http://IP_LAPTOP:3000/api
```

Response standar:

```json
{
  "success": true,
  "message": "OK",
  "data": {}
}
```

## Health

```text
GET /api/health
```

## ESP telemetry

```text
POST /api/iot/telemetry
```

```json
{
  "deviceId": "smartgarden-01",
  "soilMoisturePercent": 38,
  "soilRaw": 730,
  "temperatureC": 28.5,
  "airHumidityPercent": 70,
  "pumpState": "off",
  "isConnected": true
}
```

## ESP config

```text
GET /api/iot/config?deviceId=smartgarden-01
```

## ESP command polling

```text
GET /api/iot/commands/next?deviceId=smartgarden-01
```

## ESP command ack

```text
POST /api/iot/commands/ack
```

```json
{
  "deviceId": "smartgarden-01",
  "commandId": 1,
  "status": "success",
  "durationSeconds": 10,
  "soilMoisturePercent": 38,
  "message": "Executed"
}
```

## Dashboard

```text
GET /api/garden/dashboard?deviceId=smartgarden-01
```

## Sensor history

```text
GET /api/garden/history/sensors?deviceId=smartgarden-01&limit=100
```

## Watering history

```text
GET /api/garden/history/watering?deviceId=smartgarden-01&limit=100
```

## Mode

```text
POST /api/garden/mode
```

```json
{
  "deviceId": "smartgarden-01",
  "mode": "automatic"
}
```

## Manual pump

Manual watering duration:

```text
POST /api/garden/pump/manual
```

```json
{
  "deviceId": "smartgarden-01",
  "durationSeconds": 10
}
```

Manual pump ON/OFF command:

```json
{
  "deviceId": "smartgarden-01",
  "state": "on"
}
```

atau:

```json
{
  "deviceId": "smartgarden-01",
  "state": "off"
}
```

## Thresholds and safety

```text
POST /api/garden/settings/thresholds
```

```json
{
  "deviceId": "smartgarden-01",
  "startThreshold": 40,
  "stopThreshold": 55,
  "maxPumpDurationSeconds": 15,
  "cooldownSeconds": 60
}
```

## Schedules

Create:

```text
POST /api/garden/schedules
```

```json
{
  "deviceId": "smartgarden-01",
  "timeOfDay": "06:00",
  "durationSeconds": 10,
  "isActive": true
}
```

List:

```text
GET /api/garden/schedules?deviceId=smartgarden-01
```

Update:

```text
PATCH /api/garden/schedules/:id
```
