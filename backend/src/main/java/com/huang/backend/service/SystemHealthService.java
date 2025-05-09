package com.huang.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.CompositeHealth;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.boot.context.event.ApplicationReadyEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

/**
 * 系统健康状态服务
 * 提供REST API访问系统健康状态
 * 注意：启动时自检由SystemStartupValidator处理
 */
@Service
public class SystemHealthService {
    
    private static final Logger log = LoggerFactory.getLogger(SystemHealthService.class);

    @Autowired
    private HealthEndpoint healthEndpoint;
    
    /**
     * 获取当前系统健康状态
     */
    public Map<String, Object> getCurrentHealthStatus() {
        try {
            HealthComponent health = healthEndpoint.health();
            if (health instanceof Health) {
                return ((Health) health).getDetails();
            } else if (health instanceof CompositeHealth) {
                return Map.of("status", health.getStatus(),
                              "components", ((CompositeHealth) health).getComponents());
            } else {
                return Map.of("status", health.getStatus());
            }
        } catch (Exception e) {
            log.error("获取健康状态失败", e);
            return Map.of("status", "ERROR", 
                          "error", e.getMessage());
        }
    }
} 