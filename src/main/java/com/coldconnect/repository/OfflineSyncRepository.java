package com.coldconnect.repository;

import com.coldconnect.entity.OfflineSyncRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OfflineSyncRepository extends JpaRepository<OfflineSyncRecord, Long> {
    List<OfflineSyncRecord> findByDeviceIdAndSyncStatus(String deviceId, String syncStatus);
    List<OfflineSyncRecord> findByActorIdAndSyncStatus(Long actorId, String syncStatus);
}
