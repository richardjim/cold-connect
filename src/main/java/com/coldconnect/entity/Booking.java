package com.coldconnect.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String bookingId;
    private Long customerId;
    private Long orgId;
    private String serviceType;
    private Long hubId;

    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    private LocalDateTime scheduledWindowStart;
    private LocalDateTime scheduledWindowEnd;
    private Long quoteId;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    private String sourceChannel;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Double     finalWeightKg;
    private BigDecimal finalTotal;
    private LocalDateTime weighedAt;
    private String pickupAddress;
    private String dropoffAddress;


    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        bookingId = "BK-" + System.currentTimeMillis();
    }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public enum BookingStatus { PENDING, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED }
    public enum PaymentStatus { UNPAID, PARTIAL, PAID, REFUNDED }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }
    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }
    public Long getHubId() { return hubId; }
    public void setHubId(Long hubId) { this.hubId = hubId; }
    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }
    public LocalDateTime getScheduledWindowStart() { return scheduledWindowStart; }
    public void setScheduledWindowStart(LocalDateTime scheduledWindowStart) { this.scheduledWindowStart = scheduledWindowStart; }
    public LocalDateTime getScheduledWindowEnd() { return scheduledWindowEnd; }
    public void setScheduledWindowEnd(LocalDateTime scheduledWindowEnd) { this.scheduledWindowEnd = scheduledWindowEnd; }
    public Long getQuoteId() { return quoteId; }
    public void setQuoteId(Long quoteId) { this.quoteId = quoteId; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }
    public String getSourceChannel() { return sourceChannel; }
    public void setSourceChannel(String sourceChannel) { this.sourceChannel = sourceChannel; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Double getFinalWeightKg() { return finalWeightKg; }
    public void setFinalWeightKg(Double finalWeightKg) { this.finalWeightKg = finalWeightKg; }
    public BigDecimal getFinalTotal() { return finalTotal; }
    public void setFinalTotal(BigDecimal finalTotal) { this.finalTotal = finalTotal; }
    public LocalDateTime getWeighedAt() { return weighedAt; }
    public void setWeighedAt(LocalDateTime weighedAt) { this.weighedAt = weighedAt; }
    public String getPickupAddress() { return pickupAddress; }
    public void setPickupAddress(String pickupAddress) { this.pickupAddress = pickupAddress; }
    public String getDropoffAddress() { return dropoffAddress; }
    public void setDropoffAddress(String dropoffAddress) { this.dropoffAddress = dropoffAddress; }

}
