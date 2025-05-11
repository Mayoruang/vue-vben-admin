# System Verification Scripts

This directory contains scripts to verify the functionality of the drone telemetry system, particularly focusing on data storage in InfluxDB.

## Available Scripts

### 1. `verify_influxdb.py`

Checks if data is being stored in InfluxDB and displays any available data.

```bash
python scripts/verify_influxdb.py
```

### 2. `verify_drone_telemetry.py`

Specifically queries for drone telemetry data in InfluxDB and provides detailed output of the telemetry fields.

```bash
python scripts/verify_drone_telemetry.py
```

### 3. `influxdb_direct_test.py`

Tests direct connection to InfluxDB by writing test data points and verifying they are stored correctly.

```bash
python scripts/influxdb_direct_test.py
```

### 4. `influxdb_drone_test.py`

Directly writes drone telemetry data to InfluxDB using Line Protocol format (the same way the backend does) and verifies storage.

```bash
python scripts/influxdb_drone_test.py
```

### 5. `mqtt_test.py`

Publishes test drone telemetry messages to the MQTT broker, which should be processed by the backend and stored in InfluxDB.

```bash
python scripts/mqtt_test.py
```

## Verification Process

To verify the full data flow, follow these steps:

1. Start all services using the drone9.sh script:
   ```bash
   ./drone9.sh start
   ```

2. Start the backend:
   ```bash
   cd backend && ./mvnw spring-boot:run
   ```

3. Verify system health:
   ```bash
   curl http://localhost:8080/api/health
   ```

4. Send test MQTT telemetry data:
   ```bash
   python scripts/mqtt_test.py
   ```

5. Verify data is stored in InfluxDB:
   ```bash
   python scripts/verify_drone_telemetry.py
   ```

## Troubleshooting

If MQTT data is not appearing in InfluxDB:

1. Ensure all services are running:
   ```bash
   ./drone9.sh status
   ```

2. Check if direct InfluxDB writes work:
   ```bash
   python scripts/influxdb_drone_test.py
   ```

3. Start the backend with debug logging to see detailed MQTT and InfluxDB activities:
   ```bash
   cd backend && ./mvnw spring-boot:run -Dlogging.level.com.huang=DEBUG
   ```

4. Ensure the backend's TimeseriesService is using Line Protocol format for writing data, as this has been found to be more reliable than the Point API.

## Important Notes

- The system uses the following measurement names in InfluxDB:
  - `drone_telemetry`: Contains drone telemetry data
  - `system_check`: Contains system health check data

- The key fields for drone telemetry include:
  - `battery_level`: Battery percentage (0-100)
  - `latitude`, `longitude`, `altitude`: Position data
  - `speed`, `heading`: Movement data
  - And other sensor readings

- Each telemetry data point is tagged with `drone_id` for easy filtering by drone. 