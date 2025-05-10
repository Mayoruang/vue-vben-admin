package com.huang.backend.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.influxdb.client.InfluxDBClient;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class HealthService {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private InfluxDBClient influxDBClient;

    @Autowired
    private MqttClient mqttClient;

    /**
     * 检查所有服务状态
     */
    public Map<String, Object> checkServices() {
        Map<String, Object> serviceStatus = new HashMap<>();
        
        serviceStatus.put("postgresql", checkPostgreSQLStatus());
        serviceStatus.put("influxdb", checkInfluxDBStatus());
        serviceStatus.put("emqx", checkEMQXStatus());
        
        return serviceStatus;
    }

    /**
     * 检查 PostgreSQL 状态
     */
    private Map<String, Object> checkPostgreSQLStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("service", "PostgreSQL");
        
        try (Connection connection = dataSource.getConnection()) {
            boolean isValid = connection.isValid(3);
            status.put("status", isValid ? "UP" : "DOWN");
            if (!isValid) {
                status.put("error", "数据库连接无效");
            }
        } catch (SQLException e) {
            status.put("status", "DOWN");
            status.put("error", e.getMessage());
            log.error("PostgreSQL 连接检查失败: {}", e.getMessage());
        }
        
        return status;
    }

    /**
     * 检查 InfluxDB 状态
     */
    private Map<String, Object> checkInfluxDBStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("service", "InfluxDB");
        
        try {
            boolean pingResult = influxDBClient.ping();
            status.put("status", pingResult ? "UP" : "DOWN");
            if (!pingResult) {
                status.put("error", "InfluxDB ping 失败");
            }
        } catch (Exception e) {
            status.put("status", "DOWN");
            status.put("error", e.getMessage());
            log.error("InfluxDB 连接检查失败: {}", e.getMessage());
        }
        
        return status;
    }

    /**
     * 检查 EMQX 状态
     */
    private Map<String, Object> checkEMQXStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("service", "EMQX");
        
        try {
            boolean connected = mqttClient.isConnected();
            status.put("status", connected ? "UP" : "DOWN");
            if (!connected) {
                try {
                    mqttClient.reconnect();
                    status.put("status", "RECOVERING");
                } catch (MqttException e) {
                    status.put("error", "MQTT 重连失败: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            status.put("status", "DOWN");
            status.put("error", e.getMessage());
            log.error("EMQX 连接检查失败: {}", e.getMessage());
        }
        
        return status;
    }
} 