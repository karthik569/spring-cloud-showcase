package com.example.inventory.controller;

import com.example.inventory.dto.InventoryResponse;
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

    @GetMapping("/config-info")
    public Map<String, String> getConfigInfo() {
        return Map.of(
                "greeting", greeting,
                "warehouseLocation", warehouseLocation,
                "service", "inventory-service"
        );
    }

    @GetMapping("/{skuCode}")
    public ResponseEntity<InventoryResponse> checkStock(@PathVariable String skuCode) {
        log.info("Checking inventory stock for skuCode={}", skuCode);
        int qty = stockStore.getOrDefault(skuCode.toUpperCase(), 0);
        boolean inStock = qty > 0;
        return ResponseEntity.ok(new InventoryResponse(skuCode, inStock, qty, warehouseLocation));
    }

    @GetMapping("/slow/{skuCode}")
    public ResponseEntity<InventoryResponse> slowStockCheck(
            @PathVariable String skuCode,
            @RequestParam(defaultValue = "3000") long delayMs) throws InterruptedException {
        log.info("Simulating slow inventory response with delay of {} ms for sku={}", delayMs, skuCode);
        Thread.sleep(delayMs);
        int qty = stockStore.getOrDefault(skuCode.toUpperCase(), 5);
        return ResponseEntity.ok(new InventoryResponse(skuCode, true, qty, warehouseLocation));
    }

    @GetMapping("/fail/{skuCode}")
    public ResponseEntity<Void> failStockCheck(@PathVariable String skuCode) {
        log.warn("Simulating 500 error in inventory service for sku={}", skuCode);
        throw new RuntimeException("Simulated Inventory Service failure for testing Circuit Breaker!");
    }
}
