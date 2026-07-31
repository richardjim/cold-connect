package com.coldconnect.service;

import com.coldconnect.entity.Hub;
import com.coldconnect.entity.HubZone;
import com.coldconnect.exception.AppException;
import com.coldconnect.repository.HubRepository;
import com.coldconnect.repository.HubZoneRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HubService {

    private final HubRepository     hubRepository;
    private final HubZoneRepository hubZoneRepository;

    public HubService(HubRepository hubRepository,
                      HubZoneRepository hubZoneRepository) {
        this.hubRepository     = hubRepository;
        this.hubZoneRepository = hubZoneRepository;
    }

    public List<Hub> searchHubs(String regionId) {
        return regionId != null
                ? hubRepository.findByTenantRegionId(regionId)
                : hubRepository.findAll();
    }

    public Hub getHub(Long hubId) {
        return hubRepository.findById(hubId)
                .orElseThrow(() -> new AppException.NotFoundException(
                        "Hub not found: " + hubId));
    }

    public CapacitySnapshot getCapacity(Long hubId) {
        return new CapacitySnapshot(getHub(hubId));
    }

    public List<HubZone> getZones(Long hubId) {
        return hubZoneRepository.findByHubId(hubId);
    }

    public static class CapacitySnapshot {
        public final double  capacityKg;
        public final double  currentLoadKg;
        public final double  availableKg;
        public final int     utilizationPct;
        public final String  status;
        public final boolean stale;

        public CapacitySnapshot(Hub hub) {
            this.capacityKg    = hub.getCapacityKg() != null ? hub.getCapacityKg() : 0;
            this.currentLoadKg = hub.getCurrentLoadKg() != null ? hub.getCurrentLoadKg() : 0;
            this.availableKg   = this.capacityKg - this.currentLoadKg;
            this.utilizationPct = this.capacityKg > 0
                    ? (int) Math.round((this.currentLoadKg / this.capacityKg) * 100) : 0;
            this.status        = hub.getStatus() != null ? hub.getStatus().name() : "UNKNOWN";
            this.stale         = false;
        }
    }
}