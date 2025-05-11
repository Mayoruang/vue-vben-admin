package com.huang.backend.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;

/**
 * Test configuration to provide mock services for testing
 */
@TestConfiguration
public class TestConfig {

    /**
     * Mock connection check service for tests
     */
    @MockBean
    private ConnectionCheckService connectionCheckService;
} 