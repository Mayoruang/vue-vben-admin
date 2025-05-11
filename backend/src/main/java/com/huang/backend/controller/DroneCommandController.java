package com.huang.backend.controller;

import com.huang.backend.mqtt.model.DroneCommand;
import com.huang.backend.mqtt.service.MqttPublisherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for sending commands to drones
 */
@Slf4j
@RestController
@RequestMapping("/api/drones")
@RequiredArgsConstructor
public class DroneCommandController {

    private final MqttPublisherService mqttPublisherService;

    /**
     * Send a command to a specific drone
     * 
     * @param droneId the ID of the drone
     * @param command the command to send
     * @return response entity with success/failure status
     */
    @PostMapping("/{droneId}/commands")
    public ResponseEntity<Map<String, Object>> sendCommand(
            @PathVariable String droneId,
            @RequestBody DroneCommand command) {
        
        log.info("收到发送命令请求到无人机: {}, 命令类型: {}", droneId, command.getType());
        
        // Set the droneId in the command
        command.setDroneId(droneId);
        
        // Publish the command to the drone
        boolean result = mqttPublisherService.publishCommand(droneId, command);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", result);
        response.put("command", command);
        
        if (result) {
            log.info("命令成功发送到无人机: {}", droneId);
            return ResponseEntity.ok(response);
        } else {
            log.warn("命令发送失败到无人机: {}", droneId);
            return ResponseEntity.status(503).body(response);
        }
    }
    
    /**
     * Send an RTL (Return to Launch) command to a drone
     * 
     * @param droneId the ID of the drone
     * @return response entity with success/failure status
     */
    @PostMapping("/{droneId}/rtl")
    public ResponseEntity<Map<String, Object>> sendRtlCommand(@PathVariable String droneId) {
        DroneCommand command = DroneCommand.builder()
                .droneId(droneId)
                .type(DroneCommand.CommandType.RTL)
                .build();
        
        boolean result = mqttPublisherService.publishCommand(droneId, command);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", result);
        response.put("command", command);
        
        if (result) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(503).body(response);
        }
    }
    
    /**
     * Send a LAND command to a drone
     * 
     * @param droneId the ID of the drone
     * @return response entity with success/failure status
     */
    @PostMapping("/{droneId}/land")
    public ResponseEntity<Map<String, Object>> sendLandCommand(@PathVariable String droneId) {
        DroneCommand command = DroneCommand.builder()
                .droneId(droneId)
                .type(DroneCommand.CommandType.LAND)
                .build();
        
        boolean result = mqttPublisherService.publishCommand(droneId, command);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", result);
        response.put("command", command);
        
        if (result) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(503).body(response);
        }
    }
    
    /**
     * Send a TAKEOFF command to a drone
     * 
     * @param droneId the ID of the drone
     * @param altitude the altitude to takeoff to (in meters)
     * @return response entity with success/failure status
     */
    @PostMapping("/{droneId}/takeoff")
    public ResponseEntity<Map<String, Object>> sendTakeoffCommand(
            @PathVariable String droneId,
            @RequestParam(defaultValue = "10.0") double altitude) {
        
        Map<String, Object> params = new HashMap<>();
        params.put("altitude", altitude);
        
        DroneCommand command = DroneCommand.builder()
                .droneId(droneId)
                .type(DroneCommand.CommandType.TAKEOFF)
                .parameters(params)
                .build();
        
        boolean result = mqttPublisherService.publishCommand(droneId, command);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", result);
        response.put("command", command);
        
        if (result) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(503).body(response);
        }
    }
    
    /**
     * Send a GOTO command to a drone
     * 
     * @param droneId the ID of the drone
     * @param latitude the latitude to go to
     * @param longitude the longitude to go to
     * @param altitude the altitude to maintain (in meters)
     * @return response entity with success/failure status
     */
    @PostMapping("/{droneId}/goto")
    public ResponseEntity<Map<String, Object>> sendGotoCommand(
            @PathVariable String droneId,
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = "10.0") double altitude) {
        
        Map<String, Object> params = new HashMap<>();
        params.put("latitude", latitude);
        params.put("longitude", longitude);
        params.put("altitude", altitude);
        
        DroneCommand command = DroneCommand.builder()
                .droneId(droneId)
                .type(DroneCommand.CommandType.GOTO)
                .parameters(params)
                .build();
        
        boolean result = mqttPublisherService.publishCommand(droneId, command);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", result);
        response.put("command", command);
        
        if (result) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(503).body(response);
        }
    }
} 