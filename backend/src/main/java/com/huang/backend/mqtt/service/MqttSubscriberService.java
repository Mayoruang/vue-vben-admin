package com.huang.backend.mqtt.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huang.backend.drone.entity.Drone;
import com.huang.backend.drone.repository.DroneRepository;
import com.huang.backend.mqtt.model.CommandResponse;
import com.huang.backend.mqtt.model.DroneTelemetryData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MQTT Subscriber Service that listens for drone telemetry data
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MqttSubscriberService implements MqttCallback {

    private final MqttClient mqttClient;
    private final TimeseriesService timeseriesService;
    private final DroneRepository droneRepository;
    private final ObjectMapper objectMapper;

    @Value("${mqtt.topics.telemetry:drones/+/telemetry}")
    private String telemetryTopic;
    
    @Value("${mqtt.topics.responses:drones/+/responses}")
    private String responsesTopic;
    
    private final AtomicBoolean reconnecting = new AtomicBoolean(false);
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    private static final Pattern DRONE_ID_PATTERN = Pattern.compile("drones/([^/]+)/telemetry");
    private static final Pattern RESPONSE_ID_PATTERN = Pattern.compile("drones/([^/]+)/responses");

    /**
     * Initialize the MQTT subscription after the bean is constructed
     */
    @PostConstruct
    public void init() {
        try {
            setupMqttClient();
            initialized.set(true);
        } catch (MqttException e) {
            // 初始化失败时记录错误并计划重试，但不抛出异常以允许应用继续启动
            log.error("MQTT初始化异常: {}", e.getMessage(), e);
        }
    }
    
    private void setupMqttClient() throws MqttException {
        try {
            // 如果断开连接，尝试重新连接
            if (!mqttClient.isConnected()) {
                log.info("尝试连接到MQTT代理: {}", mqttClient.getServerURI());
                mqttClient.connect();
                log.info("成功连接到MQTT代理");
            }
            
            mqttClient.setCallback(this);
            mqttClient.subscribe(telemetryTopic);
            log.info("已订阅MQTT主题: {}", telemetryTopic);
            
            mqttClient.subscribe(responsesTopic);
            log.info("已订阅MQTT命令响应主题: {}", responsesTopic);
        } catch (MqttException e) {
            log.error("MQTT配置失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 定时检查MQTT连接状态并在必要时重连
     */
    @Scheduled(fixedDelay = 60000) // 每60秒检查一次
    public void checkConnection() {
        if (!initialized.get()) {
            log.debug("MQTT服务尚未初始化，跳过连接检查");
            return;
        }
        
        if (!mqttClient.isConnected() && !reconnecting.get()) {
            reconnecting.set(true);
            try {
                log.info("检测到MQTT连接已断开，尝试重新连接");
                setupMqttClient();
                log.info("MQTT连接重连成功");
            } catch (MqttException e) {
                log.error("MQTT重连失败: {}", e.getMessage(), e);
            } finally {
                reconnecting.set(false);
            }
        }
    }

    /**
     * Clean up resources before the bean is destroyed
     */
    @PreDestroy
    public void cleanup() {
        try {
            if (mqttClient.isConnected()) {
                mqttClient.unsubscribe(telemetryTopic);
                log.info("已取消订阅MQTT主题: {}", telemetryTopic);
                mqttClient.unsubscribe(responsesTopic);
                log.info("已取消订阅MQTT命令响应主题: {}", responsesTopic);
                mqttClient.disconnect();
                log.info("已断开MQTT连接");
            }
            mqttClient.close();
        } catch (MqttException e) {
            log.error("取消MQTT订阅失败: {}", e.getMessage(), e);
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        log.error("MQTT连接丢失: {}", cause.getMessage(), cause);
        // 连接丢失时尝试重连
        if (!reconnecting.get()) {
            new Thread(() -> {
                reconnecting.set(true);
                try {
                    // 等待一段时间后重试，避免立即重连
                    Thread.sleep(5000);
                    log.info("尝试重连MQTT...");
                    setupMqttClient();
                } catch (Exception e) {
                    log.error("MQTT重连失败: {}", e.getMessage(), e);
                } finally {
                    reconnecting.set(false);
                }
            }).start();
        }
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        try {
            log.debug("收到MQTT消息，主题: {}, 内容: {}", topic, new String(message.getPayload()));
            
            if (topic.matches(telemetryTopic.replace("+", ".*"))) {
                handleTelemetryMessage(topic, message);
            } else if (topic.matches(responsesTopic.replace("+", ".*"))) {
                handleResponseMessage(topic, message);
            } else {
                log.warn("收到未知主题的MQTT消息: {}", topic);
            }
        } catch (Exception e) {
            log.error("处理MQTT消息失败: {}", e.getMessage(), e);
        }
    }

    private void handleTelemetryMessage(String topic, MqttMessage message) throws Exception {
        // Extract drone ID from topic
        String droneId = extractDroneId(topic, DRONE_ID_PATTERN);
        if (droneId == null) {
            log.warn("无法从遥测主题中提取无人机ID: {}", topic);
            return;
        }
        
        // Parse JSON payload
        DroneTelemetryData telemetryData = objectMapper.readValue(message.getPayload(), DroneTelemetryData.class);
        
        // Set drone ID from topic if not present in payload
        if (telemetryData.getDroneId() == null) {
            telemetryData.setDroneId(droneId);
        }
        
        // Set timestamp if not present in payload
        if (telemetryData.getTimestamp() == null) {
            telemetryData.setTimestamp(Instant.now());
        }
        
        // Store data in InfluxDB
        timeseriesService.writeTelemetryData(telemetryData);
        
        // Update drone's last heartbeat timestamp in PostgreSQL
        updateDroneHeartbeat(droneId);
    }
    
    private void handleResponseMessage(String topic, MqttMessage message) throws Exception {
        // Extract drone ID from topic
        String droneId = extractDroneId(topic, RESPONSE_ID_PATTERN);
        if (droneId == null) {
            log.warn("无法从响应主题中提取无人机ID: {}", topic);
            return;
        }
        
        // Parse JSON payload
        CommandResponse response = objectMapper.readValue(message.getPayload(), CommandResponse.class);
        
        // Log the command response
        log.info("收到无人机命令响应: 无人机={}, 命令ID={}, 状态={}", 
                droneId, response.getCommandId(), response.getStatus());
        
        // Additional processing of command responses could be added here
        // For example, updating command status in a database, notifying users via WebSocket, etc.
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // Not used for subscriber
    }
    
    /**
     * Extract drone ID from topic using regex
     * 
     * @param topic the MQTT topic
     * @param pattern the regex pattern to use
     * @return the extracted drone ID or null if not found
     */
    private String extractDroneId(String topic, Pattern pattern) {
        Matcher matcher = pattern.matcher(topic);
        return matcher.matches() ? matcher.group(1) : null;
    }
    
    /**
     * Update the drone's last heartbeat timestamp in the database
     * 
     * @param droneId the ID of the drone
     */
    private void updateDroneHeartbeat(String droneId) {
        try {
            Optional<Drone> droneOpt = droneRepository.findBySerialNumber(droneId);
            if (droneOpt.isPresent()) {
                Drone drone = droneOpt.get();
                ZonedDateTime now = ZonedDateTime.ofInstant(Instant.now(), ZoneId.systemDefault());
                drone.setLastHeartbeatAt(now);
                droneRepository.save(drone);
                log.debug("已更新无人机{}的最后心跳时间", droneId);
            } else {
                log.warn("未找到序列号为{}的无人机", droneId);
            }
        } catch (Exception e) {
            log.error("更新无人机心跳时间失败: {}", e.getMessage(), e);
        }
    }
} 