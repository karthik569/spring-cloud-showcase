package com.example.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Response payload representing an order record and its fulfillment status")
public class OrderResponse {

    @Schema(description = "Unique alphanumeric order identifier", example = "ORD-A1B2C3D4")
    private String orderId;

    @Schema(description = "Stock Keeping Unit code", example = "IPHONE15")
    private String skuCode;

    @Schema(description = "Quantity ordered", example = "2")
    private int quantity;

    @Schema(description = "Total price calculated after discount", example = "1699.98")
    private double totalPrice;

    @Schema(description = "Discount amount deducted via centralized configuration", example = "300.00")
    private double discountApplied;

    @Schema(description = "Status of the order", example = "CONFIRMED", allowableValues = {"CONFIRMED", "REJECTED_OUT_OF_STOCK", "FAILED"})
    private String status;

    @Schema(description = "Fulfillment warehouse identifier", example = "San Francisco Hub")
    private String warehouse;

    @Schema(description = "Descriptive note or reason code for the order state", example = "Order placed successfully with 15% centralized discount applied")
    private String note;

    @Schema(description = "Timestamp when order was processed (ISO-8601 UTC)", example = "2026-10-02T14:30:00Z")
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
