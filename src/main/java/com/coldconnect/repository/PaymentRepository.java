package com.coldconnect.repository;

import com.coldconnect.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByPayerId(Long payerId);
    List<Payment> findByBookingId(Long bookingId);
    List<Payment> findByOrderId(Long orderId);
}
