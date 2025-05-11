package com.huang.backend.registration.service;

import com.huang.backend.drone.entity.Drone;
import com.huang.backend.drone.repository.DroneRepository;
import com.huang.backend.exception.ResourceNotFoundException;
import com.huang.backend.registration.dto.RegistrationStatusResponseDto;
import com.huang.backend.registration.entity.DroneRegistrationRequest;
import com.huang.backend.registration.repository.DroneRegistrationRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
public class RegistrationStatusServiceTest {

    @Mock
    private DroneRegistrationRequestRepository registrationRepository;
    
    @Mock
    private DroneRepository droneRepository;
    
    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private RegistrationServiceImpl registrationService;

    @Test
    void whenRequestExists_thenReturnStatus() {
        // Given
        UUID requestId = UUID.randomUUID();
        UUID droneId = UUID.randomUUID();
        ZonedDateTime requestedAt = ZonedDateTime.now().minusHours(1);
        ZonedDateTime processedAt = ZonedDateTime.now().minusMinutes(30);
        
        DroneRegistrationRequest request = DroneRegistrationRequest.builder()
                .requestId(requestId)
                .serialNumber("TEST-DRONE-123")
                .model("TestModel")
                .status(DroneRegistrationRequest.RegistrationStatus.APPROVED)
                .requestedAt(requestedAt)
                .processedAt(processedAt)
                .droneId(droneId)
                .build();
        
        Drone drone = Drone.builder()
                .droneId(droneId)
                .serialNumber("TEST-DRONE-123")
                .model("TestModel")
                .registrationRequestId(requestId)
                .mqttBrokerUrl("tcp://localhost:1883")
                .mqttUsername("drone_" + droneId.toString().substring(0, 8))
                .mqttPasswordHash("hashedPassword")
                .mqttTopicTelemetry("drones/" + droneId + "/telemetry")
                .mqttTopicCommands("drones/" + droneId + "/commands")
                .build();
        
        when(registrationRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(droneRepository.findByRegistrationRequestId(requestId)).thenReturn(Optional.of(drone));
        when(passwordEncoder.encode(any())).thenReturn("newHashedPassword");

        // When
        RegistrationStatusResponseDto statusDto = registrationService.getRegistrationStatus(requestId);

        // Then
        assertNotNull(statusDto);
        assertEquals(requestId, statusDto.getRequestId());
        assertEquals("TEST-DRONE-123", statusDto.getSerialNumber());
        assertEquals("TestModel", statusDto.getModel());
        assertEquals(DroneRegistrationRequest.RegistrationStatus.APPROVED, statusDto.getStatus());
        assertEquals(requestedAt, statusDto.getRequestedAt());
        assertEquals(processedAt, statusDto.getProcessedAt());
        assertEquals(droneId, statusDto.getDroneId());
        assertTrue(statusDto.getMessage().contains("approved"));
        assertTrue(statusDto.getMessage().contains(droneId.toString()));
        
        // Verify MQTT credentials are included
        assertNotNull(statusDto.getMqttCredentials());
    }
    
    @Test
    void whenApprovedRequest_thenReturnMqttCredentials() {
        // Given
        UUID requestId = UUID.randomUUID();
        UUID droneId = UUID.randomUUID();
        ZonedDateTime requestedAt = ZonedDateTime.now().minusHours(1);
        ZonedDateTime processedAt = ZonedDateTime.now().minusMinutes(30);
        
        DroneRegistrationRequest request = DroneRegistrationRequest.builder()
                .requestId(requestId)
                .serialNumber("TEST-DRONE-123")
                .model("TestModel")
                .status(DroneRegistrationRequest.RegistrationStatus.APPROVED)
                .requestedAt(requestedAt)
                .processedAt(processedAt)
                .droneId(droneId)
                .build();
        
        Drone drone = Drone.builder()
                .droneId(droneId)
                .serialNumber("TEST-DRONE-123")
                .model("TestModel")
                .registrationRequestId(requestId)
                .mqttBrokerUrl("tcp://localhost:1883")
                .mqttUsername("drone_" + droneId.toString().substring(0, 8))
                .mqttPasswordHash("hashedPassword")
                .mqttTopicTelemetry("drones/" + droneId + "/telemetry")
                .mqttTopicCommands("drones/" + droneId + "/commands")
                .build();
        
        when(registrationRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(droneRepository.findByRegistrationRequestId(requestId)).thenReturn(Optional.of(drone));
        when(passwordEncoder.encode(any())).thenReturn("newHashedPassword");
        
        // When
        RegistrationStatusResponseDto statusDto = registrationService.getRegistrationStatus(requestId);

        // Then
        assertNotNull(statusDto);
        assertEquals(DroneRegistrationRequest.RegistrationStatus.APPROVED, statusDto.getStatus());
        assertEquals(droneId, statusDto.getDroneId());
        
        // Verify MQTT credentials are included
        assertNotNull(statusDto.getMqttCredentials());
        assertEquals("tcp://localhost:1883", statusDto.getMqttCredentials().getMqttBrokerUrl());
        assertEquals(drone.getMqttUsername(), statusDto.getMqttCredentials().getMqttUsername());
        assertNotNull(statusDto.getMqttCredentials().getMqttPassword());
        assertEquals(drone.getMqttTopicTelemetry(), statusDto.getMqttCredentials().getMqttTopicTelemetry());
        assertEquals(drone.getMqttTopicCommands(), statusDto.getMqttCredentials().getMqttTopicCommands());
        
        // Verify password was updated
        verify(droneRepository).save(any(Drone.class));
    }

    @Test
    void whenRequestDoesNotExist_thenThrowException() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(registrationRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        // When & Then
        Exception exception = assertThrows(ResourceNotFoundException.class, () -> {
            registrationService.getRegistrationStatus(nonExistentId);
        });
        
        String expectedMessage = String.format("Registration request not found with id: '%s'", nonExistentId);
        assertTrue(exception.getMessage().contains(expectedMessage));
    }
} 