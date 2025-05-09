package com.huang.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import com.influxdb.client.InfluxDBClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class HealthCheckConfig {
    
    private static final Logger log = LoggerFactory.getLogger(HealthCheckConfig.class);

    @Value("${mqtt.broker.url}")
    private String mqttBrokerUrl;
    
    @Value("${influxdb.url}")
    private String influxdbUrl;
    
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * InfluxDB健康检查器
     * 注: Spring Boot已经自动配置了数据库连接健康检查(db)，因此不需要重复添加PostgreSQL检查
     */
    @Bean
    public HealthIndicator influxdbHealthIndicator(InfluxDBClient influxDBClient) {
        return () -> {
            String serviceName = "influxdb";
            try {
                log.debug("执行InfluxDB健康检查...");
                boolean isHealthy = influxDBClient.ping();
                if (isHealthy) {
                    return Health.up()
                        .withDetail("service", serviceName)
                        .withDetail("url", influxdbUrl)
                        .withDetail("status", "连接成功")
                        .build();
                } else {
                    return Health.down()
                        .withDetail("service", serviceName)
                        .withDetail("url", influxdbUrl)
                        .withDetail("status", "连接失败")
                        .build();
                }
            } catch (Exception e) {
                log.warn("InfluxDB健康检查失败: {}", e.getMessage());
                return Health.down()
                    .withDetail("service", serviceName)
                    .withDetail("url", influxdbUrl)
                    .withDetail("status", "连接失败")
                    .withDetail("error", e.getMessage())
                    .build();
            }
        };
    }

    /**
     * EMQX消息代理健康检查器
     */
    @Bean
    public HealthIndicator emqxHealthIndicator() {
        return () -> {
            String serviceName = "emqx";
            MqttClient mqttClient = null;
            try {
                log.debug("执行EMQX健康检查...");
                String clientId = "health-checker-" + System.currentTimeMillis();
                
                mqttClient = new MqttClient(mqttBrokerUrl, clientId, new MemoryPersistence());
                MqttConnectOptions options = new MqttConnectOptions();
                options.setConnectionTimeout(5);
                options.setAutomaticReconnect(false);
                options.setCleanSession(true);
                
                mqttClient.connect(options);
                boolean isConnected = mqttClient.isConnected();
                
                if (isConnected) {
                    return Health.up()
                        .withDetail("service", serviceName)
                        .withDetail("broker", mqttBrokerUrl)
                        .withDetail("clientId", clientId)
                        .withDetail("status", "连接成功")
                        .build();
                } else {
                    return Health.down()
                        .withDetail("service", serviceName)
                        .withDetail("broker", mqttBrokerUrl)
                        .withDetail("status", "连接失败")
                        .build();
                }
            } catch (MqttException e) {
                log.warn("EMQX健康检查失败: {}", e.getMessage());
                return Health.down()
                    .withDetail("service", serviceName)
                    .withDetail("broker", mqttBrokerUrl)
                    .withDetail("status", "连接失败")
                    .withDetail("error", e.getMessage())
                    .build();
            } finally {
                if (mqttClient != null && mqttClient.isConnected()) {
                    try {
                        mqttClient.disconnect();
                        mqttClient.close();
                    } catch (MqttException e) {
                        log.warn("关闭MQTT客户端连接失败", e);
                    }
                }
            }
        };
    }
} 