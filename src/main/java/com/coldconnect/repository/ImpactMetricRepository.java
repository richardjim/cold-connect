package com.coldconnect.repository;

import com.coldconnect.entity.ImpactMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ImpactMetricRepository extends JpaRepository<ImpactMetric, Long> {
    Optional<ImpactMetric> findByUserId(Long userId);
}