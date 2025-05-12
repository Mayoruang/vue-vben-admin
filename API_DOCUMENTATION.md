# Drone Management System API Documentation

This document provides a detailed description of the API endpoints for the Drone Management System.

## Table of Contents

1.  [Health Check](#health-check)
2.  [Drone Registration](#drone-registration)
3.  [Drone Commands](#drone-commands)
4.  [Web Pages](#web-pages)
5.  [WebSockets](#websockets)

---

## 1. Health Check

### GET `/api/health`

Checks the health status of the backend services.

**Response Body:** `application/json`

```json
{
  "status": "UP",
  "services": {
    // Key-value pairs of service names and their statuses
    "database": "UP",
    "mqttBroker": "UP"
  }
}
```

**Fields:**

*   `status` (String): Overall status of the application (e.g., "UP").
*   `services` (Object): Status of individual dependent services.

---

## 2. Drone Registration

### POST `/api/v1/drones/register`

Submits a new drone for registration. The request is processed asynchronously.

**Request Body:** `application/json` (`DroneRegistrationRequestDto`)

```json
{
  "serialNumber": "SN-DRONE-XYZ-123",
  "model": "DJI Mavic 3",
  "notes": "Test drone for development"
}
```

**Fields:**

*   `serialNumber` (String, **required**, 3-50 chars, `^[A-Za-z0-9\\-_]+$`): The drone's unique serial number.
*   `model` (String, **required**, 2-50 chars): The drone's model.
*   `notes` (String, optional, max 1000 chars): Additional notes about the drone.

**Response (202 Accepted):** `application/json` (`DroneRegistrationResponseDto`)

```json
{
  "requestId": "a1b2c3d4-e5f6-7890-1234-567890abcdef",
  "message": "Drone registration request received and is pending approval. You will be notified via WebSocket.",
  "statusCheckUrl": "/api/v1/drones/registration/a1b2c3d4-e5f6-7890-1234-567890abcdef/status"
}
```

**Fields:**

*   `requestId` (UUID): The unique ID assigned to this registration request.
*   `message` (String): A message indicating the request has been received.
*   `statusCheckUrl` (String): The URL to poll for the status of this registration request.

---

### GET `/api/v1/drones/registration/{requestId}/status`

Retrieves the current status of a drone registration request.

**Path Parameters:**

*   `requestId` (UUID): The ID of the registration request.

**Response (200 OK):** `application/json` (`RegistrationStatusResponseDto`)

```json
{
  "requestId": "a1b2c3d4-e5f6-7890-1234-567890abcdef",
  "serialNumber": "SN-DRONE-XYZ-123",
  "model": "DJI Mavic 3",
  "status": "PENDING", // PENDING, APPROVED, REJECTED
  "requestedAt": "2023-10-27T10:30:00Z",
  "processedAt": null, // Populated when status is APPROVED or REJECTED
  "droneId": null, // Populated if status is APPROVED
  "message": "Your registration request is currently pending approval.",
  "mqttCredentials": null // Populated if status is APPROVED
}
```

**If Approved:**

```json
{
  "requestId": "a1b2c3d4-e5f6-7890-1234-567890abcdef",
  "serialNumber": "SN-DRONE-XYZ-123",
  "model": "DJI Mavic 3",
  "status": "APPROVED",
  "requestedAt": "2023-10-27T10:30:00Z",
  "processedAt": "2023-10-27T10:35:00Z",
  "droneId": "b2c3d4e5-f6a7-8901-2345-678901bcdef0",
  "message": "Your registration request has been approved. MQTT credentials are provided.",
  "mqttCredentials": {
    "mqttBrokerUrl": "tcp://localhost:1883",
    "mqttUsername": "drone-b2c3d4e5-f6a7-8901-2345-678901bcdef0",
    "mqttPassword": "someSecurePassword",
    "mqttTopicTelemetry": "drones/b2c3d4e5-f6a7-8901-2345-678901bcdef0/telemetry",
    "mqttTopicCommands": "drones/b2c3d4e5-f6a7-8901-2345-678901bcdef0/commands"
  }
}
```

**Fields:**

*   `requestId` (UUID): The ID of the registration request.
*   `serialNumber` (String): Drone's serial number.
*   `model` (String): Drone's model.
*   `status` (String Enum): Current status (`PENDING`, `APPROVED`, `REJECTED`).
*   `requestedAt` (ZonedDateTime): Timestamp of when the request was submitted.
*   `processedAt` (ZonedDateTime, optional): Timestamp of when the request was processed.
*   `droneId` (UUID, optional): The assigned Drone ID if approved.
*   `message` (String): A message describing the current status.
*   `mqttCredentials` (`MqttCredentialsDto`, optional): MQTT connection details, provided only if the status is `APPROVED`.
    *   `mqttBrokerUrl` (String): URL of the MQTT broker.
    *   `mqttUsername` (String): Username for MQTT connection.
    *   `mqttPassword` (String): Password for MQTT connection.
    *   `mqttTopicTelemetry` (String): Topic for publishing telemetry data.
    *   `mqttTopicCommands` (String): Topic for receiving commands.

---

### POST `/api/v1/admin/registrations/action`

Allows an administrator to approve or reject a pending drone registration request.

**Request Body:** `application/json` (`AdminActionDto`)

```json
{
  "requestId": "a1b2c3d4-e5f6-7890-1234-567890abcdef",
  "action": "APPROVE", // or "REJECT"
  "rejectionReason": null // Required if action is "REJECT"
}
```

**Fields:**

*   `requestId` (UUID, **required**): The ID of the registration request to act upon.
*   `action` (String Enum, **required**): The action to take (`APPROVE` or `REJECT`).
*   `rejectionReason` (String, optional): Reason for rejection, required if `action` is `REJECT`.

**Response (200 OK):** `application/json` (`AdminActionResponseDto`)

```json
{
  "message": "Registration request a1b2c3d4-e5f6-7890-1234-567890abcdef approved successfully.",
  "requestId": "a1b2c3d4-e5f6-7890-1234-567890abcdef",
  "droneId": "b2c3d4e5-f6a7-8901-2345-678901bcdef0", // Populated if approved
  "action": "APPROVE"
}
```

**Fields:**

*   `message` (String): Result of the action.
*   `requestId` (UUID): The ID of the request that was acted upon.
*   `droneId` (UUID, optional): The assigned Drone ID if the request was approved.
*   `action` (String Enum): The action that was taken (`APPROVE` or `REJECT`).

---

## 3. Drone Commands

These endpoints are used to send commands to already registered and connected drones via MQTT. The backend forwards these commands to the appropriate MQTT topic. The response indicates if the command was successfully published to MQTT, not if the drone executed it.

### POST `/api/drones/{droneId}/commands`

Sends a generic command to a specific drone.

**Path Parameters:**

*   `droneId` (String): The ID of the target drone.

**Request Body:** `application/json` (`DroneCommand`)

```json
{
  "type": "CUSTOM", // See DroneCommand.CommandType for available types
  "parameters": {
    "custom_param1": "value1",
    "custom_param2": 123
  }
}
```

**`DroneCommand.CommandType` Enum Values:**

*   `ARM`: Arm the drone motors.
*   `DISARM`: Disarm the drone motors.
*   `RTL`: Return to launch position.
*   `TAKEOFF`: Take off to a specific altitude.
*   `LAND`: Land at current position.
*   `GOTO`: Go to specific coordinates.
*   `START_MISSION`: Start executing a mission plan.
*   `PAUSE_MISSION`: Pause current mission.
*   `RESUME_MISSION`: Resume paused mission.
*   `CANCEL_MISSION`: Cancel current mission.
*   `TAKE_PHOTO`: Take a photo.
*   `START_RECORDING`: Start video recording.
*   `STOP_RECORDING`: Stop video recording.
*   `CUSTOM`: Custom command type.

**Fields (in Request Body):**

*   `type` (String Enum, **required**): The type of command to send.
*   `parameters` (Map<String, Object>, optional): Key-value pairs for command-specific parameters.
    *   For `TAKEOFF`: `{"altitude": 10.0}` (altitude in meters)
    *   For `GOTO`: `{"latitude": 34.0522, "longitude": -118.2437, "altitude": 20.0}` (latitude, longitude in degrees, altitude in meters)

**Response (200 OK or 503 Service Unavailable):** `application/json`

```json
{
  "success": true, // or false if MQTT publishing failed
  "command": {
    "commandId": "c1d2e3f4-a5b6-7890-1234-567890abcdef",
    "droneId": "b2c3d4e5-f6a7-8901-2345-678901bcdef0", // Set by backend
    "timestamp": "2023-10-27T11:00:00Z",
    "type": "CUSTOM",
    "parameters": {
      "custom_param1": "value1",
      "custom_param2": 123
    }
  }
}
```

**Fields (in Response Body):**

*   `success` (Boolean): `true` if the command was successfully published to MQTT, `false` otherwise.
*   `command` (`DroneCommand`): The command object that was sent.
    *   `commandId` (String UUID): Unique ID for this command instance.
    *   `droneId` (String): Target drone ID.
    *   `timestamp` (Instant): Timestamp when the command was created.
    *   `type` (String Enum): Command type.
    *   `parameters` (Map<String, Object>): Command parameters.

---

### POST `/api/drones/{droneId}/rtl`

Sends an RTL (Return To Launch) command to the specified drone.

**Path Parameters:**

*   `droneId` (String): The ID of the target drone.

**Request Body:** None

**Response (200 OK or 503 Service Unavailable):** `application/json` (Same structure as `/api/drones/{droneId}/commands` response, with `type: "RTL"`)

---

### POST `/api/drones/{droneId}/land`

Sends a LAND command to the specified drone.

**Path Parameters:**

*   `droneId` (String): The ID of the target drone.

**Request Body:** None

**Response (200 OK or 503 Service Unavailable):** `application/json` (Same structure as `/api/drones/{droneId}/commands` response, with `type: "LAND"`)

---

### POST `/api/drones/{droneId}/takeoff`

Sends a TAKEOFF command to the specified drone.

**Path Parameters:**

*   `droneId` (String): The ID of the target drone.

**Query Parameters:**

*   `altitude` (double, optional, default: `10.0`): Target takeoff altitude in meters.

**Request Body:** None

**Response (200 OK or 503 Service Unavailable):** `application/json` (Same structure as `/api/drones/{droneId}/commands` response, with `type: "TAKEOFF"` and parameters `{"altitude": <value>}`)

---

### POST `/api/drones/{droneId}/goto`

Sends a GOTO command to the specified drone.

**Path Parameters:**

*   `droneId` (String): The ID of the target drone.

**Query Parameters:**

*   `latitude` (double, **required**): Target latitude.
*   `longitude` (double, **required**): Target longitude.
*   `altitude` (double, optional, default: `10.0`): Target altitude in meters.

**Request Body:** None

**Response (200 OK or 503 Service Unavailable):** `application/json` (Same structure as `/api/drones/{droneId}/commands` response, with `type: "GOTO"` and parameters `{"latitude": <value>, "longitude": <value>, "altitude": <value>}`)

---

## 4. Web Pages (Informational)

These endpoints serve HTML pages, primarily for testing and are not part of the core REST API for the frontend application.

### GET `/websocket-test`

Serves an HTML page for testing WebSocket connectivity (`test-websocket.html`).

### GET `/mqtt-test`

Serves an HTML page for testing MQTT connectivity (`mqtt-test.html`).

---

## 5. WebSockets

The backend uses WebSockets to send real-time notifications to connected clients (e.g., the admin frontend).

**WebSocket Endpoint:** `/ws` (Configured in `WebSocketConfig.java`)

### Subscribable Topics

#### `/topic/registration`

*   **Purpose:** Sends notifications about drone registration events (new requests, status updates).
*   **Message Payload:** `RegistrationNotificationDto`

    ```json
    {
      "type": "NEW_REGISTRATION", // or "REGISTRATION_UPDATE"
      "requestId": "a1b2c3d4-e5f6-7890-1234-567890abcdef",
      "serialNumber": "SN-DRONE-XYZ-123",
      "model": "DJI Mavic 3",
      "status": "PENDING", // Current status: PENDING, APPROVED, REJECTED
      "requestedAt": "2023-10-27T10:30:00Z",
      "processedAt": null // or timestamp if processed
    }
    ```

    **Fields:**
    *   `type` (String Enum): `NEW_REGISTRATION` or `REGISTRATION_UPDATE`.
    *   `requestId` (UUID): The ID of the registration request.
    *   `serialNumber` (String): Drone's serial number.
    *   `model` (String): Drone's model.
    *   `status` (String Enum): Current status of the registration.
    *   `requestedAt` (ZonedDateTime): Timestamp of request submission.
    *   `processedAt` (ZonedDateTime, optional): Timestamp of processing.

#### `/topic/echo` (For Testing)

*   **Purpose:** Echos back any message sent to `/app/echo`.
*   **Message Payload:** String (prefixed with `[Server] Echo: `)

### Sendable Destinations (Client to Server)

#### `/app/echo` (For Testing)

*   **Purpose:** Send a message to this destination, and the server will echo it back to `/topic/echo`.
*   **Message Payload:** String

---

## Data Transfer Objects (DTOs) / Models Referenced

This section briefly describes the structure of complex objects used in requests and responses if not fully detailed above.

### `MqttCredentialsDto`

(Described under [GET `/api/v1/drones/registration/{requestId}/status`](#get-apiv1dronesregistrationrequestidstatus))

### `DroneCommand`

(Described under [POST `/api/drones/{droneId}/commands`](#post-apidronesdroneidcommands))

### `CommandResponse` (MQTT Model, not directly via REST)

This model is used for responses from the drone over MQTT, not typically directly exposed via the REST API to the frontend but logged or processed by the backend.

```json
{
  "commandId": "c1d2e3f4-a5b6-7890-1234-567890abcdef",
  "droneId": "b2c3d4e5-f6a7-8901-2345-678901bcdef0",
  "timestamp": "2023-10-27T11:00:05Z",
  "status": "SUCCESS", // RECEIVED, IN_PROGRESS, SUCCESS, FAILED, REJECTED, DEFERRED
  "message": "Command executed successfully."
}
```

**Fields:**
*   `commandId` (String): ID of the command this is responding to.
*   `droneId` (String): ID of the drone sending the response.
*   `timestamp` (Instant): Timestamp of when the response was generated.
*   `status` (String Enum): Status of the command execution.
*   `message` (String, optional): Additional details.

### `DroneTelemetryData` (MQTT Model, not directly via REST)

This model represents telemetry data received from drones over MQTT. This data is typically stored in a time-series database (e.g., InfluxDB) and might be made available to the frontend via other API endpoints (not yet defined in the provided controller code, but potentially through a future `/api/drones/{droneId}/telemetry` endpoint).

```json
{
  "droneId": "b2c3d4e5-f6a7-8901-2345-678901bcdef0",
  "timestamp": "2023-10-27T11:05:00Z",
  "batteryLevel": 85.5,
  "batteryVoltage": 15.2,
  "latitude": 34.052235,
  "longitude": -118.243683,
  "altitude": 50.7,
  "speed": 5.2,
  "heading": 180.0,
  "satellites": 12,
  "signalStrength": 95.0,
  "flightMode": "MISSION",
  "temperature": 25.5
}
```
**Fields:**
*   `droneId` (String): Drone identifier.
*   `timestamp` (Instant): Timestamp of data recording.
*   `batteryLevel` (Double): Battery level (0-100%).
*   `batteryVoltage` (Double): Battery voltage (V).
*   `latitude` (Double): Current latitude.
*   `longitude` (Double): Current longitude.
*   `altitude` (Double): Altitude (meters).
*   `speed` (Double): Speed (m/s).
*   `heading` (Double): Heading (degrees, 0-359).
*   `satellites` (Integer): Number of GPS satellites.
*   `signalStrength` (Double): Signal strength (0-100%).
*   `flightMode` (String): Current flight mode (e.g., HOVER, RTL, MISSION).
*   `temperature` (Double): Drone temperature (°C). 