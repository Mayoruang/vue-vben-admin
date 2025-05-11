package com.huang.backend.config;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.influxdb.client.InfluxDBClient;

import lombok.extern.slf4j.Slf4j;

/**
 * Service to check connections to external services
 */
@Slf4j
@Service
public class ConnectionCheckService {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private InfluxDBClient influxDBClient;

    @Value("${mqtt.broker.url}")
    private String mqttBrokerUrl;

    @Value("${mqtt.client.id:drone-backend-check}")
    private String mqttClientId;

    /**
     * 检查所有连接状态
     */
    public void checkAllConnections() {
        checkPostgreSQLConnection();
        checkInfluxDBConnection();
        checkEMQXConnection();
        
        log.info("所有服务连接检查完成");
    }

    /**
     * 检查 PostgreSQL 连接
     */
    private void checkPostgreSQLConnection() {
        log.info("检查 PostgreSQL 数据库连接...");
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(5)) {
                log.info("✅ PostgreSQL 连接成功");
            } else {
                log.error("❌ PostgreSQL 连接无效");
            }
        } catch (SQLException e) {
            log.error("❌ PostgreSQL 连接失败: {}", e.getMessage());
        }
    }

    /**
     * 检查 InfluxDB 连接
     */
    private void checkInfluxDBConnection() {
        log.info("检查 InfluxDB 连接...");
        try {
            boolean pingResult = influxDBClient.ping();
            if (pingResult) {
                log.info("✅ InfluxDB 连接成功");
            } else {
                log.error("❌ InfluxDB 连接失败：ping 未响应");
            }
        } catch (Exception e) {
            log.error("❌ InfluxDB 连接失败: {}", e.getMessage());
        }
    }

    /**
     * 检查 EMQX 连接
     */
    private void checkEMQXConnection() {
        log.info("检查 EMQX 连接...");
        MqttClient mqttClient = null;
        try {
            mqttClient = new MqttClient(mqttBrokerUrl, mqttClientId);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setConnectionTimeout(5);
            options.setAutomaticReconnect(false);
            options.setCleanSession(true);
            
            mqttClient.connect(options);
            
            if (mqttClient.isConnected()) {
                log.info("✅ EMQX 连接成功");
            } else {
                log.error("❌ EMQX 连接失败");
            }
        } catch (MqttException e) {
            log.error("❌ EMQX 连接失败: {}", e.getMessage());
        } finally {
            if (mqttClient != null && mqttClient.isConnected()) {
                try {
                    mqttClient.disconnect();
                    mqttClient.close();
                } catch (MqttException e) {
                    log.error("关闭 MQTT 连接异常: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * Check connection to MQTT broker
     * 
     * @return true if connection is successful
     */
    public boolean checkMqttConnection() {
        // Implementation will be added later
        return true;
    }
    
    /**
     * Check connection to InfluxDB
     * 
     * @return true if connection is successful
     */
    public boolean checkInfluxDbConnection() {
        // Implementation will be added later
        return true;
    }
} 