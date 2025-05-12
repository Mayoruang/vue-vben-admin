package com.huang.backend.registration.service;

import com.huang.backend.registration.dto.AdminActionDto;
import com.huang.backend.registration.dto.AdminActionResponseDto;
import com.huang.backend.registration.dto.DroneRegistrationRequestDto;
import com.huang.backend.registration.dto.DroneRegistrationResponseDto;
import com.huang.backend.registration.dto.RegistrationStatusResponseDto;
import com.huang.backend.registration.entity.DroneRegistrationRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service for handling drone registration operations
 */
public interface RegistrationService {

    /**
     * Process a new drone registration request
     *
     * @param requestDto the registration request data
     * @return the registration response
     */
    DroneRegistrationResponseDto registerDrone(DroneRegistrationRequestDto requestDto);
    
    /**
     * Get the status of a drone registration request
     *
     * @param requestId the ID of the registration request
     * @return the registration status response
     * @throws com.huang.backend.exception.ResourceNotFoundException if request not found
     */
    RegistrationStatusResponseDto getRegistrationStatus(UUID requestId);
    
    /**
     * Get a paginated list of registration requests
     *
     * @param status optional status filter
     * @param pageable pagination information
     * @return a page of registration status responses
     */
    Page<RegistrationStatusResponseDto> getRegistrationList(DroneRegistrationRequest.RegistrationStatus status, Pageable pageable);
    
    /**
     * Process an admin action on a registration request
     *
     * @param actionDto the admin action data
     * @return the admin action response
     * @throws com.huang.backend.exception.ResourceNotFoundException if request not found
     * @throws IllegalArgumentException if the action is invalid for the current request status
     */
    AdminActionResponseDto processAdminAction(AdminActionDto actionDto);
} 