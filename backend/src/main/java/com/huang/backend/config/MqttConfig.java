package com.huang.backend.config;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class MqttConfig {

    @Value("${mqtt.broker.url}")
    private String brokerUrl;
    
    @Value("${mqtt.broker.client-id:#{null}}")
    private String clientId;
    
    @Value("${mqtt.broker.username:#{null}}")
    private String username;
    
    @Value("${mqtt.broker.password:#{null}}")
    private String password;
    
    @Value("${mqtt.broker.timeout:10}")
    private int timeout;
    
    @Value("${mqtt.broker.keep-alive:60}")
    private int keepAlive;
    
    @Value("${mqtt.broker.auto-reconnect:true}")
    private boolean autoReconnect;

    @Bean
    public MqttClient mqttClient() throws MqttException {
        // 如果配置中没有指定客户端ID，则生成一个随机ID
        String effectiveClientId = (clientId == null || clientId.isEmpty()) 
            ? "drone-backend-" + System.currentTimeMillis()
            : clientId;
        
        log.info("初始化 MQTT 客户端连接: {}, 客户端ID: {}", brokerUrl, effectiveClientId);
        
        MqttClient mqttClient = new MqttClient(brokerUrl, effectiveClientId, new MemoryPersistence());
        MqttConnectOptions connectOptions = new MqttConnectOptions();
        connectOptions.setCleanSession(true);
        connectOptions.setAutomaticReconnect(autoReconnect);
        connectOptions.setConnectionTimeout(timeout);
        connectOptions.setKeepAliveInterval(keepAlive);
        
        // 如果配置了用户名和密码，则设置认证信息
        if (username != null && !username.isEmpty()) {
            connectOptions.setUserName(username);
            if (password != null) {
                connectOptions.setPassword(password.toCharArray());
            }
        }
        
        mqttClient.connect(connectOptions);
        return mqttClient;
    }
} 