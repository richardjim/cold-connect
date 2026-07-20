package com.coldconnect.repository;

import com.coldconnect.entity.SafetyCheck;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SafetyCheckRepository extends JpaRepository<SafetyCheck, Long> {
    List<SafetyCheck> findByVehicleIdOrderByStartedAtDesc(Long vehicleId);
    List<SafetyCheck> findByLeadDriverIdOrderByStartedAtDesc(Long driverId);
    Optional<SafetyCheck> findByCheckId(String checkId);
    List<SafetyCheck> findByResultAndRunBlocked(String result, boolean runBlocked);
}