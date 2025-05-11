package com.huang.backend.registration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for accepting drone registration requests
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DroneRegistrationRequestDto {

    /**
     * The drone's unique serial number, required field
     */
    @NotBlank(message = "Serial number is required")
    @Size(min = 3, max = 50, message = "Serial number must be between 3 and 50 characters")
    @Pattern(regexp = "^[A-Za-z0-9\\-_]+$", message = "Serial number can only contain letters, numbers, hyphens and underscores")
    private String serialNumber;

    /**
     * The drone's model, required field
     */
    @NotBlank(message = "Model is required")
    @Size(min = 2, max = 50, message = "Model must be between 2 and 50 characters")
    private String model;
    
    /**
     * Optional additional information about the drone
     */
    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;
} 