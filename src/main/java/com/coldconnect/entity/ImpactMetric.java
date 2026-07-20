package com.coldconnect.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "impact_metrics")
public class ImpactMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Double foodSavedKg;
    private Double co2AvoidedKg;
    private Double spoiledFoodPreventedKg;
    private Integer totalBookings;
    private Integer totalStorageDays;
    private Double solarCoolingKwh;
    private LocalDateTime lastCalculatedAt;

    @PrePersist
    protected void onCreate() { lastCalculatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Double getFoodSavedKg() { return foodSavedKg; }
    public void setFoodSavedKg(Double foodSavedKg) { this.foodSavedKg = foodSavedKg; }
    public Double getCo2AvoidedKg() { return co2AvoidedKg; }
    public void setCo2AvoidedKg(Double co2AvoidedKg) { this.co2AvoidedKg = co2AvoidedKg; }
    public Double getSpoiledFoodPreventedKg() { return spoiledFoodPreventedKg; }
    public void setSpoiledFoodPreventedKg(Double v) { this.spoiledFoodPreventedKg = v; }
    public Integer getTotalBookings() { return totalBookings; }
    public void setTotalBookings(Integer totalBookings) { this.totalBookings = totalBookings; }
    public Integer getTotalStorageDays() { return totalStorageDays; }
    public void setTotalStorageDays(Integer totalStorageDays) { this.totalStorageDays = totalStorageDays; }
    public Double getSolarCoolingKwh() { return solarCoolingKwh; }
    public void setSolarCoolingKwh(Double solarCoolingKwh) { this.solarCoolingKwh = solarCoolingKwh; }
    public LocalDateTime getLastCalculatedAt() { return lastCalculatedAt; }
    public void setLastCalculatedAt(LocalDateTime lastCalculatedAt) { this.lastCalculatedAt = lastCalculatedAt; }
}