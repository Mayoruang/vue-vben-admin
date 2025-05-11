package com.huang.backend.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

/**
 * Standard API error response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiError {
    
    /**
     * Error message
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
} 