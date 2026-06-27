package com.coldconnect.repository;

import com.coldconnect.entity.CrateLot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CrateLotRepository extends JpaRepository<CrateLot, Long> {
    List<CrateLot> findByOwnerId(Long ownerId);
    List<CrateLot> findByBookingId(Long bookingId);
    Optional<CrateLot> findByCrateId(String crateId);
}
