package com.coldconnect.service;

import com.coldconnect.entity.Commodity;
import com.coldconnect.repository.CommodityRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommodityService {

    private final CommodityRepository commodityRepository;

    public CommodityService(CommodityRepository commodityRepository) {
        this.commodityRepository = commodityRepository;
    }

    public List<Commodity> getCommodities(String region) {
        return region != null
                ? commodityRepository.findByRegionOrRegionIsNull(region)
                : commodityRepository.findAll();
    }
}
