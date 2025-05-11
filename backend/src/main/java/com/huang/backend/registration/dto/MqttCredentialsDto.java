package com.huang.backend.registration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for MQTT credentials to be returned to clients after approval
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MqttCredentialsDto {

    /**
     * The MQTT broker URL
     */
    private String mqttBrokerUrl;
    
    /**
     * The MQTT username for the drone
     */
    private String mqttUsername;
    
    /**
     * The MQTT password for the drone (in plain text)
     */
    private String mqttPassword;
    
    /**
     * The MQTT topic for telemetry data (drone to server)
     */
    private String mqttTopicTelemetry;
    
    /**
     * The MQTT topic for commands (server to drone)
     */
    private String mqttTopicCommands;
} 