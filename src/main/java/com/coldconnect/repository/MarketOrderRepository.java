package com.coldconnect.repository;

import com.coldconnect.entity.MarketOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MarketOrderRepository extends JpaRepository<MarketOrder, Long> {
    List<MarketOrder> findByBuyerIdOrderByCreatedAtDesc(Long buyerId);
    Optional<MarketOrder> findByOrderId(String orderId);
}
