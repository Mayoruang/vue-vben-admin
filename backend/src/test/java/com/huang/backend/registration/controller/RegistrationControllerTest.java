package com.huang.backend.registration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huang.backend.registration.dto.DroneRegistrationRequestDto;
import com.huang.backend.registration.dto.DroneRegistrationResponseDto;
import com.huang.backend.registration.service.RegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = RegistrationController.class,
    excludeAutoConfiguration = {}
)
public class RegistrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RegistrationService registrationService;

    private DroneRegistrationRequestDto validRequestDto;
    private DroneRegistrationResponseDto responseDto;

    @BeforeEach
    void setUp() {
        // Set up a valid request DTO
        validRequestDto = DroneRegistrationRequestDto.builder()
                .serialNumber("TEST-DRONE-123")
                .model("TestModel")
                .notes("Test notes")
                .build();

        // Set up a mock response DTO
        UUID requestId = UUID.randomUUID();
        responseDto = DroneRegistrationResponseDto.builder()
                .requestId(requestId)
                .message("Your registration request has been received and is pending approval.")
                .statusCheckUrl("http://localhost:8080/api/v1/drones/registration/" + requestId + "/status")
                .build();
    }

    @Test
    void whenValidInput_thenReturns202() throws Exception {
        when(registrationService.registerDrone(any(DroneRegistrationRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/drones/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequestDto)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.requestId").value(responseDto.getRequestId().toString()))
                .andExpect(jsonPath("$.message").value(responseDto.getMessage()))
                .andExpect(jsonPath("$.statusCheckUrl").value(responseDto.getStatusCheckUrl()));
    }

    @Test
    void whenMissingSerialNumber_thenReturns400() throws Exception {
        DroneRegistrationRequestDto invalidDto = DroneRegistrationRequestDto.builder()
                .model("TestModel")
                .notes("Test notes")
                .build();

        mockMvc.perform(post("/api/v1/drones/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.serialNumber").exists());
    }

    @Test
    void whenMissingModel_thenReturns400() throws Exception {
        DroneRegistrationRequestDto invalidDto = DroneRegistrationRequestDto.builder()
                .serialNumber("TEST-DRONE-123")
                .notes("Test notes")
                .build();

        mockMvc.perform(post("/api/v1/drones/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.model").exists());
    }

    @Test
    void whenInvalidSerialNumberFormat_thenReturns400() throws Exception {
        DroneRegistrationRequestDto invalidDto = DroneRegistrationRequestDto.builder()
                .serialNumber("TEST DRONE 123") // Contains spaces
                .model("TestModel")
                .notes("Test notes")
                .build();

        mockMvc.perform(post("/api/v1/drones/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.serialNumber").exists());
    }

    @Test
    void whenDuplicateSerialNumber_thenReturns400() throws Exception {
        when(registrationService.registerDrone(any(DroneRegistrationRequestDto.class)))
                .thenThrow(new IllegalArgumentException("A drone with this serial number is already registered"));

        mockMvc.perform(post("/api/v1/drones/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("A drone with this serial number is already registered"));
    }
} 