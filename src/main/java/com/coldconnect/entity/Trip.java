package com.coldconnect.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "trips")
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String        tripId;
    private Long          vehicleId;
    private Long          driverId;
    private String        driverName;
    private String        vehiclePlate;
    private String        routeId;
    private Long          bookingId;
    private Long          customerId;
    private String        origin;
    private String        destination;
    private Double        targetTempMin;
    private Double        targetTempMax;
    private LocalDateTime eta;

    @Enumerated(EnumType.STRING)
    private TripStatus status;

    private Double        plannedDistanceKm;
    private Double        actualDistanceKm;
    private Boolean       tempCompliance;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        tripId    = "TRP-" + System.currentTimeMillis();
    }

    public enum TripStatus { PLANNED, IN_PROGRESS, COMPLETED, SCHEDULED, CANCELLED }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTripId() { return tripId; }
    public void setTripId(String tripId) { this.tripId = tripId; }
    public Long getVehicleId() { return vehicleId; }
    public void setVehicleId(Long vehicleId) { this.vehicleId = vehicleId; }
    public Long getDriverId() { return driverId; }
    public void setDriverId(Long driverId) { this.driverId = driverId; }
    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }
    public String getVehiclePlate() { return vehiclePlate; }
    public void setVehiclePlate(String vehiclePlate) { this.vehiclePlate = vehiclePlate; }
    public String getRouteId() { return routeId; }
    public void setRouteId(String routeId) { this.routeId = routeId; }
    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public Double getTargetTempMin() { return targetTempMin; }
    public void setTargetTempMin(Double targetTempMin) { this.targetTempMin = targetTempMin; }
    public Double getTargetTempMax() { return targetTempMax; }
    public void setTargetTempMax(Double targetTempMax) { this.targetTempMax = targetTempMax; }
    public LocalDateTime getEta() { return eta; }
    public void setEta(LocalDateTime eta) { this.eta = eta; }
    public TripStatus getStatus() { return status; }
    public void setStatus(TripStatus status) { this.status = status; }
    public Double getPlannedDistanceKm() { return plannedDistanceKm; }
    public void setPlannedDistanceKm(Double v) { this.plannedDistanceKm = v; }
    public Double getActualDistanceKm() { return actualDistanceKm; }
    public void setActualDistanceKm(Double v) { this.actualDistanceKm = v; }
    public Boolean getTempCompliance() { return tempCompliance; }
    public void setTempCompliance(Boolean tempCompliance) { this.tempCompliance = tempCompliance; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getEndedAt() { return endedAt; }
    public void setEndedAt(LocalDateTime endedAt) { this.endedAt = endedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}