package com.coldconnect.controller;

import com.coldconnect.entity.Hub;
import com.coldconnect.repository.HubRepository;
import com.coldconnect.repository.UserRepository;
import com.coldconnect.service.HubService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/hubs")
@Tag(name = "Hubs", description = "Hub search, capacity, temperature and power")
public class HubController extends BaseController {

    private final HubService    hubService;
    private final HubRepository hubRepository;

    public HubController(UserRepository userRepository,
                         HubService hubService,
                         HubRepository hubRepository) {
        super(userRepository);
        this.hubService    = hubService;
        this.hubRepository = hubRepository;
    }

    @Operation(
            summary = "Search hubs",
            description = "Filter by search term, region, status and capacity required"
    )
    @GetMapping
    public ResponseEntity<List<Hub>> getHubs(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Double capacityRequired) {

        var hubs = hubRepository.findAll();

        if (search != null && !search.isBlank()) {
            String q = search.toLowerCase();
            hubs = hubs.stream()
                    .filter(h ->
                            (h.getName() != null && h.getName().toLowerCase().contains(q))
                                    || (h.getAddress() != null && h.getAddress().toLowerCase().contains(q))
                                    || (h.getLga() != null && h.getLga().toLowerCase().contains(q))
                                    || (h.getHubId() != null && h.getHubId().toLowerCase().contains(q)))
                    .toList();
        }

        if (region != null && !region.isBlank()) {
            hubs = hubs.stream()
                    .filter(h -> region.equalsIgnoreCase(h.getTenantRegionId()))
                    .toList();
        }

        if (status != null && !status.isBlank()) {
            hubs = hubs.stream()
                    .filter(h -> h.getStatus() != null
                            && status.equalsIgnoreCase(h.getStatus().name()))
                    .toList();
        }

        if (capacityRequired != null) {
            hubs = hubs.stream()
                    .filter(h -> h.getCapacityKg() != null
                            && h.getCurrentLoadKg() != null
                            && (h.getCapacityKg() - h.getCurrentLoadKg()) >= capacityRequired)
                    .toList();
        }

        return ResponseEntity.ok(hubs);
    }

    @Operation(summary = "Get hub capacity snapshot")
    @GetMapping("/{hubId}/capacity")
    public ResponseEntity<HubService.CapacitySnapshot> getCapacity(
            @PathVariable Long hubId) {
        return ResponseEntity.ok(hubService.getCapacity(hubId));
    }

    @Operation(
            summary = "Get full hub detail",
            description = "Includes temperature, power status, GPS and capacity"
    )
    @GetMapping("/{hubId}")
    public ResponseEntity<Map<String, Object>> getHubDetail(
            @PathVariable Long hubId) {

        Hub hub = hubService.getHub(hubId);

        Map<String, Object> response = new HashMap<>();
        response.put("id",               hub.getId());
        response.put("hubId",            hub.getHubId());
        response.put("name",             hub.getName());
        response.put("address",          hub.getAddress());
        response.put("lga",              hub.getLga());
        response.put("status",           hub.getStatus());
        response.put("operatingHours",   hub.getOperatingHours());

        // Capacity
        double capacity    = hub.getCapacityKg() != null ? hub.getCapacityKg() : 0;
        double currentLoad = hub.getCurrentLoadKg() != null ? hub.getCurrentLoadKg() : 0;
        response.put("capacityKg",      capacity);
        response.put("currentLoadKg",   currentLoad);
        response.put("availableKg",     capacity - currentLoad);
        response.put("utilizationPct",  capacity > 0
                ? Math.round((currentLoad / capacity) * 100) : 0);

        // Location
        response.put("gpsLat", hub.getGpsLat());
        response.put("gpsLng", hub.getGpsLng());

        // Temperature
        response.put("tempCurrentC",  hub.getTempCurrentC());
        response.put("tempTargetMin", hub.getTempTargetMin());
        response.put("tempTargetMax", hub.getTempTargetMax());
        boolean tempAlert = hub.getTempCurrentC() != null
                && hub.getTempTargetMax() != null
                && hub.getTempCurrentC() > hub.getTempTargetMax();
        response.put("tempAlert", tempAlert);

        // Power
        response.put("powerType",          hub.getPowerType());
        response.put("powerStatus",        hub.getPowerStatus());
        response.put("solarCapacityKw",    hub.getSolarCapacityKw());
        response.put("batteryCapacityKwh", hub.getBatteryCapacityKwh());

        return ResponseEntity.ok(response);
    }
}