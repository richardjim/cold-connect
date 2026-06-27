package com.coldconnect.repository;

import com.coldconnect.entity.ChainEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChainEventRepository extends JpaRepository<ChainEvent, Long> {
    List<ChainEvent> findByLotIdOrderByStartedAtAsc(String lotId);
    List<ChainEvent> findByCrateIdOrderByStartedAtAsc(String crateId);
}
