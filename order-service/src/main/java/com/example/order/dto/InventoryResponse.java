package com.example.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Inventory stock details returned from Inventory Client")
public class InventoryResponse {

    @Schema(description = "Stock Keeping Unit code", example = "IPHONE15")
    private String skuCode;

    @Schema(description = "Stock availability flag", example = "true")
    private boolean inStock;

    @Schema(description = "Quantity available", example = "25")
    private int quantity;

    @Schema(description = "Warehouse location", example = "San Francisco Hub")
    private String warehouse;

    public InventoryResponse() {}

    public InventoryResponse(String skuCode, boolean inStock, int quantity, String warehouse) {
        this.skuCode = skuCode;
        this.inStock = inStock;
        this.quantity = quantity;
        this.warehouse = warehouse;
    }

    public String getSkuCode() { return skuCode; }
    public void setSkuCode(String skuCode) { this.skuCode = skuCode; }

    public boolean isInStock() { return inStock; }
    public void setInStock(boolean inStock) { this.inStock = inStock; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getWarehouse() { return warehouse; }
    public void setWarehouse(String warehouse) { this.warehouse = warehouse; }
}
