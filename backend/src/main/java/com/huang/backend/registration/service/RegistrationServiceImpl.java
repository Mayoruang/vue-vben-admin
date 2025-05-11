package com.huang.backend.registration.service;

import com.huang.backend.drone.entity.Drone;
import com.huang.backend.drone.repository.DroneRepository;
import com.huang.backend.exception.ResourceNotFoundException;
import com.huang.backend.registration.dto.AdminActionDto;
import com.huang.backend.registration.dto.AdminActionResponseDto;
import com.huang.backend.registration.dto.DroneRegistrationRequestDto;
import com.huang.backend.registration.dto.DroneRegistrationResponseDto;
import com.huang.backend.registration.dto.MqttCredentialsDto;
import com.huang.backend.registration.dto.RegistrationNotificationDto;
import com.huang.backend.registration.dto.RegistrationStatusResponseDto;
import com.huang.backend.registration.entity.DroneRegistrationRequest;
import com.huang.backend.registration.repository.DroneRegistrationRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.Random;

/**
 * Implementation of the RegistrationService
 */
@Service
public class RegistrationServiceImpl implements RegistrationService {

    private final DroneRegistrationRequestRepository registrationRepository;
    private final DroneRepository droneRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final SimpMessagingTemplate messagingTemplate;
    
    @Value("${application.base-url:http://localhost:8080}")
    private String baseUrl;
    
    @Value("${application.mqtt.broker-url:tcp://localhost:1883}")
    private String mqttBrokerUrl;

    @Autowired
    public RegistrationServiceImpl(
            DroneRegistrationRequestRepository registrationRepository,
            DroneRepository droneRepository,
            BCryptPasswordEncoder passwordEncoder,
            SimpMessagingTemplate messagingTemplate) {
        this.registrationRepository = registrationRepository;
        this.droneRepository = droneRepository;
        this.passwordEncoder = passwordEncoder;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Process a new drone registration request
     * 
     * @param requestDto the registration request data
     * @return the registration response with request ID and status check URL
     */
    @Override
    @Transactional
    public DroneRegistrationResponseDto registerDrone(DroneRegistrationRequestDto requestDto) {
        // Check if a drone with this serial number already exists
        if (registrationRepository.existsBySerialNumber(requestDto.getSerialNumber())) {
            throw new IllegalArgumentException("A drone with serial number " + requestDto.getSerialNumber() + " is already registered or pending approval");
        }
        
        // Create a new registration request entity
        DroneRegistrationRequest registrationRequest = DroneRegistrationRequest.builder()
                .serialNumber(requestDto.getSerialNumber())
                .model(requestDto.getModel())
                .adminNotes(requestDto.getNotes())
                .status(DroneRegistrationRequest.RegistrationStatus.PENDING_APPROVAL)
                .requestedAt(ZonedDateTime.now())
                .build();
        
        // Save the registration request
        DroneRegistrationRequest savedRequest = registrationRepository.save(registrationRequest);
        
        // Send WebSocket notification for new registration
        sendRegistrationNotification(
                RegistrationNotificationDto.NotificationType.NEW_REGISTRATION,
                savedRequest
        );
        
        // Build the status check URL
        String statusCheckUrl = baseUrl + "/api/v1/drones/registration/" + savedRequest.getRequestId() + "/status";
        
        // Return the response DTO
        return DroneRegistrationResponseDto.builder()
                .requestId(savedRequest.getRequestId())
                .message("Your registration request has been received and is pending approval. Please check the status periodically.")
                .statusCheckUrl(statusCheckUrl)
                .build();
    }

    /**
     * Get the status of a drone registration request
     *
     * @param requestId the ID of the registration request
     * @return the registration status response
     * @throws ResourceNotFoundException if request not found
     */
    @Override
    @Transactional(readOnly = true)
    public RegistrationStatusResponseDto getRegistrationStatus(UUID requestId) {
        // Find the registration request
        DroneRegistrationRequest request = registrationRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration request", "id", requestId));
        
        // Build status-specific message
        String message;
        switch (request.getStatus()) {
            case PENDING_APPROVAL:
                message = "Your registration request is pending approval from an administrator.";
                break;
            case APPROVED:
                message = "Your registration request has been approved. Your drone ID is " + request.getDroneId() + ".";
                break;
            case REJECTED:
                message = "Your registration request has been rejected.";
                break;
            default:
                message = "Current status: " + request.getStatus();
                break;
        }
        
        // Create builder for response DTO
        RegistrationStatusResponseDto.RegistrationStatusResponseDtoBuilder responseBuilder = 
            RegistrationStatusResponseDto.builder()
                .requestId(request.getRequestId())
                .serialNumber(request.getSerialNumber())
                .model(request.getModel())
                .status(request.getStatus())
                .requestedAt(request.getRequestedAt())
                .processedAt(request.getProcessedAt())
                .droneId(request.getDroneId())
                .message(message);
        
        // Add MQTT credentials if request is approved
        if (request.getStatus() == DroneRegistrationRequest.RegistrationStatus.APPROVED && request.getDroneId() != null) {
            Drone drone = droneRepository.findByRegistrationRequestId(request.getRequestId())
                    .orElseThrow(() -> new ResourceNotFoundException("Drone", "registrationRequestId", request.getRequestId()));
            
            // We need to regenerate the password since we don't store it in plaintext
            String mqttPassword = generateRandomPassword(12);
            
            // In a real production system, we would need a more secure way to handle passwords
            // For example, using a one-time token system
            
            MqttCredentialsDto credentials = MqttCredentialsDto.builder()
                    .mqttBrokerUrl(drone.getMqttBrokerUrl())
                    .mqttUsername(drone.getMqttUsername())
                    .mqttPassword(mqttPassword) // Using regenerated password for demo
                    .mqttTopicTelemetry(drone.getMqttTopicTelemetry())
                    .mqttTopicCommands(drone.getMqttTopicCommands())
                    .build();
            
            // Add credentials to response
            responseBuilder.mqttCredentials(credentials);
            
            // Update password hash in database with new password
            drone.setMqttPasswordHash(passwordEncoder.encode(mqttPassword));
            droneRepository.save(drone);
        }
        
        // Build and return the response DTO
        return responseBuilder.build();
    }
    
    /**
     * Process an admin action on a registration request
     *
     * @param actionDto the admin action data
     * @return the admin action response
     * @throws ResourceNotFoundException if request not found
     * @throws IllegalArgumentException if the action is invalid for the current request status
     */
    @Override
    @Transactional
    public AdminActionResponseDto processAdminAction(AdminActionDto actionDto) {
        // Find the registration request
        DroneRegistrationRequest request = registrationRepository.findById(actionDto.getRequestId())
                .orElseThrow(() -> new ResourceNotFoundException("Registration request", "id", actionDto.getRequestId()));
        
        // Verify the request is in PENDING_APPROVAL status
        if (request.getStatus() != DroneRegistrationRequest.RegistrationStatus.PENDING_APPROVAL) {
            throw new IllegalArgumentException("Cannot process action on a request with status: " + request.getStatus());
        }
        
        // Set processed time
        request.setProcessedAt(ZonedDateTime.now());
        
        // Process based on action type
        String message;
        UUID droneId = null;
        
        if (actionDto.getAction() == AdminActionDto.Action.APPROVE) {
            // Approve the request
            request.setStatus(DroneRegistrationRequest.RegistrationStatus.APPROVED);
            
            // Create a new drone record
            droneId = createDroneFromRequest(request);
            request.setDroneId(droneId);
            
            message = "Registration request approved successfully. Drone ID: " + droneId;
        } else {
            // Reject the request
            request.setStatus(DroneRegistrationRequest.RegistrationStatus.REJECTED);
            request.setAdminNotes(actionDto.getRejectionReason());
            
            message = "Registration request rejected.";
        }
        
        // Save the updated request
        DroneRegistrationRequest savedRequest = registrationRepository.save(request);
        
        // Send WebSocket notification for registration update
        sendRegistrationNotification(
                RegistrationNotificationDto.NotificationType.REGISTRATION_UPDATE,
                savedRequest
        );
        
        // Return the response
        return AdminActionResponseDto.builder()
                .requestId(request.getRequestId())
                .action(actionDto.getAction())
                .droneId(droneId)
                .message(message)
                .build();
    }
    
    /**
     * Helper method to create a new Drone entity from an approved registration request
     *
     * @param request the approved registration request
     * @return the generated drone ID
     */
    private UUID createDroneFromRequest(DroneRegistrationRequest request) {
        // Generate UUID for the drone
        UUID droneId = UUID.randomUUID();
        
        // Generate MQTT credentials
        String mqttUsername = "drone_" + droneId.toString().replace("-", "").substring(0, 8);
        String mqttPassword = generateRandomPassword(12);
        String passwordHash = passwordEncoder.encode(mqttPassword);
        
        // Create MQTT topics
        String topicBase = "drones/" + droneId.toString();
        String telemetryTopic = topicBase + "/telemetry";
        String commandsTopic = topicBase + "/commands";
        
        // Create new drone
        Drone drone = Drone.builder()
                .droneId(droneId)
                .serialNumber(request.getSerialNumber())
                .model(request.getModel())
                .registrationRequestId(request.getRequestId())
                .approvedAt(ZonedDateTime.now())
                .mqttBrokerUrl(mqttBrokerUrl)
                .mqttUsername(mqttUsername)
                .mqttPasswordHash(passwordHash)
                .mqttTopicTelemetry(telemetryTopic)
                .mqttTopicCommands(commandsTopic)
                .createdAt(ZonedDateTime.now())
                .updatedAt(ZonedDateTime.now())
                .currentStatus(Drone.DroneStatus.OFFLINE)
                .build();
        
        // Save the drone
        droneRepository.save(drone);
        
        // TODO: In a real system, we would need to securely communicate the plain text password
        // to the client. This could be via a one-time view, encrypted email, or other secure channel.
        // For now, the password is generated but not returned to keep the implementation simple.
        
        return droneId;
    }
    
    /**
     * Generate a random password with the specified length
     *
     * @param length the desired password length
     * @return a random password
     */
    private String generateRandomPassword(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+";
        StringBuilder password = new StringBuilder();
        Random random = new Random();
        
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(characters.length());
            password.append(characters.charAt(index));
        }
        
        return password.toString();
    }
    
    /**
     * Send registration notification via WebSocket
     *
     * @param type type of notification
     * @param request the registration request
     */
    private void sendRegistrationNotification(RegistrationNotificationDto.NotificationType type, DroneRegistrationRequest request) {
        RegistrationNotificationDto notification = RegistrationNotificationDto.builder()
                .type(type)
                .requestId(request.getRequestId())
                .serialNumber(request.getSerialNumber())
                .model(request.getModel())
                .status(request.getStatus())
                .requestedAt(request.getRequestedAt())
                .processedAt(request.getProcessedAt())
                .build();
        
        messagingTemplate.convertAndSend("/topic/registrations", notification);
    }
} 