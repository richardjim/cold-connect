package com.coldconnect.repository;

import com.coldconnect.entity.Hub;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface HubRepository extends JpaRepository<Hub, Long> {
    List<Hub> findByTenantRegionId(String regionId);
    List<Hub> findByTenantRegionIdAndStatus(String regionId, Hub.HubStatus status);

    @Query("SELECT h FROM Hub h WHERE h.tenantRegionId = :regionId AND h.currentLoadKg < h.capacityKg")
    List<Hub> findAvailableHubs(@Param("regionId") String regionId);
}
