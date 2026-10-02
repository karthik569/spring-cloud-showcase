package com.example.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Details about product stock availability and fulfillment warehouse")
public class InventoryResponse {

    @Schema(description = "Stock Keeping Unit code", example = "IPHONE15", requiredMode = Schema.RequiredMode.REQUIRED)
    private String skuCode;

    @Schema(description = "Indicates whether the SKU is currently in stock", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean inStock;

    @Schema(description = "Current stock level quantity available in warehouse", example = "25", requiredMode = Schema.RequiredMode.REQUIRED)
    private int quantity;

    @Schema(description = "Name or location identifier of the fulfillment warehouse", example = "San Francisco Distribution Center", requiredMode = Schema.RequiredMode.REQUIRED)
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
