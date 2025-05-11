package com.huang.backend.mqtt.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huang.backend.drone.entity.Drone;
import com.huang.backend.drone.repository.DroneRepository;
import com.huang.backend.mqtt.model.DroneTelemetryData;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MqttSubscriberServiceTest {

    @Mock
    private MqttClient mqttClient;

    @Mock
    private TimeseriesService timeseriesService;

    @Mock
    private DroneRepository droneRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private MqttSubscriberService subscriberService;

    @Test
    void shouldProcessTelemetryDataFromMqttMessage() throws Exception {
        // Given
        String droneId = "DRONE-TEST-123";
        String topic = "drones/" + droneId + "/telemetry";
        String messagePayload = "{\"batteryLevel\": 85.5, \"latitude\": 37.7749, \"longitude\": -122.4194}";
        MqttMessage message = new MqttMessage(messagePayload.getBytes());

        DroneTelemetryData telemetryData = DroneTelemetryData.builder()
                .droneId(droneId)
                .batteryLevel(85.5)
                .latitude(37.7749)
                .longitude(-122.4194)
                .timestamp(Instant.now())
                .build();

        Drone drone = Drone.builder()
                .droneId(UUID.randomUUID())
                .serialNumber(droneId)
                .model("TestModel")
                .build();

        // When
        when(objectMapper.readValue(message.getPayload(), DroneTelemetryData.class)).thenReturn(telemetryData);
        when(droneRepository.findBySerialNumber(droneId)).thenReturn(Optional.of(drone));

        subscriberService.messageArrived(topic, message);

        // Then
        verify(timeseriesService).writeTelemetryData(telemetryData);
        verify(droneRepository).save(any(Drone.class));
    }

    @Test
    void shouldHandleNonExistentDrone() throws Exception {
        // Given
        String droneId = "NONEXISTENT-DRONE";
        String topic = "drones/" + droneId + "/telemetry";
        String messagePayload = "{\"batteryLevel\": 85.5, \"latitude\": 37.7749, \"longitude\": -122.4194}";
        MqttMessage message = new MqttMessage(messagePayload.getBytes());

        DroneTelemetryData telemetryData = DroneTelemetryData.builder()
                .droneId(droneId)
                .batteryLevel(85.5)
                .latitude(37.7749)
                .longitude(-122.4194)
                .timestamp(Instant.now())
                .build();

        // When
        when(objectMapper.readValue(message.getPayload(), DroneTelemetryData.class)).thenReturn(telemetryData);
        when(droneRepository.findBySerialNumber(droneId)).thenReturn(Optional.empty());

        subscriberService.messageArrived(topic, message);

        // Then
        verify(timeseriesService).writeTelemetryData(telemetryData);
        verify(droneRepository, never()).save(any(Drone.class));
    }

    @Test
    void shouldHandleInvalidTopicFormat() throws Exception {
        // Given
        String invalidTopic = "invalid/topic/format";
        String messagePayload = "{\"batteryLevel\": 85.5, \"latitude\": 37.7749, \"longitude\": -122.4194}";
        MqttMessage message = new MqttMessage(messagePayload.getBytes());

        // When
        subscriberService.messageArrived(invalidTopic, message);

        // Then
        verify(timeseriesService, never()).writeTelemetryData(any());
        verify(droneRepository, never()).save(any());
    }
} 