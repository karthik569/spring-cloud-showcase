package com.example.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request payload for placing a new order")
public class OrderRequest {

    @Schema(description = "Stock Keeping Unit code of the product to order", example = "IPHONE15", requiredMode = Schema.RequiredMode.REQUIRED)
    private String skuCode;

    @Schema(description = "Quantity of items to purchase", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    private int quantity;

    @Schema(description = "Base unit price of the item before discounts", example = "999.99", requiredMode = Schema.RequiredMode.REQUIRED)
    private double price;

    public OrderRequest() {}

    public OrderRequest(String skuCode, int quantity, double price) {
        this.skuCode = skuCode;
        this.quantity = quantity;
        this.price = price;
    }

    public String getSkuCode() { return skuCode; }
    public void setSkuCode(String skuCode) { this.skuCode = skuCode; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
}
