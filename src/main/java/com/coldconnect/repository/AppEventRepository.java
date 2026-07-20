package com.coldconnect.repository;

import com.coldconnect.entity.AppEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AppEventRepository extends JpaRepository<AppEvent, Long> {
    List<AppEvent> findByUserIdOrderByOccurredAtDesc(Long userId);
    List<AppEvent> findByEventNameOrderByOccurredAtDesc(String eventName);
    List<AppEvent> findByRegionOrderByOccurredAtDesc(String region);
}