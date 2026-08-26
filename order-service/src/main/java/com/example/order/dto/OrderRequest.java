package com.example.order.dto;

public class OrderRequest {
    private String skuCode;
    private int quantity;
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
