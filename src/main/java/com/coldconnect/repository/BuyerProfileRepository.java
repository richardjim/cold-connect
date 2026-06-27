package com.coldconnect.repository;

import com.coldconnect.entity.BuyerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BuyerProfileRepository extends JpaRepository<BuyerProfile, Long> {
    Optional<BuyerProfile> findByBuyerId(Long buyerId);
}
