package com.huang.backend.registration.repository;

import com.huang.backend.config.TestConfig;
import com.huang.backend.registration.entity.DroneRegistrationRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestConfig.class)
public class DroneRegistrationRequestRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private DroneRegistrationRequestRepository repository;

    @Test
    public void saveAndFindById() {
        // Create a sample registration request
        DroneRegistrationRequest request = DroneRegistrationRequest.builder()
                .serialNumber("TEST-DRONE-123")
                .model("TestDroneModel")
                .status(DroneRegistrationRequest.RegistrationStatus.PENDING_APPROVAL)
                .requestedAt(ZonedDateTime.now())
                .build();

        // Save using EntityManager
        entityManager.persist(request);
        entityManager.flush();
        
        // Find by ID
        Optional<DroneRegistrationRequest> foundRequest = 
                repository.findById(request.getRequestId());
        
        // Verify it was found
        assertThat(foundRequest).isPresent();
        assertThat(foundRequest.get().getSerialNumber()).isEqualTo(request.getSerialNumber());
        assertThat(foundRequest.get().getModel()).isEqualTo(request.getModel());
    }

    @Test
    public void findBySerialNumber() {
        // Create a sample registration request
        DroneRegistrationRequest request = DroneRegistrationRequest.builder()
                .serialNumber("TEST-DRONE-123")
                .model("TestDroneModel")
                .status(DroneRegistrationRequest.RegistrationStatus.PENDING_APPROVAL)
                .requestedAt(ZonedDateTime.now())
                .build();

        // Save using EntityManager
        entityManager.persist(request);
        entityManager.flush();
        
        // Find by serial number
        Optional<DroneRegistrationRequest> foundRequest = 
                repository.findBySerialNumber(request.getSerialNumber());
        
        // Verify it was found
        assertThat(foundRequest).isPresent();
        assertThat(foundRequest.get().getModel()).isEqualTo(request.getModel());
    }

    @Test
    public void findByStatus() {
        // Create a pending approval request
        DroneRegistrationRequest pendingRequest = DroneRegistrationRequest.builder()
                .serialNumber("TEST-DRONE-PENDING")
                .model("PendingModel")
                .status(DroneRegistrationRequest.RegistrationStatus.PENDING_APPROVAL)
                .requestedAt(ZonedDateTime.now())
                .build();
        entityManager.persist(pendingRequest);
        
        // Create an approved request
        DroneRegistrationRequest approvedRequest = DroneRegistrationRequest.builder()
                .serialNumber("TEST-DRONE-APPROVED")
                .model("ApprovedModel")
                .status(DroneRegistrationRequest.RegistrationStatus.APPROVED)
                .requestedAt(ZonedDateTime.now().minusDays(1))
                .processedAt(ZonedDateTime.now())
                .droneId(UUID.randomUUID())
                .build();
        entityManager.persist(approvedRequest);
        entityManager.flush();
        
        // Find by status
        List<DroneRegistrationRequest> pendingRequests = 
                repository.findByStatus(DroneRegistrationRequest.RegistrationStatus.PENDING_APPROVAL);
        List<DroneRegistrationRequest> approvedRequests = 
                repository.findByStatus(DroneRegistrationRequest.RegistrationStatus.APPROVED);
        List<DroneRegistrationRequest> rejectedRequests = 
                repository.findByStatus(DroneRegistrationRequest.RegistrationStatus.REJECTED);
        
        // Verify results
        assertThat(pendingRequests).hasSize(1);
        assertThat(pendingRequests.get(0).getSerialNumber()).isEqualTo(pendingRequest.getSerialNumber());
        
        assertThat(approvedRequests).hasSize(1);
        assertThat(approvedRequests.get(0).getSerialNumber()).isEqualTo(approvedRequest.getSerialNumber());
        
        assertThat(rejectedRequests).isEmpty();
    }

    @Test
    public void existsBySerialNumber() {
        // Create a sample registration request
        DroneRegistrationRequest request = DroneRegistrationRequest.builder()
                .serialNumber("TEST-DRONE-123")
                .model("TestDroneModel")
                .status(DroneRegistrationRequest.RegistrationStatus.PENDING_APPROVAL)
                .requestedAt(ZonedDateTime.now())
                .build();

        // Save using EntityManager
        entityManager.persist(request);
        entityManager.flush();
        
        // Check if exists
        boolean exists = repository.existsBySerialNumber(request.getSerialNumber());
        boolean notExists = repository.existsBySerialNumber("NON-EXISTENT-SERIAL");
        
        // Verify
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    public void updateStatus() {
        // Create a sample registration request
        DroneRegistrationRequest request = DroneRegistrationRequest.builder()
                .serialNumber("TEST-DRONE-123")
                .model("TestDroneModel")
                .status(DroneRegistrationRequest.RegistrationStatus.PENDING_APPROVAL)
                .requestedAt(ZonedDateTime.now())
                .build();

        // Save using EntityManager
        entityManager.persist(request);
        entityManager.flush();
        
        // Update status to APPROVED
        request.setStatus(DroneRegistrationRequest.RegistrationStatus.APPROVED);
        request.setProcessedAt(ZonedDateTime.now());
        request.setDroneId(UUID.randomUUID());
        entityManager.persist(request);
        entityManager.flush();
        
        // Verify update
        Optional<DroneRegistrationRequest> updatedRequest = 
                repository.findById(request.getRequestId());
        
        assertThat(updatedRequest).isPresent();
        assertThat(updatedRequest.get().getStatus())
                .isEqualTo(DroneRegistrationRequest.RegistrationStatus.APPROVED);
        assertThat(updatedRequest.get().getProcessedAt()).isNotNull();
        assertThat(updatedRequest.get().getDroneId()).isEqualTo(request.getDroneId());
    }
} 