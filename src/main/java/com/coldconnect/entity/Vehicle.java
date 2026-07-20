package com.coldconnect.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "vehicles")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String vehicleId;
    private String plateNo;
    private String makeModel;
    private String fuelType;
    private Double payloadKg;
    private String reeferType;
    private Double tempRangeMin;
    private Double tempRangeMax;
    private String telematicsId;
    private String status;       // ACTIVE, INACTIVE, MAINTENANCE
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (vehicleId == null) vehicleId = "VEH-" + System.currentTimeMillis();
        if (status == null) status = "ACTIVE";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getVehicleId() { return vehicleId; }
    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }
    public String getPlateNo() { return plateNo; }
    public void setPlateNo(String plateNo) { this.plateNo = plateNo; }
    public String getMakeModel() { return makeModel; }
    public void setMakeModel(String makeModel) { this.makeModel = makeModel; }
    public String getFuelType() { return fuelType; }
    public void setFuelType(String fuelType) { this.fuelType = fuelType; }
    public Double getPayloadKg() { return payloadKg; }
    public void setPayloadKg(Double payloadKg) { this.payloadKg = payloadKg; }
    public String getReeferType() { return reeferType; }
    public void setReeferType(String reeferType) { this.reeferType = reeferType; }
    public Double getTempRangeMin() { return tempRangeMin; }
    public void setTempRangeMin(Double tempRangeMin) { this.tempRangeMin = tempRangeMin; }
    public Double getTempRangeMax() { return tempRangeMax; }
    public void setTempRangeMax(Double tempRangeMax) { this.tempRangeMax = tempRangeMax; }
    public String getTelematicsId() { return telematicsId; }
    public void setTelematicsId(String telematicsId) { this.telematicsId = telematicsId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}