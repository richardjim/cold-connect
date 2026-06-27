package com.coldconnect.repository;

import com.coldconnect.entity.HubZone;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HubZoneRepository extends JpaRepository<HubZone, Long> {
    List<HubZone> findByHubId(Long hubId);
}
