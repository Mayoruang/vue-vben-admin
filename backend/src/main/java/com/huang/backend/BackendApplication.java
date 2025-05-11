package com.huang.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.huang.backend.config.ConnectionCheckService;

@Slf4j
@SpringBootApplication
@EnableScheduling
public class BackendApplication {

    @Autowired
    private ConnectionCheckService connectionCheckService;

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void checkConnections() {
        log.info("应用启动完成，开始检查服务连接状态...");
        connectionCheckService.checkAllConnections();
    }
}
