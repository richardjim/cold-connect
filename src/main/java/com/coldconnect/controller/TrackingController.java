package com.coldconnect.controller;

import com.coldconnect.entity.*;
import com.coldconnect.repository.UserRepository;
import com.coldconnect.service.TrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Tracking", description = "Crate, trip and cold-chain tracking")
public class TrackingController extends BaseController {

    private final TrackingService trackingService;

    public TrackingController(UserRepository userRepository, TrackingService trackingService) {
        super(userRepository);
        this.trackingService = trackingService;
    }

    @Operation(summary = "Get crate/lot detail")
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

    @Operation(summary = "Get sensor/temperature history for asset")
    @GetMapping("/v1/sensor-readings/{assetId}")
    public ResponseEntity<List<SensorReading>> getSensorHistory(@PathVariable String assetId) {
        return ResponseEntity.ok(trackingService.getSensorHistory(assetId));
    }

    @Operation(summary = "Get full cold-chain traceability for a lot")
    @GetMapping("/v1/lots/{lotId}/chain")
    public ResponseEntity<List<ChainEvent>> getChain(@PathVariable String lotId) {
        return ResponseEntity.ok(trackingService.getChain(lotId));
    }
}
