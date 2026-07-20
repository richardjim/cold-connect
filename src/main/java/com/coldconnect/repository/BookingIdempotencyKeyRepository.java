package com.coldconnect.repository;

import com.coldconnect.entity.BookingIdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BookingIdempotencyKeyRepository extends JpaRepository<BookingIdempotencyKey, Long> {
    Optional<BookingIdempotencyKey> findByIdempotencyKey(String key);
    boolean existsByIdempotencyKey(String key);
}