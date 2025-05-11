package com.huang.backend.config;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
@Configuration
public class MqttConfig {

    @Value("${mqtt.broker.url}")
    private String brokerUrl;

    @Value("${mqtt.client.id:drone-backend}")
    private String clientIdPrefix;

    @Value("${mqtt.username:#{null}}")
    private String username;

    @Value("${mqtt.password:#{null}}")
    private String password;

    @Bean
    public MqttConnectOptions mqttConnectOptions() {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setAutomaticReconnect(true);
        options.setConnectionTimeout(30);
        options.setKeepAliveInterval(60);
        options.setMaxInflight(100);
        
        if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
            options.setUserName(username);
            options.setPassword(password.toCharArray());
        }
        
        return options;
    }

    @Bean
    public MqttClient mqttClient(MqttConnectOptions options) {
        try {
            String clientId = clientIdPrefix + "-" + UUID.randomUUID().toString().substring(0, 8);
            log.info("创建MQTT客户端: ID={}, Broker={}", clientId, brokerUrl);
            
            MqttClient client = new MqttClient(brokerUrl, clientId);
            client.connect(options);
            log.info("已连接到 MQTT 代理: {}", brokerUrl);
            return client;
        } catch (MqttException e) {
            log.error("MQTT 客户端创建失败: {}", e.getMessage(), e);
            throw new RuntimeException("无法创建 MQTT 客户端", e);
        }
    }
} 