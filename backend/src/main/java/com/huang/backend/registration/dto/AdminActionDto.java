package com.huang.backend.registration.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for admin actions on drone registration requests
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminActionDto {

    /**
     * The ID of the registration request to take action on
     */
    @NotNull(message = "Request ID is required")
    private UUID requestId;
    
    /**
     * The action to take (APPROVE or REJECT)
     */
    @NotNull(message = "Action is required")
    private Action action;
    
    /**
     * Reason for rejection (required if action is REJECT)
     */
    private String rejectionReason;
    
    /**
     * Possible admin actions
     */
    public enum Action {
        APPROVE,
        REJECT
    }
} 