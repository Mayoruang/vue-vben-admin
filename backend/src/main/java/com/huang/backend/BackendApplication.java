package com.huang.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 无人机管理系统后端应用
 */
@SpringBootApplication
public class BackendApplication {
    
    private static final Logger log = LoggerFactory.getLogger(BackendApplication.class);

    public static void main(String[] args) {
        log.info("========================================================");
        log.info("=            无人机管理系统后端启动中                    =");
        log.info("========================================================");
        log.info("正在初始化系统组件...");
        
        try {
            ConfigurableApplicationContext context = SpringApplication.run(BackendApplication.class, args);
            
            log.info("无人机管理系统后端启动完成");
            log.info("系统将执行启动自检，验证所有依赖服务的连接状态");
            log.info("请查看下方的 [系统启动自检结果] 确认系统状态");
            
            // 注册JVM关闭钩子
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("========================================================");
                log.info("=            无人机管理系统后端正在关闭                  =");
                log.info("========================================================");
            }));
            
        } catch (Exception e) {
            log.error("无人机管理系统后端启动失败", e);
            System.exit(1);
        }
    }
}
