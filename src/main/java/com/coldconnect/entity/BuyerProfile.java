package com.coldconnect.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "buyer_profiles")
public class BuyerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long buyerId;
    private Long orgId;
    private String type;
    private String kybStatus;
    private BigDecimal creditLimit;
    private Integer creditTermsDays;
    private String defaultAddress;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getBuyerId() { return buyerId; }
    public void setBuyerId(Long buyerId) { this.buyerId = buyerId; }
    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getKybStatus() { return kybStatus; }
    public void setKybStatus(String kybStatus) { this.kybStatus = kybStatus; }
    public BigDecimal getCreditLimit() { return creditLimit; }
    public void setCreditLimit(BigDecimal creditLimit) { this.creditLimit = creditLimit; }
    public Integer getCreditTermsDays() { return creditTermsDays; }
    public void setCreditTermsDays(Integer creditTermsDays) { this.creditTermsDays = creditTermsDays; }
    public String getDefaultAddress() { return defaultAddress; }
    public void setDefaultAddress(String defaultAddress) { this.defaultAddress = defaultAddress; }
}
