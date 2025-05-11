package com.huang.backend.registration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huang.backend.exception.ResourceNotFoundException;
import com.huang.backend.registration.dto.AdminActionDto;
import com.huang.backend.registration.dto.AdminActionResponseDto;
import com.huang.backend.registration.service.RegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = RegistrationController.class)
public class AdminActionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RegistrationService registrationService;

    @Test
    void whenValidApproveAction_thenReturns200() throws Exception {
        // Create test data
        UUID requestId = UUID.randomUUID();
        UUID droneId = UUID.randomUUID();
        
        AdminActionDto actionDto = AdminActionDto.builder()
                .requestId(requestId)
                .action(AdminActionDto.Action.APPROVE)
                .build();
        
        AdminActionResponseDto responseDto = AdminActionResponseDto.builder()
                .requestId(requestId)
                .action(AdminActionDto.Action.APPROVE)
                .droneId(droneId)
                .message("Registration request approved successfully. Drone ID: " + droneId)
                .build();
        
        when(registrationService.processAdminAction(any(AdminActionDto.class))).thenReturn(responseDto);

        // Perform test
        mockMvc.perform(post("/api/v1/admin/registrations/action")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(actionDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value(requestId.toString()))
                .andExpect(jsonPath("$.action").value("APPROVE"))
                .andExpect(jsonPath("$.droneId").value(droneId.toString()))
                .andExpect(jsonPath("$.message").value("Registration request approved successfully. Drone ID: " + droneId));
    }

    @Test
    void whenValidRejectAction_thenReturns200() throws Exception {
        // Create test data
        UUID requestId = UUID.randomUUID();
        String rejectionReason = "Test rejection reason";
        
        AdminActionDto actionDto = AdminActionDto.builder()
                .requestId(requestId)
                .action(AdminActionDto.Action.REJECT)
                .rejectionReason(rejectionReason)
                .build();
        
        AdminActionResponseDto responseDto = AdminActionResponseDto.builder()
                .requestId(requestId)
                .action(AdminActionDto.Action.REJECT)
                .droneId(null)
                .message("Registration request rejected.")
                .build();
        
        when(registrationService.processAdminAction(any(AdminActionDto.class))).thenReturn(responseDto);

        // Perform test
        mockMvc.perform(post("/api/v1/admin/registrations/action")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(actionDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value(requestId.toString()))
                .andExpect(jsonPath("$.action").value("REJECT"))
                .andExpect(jsonPath("$.droneId").doesNotExist())
                .andExpect(jsonPath("$.message").value("Registration request rejected."));
    }

    @Test
    void whenRequestNotFound_thenReturns404() throws Exception {
        // Create test data
        UUID nonExistentId = UUID.randomUUID();
        
        AdminActionDto actionDto = AdminActionDto.builder()
                .requestId(nonExistentId)
                .action(AdminActionDto.Action.APPROVE)
                .build();
        
        when(registrationService.processAdminAction(any(AdminActionDto.class)))
                .thenThrow(new ResourceNotFoundException("Registration request", "id", nonExistentId));

        // Perform test
        mockMvc.perform(post("/api/v1/admin/registrations/action")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(actionDto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(String.format("Registration request not found with id: '%s'", nonExistentId)))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void whenInvalidAction_thenReturns400() throws Exception {
        // Create test data
        UUID requestId = UUID.randomUUID();
        
        AdminActionDto actionDto = AdminActionDto.builder()
                .requestId(requestId)
                .action(AdminActionDto.Action.APPROVE)
                .build();
        
        when(registrationService.processAdminAction(any(AdminActionDto.class)))
                .thenThrow(new IllegalArgumentException("Cannot process action on a request with status: APPROVED"));

        // Perform test
        mockMvc.perform(post("/api/v1/admin/registrations/action")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(actionDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot process action on a request with status: APPROVED"))
                .andExpect(jsonPath("$.status").value(400));
    }
} 