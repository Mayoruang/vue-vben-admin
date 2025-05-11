package com.huang.backend.mqtt.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Model class for drone telemetry data received via MQTT
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DroneTelemetryData {
    
    /**
     * Drone identifier (typically the serial number)
     */
    private String droneId;
    
    /**
     * Timestamp of when the data was recorded on the drone
     */
    private Instant timestamp;
    
    /**
     * Battery level in percentage (0-100)
     */
    private Double batteryLevel;
    
    /**
     * Battery voltage in volts
     */
    private Double batteryVoltage;
    
    /**
     * Current latitude position
     */
    private Double latitude;
    
    /**
     * Current longitude position
     */
    private Double longitude;
    
    /**
     * Altitude in meters
     */
    private Double altitude;
    
    /**
     * Speed in meters per second
     */
    private Double speed;
    
    /**
     * Heading/direction in degrees (0-359)
     */
    private Double heading;
    
    /**
     * Satellite count used for GPS fix
     */
    private Integer satellites;
    
    /**
     * Signal strength in percentage (0-100)
     */
    private Double signalStrength;
    
    /**
     * Flight mode (e.g., HOVER, RTL, MISSION)
     */
    private String flightMode;
    
    /**
     * Temperature of the drone in Celsius
     */
    private Double temperature;
} 