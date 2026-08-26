package com.example.order.client;

import com.example.order.dto.InventoryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "inventory-service", fallback = InventoryFallback.class)
public interface InventoryClient {

    @GetMapping("/api/inventory/{skuCode}")
    InventoryResponse checkStock(@PathVariable("skuCode") String skuCode);

    @GetMapping("/api/inventory/slow/{skuCode}")
    InventoryResponse slowStockCheck(@PathVariable("skuCode") String skuCode, @RequestParam("delayMs") long delayMs);

    @GetMapping("/api/inventory/fail/{skuCode}")
    void failStockCheck(@PathVariable("skuCode") String skuCode);
}
