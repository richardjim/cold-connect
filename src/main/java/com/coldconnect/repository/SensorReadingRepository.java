package com.coldconnect.repository;

import com.coldconnect.entity.SensorReading;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface SensorReadingRepository extends JpaRepository<SensorReading, Long> {
    List<SensorReading> findByAssetIdOrderByTimestampDesc(String assetId);
    List<SensorReading> findByAssetIdAndTimestampBetween(String assetId, LocalDateTime from, LocalDateTime to);
}
