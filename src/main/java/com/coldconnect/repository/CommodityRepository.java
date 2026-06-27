package com.coldconnect.repository;

import com.coldconnect.entity.Commodity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CommodityRepository extends JpaRepository<Commodity, Long> {
    List<Commodity> findByRegion(String region);
    List<Commodity> findByRegionOrRegionIsNull(String region);
}
