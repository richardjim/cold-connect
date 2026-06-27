package com.coldconnect.service;

import com.coldconnect.entity.TenantRegion;
import com.coldconnect.exception.AppException;
import com.coldconnect.repository.TenantRegionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RegionService {

    private final TenantRegionRepository regionRepository;

    public RegionService(TenantRegionRepository regionRepository) {
        this.regionRepository = regionRepository;
    }

    public TenantRegion getRegion(String regionId) {
        return regionRepository.findByRegionId(regionId)
                .orElseThrow(() -> new AppException.NotFoundException("Region not found: " + regionId));
    }

    public List<TenantRegion> getActiveRegions() {
        return regionRepository.findByActiveTrue();
    }
}
