package com.coldconnect.repository;

import com.coldconnect.entity.HubWaitlist;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface HubWaitlistRepository extends JpaRepository<HubWaitlist, Long> {
    List<HubWaitlist> findByHubIdAndStatus(Long hubId, String status);
    Optional<HubWaitlist> findByUserIdAndHubIdAndStatus(Long userId, Long hubId, String status);
    List<HubWaitlist> findByUserIdOrderByCreatedAtDesc(Long userId);
}