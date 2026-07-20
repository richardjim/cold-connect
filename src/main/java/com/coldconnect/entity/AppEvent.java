package com.coldconnect.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "app_events")
public class AppEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String eventName;     // booking_created, lot_viewed, order_placed, screen_viewed etc
    private String screenId;
    private String entityType;    // booking, lot, order, trip
    private String entityId;
    private String region;
    private String role;
    private String networkState;  // online, offline
    private String deviceInfo;
    private String featureFlagState;
    private LocalDateTime occurredAt;

    @PrePersist
    protected void onCreate() { occurredAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }
    public String getScreenId() { return screenId; }
    public void setScreenId(String screenId) { this.screenId = screenId; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getNetworkState() { return networkState; }
    public void setNetworkState(String networkState) { this.networkState = networkState; }
    public String getDeviceInfo() { return deviceInfo; }
    public void setDeviceInfo(String deviceInfo) { this.deviceInfo = deviceInfo; }
    public String getFeatureFlagState() { return featureFlagState; }
    public void setFeatureFlagState(String featureFlagState) { this.featureFlagState = featureFlagState; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
}