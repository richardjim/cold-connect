package com.coldconnect.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "hubs")
public class Hub {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String hubId;
    private String tenantRegionId;
    private String name;
    private Double gpsLat;
    private Double gpsLng;
    private String address;
    private String lga;
    private String operatingHours;
    private Double capacityKg;
    private Double currentLoadKg;
    private Double tempCurrentC;        // current cold room temperature
    private Double tempTargetMin;       // target min temp
    private Double tempTargetMax;       // target max temp
    private String powerType;           // SOLAR, GRID, HYBRID, GENERATOR
    private Double solarCapacityKw;     // solar panel capacity
    private Double batteryCapacityKwh;  // battery storage
    private String powerStatus;         // ON_GRID, ON_SOLAR, ON_BATTERY, OUTAGE



    @Enumerated(EnumType.STRING)
    private HubStatus status;

    private Long managerId;

    public enum HubStatus { ACTIVE, INACTIVE, FULL, MAINTENANCE }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getHubId() { return hubId; }
    public void setHubId(String hubId) { this.hubId = hubId; }
    public String getTenantRegionId() { return tenantRegionId; }
    public void setTenantRegionId(String tenantRegionId) { this.tenantRegionId = tenantRegionId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Double getGpsLat() { return gpsLat; }
    public void setGpsLat(Double gpsLat) { this.gpsLat = gpsLat; }
    public Double getGpsLng() { return gpsLng; }
    public void setGpsLng(Double gpsLng) { this.gpsLng = gpsLng; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getLga() { return lga; }
    public void setLga(String lga) { this.lga = lga; }
    public String getOperatingHours() { return operatingHours; }
    public void setOperatingHours(String operatingHours) { this.operatingHours = operatingHours; }
    public Double getCapacityKg() { return capacityKg; }
    public void setCapacityKg(Double capacityKg) { this.capacityKg = capacityKg; }
    public Double getCurrentLoadKg() { return currentLoadKg; }
    public void setCurrentLoadKg(Double currentLoadKg) { this.currentLoadKg = currentLoadKg; }
    public HubStatus getStatus() { return status; }
    public void setStatus(HubStatus status) { this.status = status; }
    public Long getManagerId() { return managerId; }
    public void setManagerId(Long managerId) { this.managerId = managerId; }
    // Getters and setters
    public Double getTempCurrentC() { return tempCurrentC; }
    public void setTempCurrentC(Double tempCurrentC) { this.tempCurrentC = tempCurrentC; }
    public Double getTempTargetMin() { return tempTargetMin; }
    public void setTempTargetMin(Double tempTargetMin) { this.tempTargetMin = tempTargetMin; }
    public Double getTempTargetMax() { return tempTargetMax; }
    public void setTempTargetMax(Double tempTargetMax) { this.tempTargetMax = tempTargetMax; }
    public String getPowerType() { return powerType; }
    public void setPowerType(String powerType) { this.powerType = powerType; }
    public Double getSolarCapacityKw() { return solarCapacityKw; }
    public void setSolarCapacityKw(Double solarCapacityKw) { this.solarCapacityKw = solarCapacityKw; }
    public Double getBatteryCapacityKwh() { return batteryCapacityKwh; }
    public void setBatteryCapacityKwh(Double batteryCapacityKwh) { this.batteryCapacityKwh = batteryCapacityKwh; }
    public String getPowerStatus() { return powerStatus; }
    public void setPowerStatus(String powerStatus) { this.powerStatus = powerStatus; }
}
