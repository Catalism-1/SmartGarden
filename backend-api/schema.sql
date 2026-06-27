CREATE TABLE IF NOT EXISTS devices (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  device_id VARCHAR(64) NOT NULL,
  name VARCHAR(100) NOT NULL,
  is_connected BOOLEAN NOT NULL DEFAULT FALSE,
  last_seen_at TIMESTAMP NULL DEFAULT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_devices_device_id (device_id)
);

CREATE TABLE IF NOT EXISTS sensor_readings (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  device_id BIGINT UNSIGNED NOT NULL,
  soil_moisture_percent DECIMAL(5,2) NOT NULL,
  soil_raw INT NULL,
  temperature_c DECIMAL(5,2) NOT NULL,
  air_humidity_percent DECIMAL(5,2) NOT NULL,
  pump_state ENUM('on', 'off') NOT NULL DEFAULT 'off',
  is_connected BOOLEAN NOT NULL DEFAULT TRUE,
  read_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_sensor_device_read_at (device_id, read_at),
  CONSTRAINT fk_sensor_readings_device
    FOREIGN KEY (device_id) REFERENCES devices (id)
    ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS garden_settings (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  device_id BIGINT UNSIGNED NOT NULL,
  mode ENUM('automatic', 'manual') NOT NULL DEFAULT 'automatic',
  start_threshold INT NOT NULL DEFAULT 40,
  stop_threshold INT NOT NULL DEFAULT 55,
  max_pump_duration_seconds INT NOT NULL DEFAULT 15,
  cooldown_seconds INT NOT NULL DEFAULT 60,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_garden_settings_device (device_id),
  CONSTRAINT fk_garden_settings_device
    FOREIGN KEY (device_id) REFERENCES devices (id)
    ON DELETE CASCADE,
  CONSTRAINT chk_garden_thresholds CHECK (
    start_threshold >= 0
    AND start_threshold <= 100
    AND stop_threshold >= 0
    AND stop_threshold <= 100
    AND start_threshold < stop_threshold
  ),
  CONSTRAINT chk_garden_durations CHECK (
    max_pump_duration_seconds > 0
    AND cooldown_seconds >= 0
  )
);

CREATE TABLE IF NOT EXISTS pump_commands (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  device_id BIGINT UNSIGNED NOT NULL,
  command_type ENUM(
    'pump_on',
    'pump_off',
    'manual_watering',
    'set_mode',
    'set_thresholds',
    'set_schedule'
  ) NOT NULL,
  status ENUM('pending', 'sent', 'acked', 'failed') NOT NULL DEFAULT 'pending',
  payload JSON NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  sent_at TIMESTAMP NULL DEFAULT NULL,
  acknowledged_at TIMESTAMP NULL DEFAULT NULL,
  result_message VARCHAR(255) NULL,
  PRIMARY KEY (id),
  KEY idx_pump_commands_device_status (device_id, status, created_at),
  CONSTRAINT fk_pump_commands_device
    FOREIGN KEY (device_id) REFERENCES devices (id)
    ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS watering_logs (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  device_id BIGINT UNSIGNED NOT NULL,
  command_id BIGINT UNSIGNED NULL,
  source ENUM('manual', 'automatic', 'schedule') NOT NULL,
  started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  duration_seconds INT NOT NULL DEFAULT 0,
  result_status ENUM('pending', 'success', 'failed') NOT NULL DEFAULT 'success',
  soil_moisture_percent DECIMAL(5,2) NULL,
  message VARCHAR(255) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_watering_logs_device_started_at (device_id, started_at),
  CONSTRAINT fk_watering_logs_device
    FOREIGN KEY (device_id) REFERENCES devices (id)
    ON DELETE CASCADE,
  CONSTRAINT fk_watering_logs_command
    FOREIGN KEY (command_id) REFERENCES pump_commands (id)
    ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS watering_schedules (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  device_id BIGINT UNSIGNED NOT NULL,
  time_of_day TIME NOT NULL,
  duration_seconds INT NOT NULL DEFAULT 10,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  last_executed_at TIMESTAMP NULL DEFAULT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_watering_schedules_device_active_time (device_id, is_active, time_of_day),
  CONSTRAINT fk_watering_schedules_device
    FOREIGN KEY (device_id) REFERENCES devices (id)
    ON DELETE CASCADE,
  CONSTRAINT chk_watering_schedule_duration CHECK (duration_seconds > 0)
);

INSERT INTO devices (device_id, name)
VALUES ('smartgarden-01', 'SmartGarden 01')
ON DUPLICATE KEY UPDATE device_id = VALUES(device_id);

INSERT INTO garden_settings (
  device_id,
  mode,
  start_threshold,
  stop_threshold,
  max_pump_duration_seconds,
  cooldown_seconds
)
SELECT id, 'automatic', 40, 55, 15, 60
FROM devices
WHERE device_id = 'smartgarden-01'
ON DUPLICATE KEY UPDATE device_id = VALUES(device_id);
