package com.huang.backend.registration.dto;

import com.huang.backend.registration.entity.DroneRegistrationRequest.RegistrationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * DTO for WebSocket notifications about registration events
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationNotificationDto {

    /**
     * The type of notification
     */
    private NotificationType type;
    
    /**
     * The ID of the registration request
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
     * The status of the registration request
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
     * Possible notification types
     */
    public enum NotificationType {
        NEW_REGISTRATION,
        REGISTRATION_UPDATE
    }
} 