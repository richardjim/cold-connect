package com.coldconnect.repository;

import com.coldconnect.entity.BookingQuote;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BookingQuoteRepository extends JpaRepository<BookingQuote, Long> {
    Optional<BookingQuote> findByBookingId(Long bookingId);
}
