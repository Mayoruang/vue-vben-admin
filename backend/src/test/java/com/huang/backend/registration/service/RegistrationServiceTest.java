package com.huang.backend.registration.service;

import com.huang.backend.registration.dto.DroneRegistrationRequestDto;
import com.huang.backend.registration.dto.DroneRegistrationResponseDto;
import com.huang.backend.registration.entity.DroneRegistrationRequest;
import com.huang.backend.registration.repository.DroneRegistrationRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RegistrationServiceTest {

    @Mock
    private DroneRegistrationRequestRepository registrationRepository;

    @InjectMocks
    private RegistrationServiceImpl registrationService;

    @Captor
    private ArgumentCaptor<DroneRegistrationRequest> requestCaptor;

    private DroneRegistrationRequestDto requestDto;

    @BeforeEach
    void setUp() {
        // Set base URL for testing
        ReflectionTestUtils.setField(registrationService, "baseUrl", "http://test-url");

        // Create test request DTO
        requestDto = DroneRegistrationRequestDto.builder()
                .serialNumber("TEST-DRONE-ABC")
                .model("TestDroneModel")
                .notes("Test notes")
                .build();
    }

    @Test
    void whenValidRequest_thenRegisterSuccessfully() {
        // Given
        when(registrationRepository.existsBySerialNumber(requestDto.getSerialNumber())).thenReturn(false);
        
        DroneRegistrationRequest savedRequest = DroneRegistrationRequest.builder()
                .requestId(UUID.randomUUID())
                .serialNumber(requestDto.getSerialNumber())
                .model(requestDto.getModel())
                .adminNotes(requestDto.getNotes())
                .status(DroneRegistrationRequest.RegistrationStatus.PENDING_APPROVAL)
                .build();
        
        when(registrationRepository.save(any(DroneRegistrationRequest.class))).thenReturn(savedRequest);

        // When
        DroneRegistrationResponseDto responseDto = registrationService.registerDrone(requestDto);

        // Then
        verify(registrationRepository).existsBySerialNumber(requestDto.getSerialNumber());
        verify(registrationRepository).save(requestCaptor.capture());
        
        DroneRegistrationRequest capturedRequest = requestCaptor.getValue();
        assertEquals(requestDto.getSerialNumber(), capturedRequest.getSerialNumber());
        assertEquals(requestDto.getModel(), capturedRequest.getModel());
        assertEquals(requestDto.getNotes(), capturedRequest.getAdminNotes());
        assertEquals(DroneRegistrationRequest.RegistrationStatus.PENDING_APPROVAL, capturedRequest.getStatus());
        assertNotNull(capturedRequest.getRequestedAt());
        
        // Verify response
        assertEquals(savedRequest.getRequestId(), responseDto.getRequestId());
        assertTrue(responseDto.getMessage().contains("pending approval"));
        assertEquals("http://test-url/api/v1/drones/registration/" + savedRequest.getRequestId() + "/status", 
                responseDto.getStatusCheckUrl());
    }

    @Test
    void whenDuplicateSerialNumber_thenThrowException() {
        // Given
        when(registrationRepository.existsBySerialNumber(requestDto.getSerialNumber())).thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> registrationService.registerDrone(requestDto));
        
        assertTrue(exception.getMessage().contains("already registered"));
        verify(registrationRepository).existsBySerialNumber(requestDto.getSerialNumber());
        verify(registrationRepository, never()).save(any());
    }
}