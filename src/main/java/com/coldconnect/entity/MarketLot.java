package com.coldconnect.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "market_lots")
public class MarketLot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String lotId;
    private String crateIds;
    private Long sellerId;
    private String commodityId;
    private String grade;
    private Double kgAvailable;
    private BigDecimal pricePerKg;
    private Double minOrderKg;

    @Enumerated(EnumType.STRING)
    private LotStatus status;

    private LocalDateTime listedAt;
    private Integer traceabilityScore;

    @PrePersist
    protected void onCreate() { listedAt = LocalDateTime.now(); }

    public enum LotStatus { DRAFT, LIVE, RESERVED, SOLD, WITHDRAWN }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getLotId() { return lotId; }
    public void setLotId(String lotId) { this.lotId = lotId; }
    public String getCrateIds() { return crateIds; }
    public void setCrateIds(String crateIds) { this.crateIds = crateIds; }
    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }
    public String getCommodityId() { return commodityId; }
    public void setCommodityId(String commodityId) { this.commodityId = commodityId; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    public Double getKgAvailable() { return kgAvailable; }
    public void setKgAvailable(Double kgAvailable) { this.kgAvailable = kgAvailable; }
    public BigDecimal getPricePerKg() { return pricePerKg; }
    public void setPricePerKg(BigDecimal pricePerKg) { this.pricePerKg = pricePerKg; }
    public Double getMinOrderKg() { return minOrderKg; }
    public void setMinOrderKg(Double minOrderKg) { this.minOrderKg = minOrderKg; }
    public LotStatus getStatus() { return status; }
    public void setStatus(LotStatus status) { this.status = status; }
    public LocalDateTime getListedAt() { return listedAt; }
    public Integer getTraceabilityScore() { return traceabilityScore; }
    public void setTraceabilityScore(Integer traceabilityScore) { this.traceabilityScore = traceabilityScore; }
}
