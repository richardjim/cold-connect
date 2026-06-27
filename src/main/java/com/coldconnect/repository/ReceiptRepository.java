package com.coldconnect.repository;

import com.coldconnect.entity.Receipt;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReceiptRepository extends JpaRepository<Receipt, Long> {
    List<Receipt> findByIssuedTo(Long userId);
    List<Receipt> findByBookingId(Long bookingId);
    List<Receipt> findByOrderId(Long orderId);
}
