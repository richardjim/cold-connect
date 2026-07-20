package com.coldconnect.controller;

import com.coldconnect.entity.SensorReading;
import com.coldconnect.repository.SensorReadingRepository;
import com.coldconnect.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/iot")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin IoT", description = "Cold chain IoT monitoring — Admin only")
public class AdminIotController extends BaseController {

    private final SensorReadingRepository sensorRepository;

    public AdminIotController(UserRepository userRepository,
                              SensorReadingRepository sensorRepository) {
        super(userRepository);
        this.sensorRepository = sensorRepository;
    }

    @Operation(
            summary = "Get all sensor assets and data quality state",
            description = "Filter by assetId or qualityFlag"
    )
    @GetMapping("/assets")
    public ResponseEntity<Map<String, Object>> getAssets(
            @RequestParam(required = false) String assetId,
            @RequestParam(required = false) String qualityFlag) {

        var readings = sensorRepository.findAll();

        if (assetId != null) {
            readings = readings.stream()
                    .filter(r -> assetId.equalsIgnoreCase(r.getAssetId()))
                    .toList();
        }

        if (qualityFlag != null) {
            readings = readings.stream()
                    .filter(r -> qualityFlag.equalsIgnoreCase(r.getQualityFlag()))
                    .toList();
        }

        // Identify stale sensors — no reading in last 30 minutes
        var now = java.time.LocalDateTime.now();
        long staleCount = readings.stream()
                .filter(r -> r.getTimestamp() != null &&
                        r.getTimestamp().isBefore(now.minusMinutes(30)))
                .count();

        long alertCount = readings.stream()
                .filter(r -> "ALERT".equalsIgnoreCase(r.getQualityFlag()))
                .count();

        return ResponseEntity.ok(Map.of(
                "readings",   readings,
                "count",      readings.size(),
                "staleCount", staleCount,
                "alertCount", alertCount
        ));
    }

    @Operation(
            summary = "Get IoT alerts — temperature excursions and data quality issues",
            description = "Filter by severity: CRITICAL, HIGH, MEDIUM, LOW"
    )
    @GetMapping("/alerts")
    public ResponseEntity<Map<String, Object>> getAlerts(
            @RequestParam(required = false) String assetId) {

        var readings = sensorRepository.findAll().stream()
                .filter(r -> r.getQualityFlag() != null &&
                        !r.getQualityFlag().equalsIgnoreCase("OK"))
                .toList();

        if (assetId != null) {
            readings = readings.stream()
                    .filter(r -> assetId.equalsIgnoreCase(r.getAssetId()))
                    .toList();
        }

        return ResponseEntity.ok(Map.of(
                "alerts", readings,
                "count",  readings.size()
        ));
    }

    @Operation(summary = "Get sensor history for a specific asset")
    @GetMapping("/assets/{assetId}/readings")
    public ResponseEntity<List<SensorReading>> getReadings(
            @PathVariable String assetId) {
        return ResponseEntity.ok(
                sensorRepository.findByAssetIdOrderByTimestampDesc(assetId)
        );
    }
}