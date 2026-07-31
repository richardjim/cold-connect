package com.coldconnect.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "iot_thresholds")
public class IotThreshold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String hubId;
    private String commodityId;
    private Double tempMinC;
    private Double tempMaxC;
    private Double humidityMinPct;
    private Double humidityMaxPct;
    private Integer staleAfterMinutes;  // flag sensor as stale after N minutes
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (staleAfterMinutes == null) staleAfterMinutes = 30;
    }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getHubId() { return hubId; }
    public void setHubId(String hubId) { this.hubId = hubId; }
    public String getCommodityId() { return commodityId; }
    public void setCommodityId(String commodityId) { this.commodityId = commodityId; }
    public Double getTempMinC() { return tempMinC; }
    public void setTempMinC(Double tempMinC) { this.tempMinC = tempMinC; }
    public Double getTempMaxC() { return tempMaxC; }
    public void setTempMaxC(Double tempMaxC) { this.tempMaxC = tempMaxC; }
    public Double getHumidityMinPct() { return humidityMinPct; }
    public void setHumidityMinPct(Double humidityMinPct) { this.humidityMinPct = humidityMinPct; }
    public Double getHumidityMaxPct() { return humidityMaxPct; }
    public void setHumidityMaxPct(Double humidityMaxPct) { this.humidityMaxPct = humidityMaxPct; }
    public Integer getStaleAfterMinutes() { return staleAfterMinutes; }
    public void setStaleAfterMinutes(Integer staleAfterMinutes) { this.staleAfterMinutes = staleAfterMinutes; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}