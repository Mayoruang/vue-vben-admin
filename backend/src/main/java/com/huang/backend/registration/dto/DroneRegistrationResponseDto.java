package com.huang.backend.registration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for returning the result of a drone registration request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DroneRegistrationResponseDto {

    /**
     * The unique ID of the registration request
     */
    private UUID requestId;
    
    /**
     * Message to inform client about next steps
     */
    private String message;
    
    /**
     * URL to check registration status
     */
    private String statusCheckUrl;
} 