package com.coldconnect.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "safety_check_items")
public class SafetyCheckItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long checkId;
    private String templateItemId;
    private String label;
    private String mark;        // ok, defect
    private String photoUri;
    private String note;
    private String severity;    // CRITICAL, MAJOR, MINOR
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCheckId() { return checkId; }
    public void setCheckId(Long checkId) { this.checkId = checkId; }
    public String getTemplateItemId() { return templateItemId; }
    public void setTemplateItemId(String templateItemId) { this.templateItemId = templateItemId; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getMark() { return mark; }
    public void setMark(String mark) { this.mark = mark; }
    public String getPhotoUri() { return photoUri; }
    public void setPhotoUri(String photoUri) { this.photoUri = photoUri; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}