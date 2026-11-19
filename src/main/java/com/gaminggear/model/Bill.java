package com.gaminggear.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gaminggear.model.enums.PaymentMethod;
import com.gaminggear.model.enums.PaymentStatus;
import com.gaminggear.patterns.factory.DiscountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Bill {
    private String id;
    private String orderId;
    private String customerId;
    private String customerName;
    private String customerPhone;
    private String customerAddress;
    private List<BillItem> items = new ArrayList<>();
    private BigDecimal originalTotal;
    private DiscountType discountType;
    private BigDecimal discountRate;
    private BigDecimal discountAmount;
    private BigDecimal finalTotal;
    private LocalDateTime issuedAt;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;

    public Bill() {
    }

    public Bill(String id, String orderId, String customerId, String customerName,
                String customerPhone, String customerAddress, List<BillItem> items,
                BigDecimal originalTotal, DiscountType discountType, BigDecimal discountRate,
                BigDecimal discountAmount, BigDecimal finalTotal, LocalDateTime issuedAt,
                PaymentMethod paymentMethod, PaymentStatus paymentStatus) {
        this.id = id;
        this.orderId = orderId;
        this.customerId = customerId;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.customerAddress = customerAddress;
        this.items = items == null ? new ArrayList<>() : new ArrayList<>(items);
        this.originalTotal = originalTotal;
        this.discountType = discountType;
        this.discountRate = discountRate;
        this.discountAmount = discountAmount;
        this.finalTotal = finalTotal;
        this.issuedAt = issuedAt;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = paymentStatus;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
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

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getCustomerAddress() {
        return customerAddress;
    }

    public void setCustomerAddress(String customerAddress) {
        this.customerAddress = customerAddress;
    }

    public List<BillItem> getItems() {
        return items;
    }

    public void setItems(List<BillItem> items) {
        this.items = items == null ? new ArrayList<>() : items;
    }

    public BigDecimal getOriginalTotal() {
        return originalTotal;
    }

    public void setOriginalTotal(BigDecimal originalTotal) {
        this.originalTotal = originalTotal;
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

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Bill bill)) return false;
        return Objects.equals(id, bill.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
