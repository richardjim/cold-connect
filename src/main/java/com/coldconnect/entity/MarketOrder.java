package com.coldconnect.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "market_orders")
public class MarketOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String orderId;

    @Column(nullable = false)
    private Long buyerId;

    @Column(nullable = false)
    private String fulfilmentType;

    private String destAddress;

    @Column(nullable = false)
    private String paymentPreference;

    private LocalDateTime windowStart;

    private LocalDateTime windowEnd;

    private Long deliveryTripId;

    private Long paymentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal deliveryFee;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal total;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;

        if (orderId == null) {
            orderId = "ORD-" + System.currentTimeMillis();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum OrderStatus {
        PENDING,
        CONFIRMED,
        PACKED,
        IN_TRANSIT,
        READY_FOR_COLLECTION,
        DELIVERED,
        COLLECTED,
        CANCELLED
    }

    public Long getId() {
        return id;
    }

    public String getOrderId() {
        return orderId;
    }

    public Long getBuyerId() {
        return buyerId;
    }

    public String getFulfilmentType() {
        return fulfilmentType;
    }

    public String getDestAddress() {
        return destAddress;
    }

    public String getPaymentPreference() {
        return paymentPreference;
    }

    public LocalDateTime getWindowStart() {
        return windowStart;
    }

    public LocalDateTime getWindowEnd() {
        return windowEnd;
    }

    public Long getDeliveryTripId() {
        return deliveryTripId;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getDeliveryFee() {
        return deliveryFee;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public void setBuyerId(Long buyerId) {
        this.buyerId = buyerId;
    }

    public void setFulfilmentType(String fulfilmentType) {
        this.fulfilmentType = fulfilmentType;
    }

    public void setDestAddress(String destAddress) {
        this.destAddress = destAddress;
    }

    public void setPaymentPreference(String paymentPreference) {
        this.paymentPreference = paymentPreference;
    }

    public void setWindowStart(LocalDateTime windowStart) {
        this.windowStart = windowStart;
    }

    public void setWindowEnd(LocalDateTime windowEnd) {
        this.windowEnd = windowEnd;
    }

    public void setDeliveryTripId(Long deliveryTripId) {
        this.deliveryTripId = deliveryTripId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public void setDeliveryFee(BigDecimal deliveryFee) {
        this.deliveryFee = deliveryFee;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }
}