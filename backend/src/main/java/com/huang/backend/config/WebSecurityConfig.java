package com.huang.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Web security configuration for the application
 * This disables the default authentication for all endpoints until we implement proper authentication
 */
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    /**
     * Configure HTTP security
     * For now, we disable CSRF and permit all requests without authentication
     * In a production environment, you would configure proper security measures
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Disable CSRF and permit all requests without authentication for now
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(requests -> requests
                // Permit WebSocket endpoints
                .requestMatchers("/ws/**").permitAll()
                // Permit all other requests for now
                .anyRequest().permitAll()
            );
        
        return http.build();
    }
} 