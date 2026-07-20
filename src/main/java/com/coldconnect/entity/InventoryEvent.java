package com.coldconnect.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_events")
public class InventoryEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String eventId;
    private String crateId;
    private String eventType;    // CHECK_IN, ZONE_MOVE, QUALITY_CHECK, CHECKOUT, DISPUTE, LOSS, SALE
    private Long actorId;
    private String locationId;
    private LocalDateTime timestamp;
    private String beforeStatus;
    private String afterStatus;
    private String evidenceUri;

    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
        if (eventId == null) eventId = "EVT-" + System.currentTimeMillis();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getCrateId() { return crateId; }
    public void setCrateId(String crateId) { this.crateId = crateId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public Long getActorId() { return actorId; }
    public void setActorId(Long actorId) { this.actorId = actorId; }
    public String getLocationId() { return locationId; }
    public void setLocationId(String locationId) { this.locationId = locationId; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getBeforeStatus() { return beforeStatus; }
    public void setBeforeStatus(String beforeStatus) { this.beforeStatus = beforeStatus; }
    public String getAfterStatus() { return afterStatus; }
    public void setAfterStatus(String afterStatus) { this.afterStatus = afterStatus; }
    public String getEvidenceUri() { return evidenceUri; }
    public void setEvidenceUri(String evidenceUri) { this.evidenceUri = evidenceUri; }
}