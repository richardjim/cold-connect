package com.coldconnect.controller;

import com.coldconnect.entity.Hub;
import com.coldconnect.exception.AppException;
import com.coldconnect.repository.HubRepository;
import com.coldconnect.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/hubs")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Operators", description = "Hub management — Admin only")
public class AdminHubController extends BaseController {

    private final HubRepository hubRepository;

    public AdminHubController(UserRepository userRepository,
                              HubRepository hubRepository) {
        super(userRepository);
        this.hubRepository = hubRepository;
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
}