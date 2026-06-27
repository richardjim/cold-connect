package com.coldconnect.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "trips")
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String tripId;
    private Long vehicleId;
    private Long driverId;
    private String routeId;

    @Enumerated(EnumType.STRING)
    private TripStatus status;

    private Double plannedDistanceKm;
    private Double actualDistanceKm;
    private Boolean tempCompliance;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;

    public enum TripStatus { SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTripId() { return tripId; }
    public void setTripId(String tripId) { this.tripId = tripId; }
    public Long getVehicleId() { return vehicleId; }
    public void setVehicleId(Long vehicleId) { this.vehicleId = vehicleId; }
    public Long getDriverId() { return driverId; }
    public void setDriverId(Long driverId) { this.driverId = driverId; }
    public String getRouteId() { return routeId; }
    public void setRouteId(String routeId) { this.routeId = routeId; }
    public TripStatus getStatus() { return status; }
    public void setStatus(TripStatus status) { this.status = status; }
    public Double getPlannedDistanceKm() { return plannedDistanceKm; }
    public void setPlannedDistanceKm(Double plannedDistanceKm) { this.plannedDistanceKm = plannedDistanceKm; }
    public Double getActualDistanceKm() { return actualDistanceKm; }
    public void setActualDistanceKm(Double actualDistanceKm) { this.actualDistanceKm = actualDistanceKm; }
    public Boolean getTempCompliance() { return tempCompliance; }
    public void setTempCompliance(Boolean tempCompliance) { this.tempCompliance = tempCompliance; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getEndedAt() { return endedAt; }
    public void setEndedAt(LocalDateTime endedAt) { this.endedAt = endedAt; }
}
