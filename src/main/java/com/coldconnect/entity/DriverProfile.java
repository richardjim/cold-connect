package com.coldconnect.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "driver_profiles")
public class DriverProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String driverId;
    private Long userId;
    private String licenseNo;
    private String vettingStatus;   // PENDING, APPROVED, REJECTED, SUSPENDED
    private String nextOfKin;
    private String trainingStatus;  // NOT_STARTED, IN_PROGRESS, COMPLETED
    private Double rating;
    private Long assignedVehicleId;
    private boolean misconduct;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (driverId == null) driverId = "DRV-" + System.currentTimeMillis();
        if (vettingStatus == null) vettingStatus = "PENDING";
        if (trainingStatus == null) trainingStatus = "NOT_STARTED";
        misconduct = false;
    }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDriverId() { return driverId; }
    public void setDriverId(String driverId) { this.driverId = driverId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getLicenseNo() { return licenseNo; }
    public void setLicenseNo(String licenseNo) { this.licenseNo = licenseNo; }
    public String getVettingStatus() { return vettingStatus; }
    public void setVettingStatus(String vettingStatus) { this.vettingStatus = vettingStatus; }
    public String getNextOfKin() { return nextOfKin; }
    public void setNextOfKin(String nextOfKin) { this.nextOfKin = nextOfKin; }
    public String getTrainingStatus() { return trainingStatus; }
    public void setTrainingStatus(String trainingStatus) { this.trainingStatus = trainingStatus; }
    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }
    public Long getAssignedVehicleId() { return assignedVehicleId; }
    public void setAssignedVehicleId(Long assignedVehicleId) { this.assignedVehicleId = assignedVehicleId; }
    public boolean isMisconduct() { return misconduct; }
    public void setMisconduct(boolean misconduct) { this.misconduct = misconduct; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}