package com.huang.backend.registration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for admin action response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminActionResponseDto {

    /**
     * The result of the action (success or error message)
     */
    private String message;
    
    /**
     * The ID of the request that was acted upon
     */
    private UUID requestId;
    
    /**
     * The drone ID (if request was approved)
     */
    private UUID droneId;
    
    /**
     * The action that was taken
     */
    private AdminActionDto.Action action;
} 