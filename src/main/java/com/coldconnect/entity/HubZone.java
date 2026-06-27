package com.coldconnect.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "hub_zones")
public class HubZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long hubId;
    private String label;
    private Double tempTargetMin;
    private Double tempTargetMax;
    private String commodityRules;
    private Double capacityKg;
    private Double currentLoadKg;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getHubId() { return hubId; }
    public void setHubId(Long hubId) { this.hubId = hubId; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public Double getTempTargetMin() { return tempTargetMin; }
    public void setTempTargetMin(Double tempTargetMin) { this.tempTargetMin = tempTargetMin; }
    public Double getTempTargetMax() { return tempTargetMax; }
    public void setTempTargetMax(Double tempTargetMax) { this.tempTargetMax = tempTargetMax; }
    public String getCommodityRules() { return commodityRules; }
    public void setCommodityRules(String commodityRules) { this.commodityRules = commodityRules; }
    public Double getCapacityKg() { return capacityKg; }
    public void setCapacityKg(Double capacityKg) { this.capacityKg = capacityKg; }
    public Double getCurrentLoadKg() { return currentLoadKg; }
    public void setCurrentLoadKg(Double currentLoadKg) { this.currentLoadKg = currentLoadKg; }
}
