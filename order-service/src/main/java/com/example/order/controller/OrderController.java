package com.example.order.controller;

import com.example.order.client.InventoryClient;
import com.example.order.dto.InventoryResponse;
import com.example.order.dto.OrderRequest;
import com.example.order.dto.OrderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Order Management", description = "Operations for placing orders, reviewing order history, and simulating circuit breaker fallbacks")
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

    @Operation(summary = "Get configuration info", description = "Retrieves centralized greeting and discount percentage loaded dynamically from Spring Cloud Config Server.")
    @ApiResponse(responseCode = "200", description = "Configuration properties retrieved successfully")
    @GetMapping("/config-info")
    public Map<String, Object> getConfigInfo() {
        return Map.of(
                "greeting", greeting,
                "discountPercentage", discountPercentage,
                "service", "order-service"
        );
    }

    @Operation(summary = "List all orders", description = "Retrieves all customer orders placed in the system during current runtime.")
    @ApiResponse(responseCode = "200", description = "List of orders",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = OrderResponse.class))))
    @GetMapping("/all")
    public Collection<OrderResponse> getAllOrders() {
        return ordersDb.values();
    }

    @Operation(summary = "Place a new order", description = "Verifies stock in Inventory Service via OpenFeign client, applies centralized discount, and generates confirmed or rejected order.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Order placed and confirmed successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "400", description = "Order rejected due to insufficient stock or fallback response",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderResponse.class)))
    })
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

    @Operation(summary = "Simulate Resilience4j circuit breaker", description = "Triggers either delayed (slow) or failing (error) calls to inventory-service to verify OpenFeign fallback execution.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Simulated call processed (or handled by fallback) successfully"),
            @ApiResponse(responseCode = "503", description = "Service unavailable or fallback caught error")
    })
    @PostMapping("/simulate-circuit-breaker")
    public ResponseEntity<Map<String, Object>> simulateCircuitBreaker(
            @Parameter(description = "Product SKU code", example = "IPHONE15")
            @RequestParam(defaultValue = "IPHONE15") String skuCode,
            @Parameter(description = "Simulation mode: 'slow' for timeout or 'fail' for 500 error", example = "fail")
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
