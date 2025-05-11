package com.huang.backend.registration.service;

import com.huang.backend.drone.repository.DroneRepository;
import com.huang.backend.registration.dto.AdminActionDto;
import com.huang.backend.registration.dto.DroneRegistrationRequestDto;
import com.huang.backend.registration.dto.RegistrationNotificationDto;
import com.huang.backend.registration.entity.DroneRegistrationRequest;
import com.huang.backend.registration.repository.DroneRegistrationRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RegistrationWebSocketTest {

    @Mock
    private DroneRegistrationRequestRepository registrationRepository;

    @Mock
    private DroneRepository droneRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private RegistrationServiceImpl registrationService;

    @Test
    void shouldSendWebSocketNotificationWhenRegisteringDrone() {
        // Given
        DroneRegistrationRequestDto requestDto = DroneRegistrationRequestDto.builder()
                .serialNumber("TEST-DRONE-123")
                .model("TestModel")
                .notes("Test notes")
                .build();

        DroneRegistrationRequest savedRequest = DroneRegistrationRequest.builder()
                .requestId(UUID.randomUUID())
                .serialNumber(requestDto.getSerialNumber())
                .model(requestDto.getModel())
                .adminNotes(requestDto.getNotes())
                .status(DroneRegistrationRequest.RegistrationStatus.PENDING_APPROVAL)
                .build();

        when(registrationRepository.existsBySerialNumber(any())).thenReturn(false);
        when(registrationRepository.save(any())).thenReturn(savedRequest);

        // When
        registrationService.registerDrone(requestDto);

        // Then
        ArgumentCaptor<RegistrationNotificationDto> notificationCaptor = ArgumentCaptor.forClass(RegistrationNotificationDto.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/registrations"), notificationCaptor.capture());

        RegistrationNotificationDto notification = notificationCaptor.getValue();
        assertEquals(RegistrationNotificationDto.NotificationType.NEW_REGISTRATION, notification.getType());
        assertEquals(savedRequest.getRequestId(), notification.getRequestId());
        assertEquals(savedRequest.getSerialNumber(), notification.getSerialNumber());
        assertEquals(savedRequest.getModel(), notification.getModel());
        assertEquals(savedRequest.getStatus(), notification.getStatus());
    }

    @Test
    void shouldSendWebSocketNotificationWhenProcessingAdminAction() {
        // Given
        UUID requestId = UUID.randomUUID();
        
        DroneRegistrationRequest request = DroneRegistrationRequest.builder()
                .requestId(requestId)
                .serialNumber("TEST-DRONE-123")
                .model("TestModel")
                .status(DroneRegistrationRequest.RegistrationStatus.PENDING_APPROVAL)
                .build();
                
        AdminActionDto actionDto = AdminActionDto.builder()
                .requestId(requestId)
                .action(AdminActionDto.Action.REJECT)
                .rejectionReason("Test rejection")
                .build();
                
        DroneRegistrationRequest savedRequest = DroneRegistrationRequest.builder()
                .requestId(requestId)
                .serialNumber("TEST-DRONE-123")
                .model("TestModel")
                .status(DroneRegistrationRequest.RegistrationStatus.REJECTED)
                .adminNotes("Test rejection")
                .build();

        when(registrationRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(registrationRepository.save(any())).thenReturn(savedRequest);

        // When
        registrationService.processAdminAction(actionDto);

        // Then
        ArgumentCaptor<RegistrationNotificationDto> notificationCaptor = ArgumentCaptor.forClass(RegistrationNotificationDto.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/registrations"), notificationCaptor.capture());

        RegistrationNotificationDto notification = notificationCaptor.getValue();
        assertEquals(RegistrationNotificationDto.NotificationType.REGISTRATION_UPDATE, notification.getType());
        assertEquals(savedRequest.getRequestId(), notification.getRequestId());
        assertEquals(savedRequest.getSerialNumber(), notification.getSerialNumber());
        assertEquals(savedRequest.getModel(), notification.getModel());
        assertEquals(savedRequest.getStatus(), notification.getStatus());
    }
} 