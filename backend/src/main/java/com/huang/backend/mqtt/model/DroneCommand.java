package com.huang.backend.mqtt.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Model class for commands sent to drones via MQTT
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DroneCommand {
    
    /**
     * Unique command identifier
     */
    @Builder.Default
    private String commandId = UUID.randomUUID().toString();
    
    /**
     * Target drone identifier
     */
    private String droneId;
    
    /**
     * Timestamp when the command was issued
     */
    @Builder.Default
    private Instant timestamp = Instant.now();
    
    /**
     * Type of command (e.g., ARM, DISARM, RTL, TAKEOFF, LAND, GOTO)
     */
    private CommandType type;
    
    /**
     * Command parameters as key-value pairs
     */
    private Map<String, Object> parameters;
    
    /**
     * Enum representing the types of commands that can be sent to a drone
     */
    public enum CommandType {
        /**
         * Arm the drone motors
         */
        ARM,
        
        /**
         * Disarm the drone motors
         */
        DISARM,
        
        /**
         * Return to launch position
         */
        RTL,
        
        /**
         * Take off to a specific altitude
         */
        TAKEOFF,
        
        /**
         * Land at current position
         */
        LAND,
        
        /**
         * Go to specific coordinates
         */
        GOTO,
        
        /**
         * Start executing a mission plan
         */
        START_MISSION,
        
        /**
         * Pause current mission
         */
        PAUSE_MISSION,
        
        /**
         * Resume paused mission
         */
        RESUME_MISSION,
        
        /**
         * Cancel current mission
         */
        CANCEL_MISSION,
        
        /**
         * Take a photo
         */
        TAKE_PHOTO,
        
        /**
         * Start video recording
         */
        START_RECORDING,
        
        /**
         * Stop video recording
         */
        STOP_RECORDING,
        
        /**
         * Custom command type
         */
        CUSTOM
    }
} 