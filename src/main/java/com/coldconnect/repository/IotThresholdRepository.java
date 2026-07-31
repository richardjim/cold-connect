package com.coldconnect.repository;

import com.coldconnect.entity.IotThreshold;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface IotThresholdRepository extends JpaRepository<IotThreshold, Long> {
    List<IotThreshold> findByHubId(String hubId);
    Optional<IotThreshold> findByHubIdAndCommodityId(String hubId, String commodityId);
    List<IotThreshold> findByCommodityId(String commodityId);
}