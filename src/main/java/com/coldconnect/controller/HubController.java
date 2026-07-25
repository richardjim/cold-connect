package com.coldconnect.controller;

import com.coldconnect.entity.Hub;
import com.coldconnect.repository.UserRepository;
import com.coldconnect.service.HubService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/hubs")
@Tag(name = "Hubs", description = "Hub search and capacity")
public class HubController extends BaseController {

    private final HubService hubService;

    public HubController(UserRepository userRepository, HubService hubService) {
        super(userRepository);
        this.hubService = hubService;
    }

    @Operation(summary = "Search hubs by region")
    @GetMapping
    public ResponseEntity<List<Hub>> searchHubs(@RequestParam(required = false) String region) {
        return ResponseEntity.ok(hubService.searchHubs(region));
    }

    @Operation(summary = "Get hub capacity snapshot")
    @GetMapping("/{hubId}/capacity")
    public ResponseEntity<HubService.CapacitySnapshot> getCapacity(@PathVariable Long hubId) {
        return ResponseEntity.ok(hubService.getCapacity(hubId));
    }

    @Operation(summary = "Get full hub detail including temperature and power status")
    @GetMapping("/{hubId}")
    public ResponseEntity<Map<String, Object>> getHubDetail(
            @PathVariable Long hubId) {

        Hub hub = hubService.getHub(hubId);

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("id",               hub.getId());
        response.put("hubId",            hub.getHubId());
        response.put("name",             hub.getName());
        response.put("address",          hub.getAddress());
        response.put("lga",              hub.getLga());
        response.put("status",           hub.getStatus());
        response.put("operatingHours",   hub.getOperatingHours());

        // Capacity
        response.put("capacityKg",       hub.getCapacityKg());
        response.put("currentLoadKg",    hub.getCurrentLoadKg());
        response.put("availableKg",      hub.getCapacityKg() - hub.getCurrentLoadKg());
        response.put("utilizationPct",   hub.getCapacityKg() > 0
                ? Math.round((hub.getCurrentLoadKg() / hub.getCapacityKg()) * 100) : 0);

        // Location
        response.put("gpsLat",           hub.getGpsLat());
        response.put("gpsLng",           hub.getGpsLng());

        // Temperature
        response.put("tempCurrentC",     hub.getTempCurrentC());
        response.put("tempTargetMin",    hub.getTempTargetMin());
        response.put("tempTargetMax",    hub.getTempTargetMax());
        boolean tempAlert = hub.getTempCurrentC() != null
                && hub.getTempTargetMax() != null
                && hub.getTempCurrentC() > hub.getTempTargetMax();
        response.put("tempAlert",        tempAlert);

        // Power
        response.put("powerType",        hub.getPowerType());
        response.put("powerStatus",      hub.getPowerStatus());
        response.put("solarCapacityKw",  hub.getSolarCapacityKw());
        response.put("batteryCapacityKwh", hub.getBatteryCapacityKwh());

        return ResponseEntity.ok(response);
    }


}
