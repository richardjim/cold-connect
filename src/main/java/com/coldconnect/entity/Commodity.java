package com.coldconnect.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "commodities")
public class Commodity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String commodityId;
    private String name;
    private String category;
    private Double tempRangeMin;
    private Double tempRangeMax;
    private Integer shelfLifeDays;
    private String packagingRules;
    private String handlingNotes;
    private String region;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCommodityId() { return commodityId; }
    public void setCommodityId(String commodityId) { this.commodityId = commodityId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Double getTempRangeMin() { return tempRangeMin; }
    public void setTempRangeMin(Double tempRangeMin) { this.tempRangeMin = tempRangeMin; }
    public Double getTempRangeMax() { return tempRangeMax; }
    public void setTempRangeMax(Double tempRangeMax) { this.tempRangeMax = tempRangeMax; }
    public Integer getShelfLifeDays() { return shelfLifeDays; }
    public void setShelfLifeDays(Integer shelfLifeDays) { this.shelfLifeDays = shelfLifeDays; }
    public String getPackagingRules() { return packagingRules; }
    public void setPackagingRules(String packagingRules) { this.packagingRules = packagingRules; }
    public String getHandlingNotes() { return handlingNotes; }
    public void setHandlingNotes(String handlingNotes) { this.handlingNotes = handlingNotes; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
}
