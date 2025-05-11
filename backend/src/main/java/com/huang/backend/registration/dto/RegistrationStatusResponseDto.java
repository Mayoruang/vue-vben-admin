package com.huang.backend.registration.dto;

import com.huang.backend.registration.entity.DroneRegistrationRequest.RegistrationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * DTO for returning the status of a drone registration request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationStatusResponseDto {

    /**
     * The unique ID of the registration request
     */
    private UUID requestId;
    
    /**
     * The serial number of the drone
     */
    private String serialNumber;
    
    /**
     * The model of the drone
     */
    private String model;
    
    /**
     * Current status of the registration request
     */
    private RegistrationStatus status;
    
    /**
     * When the request was submitted
     */
    private ZonedDateTime requestedAt;
    
    /**
     * When the request was processed (if applicable)
     */
    private ZonedDateTime processedAt;
    
    /**
     * Drone ID (if request was approved)
     */
    private UUID droneId;
    
    /**
     * A message describing the current status
     */
    private String message;
    
    /**
     * MQTT credentials (only populated if request was approved)
     */
    private MqttCredentialsDto mqttCredentials;
} 