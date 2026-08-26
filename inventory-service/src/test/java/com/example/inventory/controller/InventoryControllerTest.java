package com.example.inventory.controller;

import com.example.inventory.dto.InventoryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class InventoryControllerTest {

    private InventoryController inventoryController;

    @BeforeEach
    void setUp() {
        inventoryController = new InventoryController();
    }

    @Test
    void testCheckStockAvailable() {
        ResponseEntity<InventoryResponse> response = inventoryController.checkStock("IPHONE15");
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isInStock());
        assertEquals(25, response.getBody().getQuantity());
    }

    @Test
    void testCheckStockOutOfStock() {
        ResponseEntity<InventoryResponse> response = inventoryController.checkStock("AIRPODS-PRO");
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isInStock());
        assertEquals(0, response.getBody().getQuantity());
    }

    @Test
    void testCheckStockUnknownItem() {
        ResponseEntity<InventoryResponse> response = inventoryController.checkStock("NON_EXISTENT_ITEM");
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isInStock());
        assertEquals(0, response.getBody().getQuantity());
    }
}
