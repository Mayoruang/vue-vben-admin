package com.huang.backend.registration.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

/**
 * Controller for handling WebSocket messages
 */
@Controller
public class WebSocketController {

    /**
     * Simple echo endpoint for testing WebSocket connectivity
     *
     * @param message the message to echo
     * @return the echo response
     */
    @MessageMapping("/echo")
    @SendTo("/topic/echo")
    public String echo(String message) {
        return "[Server] Echo: " + message;
    }
} 