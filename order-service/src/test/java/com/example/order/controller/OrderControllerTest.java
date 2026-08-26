package com.example.order.controller;

import com.example.order.client.InventoryClient;
import com.example.order.dto.InventoryResponse;
import com.example.order.dto.OrderRequest;
import com.example.order.dto.OrderResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class OrderControllerTest {

    private OrderController orderController;
    private StubInventoryClient stubInventoryClient;

    static class StubInventoryClient implements InventoryClient {
        private boolean inStock = true;
        private int quantity = 10;

        public void setStock(boolean inStock, int quantity) {
            this.inStock = inStock;
            this.quantity = quantity;
        }

        @Override
        public InventoryResponse checkStock(String skuCode) {
            return new InventoryResponse(skuCode, inStock, quantity, "Warehouse-West-Zone-A");
        }

        @Override
        public InventoryResponse slowStockCheck(String skuCode, long delayMs) {
            return checkStock(skuCode);
        }

        @Override
        public void failStockCheck(String skuCode) {
            throw new RuntimeException("Simulated Failure");
        }
    }

    @BeforeEach
    void setUp() {
        stubInventoryClient = new StubInventoryClient();
        orderController = new OrderController(stubInventoryClient);
    }

    @Test
    void testPlaceOrderSuccess() {
        stubInventoryClient.setStock(true, 10);

        OrderRequest request = new OrderRequest("IPHONE15", 2, 1000.0);
        ResponseEntity<OrderResponse> response = orderController.placeOrder(request);

        assertNotNull(response);
        assertEquals(201, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("CONFIRMED", response.getBody().getStatus());
        assertEquals(2000.0, response.getBody().getTotalPrice());
    }

    @Test
    void testPlaceOrderOutOfStock() {
        stubInventoryClient.setStock(false, 0);

        OrderRequest request = new OrderRequest("IPHONE15", 1, 1000.0);
        ResponseEntity<OrderResponse> response = orderController.placeOrder(request);

        assertNotNull(response);
        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("REJECTED_OUT_OF_STOCK", response.getBody().getStatus());
    }
}
