require("dotenv").config();

const cors = require("cors");
const express = require("express");
const pool = require("./db");

const app = express();
const PORT = Number(process.env.PORT || 3000);
const HOST = process.env.HOST || "0.0.0.0";
const DEFAULT_DEVICE_ID = process.env.DEFAULT_DEVICE_ID || "smartgarden-01";

app.use(cors());
app.use(express.json({ limit: "1mb" }));

function ok(res, message, data = null, status = 200) {
  return res.status(status).json({ success: true, message, data });
}

function fail(res, status, message, data = null) {
  return res.status(status).json({ success: false, message, data });
}

function asDeviceId(value) {
  return String(value || DEFAULT_DEVICE_ID).trim();
}

function asMode(value) {
  const normalized = String(value || "").toLowerCase();
  if (normalized === "auto") return "automatic";
  if (normalized === "automatic" || normalized === "manual") return normalized;
  return null;
}

function asPumpState(value) {
  const normalized = String(value || "").toLowerCase();
  if (normalized === "on" || normalized === "off") return normalized;
  return null;
}

function clampInteger(value, min, max, fallback) {
  const number = Number(value);
  if (!Number.isFinite(number)) return fallback;
  return Math.min(Math.max(Math.trunc(number), min), max);
}

async function ensureDevice(deviceId) {
  await pool.execute(
    `
      INSERT INTO devices (device_id, name)
      VALUES (?, ?)
      ON DUPLICATE KEY UPDATE device_id = VALUES(device_id)
    `,
    [deviceId, deviceId],
  );
  const [rows] = await pool.execute("SELECT * FROM devices WHERE device_id = ? LIMIT 1", [deviceId]);
  return rows[0];
}

async function ensureSettings(deviceId) {
  const device = await ensureDevice(deviceId);
  await pool.execute(
    `
      INSERT INTO garden_settings (
        device_id,
        mode,
        start_threshold,
        stop_threshold,
        max_pump_duration_seconds,
        cooldown_seconds
      )
      VALUES (?, 'automatic', 40, 55, 15, 60)
      ON DUPLICATE KEY UPDATE device_id = VALUES(device_id)
    `,
    [device.id],
  );
  const [rows] = await pool.execute("SELECT * FROM garden_settings WHERE device_id = ? LIMIT 1", [device.id]);
  return { device, settings: rows[0] };
}

async function latestReading(deviceId) {
  const [rows] = await pool.execute(
    `
      SELECT sr.*
      FROM sensor_readings sr
      JOIN devices d ON d.id = sr.device_id
      WHERE d.device_id = ?
      ORDER BY sr.read_at DESC, sr.id DESC
      LIMIT 1
    `,
    [deviceId],
  );
  return rows[0] || null;
}

async function hasPendingCommand(devicePk, commandType) {
  const [rows] = await pool.execute(
    `
      SELECT id
      FROM pump_commands
      WHERE device_id = ?
        AND command_type = ?
        AND status IN ('pending', 'sent')
      LIMIT 1
    `,
    [devicePk, commandType],
  );
  return rows.length > 0;
}

async function createCommand(devicePk, commandType, payload) {
  const [result] = await pool.execute(
    "INSERT INTO pump_commands (device_id, command_type, payload) VALUES (?, ?, ?)",
    [devicePk, commandType, JSON.stringify(payload || {})],
  );
  const [rows] = await pool.execute("SELECT * FROM pump_commands WHERE id = ?", [result.insertId]);
  return rows[0];
}

function normalizeCommand(row) {
  if (!row) return null;
  return {
    id: row.id,
    commandType: row.command_type,
    status: row.status,
    payload: typeof row.payload === "string" ? JSON.parse(row.payload) : row.payload,
    createdAt: row.created_at,
    sentAt: row.sent_at,
    acknowledgedAt: row.acknowledged_at,
    resultMessage: row.result_message,
  };
}

async function maybeCreateAutomaticCommand(device, settings, telemetry) {
  if (settings.mode !== "automatic") return null;

  const soil = Number(telemetry.soil_moisture_percent);
  if (!Number.isFinite(soil)) return null;

  if (soil >= settings.stop_threshold && telemetry.pump_state === "on") {
    if (await hasPendingCommand(device.id, "pump_off")) return null;
    return createCommand(device.id, "pump_off", {
      source: "automatic",
      reason: "soil moisture reached stop threshold",
      soilMoisturePercent: soil,
      stopThreshold: settings.stop_threshold,
    });
  }

  if (soil >= settings.start_threshold || telemetry.pump_state === "on") return null;

  const [cooldownRows] = await pool.execute(
    `
      SELECT created_at
      FROM watering_logs
      WHERE device_id = ?
        AND source = 'automatic'
      ORDER BY created_at DESC
      LIMIT 1
    `,
    [device.id],
  );
  const lastAutomaticLog = cooldownRows[0]?.created_at ? new Date(cooldownRows[0].created_at).getTime() : 0;
  const cooldownMs = Number(settings.cooldown_seconds) * 1000;
  if (lastAutomaticLog && Date.now() - lastAutomaticLog < cooldownMs) return null;

  if (await hasPendingCommand(device.id, "manual_watering")) return null;

  return createCommand(device.id, "manual_watering", {
    source: "automatic",
    durationSeconds: settings.max_pump_duration_seconds,
    reason: "soil moisture below start threshold",
    soilMoisturePercent: soil,
    startThreshold: settings.start_threshold,
  });
}

app.get("/api/health", async (_req, res) => {
  try {
    await pool.query("SELECT 1");
    return ok(res, "SmartGarden API is healthy", {
      service: "smartgarden-backend-api",
      database: "connected",
      timestamp: new Date().toISOString(),
    });
  } catch (error) {
    return fail(res, 503, "Database is not reachable", { error: error.message });
  }
});

app.post("/api/iot/telemetry", async (req, res) => {
  const body = req.body || {};
  const deviceId = asDeviceId(body.deviceId || body.device_id);
  const pumpState = asPumpState(body.pumpState || body.pump_state);
  const soilMoisture = Number(body.soilMoisturePercent ?? body.soil_moisture_percent);
  const temperature = Number(body.temperatureC ?? body.temperature_c);
  const airHumidity = Number(body.airHumidityPercent ?? body.air_humidity_percent);

  if (!Number.isFinite(soilMoisture) || soilMoisture < 0 || soilMoisture > 100) {
    return fail(res, 400, "soil moisture percent must be a number between 0 and 100");
  }
  if (!Number.isFinite(temperature)) {
    return fail(res, 400, "temperature celsius must be a number");
  }
  if (!Number.isFinite(airHumidity) || airHumidity < 0 || airHumidity > 100) {
    return fail(res, 400, "air humidity percent must be a number between 0 and 100");
  }
  if (!pumpState) return fail(res, 400, "pump state must be 'on' or 'off'");

  try {
    const { device, settings } = await ensureSettings(deviceId);
    const readAt = body.readAt || body.read_at || new Date();
    const soilRaw = body.soilRaw ?? body.soil_raw ?? null;
    await pool.execute(
      `
        INSERT INTO sensor_readings (
          device_id,
          soil_moisture_percent,
          soil_raw,
          temperature_c,
          air_humidity_percent,
          pump_state,
          is_connected,
          read_at
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
      `,
      [
        device.id,
        soilMoisture,
        soilRaw === null ? null : Number(soilRaw),
        temperature,
        airHumidity,
        pumpState,
        body.isConnected ?? body.is_connected ?? true,
        readAt,
      ],
    );
    await pool.execute(
      "UPDATE devices SET is_connected = TRUE, last_seen_at = CURRENT_TIMESTAMP WHERE id = ?",
      [device.id],
    );

    const automaticCommand = await maybeCreateAutomaticCommand(device, settings, {
      soil_moisture_percent: soilMoisture,
      pump_state: pumpState,
    });

    return ok(res, "Telemetry stored", {
      deviceId,
      automaticCommand: normalizeCommand(automaticCommand),
    }, 201);
  } catch (error) {
    return fail(res, 500, "Failed to store telemetry", { error: error.message });
  }
});

app.get("/api/iot/config", async (req, res) => {
  const deviceId = asDeviceId(req.query.deviceId || req.query.device_id);
  try {
    const { device, settings } = await ensureSettings(deviceId);
    const [schedules] = await pool.execute(
      `
        SELECT id, time_of_day AS timeOfDay, duration_seconds AS durationSeconds, is_active AS isActive
        FROM watering_schedules
        WHERE device_id = ?
        ORDER BY time_of_day ASC
      `,
      [device.id],
    );
    return ok(res, "Configuration loaded", {
      deviceId,
      mode: settings.mode,
      startThreshold: settings.start_threshold,
      stopThreshold: settings.stop_threshold,
      maxPumpDurationSeconds: settings.max_pump_duration_seconds,
      cooldownSeconds: settings.cooldown_seconds,
      schedules,
    });
  } catch (error) {
    return fail(res, 500, "Failed to load configuration", { error: error.message });
  }
});

app.get("/api/iot/commands/next", async (req, res) => {
  const deviceId = asDeviceId(req.query.deviceId || req.query.device_id);
  try {
    const { device } = await ensureSettings(deviceId);
    const connection = await pool.getConnection();
    try {
      await connection.beginTransaction();
      const [rows] = await connection.execute(
        `
          SELECT *
          FROM pump_commands
          WHERE device_id = ?
            AND status = 'pending'
          ORDER BY created_at ASC, id ASC
          LIMIT 1
          FOR UPDATE
        `,
        [device.id],
      );
      if (rows.length === 0) {
        await connection.commit();
        return ok(res, "No pending command", null);
      }
      const command = rows[0];
      await connection.execute(
        "UPDATE pump_commands SET status = 'sent', sent_at = CURRENT_TIMESTAMP WHERE id = ?",
        [command.id],
      );
      await connection.commit();
      command.status = "sent";
      command.sent_at = new Date();
      return ok(res, "Command loaded", normalizeCommand(command));
    } catch (error) {
      await connection.rollback();
      throw error;
    } finally {
      connection.release();
    }
  } catch (error) {
    return fail(res, 500, "Failed to load next command", { error: error.message });
  }
});

app.post("/api/iot/commands/ack", async (req, res) => {
  const body = req.body || {};
  const commandId = Number(body.commandId || body.command_id);
  const status = String(body.status || "success").toLowerCase() === "failed" ? "failed" : "acked";
  const deviceId = asDeviceId(body.deviceId || body.device_id);

  if (!Number.isInteger(commandId) || commandId <= 0) return fail(res, 400, "command id is required");

  try {
    const { device } = await ensureSettings(deviceId);
    const [rows] = await pool.execute(
      "SELECT * FROM pump_commands WHERE id = ? AND device_id = ? LIMIT 1",
      [commandId, device.id],
    );
    if (rows.length === 0) return fail(res, 404, "Command not found");

    const command = rows[0];
    const payload = typeof command.payload === "string" ? JSON.parse(command.payload) : command.payload || {};
    await pool.execute(
      `
        UPDATE pump_commands
        SET status = ?,
            acknowledged_at = CURRENT_TIMESTAMP,
            result_message = ?
        WHERE id = ?
      `,
      [status, body.message || null, commandId],
    );

    const source = payload.source || (command.command_type === "manual_watering" ? "manual" : null);
    if (["manual", "automatic", "schedule"].includes(source)) {
      const durationSeconds = clampInteger(
        body.durationSeconds ?? body.duration_seconds ?? payload.durationSeconds,
        0,
        3600,
        0,
      );
      await pool.execute(
        `
          INSERT INTO watering_logs (
            device_id,
            command_id,
            source,
            started_at,
            duration_seconds,
            result_status,
            soil_moisture_percent,
            message
          )
          VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        `,
        [
          device.id,
          commandId,
          source,
          body.executedAt || body.executed_at || new Date(),
          durationSeconds,
          status === "acked" ? "success" : "failed",
          body.soilMoisturePercent ?? body.soil_moisture_percent ?? payload.soilMoisturePercent ?? null,
          body.message || null,
        ],
      );
    }

    return ok(res, "Command acknowledged", { commandId, status });
  } catch (error) {
    return fail(res, 500, "Failed to acknowledge command", { error: error.message });
  }
});

app.get("/api/garden/dashboard", async (req, res) => {
  const deviceId = asDeviceId(req.query.deviceId || req.query.device_id);
  try {
    const { device, settings } = await ensureSettings(deviceId);
    const [pendingRows] = await pool.execute(
      "SELECT COUNT(*) AS count FROM pump_commands WHERE device_id = ? AND status IN ('pending', 'sent')",
      [device.id],
    );
    return ok(res, "Dashboard loaded", {
      device: {
        id: device.device_id,
        name: device.name,
        isConnected: Boolean(device.is_connected),
        lastSeenAt: device.last_seen_at,
      },
      latestReading: await latestReading(deviceId),
      settings: {
        mode: settings.mode,
        startThreshold: settings.start_threshold,
        stopThreshold: settings.stop_threshold,
        maxPumpDurationSeconds: settings.max_pump_duration_seconds,
        cooldownSeconds: settings.cooldown_seconds,
      },
      pendingCommands: pendingRows[0].count,
    });
  } catch (error) {
    return fail(res, 500, "Failed to load dashboard", { error: error.message });
  }
});

app.get("/api/garden/history/sensors", async (req, res) => {
  const deviceId = asDeviceId(req.query.deviceId || req.query.device_id);
  const limit = clampInteger(req.query.limit, 1, 500, 100);
  try {
    await ensureSettings(deviceId);
    const [rows] = await pool.execute(
      `
        SELECT sr.*
        FROM sensor_readings sr
        JOIN devices d ON d.id = sr.device_id
        WHERE d.device_id = ?
        ORDER BY sr.read_at DESC, sr.id DESC
        LIMIT ${limit}
      `,
      [deviceId],
    );
    return ok(res, "Sensor history loaded", rows);
  } catch (error) {
    return fail(res, 500, "Failed to load sensor history", { error: error.message });
  }
});

app.get("/api/garden/history/watering", async (req, res) => {
  const deviceId = asDeviceId(req.query.deviceId || req.query.device_id);
  const limit = clampInteger(req.query.limit, 1, 500, 100);
  try {
    await ensureSettings(deviceId);
    const [rows] = await pool.execute(
      `
        SELECT wl.*
        FROM watering_logs wl
        JOIN devices d ON d.id = wl.device_id
        WHERE d.device_id = ?
        ORDER BY wl.started_at DESC, wl.id DESC
        LIMIT ${limit}
      `,
      [deviceId],
    );
    return ok(res, "Watering history loaded", rows);
  } catch (error) {
    return fail(res, 500, "Failed to load watering history", { error: error.message });
  }
});

app.post("/api/garden/mode", async (req, res) => {
  const deviceId = asDeviceId(req.body?.deviceId || req.body?.device_id);
  const mode = asMode(req.body?.mode);
  if (!mode) return fail(res, 400, "mode must be automatic or manual");

  try {
    const { device } = await ensureSettings(deviceId);
    await pool.execute("UPDATE garden_settings SET mode = ? WHERE device_id = ?", [mode, device.id]);
    const command = await createCommand(device.id, "set_mode", { mode });
    return ok(res, "Garden mode updated", { mode, command: normalizeCommand(command) });
  } catch (error) {
    return fail(res, 500, "Failed to update garden mode", { error: error.message });
  }
});

app.post("/api/garden/pump/manual", async (req, res) => {
  const deviceId = asDeviceId(req.body?.deviceId || req.body?.device_id);
  const durationSeconds = clampInteger(req.body?.durationSeconds ?? req.body?.duration_seconds, 1, 3600, 15);
  try {
    const { device, settings } = await ensureSettings(deviceId);
    const safeDuration = Math.min(durationSeconds, settings.max_pump_duration_seconds);
    const command = await createCommand(device.id, "manual_watering", {
      source: "manual",
      durationSeconds: safeDuration,
      requestedDurationSeconds: durationSeconds,
    });
    return ok(res, "Manual pump command created", normalizeCommand(command), 201);
  } catch (error) {
    return fail(res, 500, "Failed to create manual pump command", { error: error.message });
  }
});

app.post("/api/garden/settings/thresholds", async (req, res) => {
  const deviceId = asDeviceId(req.body?.deviceId || req.body?.device_id);
  const startThreshold = clampInteger(req.body?.startThreshold ?? req.body?.start_threshold, 1, 99, 40);
  const stopThreshold = clampInteger(req.body?.stopThreshold ?? req.body?.stop_threshold, startThreshold + 1, 100, 55);
  const maxPumpDurationSeconds = clampInteger(
    req.body?.maxPumpDurationSeconds ?? req.body?.max_pump_duration_seconds,
    1,
    3600,
    15,
  );
  const cooldownSeconds = clampInteger(req.body?.cooldownSeconds ?? req.body?.cooldown_seconds, 0, 86400, 60);

  try {
    const { device } = await ensureSettings(deviceId);
    await pool.execute(
      `
        UPDATE garden_settings
        SET start_threshold = ?,
            stop_threshold = ?,
            max_pump_duration_seconds = ?,
            cooldown_seconds = ?
        WHERE device_id = ?
      `,
      [startThreshold, stopThreshold, maxPumpDurationSeconds, cooldownSeconds, device.id],
    );
    const command = await createCommand(device.id, "set_thresholds", {
      startThreshold,
      stopThreshold,
      maxPumpDurationSeconds,
      cooldownSeconds,
    });
    return ok(res, "Threshold settings updated", {
      startThreshold,
      stopThreshold,
      maxPumpDurationSeconds,
      cooldownSeconds,
      command: normalizeCommand(command),
    });
  } catch (error) {
    return fail(res, 500, "Failed to update threshold settings", { error: error.message });
  }
});

app.post("/api/garden/schedules", async (req, res) => {
  const deviceId = asDeviceId(req.body?.deviceId || req.body?.device_id);
  const timeOfDay = String(req.body?.timeOfDay || req.body?.time_of_day || "").trim();
  const durationSeconds = clampInteger(req.body?.durationSeconds ?? req.body?.duration_seconds, 1, 3600, 10);
  const isActive = req.body?.isActive ?? req.body?.is_active ?? true;

  if (!/^([01]\d|2[0-3]):[0-5]\d(:[0-5]\d)?$/.test(timeOfDay)) {
    return fail(res, 400, "timeOfDay must use HH:mm or HH:mm:ss format");
  }

  try {
    const { device } = await ensureSettings(deviceId);
    const [result] = await pool.execute(
      "INSERT INTO watering_schedules (device_id, time_of_day, duration_seconds, is_active) VALUES (?, ?, ?, ?)",
      [device.id, timeOfDay.length === 5 ? `${timeOfDay}:00` : timeOfDay, durationSeconds, Boolean(isActive)],
    );
    const command = await createCommand(device.id, "set_schedule", {
      scheduleId: result.insertId,
      action: "create",
      timeOfDay,
      durationSeconds,
      isActive: Boolean(isActive),
    });
    return ok(res, "Schedule created", {
      id: result.insertId,
      command: normalizeCommand(command),
    }, 201);
  } catch (error) {
    return fail(res, 500, "Failed to create schedule", { error: error.message });
  }
});

app.get("/api/garden/schedules", async (req, res) => {
  const deviceId = asDeviceId(req.query.deviceId || req.query.device_id);
  try {
    const { device } = await ensureSettings(deviceId);
    const [rows] = await pool.execute(
      `
        SELECT id, time_of_day AS timeOfDay, duration_seconds AS durationSeconds, is_active AS isActive,
               last_executed_at AS lastExecutedAt, created_at AS createdAt, updated_at AS updatedAt
        FROM watering_schedules
        WHERE device_id = ?
        ORDER BY time_of_day ASC
      `,
      [device.id],
    );
    return ok(res, "Schedules loaded", rows);
  } catch (error) {
    return fail(res, 500, "Failed to load schedules", { error: error.message });
  }
});

app.patch("/api/garden/schedules/:id", async (req, res) => {
  const deviceId = asDeviceId(req.body?.deviceId || req.body?.device_id || req.query.deviceId || req.query.device_id);
  const scheduleId = Number(req.params.id);
  if (!Number.isInteger(scheduleId) || scheduleId <= 0) return fail(res, 400, "schedule id is invalid");

  const updates = [];
  const params = [];
  const payload = { scheduleId, action: "update" };

  if (req.body?.timeOfDay || req.body?.time_of_day) {
    const timeOfDay = String(req.body.timeOfDay || req.body.time_of_day).trim();
    if (!/^([01]\d|2[0-3]):[0-5]\d(:[0-5]\d)?$/.test(timeOfDay)) {
      return fail(res, 400, "timeOfDay must use HH:mm or HH:mm:ss format");
    }
    updates.push("time_of_day = ?");
    params.push(timeOfDay.length === 5 ? `${timeOfDay}:00` : timeOfDay);
    payload.timeOfDay = timeOfDay;
  }
  if (req.body?.durationSeconds !== undefined || req.body?.duration_seconds !== undefined) {
    const durationSeconds = clampInteger(req.body.durationSeconds ?? req.body.duration_seconds, 1, 3600, 10);
    updates.push("duration_seconds = ?");
    params.push(durationSeconds);
    payload.durationSeconds = durationSeconds;
  }
  if (req.body?.isActive !== undefined || req.body?.is_active !== undefined) {
    const isActive = Boolean(req.body.isActive ?? req.body.is_active);
    updates.push("is_active = ?");
    params.push(isActive);
    payload.isActive = isActive;
  }
  if (updates.length === 0) return fail(res, 400, "No schedule fields to update");

  try {
    const { device } = await ensureSettings(deviceId);
    params.push(scheduleId, device.id);
    const [result] = await pool.execute(
      `UPDATE watering_schedules SET ${updates.join(", ")} WHERE id = ? AND device_id = ?`,
      params,
    );
    if (result.affectedRows === 0) return fail(res, 404, "Schedule not found");
    const command = await createCommand(device.id, "set_schedule", payload);
    return ok(res, "Schedule updated", { id: scheduleId, command: normalizeCommand(command) });
  } catch (error) {
    return fail(res, 500, "Failed to update schedule", { error: error.message });
  }
});

app.use((req, res) => fail(res, 404, "Endpoint not found", { path: req.path }));

if (require.main === module) {
  app.listen(PORT, HOST, () => {
    console.log(`SmartGarden API listening on http://${HOST}:${PORT}`);
  });
}

module.exports = app;
