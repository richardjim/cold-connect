package com.coldconnect.controller;

import com.coldconnect.entity.*;
import com.coldconnect.repository.SensorReadingRepository;
import com.coldconnect.repository.UserRepository;
import com.coldconnect.service.TrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Tracking", description = "Crate, trip and cold-chain tracking")
public class TrackingController extends BaseController {

    private final TrackingService         trackingService;
    private final SensorReadingRepository sensorRepository;

    public TrackingController(UserRepository userRepository,
                              TrackingService trackingService,
                              SensorReadingRepository sensorRepository) {
        super(userRepository);
        this.trackingService  = trackingService;
        this.sensorRepository = sensorRepository;
    }

    @Operation(summary = "Get all my crates")
    @GetMapping("/v1/crates")
    public ResponseEntity<List<CrateLot>> getMyCrates(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = resolveUser(userDetails).getId();
        return ResponseEntity.ok(trackingService.getCustomerCrates(userId));
    }

    @Operation(summary = "Get crate detail")
    @GetMapping("/v1/crates/{crateId}")
    public ResponseEntity<CrateLot> getCrate(@PathVariable String crateId) {
        return ResponseEntity.ok(trackingService.getCrate(crateId));
    }

    @Operation(summary = "Get trip detail")
    @GetMapping("/v1/trips/{tripId}")
    public ResponseEntity<Trip> getTrip(@PathVariable String tripId) {
        return ResponseEntity.ok(trackingService.getTrip(tripId));
    }

    @Operation(summary = "Get trip stops in sequence")
    @GetMapping("/v1/trips/{tripId}/stops")
    public ResponseEntity<List<TripStop>> getTripStops(@PathVariable String tripId) {
        return ResponseEntity.ok(trackingService.getTripStops(tripId));
    }

    @Operation(summary = "Get sensor history for an asset")
    @GetMapping("/v1/sensor-readings/{assetId}")
    public ResponseEntity<List<SensorReading>> getSensorHistory(
            @PathVariable String assetId) {
        return ResponseEntity.ok(trackingService.getSensorHistory(assetId));
    }

    @Operation(summary = "Get cold-chain traceability for a lot")
    @GetMapping("/v1/lots/{lotId}/chain")
    public ResponseEntity<List<ChainEvent>> getChain(@PathVariable String lotId) {
        return ResponseEntity.ok(trackingService.getChain(lotId));
    }

    @Operation(
            summary = "Get live IoT readings for an asset",
            description = "assetId can be a hub zone, truck, pallet or crate sensor ID"
    )
    @GetMapping("/v1/iot/readings/{assetId}")
    public ResponseEntity<Map<String, Object>> getIotReadings(
            @PathVariable String assetId,
            @RequestParam(defaultValue = "20") int limit) {

        List<SensorReading> readings =
                sensorRepository.findByAssetIdOrderByTimestampDesc(assetId);

        SensorReading latest = readings.isEmpty() ? null : readings.get(0);

        boolean tempAlert = false;
        if (latest != null && latest.getTempC() != null) {
            tempAlert = latest.getTempC() > 8.0 || latest.getTempC() < 0.0;
        }

        return ResponseEntity.ok(Map.of(
                "assetId",   assetId,
                "latest",    latest != null ? latest : Map.of(),
                "history",   readings.stream().limit(limit).toList(),
                "tempAlert", tempAlert,
                "count",     readings.size()
        ));
    }

    @Operation(
            summary = "Get all IoT sensor readings",
            description = "Returns latest reading per asset with alert flags"
    )
    @GetMapping("/v1/iot/readings")
    public ResponseEntity<Map<String, Object>> getAllIotReadings(
            @RequestParam(required = false) String assetId,
            @RequestParam(defaultValue = "20") int limit) {

        List<SensorReading> readings;

        if (assetId != null) {
            readings = sensorRepository.findByAssetIdOrderByTimestampDesc(assetId);
        } else {
            readings = sensorRepository.findAll(
                    org.springframework.data.domain.PageRequest.of(0, limit,
                            org.springframework.data.domain.Sort.by("timestamp").descending())
            ).getContent();
        }

        long alertCount = readings.stream()
                .filter(r -> r.getTempC() != null &&
                        (r.getTempC() > 8.0 || r.getTempC() < 0.0))
                .count();

        return ResponseEntity.ok(Map.of(
                "readings",   readings,
                "count",      readings.size(),
                "alertCount", alertCount
        ));
    }

    @Operation(
            summary = "Get my crates eligible for sale",
            description = """
        Returns crates owned by the user that are currently IN_STORAGE
        and can be listed on the marketplace.
        Use the crateId from this response when posting to POST /v1/marketplace/lots.
        """
    )
    @GetMapping("/v1/sell/crates")
    public ResponseEntity<Map<String, Object>> getEligibleCratesForSale(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = resolveUser(userDetails).getId();
        var crates  = trackingService.getEligibleCratesForSale(userId);
        return ResponseEntity.ok(Map.of(
                "crates", crates,
                "count",  crates.size(),
                "hint",   "Use crateId when listing on POST /v1/marketplace/lots"
        ));
    }
}