package com.example.inventory.integration;

import com.example.inventory.controller.InventoryController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class InventoryIntegrationTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        InventoryController controller = new InventoryController();
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void testCheckStockEndpointReturnsJson() throws Exception {
        mockMvc.perform(get("/api/inventory/IPHONE15")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.skuCode").value("IPHONE15"))
                .andExpect(jsonPath("$.inStock").value(true))
                .andExpect(jsonPath("$.quantity").value(25));
    }

    @Test
    void testConfigInfoEndpoint() throws Exception {
        mockMvc.perform(get("/api/inventory/config-info")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("inventory-service"));
    }
}
