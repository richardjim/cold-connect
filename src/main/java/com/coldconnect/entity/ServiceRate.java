package com.coldconnect.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "service_rates")
public class ServiceRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String region;
    private String serviceType;
    private String commodityId;
    private String unit;
    private BigDecimal baseFee;
    private BigDecimal storageDayFee;
    private BigDecimal transportKmFee;
    private LocalDate validity;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }
    public String getCommodityId() { return commodityId; }
    public void setCommodityId(String commodityId) { this.commodityId = commodityId; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public BigDecimal getBaseFee() { return baseFee; }
    public void setBaseFee(BigDecimal baseFee) { this.baseFee = baseFee; }
    public BigDecimal getStorageDayFee() { return storageDayFee; }
    public void setStorageDayFee(BigDecimal storageDayFee) { this.storageDayFee = storageDayFee; }
    public BigDecimal getTransportKmFee() { return transportKmFee; }
    public void setTransportKmFee(BigDecimal transportKmFee) { this.transportKmFee = transportKmFee; }
    public LocalDate getValidity() { return validity; }
    public void setValidity(LocalDate validity) { this.validity = validity; }
}
