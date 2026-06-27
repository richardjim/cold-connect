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

    private final HubRepository hubRepository;
    private final HubZoneRepository hubZoneRepository;

    public HubService(HubRepository hubRepository, HubZoneRepository hubZoneRepository) {
        this.hubRepository = hubRepository;
        this.hubZoneRepository = hubZoneRepository;
    }

    public List<Hub> searchHubs(String regionId) {
        return regionId != null
                ? hubRepository.findByTenantRegionId(regionId)
                : hubRepository.findAll();
    }

    public Hub getHub(Long hubId) {
        return hubRepository.findById(hubId)
                .orElseThrow(() -> new AppException.NotFoundException("Hub not found"));
    }

    public CapacitySnapshot getCapacity(Long hubId) {
        Hub hub = getHub(hubId);
        double available = hub.getCapacityKg() - hub.getCurrentLoadKg();
        return new CapacitySnapshot(hub.getCapacityKg(), hub.getCurrentLoadKg(), available, false);
    }

    public List<HubZone> getZones(Long hubId) {
        return hubZoneRepository.findByHubId(hubId);
    }

    public static class CapacitySnapshot {
        public final Double capacityKg;
        public final Double currentLoadKg;
        public final Double availableKg;
        public final boolean stale;

        public CapacitySnapshot(Double capacityKg, Double currentLoadKg, Double availableKg, boolean stale) {
            this.capacityKg = capacityKg;
            this.currentLoadKg = currentLoadKg;
            this.availableKg = availableKg;
            this.stale = stale;
        }
    }
}
