package com.huang.backend.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller for web pages (non-API endpoints)
 */
@Controller
public class WebPageController {

    /**
     * Serve the WebSocket test page
     * 
     * @return view name
     */
    @GetMapping("/websocket-test")
    public String websocketTest() {
        return "test-websocket";
    }
    
    /**
     * Serve the MQTT test page
     * 
     * @return view name
     */
    @GetMapping("/mqtt-test")
    public String mqttTest() {
        return "mqtt-test";
    }
} 