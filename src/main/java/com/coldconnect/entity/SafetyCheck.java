package com.coldconnect.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "safety_checks")
public class SafetyCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String checkId;
    private Long vehicleId;
    private Long tripId;
    private Long leadDriverId;
    private Long coDriverId;
    private String templateVersion;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String result;          // PASS, FAIL, PENDING
    private Double gpsLat;
    private Double gpsLng;
    private Integer defectCount;
    private boolean coDriverConfirmed;
    private boolean runBlocked;

    @PrePersist
    protected void onCreate() {
        startedAt = LocalDateTime.now();
        if (checkId == null) checkId = "CHK-" + System.currentTimeMillis();
        result = "PENDING";
        runBlocked = false;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCheckId() { return checkId; }
    public void setCheckId(String checkId) { this.checkId = checkId; }
    public Long getVehicleId() { return vehicleId; }
    public void setVehicleId(Long vehicleId) { this.vehicleId = vehicleId; }
    public Long getTripId() { return tripId; }
    public void setTripId(Long tripId) { this.tripId = tripId; }
    public Long getLeadDriverId() { return leadDriverId; }
    public void setLeadDriverId(Long leadDriverId) { this.leadDriverId = leadDriverId; }
    public Long getCoDriverId() { return coDriverId; }
    public void setCoDriverId(Long coDriverId) { this.coDriverId = coDriverId; }
    public String getTemplateVersion() { return templateVersion; }
    public void setTemplateVersion(String templateVersion) { this.templateVersion = templateVersion; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public Double getGpsLat() { return gpsLat; }
    public void setGpsLat(Double gpsLat) { this.gpsLat = gpsLat; }
    public Double getGpsLng() { return gpsLng; }
    public void setGpsLng(Double gpsLng) { this.gpsLng = gpsLng; }
    public Integer getDefectCount() { return defectCount; }
    public void setDefectCount(Integer defectCount) { this.defectCount = defectCount; }
    public boolean isCoDriverConfirmed() { return coDriverConfirmed; }
    public void setCoDriverConfirmed(boolean coDriverConfirmed) { this.coDriverConfirmed = coDriverConfirmed; }
    public boolean isRunBlocked() { return runBlocked; }
    public void setRunBlocked(boolean runBlocked) { this.runBlocked = runBlocked; }
}