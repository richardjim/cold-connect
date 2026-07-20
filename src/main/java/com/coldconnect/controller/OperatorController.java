package com.coldconnect.controller;

import com.coldconnect.entity.HubZone;
import com.coldconnect.entity.SensorReading;
import com.coldconnect.exception.AppException;
import com.coldconnect.repository.HubRepository;
import com.coldconnect.repository.HubZoneRepository;
import com.coldconnect.repository.SensorReadingRepository;
import com.coldconnect.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/operator")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Operator", description = "Hub operator cold room monitoring")
public class OperatorController extends BaseController {

    private final HubRepository           hubRepository;
    private final HubZoneRepository       hubZoneRepository;
    private final SensorReadingRepository sensorRepository;

    public OperatorController(UserRepository userRepository,
                              HubRepository hubRepository,
                              HubZoneRepository hubZoneRepository,
                              SensorReadingRepository sensorRepository) {
        super(userRepository);
        this.hubRepository     = hubRepository;
        this.hubZoneRepository = hubZoneRepository;
        this.sensorRepository  = sensorRepository;
    }

    @Operation(
            summary = "Get live cold room status for a hub",
            description = "Returns per-container live temperature, door events, battery and alerts."
    )
    @GetMapping("/hubs/{hubId}/cold-rooms")
    public ResponseEntity<Map<String, Object>> getColdRooms(
            @PathVariable Long hubId) {

        hubRepository.findById(hubId)
                .orElseThrow(() -> new AppException.NotFoundException(
                        "Hub not found: " + hubId));

        List<HubZone> zones = hubZoneRepository.findByHubId(hubId);

        List<Map<String, Object>> zoneStatus = new ArrayList<>();

        for (HubZone zone : zones) {
            List<SensorReading> readings =
                    sensorRepository.findByAssetIdOrderByTimestampDesc(
                            String.valueOf(zone.getId()));

            SensorReading latest = readings.isEmpty() ? null : readings.get(0);

            boolean tempAlert = latest != null
                    && zone.getTempTargetMax() != null
                    && latest.getTempC() != null
                    && latest.getTempC() > zone.getTempTargetMax();

            Map<String, Object> zoneMap = new HashMap<>();
            zoneMap.put("zoneId",        zone.getId());
            zoneMap.put("label",         zone.getLabel() != null ? zone.getLabel() : "");
            zoneMap.put("tempTargetMin", zone.getTempTargetMin() != null ? zone.getTempTargetMin() : 0);
            zoneMap.put("tempTargetMax", zone.getTempTargetMax() != null ? zone.getTempTargetMax() : 0);
            zoneMap.put("currentTemp",   latest != null && latest.getTempC() != null ? latest.getTempC() : "N/A");
            zoneMap.put("humidity",      latest != null && latest.getHumidityPct() != null ? latest.getHumidityPct() : "N/A");
            zoneMap.put("doorEvent",     latest != null && latest.getDoorEvent() != null ? latest.getDoorEvent() : "UNKNOWN");
            zoneMap.put("batteryPct",    latest != null && latest.getBatteryPct() != null ? latest.getBatteryPct() : "N/A");
            zoneMap.put("tempAlert",     tempAlert);
            zoneMap.put("lastReading",   latest != null ? latest.getTimestamp() : "N/A");
            zoneMap.put("capacityKg",    zone.getCapacityKg() != null ? zone.getCapacityKg() : 0);
            zoneMap.put("currentLoadKg", zone.getCurrentLoadKg() != null ? zone.getCurrentLoadKg() : 0);
            zoneStatus.add(zoneMap);
        }

        long alertCount = zoneStatus.stream()
                .filter(z -> Boolean.TRUE.equals(z.get("tempAlert")))
                .count();

        return ResponseEntity.ok(Map.of(
                "hubId",      hubId,
                "coldRooms",  zoneStatus,
                "zoneCount",  zones.size(),
                "alertCount", alertCount
        ));
    }
}