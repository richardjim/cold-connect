package com.coldconnect.service;

import com.coldconnect.entity.Payment;
import com.coldconnect.entity.Receipt;
import com.coldconnect.exception.AppException;
import com.coldconnect.repository.PaymentRepository;
import com.coldconnect.repository.ReceiptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class WalletService {

    private final PaymentRepository paymentRepository;
    private final ReceiptRepository receiptRepository;

    public WalletService(PaymentRepository paymentRepository, ReceiptRepository receiptRepository) {
        this.paymentRepository = paymentRepository;
        this.receiptRepository = receiptRepository;
    }

    public static class WalletSummary {
        public final BigDecimal balance;
        public final List<Payment> transactions;
        public WalletSummary(BigDecimal balance, List<Payment> transactions) {
            this.balance = balance;
            this.transactions = transactions;
        }
    }

    public WalletSummary getWallet(Long userId) {
        List<Payment> payments = paymentRepository.findByPayerId(userId);
        BigDecimal balance = payments.stream()
                .filter(p -> "CAPTURED".equals(p.getStatus()))
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new WalletSummary(balance, payments);
    }

    @Transactional
    public Payment initiatePayment(Long payerId, Long bookingId, Long orderId,
                                    BigDecimal amount, String method) {
        Payment payment = new Payment();
        payment.setPayerId(payerId);
        payment.setBookingId(bookingId);
        payment.setOrderId(orderId);
        payment.setAmount(amount);
        payment.setMethod(method);
        payment.setStatus("PENDING");
        return paymentRepository.save(payment);
    }

    public List<Receipt> getReceipts(Long userId) {
        return receiptRepository.findByIssuedTo(userId);
    }

    public Receipt getReceipt(Long receiptId) {
        return receiptRepository.findById(receiptId)
                .orElseThrow(() -> new AppException.NotFoundException("Receipt not found"));
    }
}
