package com.coldconnect.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;
    private String lotId;
    private Double kg;
    private BigDecimal pricePerKgAtOrder;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getLotId() { return lotId; }
    public void setLotId(String lotId) { this.lotId = lotId; }
    public Double getKg() { return kg; }
    public void setKg(Double kg) { this.kg = kg; }
    public BigDecimal getPricePerKgAtOrder() { return pricePerKgAtOrder; }
    public void setPricePerKgAtOrder(BigDecimal pricePerKgAtOrder) { this.pricePerKgAtOrder = pricePerKgAtOrder; }
}
