package com.huang.backend.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.Map;

/**
 * Validation error response with field-specific error messages
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationError {
    
    /**
     * General error message
     */
    private String message;
    
    /**
     * HTTP status code
     */
    private int status;
    
    /**
     * Timestamp when the error occurred
     */
    private ZonedDateTime timestamp;
    
    /**
     * Field-specific error messages
     */
    private Map<String, String> errors;
} 