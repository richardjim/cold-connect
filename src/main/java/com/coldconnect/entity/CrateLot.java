package com.coldconnect.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "crate_lots")
public class CrateLot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String crateId;
    private String lotId;
    private Long bookingId;
    private Long ownerId;
    private String commodityId;
    private String grade;
    private Double netWeightKg;
    private Long zoneId;

    @Enumerated(EnumType.STRING)
    private CrateStatus status;

    private String photos;

    public enum CrateStatus { INTAKE, IN_STORAGE, IN_TRANSIT, DELIVERED, SOLD, LOST }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCrateId() { return crateId; }
    public void setCrateId(String crateId) { this.crateId = crateId; }
    public String getLotId() { return lotId; }
    public void setLotId(String lotId) { this.lotId = lotId; }
    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public String getCommodityId() { return commodityId; }
    public void setCommodityId(String commodityId) { this.commodityId = commodityId; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    public Double getNetWeightKg() { return netWeightKg; }
    public void setNetWeightKg(Double netWeightKg) { this.netWeightKg = netWeightKg; }
    public Long getZoneId() { return zoneId; }
    public void setZoneId(Long zoneId) { this.zoneId = zoneId; }
    public CrateStatus getStatus() { return status; }
    public void setStatus(CrateStatus status) { this.status = status; }
    public String getPhotos() { return photos; }
    public void setPhotos(String photos) { this.photos = photos; }
}
