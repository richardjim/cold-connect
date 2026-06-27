package com.coldconnect.controller;

import com.coldconnect.entity.OfflineSyncRecord;
import com.coldconnect.repository.UserRepository;
import com.coldconnect.service.SyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/sync")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Offline Sync", description = "Batch sync for offline-first mobile clients")
public class SyncController extends BaseController {

    private final SyncService syncService;

    public SyncController(UserRepository userRepository, SyncService syncService) {
        super(userRepository);
        this.syncService = syncService;
    }

    public record SyncRequest(String deviceId, List<OfflineSyncRecord> records) {}

    @Operation(summary = "Process offline batch sync")
    @PostMapping("/batch")
    public ResponseEntity<SyncService.SyncResult> sync(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody SyncRequest req) {
        Long userId = resolveUser(userDetails).getId();
        return ResponseEntity.ok(syncService.processBatch(req.deviceId(), userId, req.records()));
    }
}
