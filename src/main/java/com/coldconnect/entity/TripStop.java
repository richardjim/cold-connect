package com.coldconnect.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "trip_stops")
public class TripStop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long tripId;
    private Integer sequence;
    private String stopType;
    private Long customerId;
    private Long hubId;
    private Double gpsLat;
    private Double gpsLng;
    private LocalDateTime eta;
    private String status;
    private String podUri;
    private Double temperatureAtStop;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTripId() { return tripId; }
    public void setTripId(Long tripId) { this.tripId = tripId; }
    public Integer getSequence() { return sequence; }
    public void setSequence(Integer sequence) { this.sequence = sequence; }
    public String getStopType() { return stopType; }
    public void setStopType(String stopType) { this.stopType = stopType; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public Long getHubId() { return hubId; }
    public void setHubId(Long hubId) { this.hubId = hubId; }
    public Double getGpsLat() { return gpsLat; }
    public void setGpsLat(Double gpsLat) { this.gpsLat = gpsLat; }
    public Double getGpsLng() { return gpsLng; }
    public void setGpsLng(Double gpsLng) { this.gpsLng = gpsLng; }
    public LocalDateTime getEta() { return eta; }
    public void setEta(LocalDateTime eta) { this.eta = eta; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPodUri() { return podUri; }
    public void setPodUri(String podUri) { this.podUri = podUri; }
    public Double getTemperatureAtStop() { return temperatureAtStop; }
    public void setTemperatureAtStop(Double temperatureAtStop) { this.temperatureAtStop = temperatureAtStop; }
}
