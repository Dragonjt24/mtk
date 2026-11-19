package com.gaminggear.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gaminggear.model.enums.OrderStatus;
import com.gaminggear.patterns.factory.DiscountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Order {
    private String id;
    private String customerId;
    private String customerName;
    private List<OrderItem> items = new ArrayList<>();
    private DiscountType discountType;
    private BigDecimal discountRate;
    private BigDecimal originalTotal;
    private BigDecimal discountAmount;
    private BigDecimal finalTotal;
    private LocalDateTime createdAt;
    private OrderStatus status;

    public Order() {
        this.items = new ArrayList<>();
        this.discountType = DiscountType.NO_DISCOUNT;
        this.discountRate = BigDecimal.ZERO;
        this.originalTotal = BigDecimal.ZERO;
        this.discountAmount = BigDecimal.ZERO;
        this.finalTotal = BigDecimal.ZERO;
        this.status = OrderStatus.PENDING;
    }

    public Order(String id, String customerId, String customerName, List<OrderItem> items,
                 DiscountType discountType, BigDecimal discountRate, BigDecimal originalTotal,
                 BigDecimal discountAmount, BigDecimal finalTotal, LocalDateTime createdAt,
                 OrderStatus status) {
        this.id = id;
        this.customerId = customerId;
        this.customerName = customerName;
        this.items = items == null ? new ArrayList<>() : new ArrayList<>(items);
        this.discountType = discountType == null ? DiscountType.NO_DISCOUNT : discountType;
        this.discountRate = discountRate == null ? BigDecimal.ZERO : discountRate;
        this.originalTotal = originalTotal == null ? BigDecimal.ZERO : originalTotal;
        this.discountAmount = discountAmount == null ? BigDecimal.ZERO : discountAmount;
        this.finalTotal = finalTotal == null ? BigDecimal.ZERO : finalTotal;
        this.createdAt = createdAt;
        this.status = status == null ? OrderStatus.PENDING : status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items == null ? new ArrayList<>() : items;
    }

    public DiscountType getDiscountType() {
        return discountType;
    }

    public void setDiscountType(DiscountType discountType) {
        this.discountType = discountType;
    }

    public BigDecimal getDiscountRate() {
        return discountRate;
    }

    public void setDiscountRate(BigDecimal discountRate) {
        this.discountRate = discountRate;
    }

    public BigDecimal getOriginalTotal() {
        return originalTotal;
    }

    public void setOriginalTotal(BigDecimal originalTotal) {
        this.originalTotal = originalTotal;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getFinalTotal() {
        return finalTotal;
    }

    public void setFinalTotal(BigDecimal finalTotal) {
        this.finalTotal = finalTotal;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Order order)) return false;
        return Objects.equals(id, order.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
