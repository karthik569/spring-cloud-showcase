package com.example.order.client;

import com.example.order.dto.InventoryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class InventoryFallback implements InventoryClient {

    private static final Logger log = LoggerFactory.getLogger(InventoryFallback.class);

    @Override
    public InventoryResponse checkStock(String skuCode) {
        log.warn("[FEIGN-FALLBACK] Inventory service is down or failing for sku={}. Executing fallback response.", skuCode);
        return new InventoryResponse(skuCode, false, 0, "FALLBACK_CACHE_WAREHOUSE (Inventory Service Unavailable)");
    }

    @Override
    public InventoryResponse slowStockCheck(String skuCode, long delayMs) {
        log.warn("[FEIGN-FALLBACK] Inventory service timed out for slowStockCheck sku={}", skuCode);
        return new InventoryResponse(skuCode, false, 0, "FALLBACK_TIMEOUT_WAREHOUSE");
    }

    @Override
    public void failStockCheck(String skuCode) {
        log.warn("[FEIGN-FALLBACK] Inventory service failed explicitly for sku={}", skuCode);
    }
}
