package com.coldconnect.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "tenant_regions")
public class TenantRegion {

    @Id
    private String regionId;
    private String name;
    private String country;
    private String currency;
    private String defaultLanguage;

    @Column(columnDefinition = "TEXT")
    private String featureFlags;

    private boolean active;

    public String getRegionId() { return regionId; }
    public void setRegionId(String regionId) { this.regionId = regionId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getDefaultLanguage() { return defaultLanguage; }
    public void setDefaultLanguage(String defaultLanguage) { this.defaultLanguage = defaultLanguage; }
    public String getFeatureFlags() { return featureFlags; }
    public void setFeatureFlags(String featureFlags) { this.featureFlags = featureFlags; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
