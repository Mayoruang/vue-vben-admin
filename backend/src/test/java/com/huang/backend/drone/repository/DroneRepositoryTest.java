package com.huang.backend.drone.repository;

import com.huang.backend.config.TestConfig;
import com.huang.backend.drone.entity.Drone;
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
public class DroneRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private DroneRepository repository;

    @Test
    public void saveAndFindById() {
        // Create a sample drone
        UUID droneId = UUID.randomUUID();
        UUID registrationRequestId = UUID.randomUUID();
        
        Drone drone = Drone.builder()
                .droneId(droneId)
                .serialNumber("TEST-DRONE-123")
                .model("TestDroneModel")
                .registrationRequestId(registrationRequestId)
                .approvedAt(ZonedDateTime.now())
                .mqttBrokerUrl("tcp://emqx:1883")
                .mqttUsername("drone_" + droneId)
                .mqttPasswordHash("hashed_password_123")
                .mqttTopicTelemetry("drones/" + droneId + "/telemetry")
                .mqttTopicCommands("drones/" + droneId + "/commands")
                .currentStatus(Drone.DroneStatus.OFFLINE)
                .createdAt(ZonedDateTime.now())
                .updatedAt(ZonedDateTime.now())
                .build();

        // Save using EntityManager
        entityManager.persist(drone);
        entityManager.flush();
        
        // Find by ID
        Optional<Drone> foundDrone = repository.findById(drone.getDroneId());
        
        // Verify it was found
        assertThat(foundDrone).isPresent();
        assertThat(foundDrone.get().getSerialNumber()).isEqualTo(drone.getSerialNumber());
        assertThat(foundDrone.get().getModel()).isEqualTo(drone.getModel());
    }

    @Test
    public void findBySerialNumber() {
        // Create a sample drone
        UUID droneId = UUID.randomUUID();
        UUID registrationRequestId = UUID.randomUUID();
        
        Drone drone = Drone.builder()
                .droneId(droneId)
                .serialNumber("TEST-DRONE-123")
                .model("TestDroneModel")
                .registrationRequestId(registrationRequestId)
                .approvedAt(ZonedDateTime.now())
                .mqttBrokerUrl("tcp://emqx:1883")
                .mqttUsername("drone_" + droneId)
                .mqttPasswordHash("hashed_password_123")
                .mqttTopicTelemetry("drones/" + droneId + "/telemetry")
                .mqttTopicCommands("drones/" + droneId + "/commands")
                .currentStatus(Drone.DroneStatus.OFFLINE)
                .createdAt(ZonedDateTime.now())
                .updatedAt(ZonedDateTime.now())
                .build();

        // Save using EntityManager
        entityManager.persist(drone);
        entityManager.flush();
        
        // Find by serial number
        Optional<Drone> foundDrone = repository.findBySerialNumber(drone.getSerialNumber());
        
        // Verify it was found
        assertThat(foundDrone).isPresent();
        assertThat(foundDrone.get().getDroneId()).isEqualTo(drone.getDroneId());
    }

    @Test
    public void findByRegistrationRequestId() {
        // Create a sample drone
        UUID droneId = UUID.randomUUID();
        UUID registrationRequestId = UUID.randomUUID();
        
        Drone drone = Drone.builder()
                .droneId(droneId)
                .serialNumber("TEST-DRONE-123")
                .model("TestDroneModel")
                .registrationRequestId(registrationRequestId)
                .approvedAt(ZonedDateTime.now())
                .mqttBrokerUrl("tcp://emqx:1883")
                .mqttUsername("drone_" + droneId)
                .mqttPasswordHash("hashed_password_123")
                .mqttTopicTelemetry("drones/" + droneId + "/telemetry")
                .mqttTopicCommands("drones/" + droneId + "/commands")
                .currentStatus(Drone.DroneStatus.OFFLINE)
                .createdAt(ZonedDateTime.now())
                .updatedAt(ZonedDateTime.now())
                .build();

        // Save using EntityManager
        entityManager.persist(drone);
        entityManager.flush();
        
        // Find by registration request ID
        Optional<Drone> foundDrone = repository.findByRegistrationRequestId(registrationRequestId);
        
        // Verify it was found
        assertThat(foundDrone).isPresent();
        assertThat(foundDrone.get().getDroneId()).isEqualTo(drone.getDroneId());
    }

    @Test
    public void findByCurrentStatus() {
        // Create an OFFLINE drone
        UUID offlineDroneId = UUID.randomUUID();
        UUID offlineRequestId = UUID.randomUUID();
        
        Drone offlineDrone = Drone.builder()
                .droneId(offlineDroneId)
                .serialNumber("TEST-DRONE-OFFLINE")
                .model("OfflineDroneModel")
                .registrationRequestId(offlineRequestId)
                .approvedAt(ZonedDateTime.now())
                .mqttBrokerUrl("tcp://emqx:1883")
                .mqttUsername("drone_offline")
                .mqttPasswordHash("hashed_password_offline")
                .mqttTopicTelemetry("drones/offline/telemetry")
                .mqttTopicCommands("drones/offline/commands")
                .currentStatus(Drone.DroneStatus.OFFLINE)
                .createdAt(ZonedDateTime.now())
                .updatedAt(ZonedDateTime.now())
                .build();
        entityManager.persist(offlineDrone);
        
        // Create an ONLINE drone
        UUID onlineDroneId = UUID.randomUUID();
        UUID onlineRequestId = UUID.randomUUID();
        
        Drone onlineDrone = Drone.builder()
                .droneId(onlineDroneId)
                .serialNumber("TEST-DRONE-ONLINE")
                .model("OnlineDroneModel")
                .registrationRequestId(onlineRequestId)
                .approvedAt(ZonedDateTime.now())
                .mqttBrokerUrl("tcp://emqx:1883")
                .mqttUsername("drone_online")
                .mqttPasswordHash("hashed_password_online")
                .mqttTopicTelemetry("drones/online/telemetry")
                .mqttTopicCommands("drones/online/commands")
                .currentStatus(Drone.DroneStatus.ONLINE)
                .lastHeartbeatAt(ZonedDateTime.now())
                .createdAt(ZonedDateTime.now())
                .updatedAt(ZonedDateTime.now())
                .build();
        entityManager.persist(onlineDrone);
        entityManager.flush();
        
        // Find by status
        List<Drone> offlineDrones = repository.findByCurrentStatus(Drone.DroneStatus.OFFLINE);
        List<Drone> onlineDrones = repository.findByCurrentStatus(Drone.DroneStatus.ONLINE);
        List<Drone> flyingDrones = repository.findByCurrentStatus(Drone.DroneStatus.FLYING);
        
        // Verify results
        assertThat(offlineDrones).hasSize(1);
        assertThat(offlineDrones.get(0).getSerialNumber()).isEqualTo(offlineDrone.getSerialNumber());
        
        assertThat(onlineDrones).hasSize(1);
        assertThat(onlineDrones.get(0).getSerialNumber()).isEqualTo(onlineDrone.getSerialNumber());
        
        assertThat(flyingDrones).isEmpty();
    }

    @Test
    public void findByLastHeartbeatAtBeforeOrLastHeartbeatAtIsNull() {
        // Create a drone with no heartbeat (null)
        UUID nullHeartbeatDroneId = UUID.randomUUID();
        UUID nullHeartbeatRequestId = UUID.randomUUID();
        
        Drone nullHeartbeatDrone = Drone.builder()
                .droneId(nullHeartbeatDroneId)
                .serialNumber("TEST-DRONE-NULL-HEARTBEAT")
                .model("NullHeartbeatModel")
                .registrationRequestId(nullHeartbeatRequestId)
                .approvedAt(ZonedDateTime.now())
                .mqttBrokerUrl("tcp://emqx:1883")
                .mqttUsername("drone_null_heartbeat")
                .mqttPasswordHash("hashed_password_null")
                .mqttTopicTelemetry("drones/null_heartbeat/telemetry")
                .mqttTopicCommands("drones/null_heartbeat/commands")
                .currentStatus(Drone.DroneStatus.OFFLINE)
                .createdAt(ZonedDateTime.now())
                .updatedAt(ZonedDateTime.now())
                .build();
        entityManager.persist(nullHeartbeatDrone);
        
        // Create a drone with recent heartbeat
        UUID recentHeartbeatDroneId = UUID.randomUUID();
        UUID recentHeartbeatRequestId = UUID.randomUUID();
        
        Drone recentHeartbeatDrone = Drone.builder()
                .droneId(recentHeartbeatDroneId)
                .serialNumber("TEST-DRONE-RECENT")
                .model("RecentModel")
                .registrationRequestId(recentHeartbeatRequestId)
                .approvedAt(ZonedDateTime.now())
                .mqttBrokerUrl("tcp://emqx:1883")
                .mqttUsername("drone_recent")
                .mqttPasswordHash("hashed_password_recent")
                .mqttTopicTelemetry("drones/recent/telemetry")
                .mqttTopicCommands("drones/recent/commands")
                .currentStatus(Drone.DroneStatus.ONLINE)
                .lastHeartbeatAt(ZonedDateTime.now())
                .createdAt(ZonedDateTime.now())
                .updatedAt(ZonedDateTime.now())
                .build();
        entityManager.persist(recentHeartbeatDrone);
        
        // Create a drone with old heartbeat
        UUID oldHeartbeatDroneId = UUID.randomUUID();
        UUID oldHeartbeatRequestId = UUID.randomUUID();
        
        Drone oldHeartbeatDrone = Drone.builder()
                .droneId(oldHeartbeatDroneId)
                .serialNumber("TEST-DRONE-OLD")
                .model("OldModel")
                .registrationRequestId(oldHeartbeatRequestId)
                .approvedAt(ZonedDateTime.now())
                .mqttBrokerUrl("tcp://emqx:1883")
                .mqttUsername("drone_old")
                .mqttPasswordHash("hashed_password_old")
                .mqttTopicTelemetry("drones/old/telemetry")
                .mqttTopicCommands("drones/old/commands")
                .currentStatus(Drone.DroneStatus.ONLINE)
                .lastHeartbeatAt(ZonedDateTime.now().minusHours(2))
                .createdAt(ZonedDateTime.now())
                .updatedAt(ZonedDateTime.now())
                .build();
        entityManager.persist(oldHeartbeatDrone);
        entityManager.flush();
        
        // Find drones with no heartbeat after a certain time
        ZonedDateTime cutoffTime = ZonedDateTime.now().minusHours(1);
        List<Drone> inactiveDrones = repository.findByLastHeartbeatAtBeforeOrLastHeartbeatAtIsNull(cutoffTime);
        
        // Verify results
        assertThat(inactiveDrones).hasSize(2);
        
        boolean foundNullHeartbeatDrone = false;
        boolean foundOldHeartbeatDrone = false;
        
        for (Drone drone : inactiveDrones) {
            if (drone.getSerialNumber().equals(nullHeartbeatDrone.getSerialNumber())) {
                foundNullHeartbeatDrone = true;
            } else if (drone.getSerialNumber().equals(oldHeartbeatDrone.getSerialNumber())) {
                foundOldHeartbeatDrone = true;
            }
        }
        
        assertThat(foundNullHeartbeatDrone).isTrue();
        assertThat(foundOldHeartbeatDrone).isTrue();
    }

    @Test
    public void existsBySerialNumber() {
        // Create a sample drone
        UUID droneId = UUID.randomUUID();
        UUID registrationRequestId = UUID.randomUUID();
        
        Drone drone = Drone.builder()
                .droneId(droneId)
                .serialNumber("TEST-DRONE-123")
                .model("TestDroneModel")
                .registrationRequestId(registrationRequestId)
                .approvedAt(ZonedDateTime.now())
                .mqttBrokerUrl("tcp://emqx:1883")
                .mqttUsername("drone_" + droneId)
                .mqttPasswordHash("hashed_password_123")
                .mqttTopicTelemetry("drones/" + droneId + "/telemetry")
                .mqttTopicCommands("drones/" + droneId + "/commands")
                .currentStatus(Drone.DroneStatus.OFFLINE)
                .createdAt(ZonedDateTime.now())
                .updatedAt(ZonedDateTime.now())
                .build();

        // Save using EntityManager
        entityManager.persist(drone);
        entityManager.flush();
        
        // Check if exists
        boolean exists = repository.existsBySerialNumber(drone.getSerialNumber());
        boolean notExists = repository.existsBySerialNumber("NON-EXISTENT-SERIAL");
        
        // Verify
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    public void existsByMqttUsername() {
        // Create a sample drone
        UUID droneId = UUID.randomUUID();
        UUID registrationRequestId = UUID.randomUUID();
        
        Drone drone = Drone.builder()
                .droneId(droneId)
                .serialNumber("TEST-DRONE-123")
                .model("TestDroneModel")
                .registrationRequestId(registrationRequestId)
                .approvedAt(ZonedDateTime.now())
                .mqttBrokerUrl("tcp://emqx:1883")
                .mqttUsername("drone_" + droneId)
                .mqttPasswordHash("hashed_password_123")
                .mqttTopicTelemetry("drones/" + droneId + "/telemetry")
                .mqttTopicCommands("drones/" + droneId + "/commands")
                .currentStatus(Drone.DroneStatus.OFFLINE)
                .createdAt(ZonedDateTime.now())
                .updatedAt(ZonedDateTime.now())
                .build();

        // Save using EntityManager
        entityManager.persist(drone);
        entityManager.flush();
        
        // Check if exists
        boolean exists = repository.existsByMqttUsername(drone.getMqttUsername());
        boolean notExists = repository.existsByMqttUsername("NON-EXISTENT-USERNAME");
        
        // Verify
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    public void updateDroneStatus() {
        // Create a sample drone
        UUID droneId = UUID.randomUUID();
        UUID registrationRequestId = UUID.randomUUID();
        
        Drone drone = Drone.builder()
                .droneId(droneId)
                .serialNumber("TEST-DRONE-123")
                .model("TestDroneModel")
                .registrationRequestId(registrationRequestId)
                .approvedAt(ZonedDateTime.now())
                .mqttBrokerUrl("tcp://emqx:1883")
                .mqttUsername("drone_" + droneId)
                .mqttPasswordHash("hashed_password_123")
                .mqttTopicTelemetry("drones/" + droneId + "/telemetry")
                .mqttTopicCommands("drones/" + droneId + "/commands")
                .currentStatus(Drone.DroneStatus.OFFLINE)
                .createdAt(ZonedDateTime.now())
                .updatedAt(ZonedDateTime.now())
                .build();

        // Save using EntityManager
        entityManager.persist(drone);
        entityManager.flush();
        
        // Update status to ONLINE
        drone.setCurrentStatus(Drone.DroneStatus.ONLINE);
        drone.setLastHeartbeatAt(ZonedDateTime.now());
        entityManager.persist(drone);
        entityManager.flush();
        
        // Verify update
        Optional<Drone> updatedDrone = repository.findById(drone.getDroneId());
        
        assertThat(updatedDrone).isPresent();
        assertThat(updatedDrone.get().getCurrentStatus()).isEqualTo(Drone.DroneStatus.ONLINE);
        assertThat(updatedDrone.get().getLastHeartbeatAt()).isNotNull();
    }
} 