package com.example.inventory.dto;

public class InventoryResponse {
    private String skuCode;
    private boolean inStock;
    private int quantity;
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
