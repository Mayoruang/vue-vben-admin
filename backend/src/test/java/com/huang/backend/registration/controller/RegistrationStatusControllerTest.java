package com.huang.backend.registration.controller;

import com.huang.backend.exception.ResourceNotFoundException;
import com.huang.backend.registration.dto.MqttCredentialsDto;
import com.huang.backend.registration.dto.RegistrationStatusResponseDto;
import com.huang.backend.registration.entity.DroneRegistrationRequest;
import com.huang.backend.registration.service.RegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.ZonedDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = RegistrationController.class,
    excludeAutoConfiguration = {}
)
public class RegistrationStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RegistrationService registrationService;

    @Test
    void whenValidRequestId_thenReturns200() throws Exception {
        // Create test data
        UUID requestId = UUID.randomUUID();
        ZonedDateTime requestTime = ZonedDateTime.now().minusHours(1);
        
        RegistrationStatusResponseDto statusDto = RegistrationStatusResponseDto.builder()
                .requestId(requestId)
                .serialNumber("TEST-DRONE-123")
                .model("TestModel")
                .status(DroneRegistrationRequest.RegistrationStatus.PENDING_APPROVAL)
                .requestedAt(requestTime)
                .message("Your registration request is pending approval from an administrator.")
                .build();
        
        when(registrationService.getRegistrationStatus(any(UUID.class))).thenReturn(statusDto);

        // Perform test
        mockMvc.perform(get("/api/v1/drones/registration/{requestId}/status", requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value(requestId.toString()))
                .andExpect(jsonPath("$.serialNumber").value("TEST-DRONE-123"))
                .andExpect(jsonPath("$.model").value("TestModel"))
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"))
                .andExpect(jsonPath("$.message").value("Your registration request is pending approval from an administrator."));
    }
    
    @Test
    void whenApprovedRequestId_thenReturns200WithMqttCredentials() throws Exception {
        // Create test data
        UUID requestId = UUID.randomUUID();
        UUID droneId = UUID.randomUUID();
        ZonedDateTime requestTime = ZonedDateTime.now().minusHours(1);
        ZonedDateTime processedTime = ZonedDateTime.now().minusMinutes(30);
        
        // Create MQTT credentials
        MqttCredentialsDto mqttCredentials = MqttCredentialsDto.builder()
                .mqttBrokerUrl("tcp://localhost:1883")
                .mqttUsername("drone_12345678")
                .mqttPassword("password123!")
                .mqttTopicTelemetry("drones/" + droneId + "/telemetry")
                .mqttTopicCommands("drones/" + droneId + "/commands")
                .build();
        
        RegistrationStatusResponseDto statusDto = RegistrationStatusResponseDto.builder()
                .requestId(requestId)
                .serialNumber("TEST-DRONE-123")
                .model("TestModel")
                .status(DroneRegistrationRequest.RegistrationStatus.APPROVED)
                .requestedAt(requestTime)
                .processedAt(processedTime)
                .droneId(droneId)
                .message("Your registration request has been approved. Your drone ID is " + droneId + ".")
                .mqttCredentials(mqttCredentials)
                .build();
        
        when(registrationService.getRegistrationStatus(any(UUID.class))).thenReturn(statusDto);

        // Perform test
        mockMvc.perform(get("/api/v1/drones/registration/{requestId}/status", requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value(requestId.toString()))
                .andExpect(jsonPath("$.serialNumber").value("TEST-DRONE-123"))
                .andExpect(jsonPath("$.model").value("TestModel"))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.droneId").value(droneId.toString()))
                .andExpect(jsonPath("$.message").value("Your registration request has been approved. Your drone ID is " + droneId + "."))
                .andExpect(jsonPath("$.mqttCredentials").exists())
                .andExpect(jsonPath("$.mqttCredentials.mqttBrokerUrl").value("tcp://localhost:1883"))
                .andExpect(jsonPath("$.mqttCredentials.mqttUsername").value("drone_12345678"))
                .andExpect(jsonPath("$.mqttCredentials.mqttPassword").value("password123!"))
                .andExpect(jsonPath("$.mqttCredentials.mqttTopicTelemetry").value("drones/" + droneId + "/telemetry"))
                .andExpect(jsonPath("$.mqttCredentials.mqttTopicCommands").value("drones/" + droneId + "/commands"));
    }

    @Test
    void whenInvalidRequestId_thenReturns404() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        
        when(registrationService.getRegistrationStatus(any(UUID.class)))
                .thenThrow(new ResourceNotFoundException("Registration request", "id", nonExistentId));

        mockMvc.perform(get("/api/v1/drones/registration/{requestId}/status", nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(String.format("Registration request not found with id: '%s'", nonExistentId)))
                .andExpect(jsonPath("$.status").value(404));
    }
} 