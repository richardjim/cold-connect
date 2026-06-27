package com.coldconnect.repository;

import com.coldconnect.entity.ServiceRate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ServiceRateRepository extends JpaRepository<ServiceRate, Long> {
    List<ServiceRate> findByRegionAndServiceType(String region, String serviceType);
    List<ServiceRate> findByRegion(String region);
}
