package com.coldconnect.service;

import com.coldconnect.entity.OfflineSyncRecord;
import com.coldconnect.repository.OfflineSyncRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class SyncService {

    private final OfflineSyncRepository syncRepository;

    public SyncService(OfflineSyncRepository syncRepository) {
        this.syncRepository = syncRepository;
    }

    public static class SyncResult {
        public final List<String> acknowledged;
        public final List<String> conflicts;
        public SyncResult(List<String> acknowledged, List<String> conflicts) {
            this.acknowledged = acknowledged;
            this.conflicts = conflicts;
        }
    }

    @Transactional
    public SyncResult processBatch(String deviceId, Long actorId, List<OfflineSyncRecord> records) {
        List<String> acknowledged = new ArrayList<>();
        List<String> conflicts = new ArrayList<>();

        for (OfflineSyncRecord record : records) {
            record.setDeviceId(deviceId);
            record.setActorId(actorId);

            boolean hasConflict = syncRepository
                    .findByDeviceIdAndSyncStatus(deviceId, "SYNCED")
                    .stream()
                    .anyMatch(r -> r.getLocalId() != null && r.getLocalId().equals(record.getLocalId()));

            if (hasConflict) {
                record.setSyncStatus("CONFLICT");
                record.setConflictReason("Server record already exists");
                conflicts.add(record.getLocalId());
            } else {
                record.setSyncStatus("SYNCED");
                record.setSyncedAt(LocalDateTime.now());
                acknowledged.add(record.getLocalId());
            }
            syncRepository.save(record);
        }
        return new SyncResult(acknowledged, conflicts);
    }
}
