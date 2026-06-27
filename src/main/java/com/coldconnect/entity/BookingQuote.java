package com.coldconnect.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "booking_quotes")
public class BookingQuote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long bookingId;
    private String rateVersion;
    private Double quantityEstimateKg;
    private Integer days;
    private Double distanceKm;
    private BigDecimal total;
    private LocalDateTime expiry;
    private String assumptions;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }
    public String getRateVersion() { return rateVersion; }
    public void setRateVersion(String rateVersion) { this.rateVersion = rateVersion; }
    public Double getQuantityEstimateKg() { return quantityEstimateKg; }
    public void setQuantityEstimateKg(Double quantityEstimateKg) { this.quantityEstimateKg = quantityEstimateKg; }
    public Integer getDays() { return days; }
    public void setDays(Integer days) { this.days = days; }
    public Double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(Double distanceKm) { this.distanceKm = distanceKm; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public LocalDateTime getExpiry() { return expiry; }
    public void setExpiry(LocalDateTime expiry) { this.expiry = expiry; }
    public String getAssumptions() { return assumptions; }
    public void setAssumptions(String assumptions) { this.assumptions = assumptions; }
}
