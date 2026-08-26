package com.example.gateway.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FallbackControllerTest {

    private final FallbackController fallbackController = new FallbackController();

    @Test
    void testOrderServiceFallbackResponse() {
        ResponseEntity<Map<String, Object>> response = fallbackController.orderServiceFallback();
        assertNotNull(response);
        assertEquals(503, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("FALLBACK_TRIGGERED", response.getBody().get("status"));
        assertTrue(response.getBody().containsKey("message"));
        assertTrue(response.getBody().containsKey("timestamp"));
    }
}
