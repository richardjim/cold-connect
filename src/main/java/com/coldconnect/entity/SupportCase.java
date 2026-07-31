package com.coldconnect.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "support_cases")
public class SupportCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long requesterId;
    private Long bookingId;
    private String crateId;
    private Long tripId;
    private String type;
    private String severity;
    private String sla;
    private Long ownerId;
    private String status;
    private String resolution;
    private String message;
    private String photoUri;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = "OPEN";
        if (severity == null) severity = "MEDIUM";
        // Auto-set SLA based on severity
        if (sla == null) {
            sla = switch (severity) {
                case "CRITICAL" -> "2h";
                case "HIGH"     -> "4h";
                case "LOW"      -> "72h";
                default          -> "24h";
            };
        }
    }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRequesterId() { return requesterId; }
    public void setRequesterId(Long requesterId) { this.requesterId = requesterId; }
    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }
    public String getCrateId() { return crateId; }
    public void setCrateId(String crateId) { this.crateId = crateId; }
    public Long getTripId() { return tripId; }
    public void setTripId(Long tripId) { this.tripId = tripId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getSla() { return sla; }
    public void setSla(String sla) { this.sla = sla; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getPhotoUri() { return photoUri; }
    public void setPhotoUri(String photoUri) { this.photoUri = photoUri; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}