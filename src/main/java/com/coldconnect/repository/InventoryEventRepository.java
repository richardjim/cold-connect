package com.coldconnect.repository;

import com.coldconnect.entity.InventoryEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InventoryEventRepository extends JpaRepository<InventoryEvent, Long> {
    List<InventoryEvent> findByCrateIdOrderByTimestampDesc(String crateId);
    List<InventoryEvent> findByEventTypeOrderByTimestampDesc(String eventType);
    List<InventoryEvent> findByActorIdOrderByTimestampDesc(Long actorId);
}