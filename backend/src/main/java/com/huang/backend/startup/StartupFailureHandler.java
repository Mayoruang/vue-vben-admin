package com.huang.backend.startup;

import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 启动失败处理器
 * 当启动自检发现问题时执行适当的措施
 * 默认情况下不启用，设置 spring.profiles.active=failFast 来启用严格失败模式
 */
@Component
@Profile("failFast")
public class StartupFailureHandler implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger log = LoggerFactory.getLogger(StartupFailureHandler.class);
    
    private final SystemStartupValidator startupValidator;
    private final Environment environment;

    public StartupFailureHandler(SystemStartupValidator startupValidator, Environment environment) {
        this.startupValidator = startupValidator;
        this.environment = environment;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // 应用程序上下文刷新后检查启动验证结果
        if (!startupValidator.isAllServicesConnected()) {
            boolean strictMode = isStrictMode();
            
            log.error("系统启动自检发现依赖服务连接异常!");
            
            if (strictMode) {
                log.error("严格启动模式已启用，系统将退出...");
                // 给日志一些时间写入
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                System.exit(1);
            } else {
                log.warn("系统将继续运行，但某些功能可能不可用");
                log.warn("设置 spring.profiles.active=failFast 可启用严格失败模式");
            }
        }
    }
    
    private boolean isStrictMode() {
        return environment.matchesProfiles("failFast");
    }
} 