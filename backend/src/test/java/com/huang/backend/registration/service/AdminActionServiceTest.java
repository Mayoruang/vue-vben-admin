package com.huang.backend.registration.service;

import com.huang.backend.drone.entity.Drone;
import com.huang.backend.drone.repository.DroneRepository;
import com.huang.backend.exception.ResourceNotFoundException;
import com.huang.backend.registration.dto.AdminActionDto;
import com.huang.backend.registration.dto.AdminActionResponseDto;
import com.huang.backend.registration.entity.DroneRegistrationRequest;
import com.huang.backend.registration.repository.DroneRegistrationRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminActionServiceTest {

    @Mock
    private DroneRegistrationRequestRepository registrationRepository;

    @Mock
    private DroneRepository droneRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private RegistrationServiceImpl registrationService;

    @Test
    void whenApproveValidRequest_thenSuccess() {
        // Given
        UUID requestId = UUID.randomUUID();
        
        DroneRegistrationRequest request = DroneRegistrationRequest.builder()
                .requestId(requestId)
                .serialNumber("TEST-DRONE-123")
                .model("TestModel")
                .status(DroneRegistrationRequest.RegistrationStatus.PENDING_APPROVAL)
                .requestedAt(ZonedDateTime.now().minusHours(1))
                .build();
        
        AdminActionDto actionDto = AdminActionDto.builder()
                .requestId(requestId)
                .action(AdminActionDto.Action.APPROVE)
                .build();
        
        when(registrationRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(passwordEncoder.encode(any())).thenReturn("hashedPassword");
        when(droneRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // When
        AdminActionResponseDto response = registrationService.processAdminAction(actionDto);

        // Then
        assertNotNull(response);
        assertEquals(requestId, response.getRequestId());
        assertEquals(AdminActionDto.Action.APPROVE, response.getAction());
        assertNotNull(response.getDroneId());
        
        // Verify registration request was updated
        ArgumentCaptor<DroneRegistrationRequest> requestCaptor = ArgumentCaptor.forClass(DroneRegistrationRequest.class);
        verify(registrationRepository).save(requestCaptor.capture());
        
        DroneRegistrationRequest savedRequest = requestCaptor.getValue();
        assertEquals(DroneRegistrationRequest.RegistrationStatus.APPROVED, savedRequest.getStatus());
        assertNotNull(savedRequest.getProcessedAt());
        assertNotNull(savedRequest.getDroneId());
        
        // Verify drone was created
        ArgumentCaptor<Drone> droneCaptor = ArgumentCaptor.forClass(Drone.class);
        verify(droneRepository).save(droneCaptor.capture());
        
        Drone savedDrone = droneCaptor.getValue();
        assertEquals(request.getSerialNumber(), savedDrone.getSerialNumber());
        assertEquals(request.getModel(), savedDrone.getModel());
        assertEquals(request.getRequestId(), savedDrone.getRegistrationRequestId());
        assertEquals("hashedPassword", savedDrone.getMqttPasswordHash());
        assertNotNull(savedDrone.getMqttUsername());
        assertNotNull(savedDrone.getMqttTopicTelemetry());
        assertNotNull(savedDrone.getMqttTopicCommands());
    }

    @Test
    void whenRejectValidRequest_thenSuccess() {
        // Given
        UUID requestId = UUID.randomUUID();
        String rejectionReason = "Test rejection reason";
        
        DroneRegistrationRequest request = DroneRegistrationRequest.builder()
                .requestId(requestId)
                .serialNumber("TEST-DRONE-123")
                .model("TestModel")
                .status(DroneRegistrationRequest.RegistrationStatus.PENDING_APPROVAL)
                .requestedAt(ZonedDateTime.now().minusHours(1))
                .build();
        
        AdminActionDto actionDto = AdminActionDto.builder()
                .requestId(requestId)
                .action(AdminActionDto.Action.REJECT)
                .rejectionReason(rejectionReason)
                .build();
        
        when(registrationRepository.findById(requestId)).thenReturn(Optional.of(request));

        // When
        AdminActionResponseDto response = registrationService.processAdminAction(actionDto);

        // Then
        assertNotNull(response);
        assertEquals(requestId, response.getRequestId());
        assertEquals(AdminActionDto.Action.REJECT, response.getAction());
        assertNull(response.getDroneId());
        
        // Verify registration request was updated
        ArgumentCaptor<DroneRegistrationRequest> requestCaptor = ArgumentCaptor.forClass(DroneRegistrationRequest.class);
        verify(registrationRepository).save(requestCaptor.capture());
        
        DroneRegistrationRequest savedRequest = requestCaptor.getValue();
        assertEquals(DroneRegistrationRequest.RegistrationStatus.REJECTED, savedRequest.getStatus());
        assertEquals(rejectionReason, savedRequest.getAdminNotes());
        assertNotNull(savedRequest.getProcessedAt());
        
        // Verify no drone was created
        verify(droneRepository, never()).save(any());
    }

    @Test
    void whenRequestNotFound_thenThrowException() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        
        AdminActionDto actionDto = AdminActionDto.builder()
                .requestId(nonExistentId)
                .action(AdminActionDto.Action.APPROVE)
                .build();
        
        when(registrationRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When & Then
        Exception exception = assertThrows(ResourceNotFoundException.class, () -> {
            registrationService.processAdminAction(actionDto);
        });
        
        String expectedMessage = String.format("Registration request not found with id: '%s'", nonExistentId);
        assertTrue(exception.getMessage().contains(expectedMessage));
    }

    @Test
    void whenRequestNotPending_thenThrowException() {
        // Given
        UUID requestId = UUID.randomUUID();
        
        DroneRegistrationRequest request = DroneRegistrationRequest.builder()
                .requestId(requestId)
                .serialNumber("TEST-DRONE-123")
                .model("TestModel")
                .status(DroneRegistrationRequest.RegistrationStatus.APPROVED) // Already approved
                .requestedAt(ZonedDateTime.now().minusHours(1))
                .build();
        
        AdminActionDto actionDto = AdminActionDto.builder()
                .requestId(requestId)
                .action(AdminActionDto.Action.APPROVE)
                .build();
        
        when(registrationRepository.findById(requestId)).thenReturn(Optional.of(request));

        // When & Then
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            registrationService.processAdminAction(actionDto);
        });
        
        assertTrue(exception.getMessage().contains("Cannot process action on a request with status"));
    }
} 