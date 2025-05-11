package com.huang.backend.mqtt.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Model class for responses to commands sent by drones
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommandResponse {
    
    /**
     * ID of the command this is responding to
     */
    private String commandId;
    
    /**
     * ID of the drone sending the response
     */
    private String droneId;
    
    /**
     * Timestamp of when the response was generated
     */
    @Builder.Default
    private Instant timestamp = Instant.now();
    
    /**
     * Status of the command execution
     */
    private CommandStatus status;
    
    /**
     * Optional message with additional details (especially useful for errors)
     */
    private String message;
    
    /**
     * Enum representing the possible statuses of a command execution
     */
    public enum CommandStatus {
        /**
         * Command was received but execution hasn't started yet
         */
        RECEIVED,
        
        /**
         * Command execution is in progress
         */
        IN_PROGRESS,
        
        /**
         * Command was successfully completed
         */
        SUCCESS,
        
        /**
         * Command execution failed
         */
        FAILED,
        
        /**
         * Command was rejected (e.g., invalid parameters, unsafe, etc.)
         */
        REJECTED,
        
        /**
         * Command was acknowledged but execution was deferred
         */
        DEFERRED
    }
} 