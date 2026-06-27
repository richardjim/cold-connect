package com.coldconnect.repository;

import com.coldconnect.entity.TenantRegion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TenantRegionRepository extends JpaRepository<TenantRegion, String> {
    Optional<TenantRegion> findByRegionId(String regionId);
    List<TenantRegion> findByActiveTrue();
}
