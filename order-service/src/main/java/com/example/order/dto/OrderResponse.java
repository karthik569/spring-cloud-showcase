package com.example.order.dto;

import java.time.Instant;

public class OrderResponse {
    private String orderId;
    private String skuCode;
    private int quantity;
    private double totalPrice;
    private double discountApplied;
    private String status;
    private String warehouse;
    private String note;
    private String timestamp;

    public OrderResponse() {}

    public OrderResponse(String orderId, String skuCode, int quantity, double totalPrice,
                         double discountApplied, String status, String warehouse, String note) {
        this.orderId = orderId;
        this.skuCode = skuCode;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
        this.discountApplied = discountApplied;
        this.status = status;
        this.warehouse = warehouse;
        this.note = note;
        this.timestamp = Instant.now().toString();
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getSkuCode() { return skuCode; }
    public void setSkuCode(String skuCode) { this.skuCode = skuCode; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }

    public double getDiscountApplied() { return discountApplied; }
    public void setDiscountApplied(double discountApplied) { this.discountApplied = discountApplied; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getWarehouse() { return warehouse; }
    public void setWarehouse(String warehouse) { this.warehouse = warehouse; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}
