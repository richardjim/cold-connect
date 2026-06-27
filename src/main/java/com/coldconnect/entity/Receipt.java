package com.coldconnect.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "receipts")
public class Receipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long paymentId;
    private Long bookingId;
    private Long orderId;
    private BigDecimal amount;
    private Long issuedTo;
    private Long issuedBy;
    private String receiptUri;
    private String shareChannels;
    private LocalDateTime issuedAt;

    @PrePersist
    protected void onCreate() { issuedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }
    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public Long getIssuedTo() { return issuedTo; }
    public void setIssuedTo(Long issuedTo) { this.issuedTo = issuedTo; }
    public Long getIssuedBy() { return issuedBy; }
    public void setIssuedBy(Long issuedBy) { this.issuedBy = issuedBy; }
    public String getReceiptUri() { return receiptUri; }
    public void setReceiptUri(String receiptUri) { this.receiptUri = receiptUri; }
    public String getShareChannels() { return shareChannels; }
    public void setShareChannels(String shareChannels) { this.shareChannels = shareChannels; }
    public LocalDateTime getIssuedAt() { return issuedAt; }
}
