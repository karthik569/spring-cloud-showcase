package com.example.order.controller;

import com.example.order.client.InventoryClient;
import com.example.order.dto.InventoryResponse;
import com.example.order.dto.OrderRequest;
import com.example.order.dto.OrderResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/orders")
@RefreshScope
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final InventoryClient inventoryClient;
    private final Map<String, OrderResponse> ordersDb = new ConcurrentHashMap<>();

    @Value("${app.greeting:Default Order Greeting}")
    private String greeting = "Default Order Greeting";

    @Value("${app.discount-percentage:0}")
    private double discountPercentage = 0.0;

    public OrderController(InventoryClient inventoryClient) {
        this.inventoryClient = inventoryClient;
    }

    @GetMapping("/config-info")
    public Map<String, Object> getConfigInfo() {
        return Map.of(
                "greeting", greeting,
                "discountPercentage", discountPercentage,
                "service", "order-service"
        );
    }

    @GetMapping("/all")
    public Collection<OrderResponse> getAllOrders() {
        return ordersDb.values();
    }

    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(@RequestBody OrderRequest request) {
        log.info("Processing order for SKU={} Quantity={}", request.getSkuCode(), request.getQuantity());

        // 1. Call Inventory Service via Declarative OpenFeign client
        InventoryResponse inventory = inventoryClient.checkStock(request.getSkuCode());

        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        double baseTotal = request.getPrice() * request.getQuantity();
        double discountAmount = (baseTotal * discountPercentage) / 100.0;
        double finalTotal = baseTotal - discountAmount;

        if (!inventory.isInStock() || inventory.getQuantity() < request.getQuantity()) {
            OrderResponse failedOrder = new OrderResponse(
                    orderId,
                    request.getSkuCode(),
                    request.getQuantity(),
                    0.0,
                    0.0,
                    "REJECTED_OUT_OF_STOCK",
                    inventory.getWarehouse(),
                    "Insufficient stock or inventory service fallback triggered"
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(failedOrder);
        }

        OrderResponse placedOrder = new OrderResponse(
                orderId,
                request.getSkuCode(),
                request.getQuantity(),
                finalTotal,
                discountAmount,
                "CONFIRMED",
                inventory.getWarehouse(),
                "Order placed successfully with " + discountPercentage + "% centralized discount applied"
        );

        ordersDb.put(orderId, placedOrder);
        log.info("Order successfully created: {}", orderId);
        return ResponseEntity.status(HttpStatus.CREATED).body(placedOrder);
    }

    @PostMapping("/simulate-circuit-breaker")
    public ResponseEntity<Map<String, Object>> simulateCircuitBreaker(
            @RequestParam(defaultValue = "IPHONE15") String skuCode,
            @RequestParam(defaultValue = "fail") String mode) {

        log.info("Simulating circuit breaker call with mode={} for sku={}", mode, skuCode);
        try {
            if ("slow".equalsIgnoreCase(mode)) {
                InventoryResponse resp = inventoryClient.slowStockCheck(skuCode, 4000);
                return ResponseEntity.ok(Map.of("status", "SUCCESS", "response", resp));
            } else {
                inventoryClient.failStockCheck(skuCode);
                return ResponseEntity.ok(Map.of("status", "SUCCESS"));
            }
        } catch (Exception ex) {
            log.error("Exception caught during resilience simulation: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                    "status", "FALLBACK_CAUGHT",
                    "error", ex.getMessage()
            ));
        }
    }
}
