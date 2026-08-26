package com.example.order.integration;

import com.example.order.client.InventoryClient;
import com.example.order.controller.OrderController;
import com.example.order.dto.InventoryResponse;
import com.example.order.dto.OrderRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class OrderIntegrationTest {

    private MockMvc mockMvc;
    private StubInventoryClient stubClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    static class StubInventoryClient implements InventoryClient {
        @Override
        public InventoryResponse checkStock(String skuCode) {
            return new InventoryResponse(skuCode, true, 20, "Warehouse-West-Zone-A");
        }

        @Override
        public InventoryResponse slowStockCheck(String skuCode, long delayMs) {
            return checkStock(skuCode);
        }

        @Override
        public void failStockCheck(String skuCode) {
            throw new RuntimeException("Simulated failure");
        }
    }

    @BeforeEach
    void setUp() {
        stubClient = new StubInventoryClient();
        OrderController controller = new OrderController(stubClient);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void testPlaceOrderEndpointFlow() throws Exception {
        OrderRequest request = new OrderRequest("IPHONE15", 2, 999.99);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.orderId").isNotEmpty())
                .andExpect(jsonPath("$.skuCode").value("IPHONE15"))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void testConfigInfoEndpoint() throws Exception {
        mockMvc.perform(get("/api/orders/config-info")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("order-service"));
    }
}
