package com.example.inventory.controller;

import com.example.inventory.dto.InventoryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/inventory")
@RefreshScope
@Tag(name = "Inventory Management", description = "Operations for inspecting product stock, warehouse allocation, and simulating resilience test scenarios")
public class InventoryController {

    private static final Logger log = LoggerFactory.getLogger(InventoryController.class);

    @Value("${app.greeting:Default Greeting}")
    private String greeting = "Default Greeting";

    @Value("${app.warehouse-location:Default Location}")
    private String warehouseLocation = "Default Location";

    private final Map<String, Integer> stockStore = new ConcurrentHashMap<>();

    public InventoryController() {
        stockStore.put("IPHONE15", 25);
        stockStore.put("MACBOOK-PRO", 10);
        stockStore.put("AIRPODS-PRO", 0);
    }

    @Operation(summary = "Get configuration info", description = "Retrieves centralized greeting and warehouse location supplied by Spring Cloud Config Server.")
    @ApiResponse(responseCode = "200", description = "Configuration properties retrieved successfully")
    @GetMapping("/config-info")
    public Map<String, String> getConfigInfo() {
        return Map.of(
                "greeting", greeting,
                "warehouseLocation", warehouseLocation,
                "service", "inventory-service"
        );
    }

    @Operation(summary = "Check product stock", description = "Checks the real-time warehouse stock level and availability for a specified SKU.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stock status successfully retrieved",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = InventoryResponse.class)))
    })
    @GetMapping("/{skuCode}")
    public ResponseEntity<InventoryResponse> checkStock(
            @Parameter(description = "Product SKU code to verify", example = "IPHONE15", required = true)
            @PathVariable String skuCode) {
        log.info("Checking inventory stock for skuCode={}", skuCode);
        int qty = stockStore.getOrDefault(skuCode.toUpperCase(), 0);
        boolean inStock = qty > 0;
        return ResponseEntity.ok(new InventoryResponse(skuCode, inStock, qty, warehouseLocation));
    }

    @Operation(summary = "Simulate slow inventory response", description = "Simulates artificial latency in inventory retrieval to verify timeout limits and circuit breaker behavior.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Delayed response completed successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = InventoryResponse.class)))
    })
    @GetMapping("/slow/{skuCode}")
    public ResponseEntity<InventoryResponse> slowStockCheck(
            @Parameter(description = "Product SKU code", example = "IPHONE15", required = true)
            @PathVariable String skuCode,
            @Parameter(description = "Simulated delay in milliseconds", example = "3000")
            @RequestParam(defaultValue = "3000") long delayMs) throws InterruptedException {
        log.info("Simulating slow inventory response with delay of {} ms for sku={}", delayMs, skuCode);
        Thread.sleep(delayMs);
        int qty = stockStore.getOrDefault(skuCode.toUpperCase(), 5);
        return ResponseEntity.ok(new InventoryResponse(skuCode, true, qty, warehouseLocation));
    }

    @Operation(summary = "Simulate inventory failure", description = "Throws a simulated RuntimeException (500) to trigger circuit breaker opening and fallback logic in consumer services.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "500", description = "Simulated inventory exception thrown")
    })
    @GetMapping("/fail/{skuCode}")
    public ResponseEntity<Void> failStockCheck(
            @Parameter(description = "Product SKU code", example = "IPHONE15", required = true)
            @PathVariable String skuCode) {
        log.warn("Simulating 500 error in inventory service for sku={}", skuCode);
        throw new RuntimeException("Simulated Inventory Service failure for testing Circuit Breaker!");
    }
}
