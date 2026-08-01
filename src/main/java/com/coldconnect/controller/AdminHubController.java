package com.coldconnect.controller;

import com.coldconnect.entity.Hub;
import com.coldconnect.entity.SensorReading;
import com.coldconnect.exception.AppException;
import com.coldconnect.repository.HubRepository;
import com.coldconnect.repository.SensorReadingRepository;
import com.coldconnect.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/hubs")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Operators", description = "Hub management — Admin only")
public class AdminHubController extends BaseController {

    private final HubRepository           hubRepository;
    private final SensorReadingRepository sensorReadingRepository;

    public AdminHubController(UserRepository userRepository,
                              HubRepository hubRepository,
                              SensorReadingRepository sensorReadingRepository) {
        super(userRepository);
        this.hubRepository           = hubRepository;
        this.sensorReadingRepository = sensorReadingRepository;
    }

    public record CreateHubRequest(
            @NotBlank String hubId,
            @NotBlank String tenantRegionId,
            @NotBlank String name,
            @NotBlank String address,
            @NotBlank String lga,
            @NotNull  Double capacityKg,
            Double gpsLat,
            Double gpsLng,
            String operatingHours
    ) {}

    @Operation(summary = "Get all hubs")
    @GetMapping
    public ResponseEntity<List<Hub>> getAllHubs(
            @RequestParam(required = false) String region) {
        if (region != null) {
            return ResponseEntity.ok(hubRepository.findByTenantRegionId(region));
        }
        return ResponseEntity.ok(hubRepository.findAll());
    }

    @Operation(summary = "Commission a new hub")
    @PostMapping
    public ResponseEntity<Hub> createHub(@RequestBody CreateHubRequest req) {
        Hub hub = new Hub();
        hub.setHubId(req.hubId());
        hub.setTenantRegionId(req.tenantRegionId());
        hub.setName(req.name());
        hub.setAddress(req.address());
        hub.setLga(req.lga());
        hub.setCapacityKg(req.capacityKg());
        hub.setCurrentLoadKg(0.0);
        hub.setGpsLat(req.gpsLat());
        hub.setGpsLng(req.gpsLng());
        hub.setOperatingHours(req.operatingHours());
        hub.setStatus(Hub.HubStatus.ACTIVE);
        return ResponseEntity.ok(hubRepository.save(hub));
    }

    @Operation(summary = "Update hub status")
    @PatchMapping("/{hubId}/status")
    public ResponseEntity<Map<String, Object>> updateStatus(
            @PathVariable Long hubId,
            @RequestBody Map<String, String> body) {
        Hub hub = hubRepository.findById(hubId)
                .orElseThrow(() -> new AppException.NotFoundException("Hub not found"));
        try {
            hub.setStatus(Hub.HubStatus.valueOf(body.get("status").toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new AppException.BadRequestException(
                    "Invalid status. Must be: ACTIVE, INACTIVE, FULL, MAINTENANCE");
        }
        hubRepository.save(hub);
        return ResponseEntity.ok(Map.of(
                "message", "Hub status updated",
                "hubId",   hubId,
                "status",  hub.getStatus().name()
        ));
    }

    @Operation(summary = "Update hub capacity")
    @PatchMapping("/{hubId}/capacity")
    public ResponseEntity<Map<String, Object>> updateCapacity(
            @PathVariable Long hubId,
            @RequestBody Map<String, Double> body) {
        Hub hub = hubRepository.findById(hubId)
                .orElseThrow(() -> new AppException.NotFoundException("Hub not found"));
        if (body.containsKey("capacityKg")) {
            hub.setCapacityKg(body.get("capacityKg"));
        }
        if (body.containsKey("currentLoadKg")) {
            hub.setCurrentLoadKg(body.get("currentLoadKg"));
        }
        hubRepository.save(hub);
        return ResponseEntity.ok(Map.of(
                "message",       "Hub capacity updated",
                "hubId",         hubId,
                "capacityKg",    hub.getCapacityKg(),
                "currentLoadKg", hub.getCurrentLoadKg()
        ));
    }

    @Operation(summary = "Get hub energy and solar stats")
    @GetMapping("/{hubId}/energy")
    public ResponseEntity<Map<String, Object>> getHubEnergy(
            @PathVariable Long hubId) {

        Hub hub = hubRepository.findById(hubId)
                .orElseThrow(() -> new AppException.NotFoundException(
                        "Hub not found: " + hubId));

        double capacity    = hub.getCapacityKg() != null ? hub.getCapacityKg() : 0;
        double currentLoad = hub.getCurrentLoadKg() != null ? hub.getCurrentLoadKg() : 0;
        double utilPct     = capacity > 0 ? (currentLoad / capacity) * 100 : 0;
        double solarKw     = hub.getSolarCapacityKw() != null ? hub.getSolarCapacityKw() : 0;
        double batteryKwh  = hub.getBatteryCapacityKwh() != null ? hub.getBatteryCapacityKwh() : 0;
        double pueEstimate = utilPct > 0 ? 1.3 + (1 - utilPct / 100) * 0.5 : 2.0;

        Map<String, Object> response = new HashMap<>();
        response.put("hubId",              hub.getHubId());
        response.put("name",               hub.getName());
        response.put("powerType",          hub.getPowerType());
        response.put("powerStatus",        hub.getPowerStatus());
        response.put("solarCapacityKw",    solarKw);
        response.put("batteryCapacityKwh", batteryKwh);
        response.put("utilizationPct",     Math.round(utilPct));
        response.put("pueEstimate",        Math.round(pueEstimate * 100.0) / 100.0);
        response.put("pueRating",          pueEstimate <= 1.4 ? "EXCELLENT"
                : pueEstimate <= 1.6 ? "GOOD"
                : pueEstimate <= 1.8 ? "AVERAGE" : "POOR");
        response.put("tempCurrentC",       hub.getTempCurrentC());
        response.put("tempTargetMin",      hub.getTempTargetMin());
        response.put("tempTargetMax",      hub.getTempTargetMax());
        response.put("solarShareEstimatePct",
                "SOLAR".equals(hub.getPowerType()) ? 100
                        : "HYBRID".equals(hub.getPowerType()) ? 60 : 0);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get all assets with maintenance status")
    @GetMapping("/assets")
    public ResponseEntity<Map<String, Object>> getAssets(
            @RequestParam(required = false) String hubId) {

        List<SensorReading> readings = sensorReadingRepository.findAll();

        // Group by assetId — keep latest reading per asset
        var assetMap = readings.stream()
                .collect(Collectors.toMap(
                        SensorReading::getAssetId,
                        r -> r,
                        (r1, r2) -> r1.getTimestamp() != null
                                && r2.getTimestamp() != null
                                && r1.getTimestamp().isAfter(r2.getTimestamp()) ? r1 : r2));

        var assets = assetMap.entrySet().stream().map(e -> {
            SensorReading reading = e.getValue();
            Map<String, Object> asset = new HashMap<>();
            asset.put("assetId",     e.getKey());
            asset.put("lastReading", reading.getTimestamp());
            asset.put("tempC",       reading.getTempC());
            asset.put("batteryPct",  reading.getBatteryPct());
            asset.put("qualityFlag", reading.getQualityFlag());
            asset.put("health",      "OK".equalsIgnoreCase(reading.getQualityFlag())
                    ? "Good" : "Attention");
            return asset;
        }).toList();

        return ResponseEntity.ok(Map.of(
                "assets", assets,
                "count",  assets.size()
        ));
    }
}