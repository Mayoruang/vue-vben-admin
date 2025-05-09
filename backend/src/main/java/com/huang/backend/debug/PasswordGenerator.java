package com.huang.backend.debug;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/debug/password")
public class PasswordGenerator {
    
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    @GetMapping("/encode")
    public Map<String, String> encodePassword(@RequestParam String password) {
        String encoded = passwordEncoder.encode(password);
        
        Map<String, String> response = new HashMap<>();
        response.put("rawPassword", password);
        response.put("encodedPassword", encoded);
        
        return response;
    }
    
    @GetMapping("/verify")
    public Map<String, Object> verifyPassword(
            @RequestParam String rawPassword, 
            @RequestParam String encodedPassword) {
        
        boolean matches = passwordEncoder.matches(rawPassword, encodedPassword);
        
        Map<String, Object> response = new HashMap<>();
        response.put("rawPassword", rawPassword);
        response.put("encodedPassword", encodedPassword);
        response.put("matches", matches);
        
        return response;
    }
} 