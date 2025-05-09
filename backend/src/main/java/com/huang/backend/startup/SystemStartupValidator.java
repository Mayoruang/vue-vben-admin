package com.huang.backend.startup;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApi;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;

import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.DataSource;

/**
 * 系统启动验证器
 * 在系统启动时检查所有必要的依赖服务是否可连接且功能正常
 */
@Component
public class SystemStartupValidator implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SystemStartupValidator.class);
    
    @Autowired
    private DataSource dataSource;
    
    @Autowired(required = false)
    private InfluxDBClient influxDBClient;
    
    @Value("${influxdb.url}")
    private String influxdbUrl;
    
    @Value("${influxdb.bucket}")
    private String influxdbBucket;
    
    @Value("${influxdb.org}")
    private String influxdbOrg;
    
    @Value("${mqtt.broker.url}")
    private String mqttBrokerUrl;
    
    // 存储启动验证结果
    private final AtomicBoolean allServicesConnected = new AtomicBoolean(true);
    private final Map<String, Boolean> servicesStatus = new LinkedHashMap<>();
    
    // 存储详细检查结果
    private final Map<String, Map<String, Boolean>> detailedChecks = new LinkedHashMap<>();
    
    /**
     * 获取所有服务连接状态
     * @return 如果所有服务连接正常则返回true
     */
    public boolean isAllServicesConnected() {
        return allServicesConnected.get();
    }
    
    /**
     * 获取各个服务的连接状态
     * @return 服务状态Map，服务名称 -> 是否连接
     */
    public Map<String, Boolean> getServicesStatus() {
        return new LinkedHashMap<>(servicesStatus);
    }
    
    /**
     * 获取详细检查结果
     * @return 详细检查结果Map，服务名称 -> (检查项 -> 是否通过)
     */
    public Map<String, Map<String, Boolean>> getDetailedChecks() {
        return new LinkedHashMap<>(detailedChecks);
    }
    
    @Override
    public void run(String... args) {
        log.info("==================== 系统启动自检 ====================");
        log.info("开始检查关键服务连接状态和功能...");
        
        // 清空之前的状态
        servicesStatus.clear();
        detailedChecks.clear();
        
        // 1. 检查PostgreSQL连接
        checkPostgresConnection(servicesStatus, detailedChecks);
        
        // 2. 检查InfluxDB连接
        checkInfluxDBConnection(servicesStatus, detailedChecks);
        
        // 3. 检查MQTT代理连接
        checkMqttConnection(servicesStatus, detailedChecks);
        
        // 汇总结果
        boolean allConnected = !servicesStatus.containsValue(false);
        this.allServicesConnected.set(allConnected);
        
        // 构建报告
        StringBuilder report = new StringBuilder();
        report.append("\n==================== 系统启动自检结果 ====================");
        
        servicesStatus.forEach((service, isConnected) -> {
            String status = isConnected ? "✅ 连接正常" : "❌ 连接失败";
            report.append(String.format("\n%-12s: %s", service, status));
            
            // 添加详细检查信息
            if (detailedChecks.containsKey(service)) {
                Map<String, Boolean> serviceChecks = detailedChecks.get(service);
                serviceChecks.forEach((checkName, checkPassed) -> {
                    String checkStatus = checkPassed ? "✓" : "✗";
                    report.append(String.format("\n  %-18s: %s", checkName, checkStatus));
                });
            }
        });
        
        report.append("\n");
        report.append("\n总体状态: ").append(allConnected ? 
                "✅ 所有服务连接正常且功能可用" : 
                "❌ 部分服务连接异常或功能不可用");
        report.append("\n===========================================================");
        
        log.info(report.toString());
        
        if (!allConnected) {
            log.warn("系统自检发现部分服务异常，请检查配置或服务状态！");
        } else {
            log.info("系统自检完成，所有依赖服务连接正常且功能可用！");
        }
    }
    
    /**
     * 检查PostgreSQL连接和基本读写功能
     */
    @Transactional
    private void checkPostgresConnection(Map<String, Boolean> servicesStatus, 
                                         Map<String, Map<String, Boolean>> detailedChecks) {
        Map<String, Boolean> checks = new LinkedHashMap<>();
        detailedChecks.put("PostgreSQL", checks);
        boolean overallStatus = true;
        
        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            
            // 1. 基本连接检查
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            boolean connectionOk = result != null && result == 1;
            checks.put("基本连接", connectionOk);
            overallStatus = overallStatus && connectionOk;
            
            if (connectionOk) {
                // 2. 创建临时测试表
                String testTableName = "system_check_" + System.currentTimeMillis();
                try {
                    jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS " + testTableName + 
                                         " (id VARCHAR(36) PRIMARY KEY, test_value VARCHAR(100))");
                    checks.put("创建表", true);
                    
                    // 3. 测试写入数据
                    String id = UUID.randomUUID().toString();
                    String testValue = "Connection Test " + Instant.now();
                    jdbcTemplate.update("INSERT INTO " + testTableName + " (id, test_value) VALUES (?, ?)", 
                                        id, testValue);
                    checks.put("写入数据", true);
                    
                    // 4. 测试读取数据
                    String readValue = jdbcTemplate.queryForObject(
                            "SELECT test_value FROM " + testTableName + " WHERE id = ?", 
                            String.class, id);
                    boolean readOk = testValue.equals(readValue);
                    checks.put("读取数据", readOk);
                    overallStatus = overallStatus && readOk;
                    
                    // 5. 删除测试表
                    jdbcTemplate.execute("DROP TABLE " + testTableName);
                    checks.put("删除表", true);
                } catch (Exception e) {
                    log.error("PostgreSQL 数据操作测试失败: {}", e.getMessage());
                    // 标记失败的操作
                    if (!checks.containsKey("创建表")) checks.put("创建表", false);
                    if (!checks.containsKey("写入数据")) checks.put("写入数据", false);
                    if (!checks.containsKey("读取数据")) checks.put("读取数据", false);
                    if (!checks.containsKey("删除表")) checks.put("删除表", false);
                    overallStatus = false;
                }
            } else {
                log.warn("PostgreSQL 基本连接失败: 查询未返回预期结果");
                checks.put("创建表", false);
                checks.put("写入数据", false);
                checks.put("读取数据", false);
                checks.put("删除表", false);
                overallStatus = false;
            }
        } catch (Exception e) {
            log.error("PostgreSQL 连接失败: {}", e.getMessage());
            checks.put("基本连接", false);
            checks.put("创建表", false);
            checks.put("写入数据", false);
            checks.put("读取数据", false);
            checks.put("删除表", false);
            overallStatus = false;
        }
        
        // 设置整体状态
        servicesStatus.put("PostgreSQL", overallStatus);
        
        if (overallStatus) {
            log.debug("PostgreSQL 完整功能测试成功");
        } else {
            log.warn("PostgreSQL 完整功能测试失败，部分功能不可用");
        }
    }
    
    /**
     * 检查InfluxDB连接和基本写入功能
     */
    private void checkInfluxDBConnection(Map<String, Boolean> servicesStatus, 
                                         Map<String, Map<String, Boolean>> detailedChecks) {
        Map<String, Boolean> checks = new LinkedHashMap<>();
        detailedChecks.put("InfluxDB", checks);
        boolean overallStatus = true;
        
        if (influxDBClient == null) {
            servicesStatus.put("InfluxDB", false);
            checks.put("客户端配置", false);
            checks.put("基本连接", false);
            checks.put("写入数据", false);
            checks.put("读取数据", false);
            log.warn("InfluxDB 客户端未配置");
            return;
        }
        
        checks.put("客户端配置", true);
        
        try {
            // 1. 基本连接检查
            boolean pingResult = influxDBClient.ping();
            checks.put("基本连接", pingResult);
            overallStatus = overallStatus && pingResult;
            
            if (pingResult) {
                // 2. 测试写入数据
                try (WriteApi writeApi = influxDBClient.getWriteApi()) {
                    String measurementName = "system_check";
                    Point point = Point.measurement(measurementName)
                            .addTag("test_id", UUID.randomUUID().toString())
                            .addField("value", "Connection Test")
                            .time(Instant.now(), WritePrecision.NS);
                    
                    writeApi.writePoint(influxdbBucket, influxdbOrg, point);
                    checks.put("写入数据", true);
                    
                    // 3. 尝试读取最新数据 (简单验证API可用性)
                    // 注意：我们不进行实际的查询验证，因为写入有延迟
                    boolean queryApiWorks = influxDBClient.getQueryApi() != null;
                    checks.put("读取API", queryApiWorks);
                    overallStatus = overallStatus && queryApiWorks;
                    
                } catch (Exception e) {
                    log.error("InfluxDB 数据写入测试失败: {}", e.getMessage());
                    checks.put("写入数据", false);
                    checks.put("读取API", false);
                    overallStatus = false;
                }
            } else {
                log.warn("InfluxDB 基本连接失败: ping 返回 false");
                checks.put("写入数据", false);
                checks.put("读取API", false);
                overallStatus = false;
            }
        } catch (Exception e) {
            log.error("InfluxDB 连接失败: {}", e.getMessage());
            checks.put("基本连接", false);
            checks.put("写入数据", false);
            checks.put("读取API", false);
            overallStatus = false;
        }
        
        // 设置整体状态
        servicesStatus.put("InfluxDB", overallStatus);
        
        if (overallStatus) {
            log.debug("InfluxDB 功能测试成功: {}", influxdbUrl);
        } else {
            log.warn("InfluxDB 功能测试失败，部分功能不可用");
        }
    }
    
    /**
     * 检查MQTT连接以及消息发布订阅功能
     */
    private void checkMqttConnection(Map<String, Boolean> servicesStatus, 
                                     Map<String, Map<String, Boolean>> detailedChecks) {
        Map<String, Boolean> checks = new LinkedHashMap<>();
        detailedChecks.put("MQTT代理", checks);
        boolean overallStatus = true;
        
        MqttClient mqttClient = null;
        try {
            // 1. 基本连接测试
            String clientId = "startup-validator-" + System.currentTimeMillis();
            mqttClient = new MqttClient(mqttBrokerUrl, clientId, new MemoryPersistence());
            
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setConnectionTimeout(5);
            options.setAutomaticReconnect(false);
            
            mqttClient.connect(options);
            boolean connected = mqttClient.isConnected();
            checks.put("基本连接", connected);
            overallStatus = overallStatus && connected;
            
            if (connected) {
                // 2. 测试消息发布和订阅
                String testTopic = "system/check/" + UUID.randomUUID().toString();
                String testMessage = "System connectivity check: " + Instant.now();
                final CountDownLatch messageReceived = new CountDownLatch(1);
                final AtomicBoolean messageMatched = new AtomicBoolean(false);
                
                // 设置订阅
                try {
                    mqttClient.subscribe(testTopic, 1, new IMqttMessageListener() {
                        @Override
                        public void messageArrived(String topic, MqttMessage message) {
                            String receivedMsg = new String(message.getPayload());
                            if (testMessage.equals(receivedMsg)) {
                                messageMatched.set(true);
                            }
                            messageReceived.countDown();
                        }
                    });
                    checks.put("订阅主题", true);
                    
                    // 发布消息
                    MqttMessage message = new MqttMessage(testMessage.getBytes());
                    message.setQos(1);
                    mqttClient.publish(testTopic, message);
                    checks.put("发布消息", true);
                    
                    // 等待消息接收
                    boolean received = messageReceived.await(5, TimeUnit.SECONDS);
                    checks.put("接收消息", received && messageMatched.get());
                    overallStatus = overallStatus && received && messageMatched.get();
                    
                    // 取消订阅
                    mqttClient.unsubscribe(testTopic);
                    checks.put("取消订阅", true);
                    
                } catch (Exception e) {
                    log.error("MQTT 消息收发测试失败: {}", e.getMessage());
                    if (!checks.containsKey("订阅主题")) checks.put("订阅主题", false);
                    if (!checks.containsKey("发布消息")) checks.put("发布消息", false);
                    if (!checks.containsKey("接收消息")) checks.put("接收消息", false);
                    if (!checks.containsKey("取消订阅")) checks.put("取消订阅", false);
                    overallStatus = false;
                }
            } else {
                log.warn("MQTT代理连接失败: 客户端未连接");
                checks.put("订阅主题", false);
                checks.put("发布消息", false);
                checks.put("接收消息", false);
                checks.put("取消订阅", false);
                overallStatus = false;
            }
        } catch (Exception e) {
            log.error("MQTT代理连接失败: {}", e.getMessage());
            checks.put("基本连接", false);
            checks.put("订阅主题", false);
            checks.put("发布消息", false);
            checks.put("接收消息", false);
            checks.put("取消订阅", false);
            overallStatus = false;
        } finally {
            if (mqttClient != null && mqttClient.isConnected()) {
                try {
                    mqttClient.disconnect();
                    mqttClient.close();
                } catch (Exception e) {
                    log.warn("关闭MQTT客户端失败", e);
                }
            }
        }
        
        // 设置整体状态
        servicesStatus.put("MQTT代理", overallStatus);
        
        if (overallStatus) {
            log.debug("MQTT代理功能测试成功: {}", mqttBrokerUrl);
        } else {
            log.warn("MQTT代理功能测试失败，部分功能不可用");
        }
    }
} 