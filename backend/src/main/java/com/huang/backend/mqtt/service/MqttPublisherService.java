package com.huang.backend.mqtt.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.stereotype.Service;

/**
 * Service for publishing messages to MQTT topics
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MqttPublisherService {

    private final MqttClient mqttClient;
    private final ObjectMapper objectMapper;

    /**
     * Publishes a command to a specific drone
     *
     * @param droneId the ID of the drone
     * @param command the command object to publish
     * @param <T> the type of the command
     * @return true if the message was published successfully
     */
    public <T> boolean publishCommand(String droneId, T command) {
        String topic = "drones/" + droneId + "/commands";
        return publishMessage(topic, command);
    }

    /**
     * Publishes a message to a specific topic
     *
     * @param topic the topic to publish to
     * @param payload the payload to publish
     * @param <T> the type of the payload
     * @return true if the message was published successfully
     */
    public <T> boolean publishMessage(String topic, T payload) {
        try {
            if (!mqttClient.isConnected()) {
                log.warn("MQTT客户端未连接，无法发布消息到主题: {}", topic);
                return false;
            }

            String jsonPayload = objectMapper.writeValueAsString(payload);
            MqttMessage message = new MqttMessage(jsonPayload.getBytes());
            message.setQos(1);  // 至少一次传递
            message.setRetained(false);  // 不保留消息

            mqttClient.publish(topic, message);
            log.info("已发布消息到主题: {}", topic);
            return true;
        } catch (MqttException e) {
            log.error("MQTT消息发布失败: {}", e.getMessage(), e);
            return false;
        } catch (Exception e) {
            log.error("消息序列化或发布过程中发生错误: {}", e.getMessage(), e);
            return false;
        }
    }
} 