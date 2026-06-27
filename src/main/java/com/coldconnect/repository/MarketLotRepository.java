package com.coldconnect.repository;

import com.coldconnect.entity.MarketLot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MarketLotRepository extends JpaRepository<MarketLot, Long> {
    List<MarketLot> findByStatus(MarketLot.LotStatus status);
    List<MarketLot> findByCommodityIdAndStatus(String commodityId, MarketLot.LotStatus status);
    Optional<MarketLot> findByLotId(String lotId);
    List<MarketLot> findBySellerId(Long sellerId);
}
