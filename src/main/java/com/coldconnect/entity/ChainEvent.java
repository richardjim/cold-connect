package com.coldconnect.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chain_events")
public class ChainEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String lotId;
    private String crateId;
    private String type;
    private Long actorId;
    private String location;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private String tempSummary;
    private String evidenceUri;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getLotId() { return lotId; }
    public void setLotId(String lotId) { this.lotId = lotId; }
    public String getCrateId() { return crateId; }
    public void setCrateId(String crateId) { this.crateId = crateId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Long getActorId() { return actorId; }
    public void setActorId(Long actorId) { this.actorId = actorId; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getEndedAt() { return endedAt; }
    public void setEndedAt(LocalDateTime endedAt) { this.endedAt = endedAt; }
    public String getTempSummary() { return tempSummary; }
    public void setTempSummary(String tempSummary) { this.tempSummary = tempSummary; }
    public String getEvidenceUri() { return evidenceUri; }
    public void setEvidenceUri(String evidenceUri) { this.evidenceUri = evidenceUri; }
}
