package com.coldconnect.controller;

import com.coldconnect.entity.Trip;
import com.coldconnect.entity.TripStop;
import com.coldconnect.exception.AppException;
import com.coldconnect.repository.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/transport-legs")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Tracking", description = "Customer transport tracking")
public class TransportLegController extends BaseController {

    private final TripRepository          tripRepository;
    private final TripStopRepository      tripStopRepository;
    private final BookingRepository       bookingRepository;
    private final SensorReadingRepository sensorRepository;

    public TransportLegController(UserRepository userRepository,
                                  TripRepository tripRepository,
                                  TripStopRepository tripStopRepository,
                                  BookingRepository bookingRepository,
                                  SensorReadingRepository sensorRepository) {
        super(userRepository);
        this.tripRepository    = tripRepository;
        this.tripStopRepository = tripStopRepository;
        this.bookingRepository  = bookingRepository;
        this.sensorRepository   = sensorRepository;
    }

    @Operation(
            summary = "Get active transport legs for customer",
            description = """
            Returns all active transport legs linked to the logged-in customer.
            Sorted by priority: ALERT first, then soonest ETA, then IN_TRANSIT, then recent.
            """
    )
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getActiveLegs(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String status) {

        Long userId = resolveUser(userDetails).getId();

        var trips = tripRepository.findAll().stream()
                .filter(t -> userId.equals(t.getCustomerId()))
                .filter(t -> {
                    if (status != null) {
                        return status.equalsIgnoreCase(
                                t.getStatus() != null ? t.getStatus().name() : "");
                    }
                    // Default: show active legs
                    return t.getStatus() == Trip.TripStatus.PLANNED
                            || t.getStatus() == Trip.TripStatus.IN_PROGRESS;
                })
                .map(this::toSummary)
                .toList();

        return ResponseEntity.ok(trips);
    }

    @Operation(summary = "Get transport leg detail")
    @GetMapping("/{tripId}")
    public ResponseEntity<Map<String, Object>> getLegDetail(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String tripId) {

        Long userId = resolveUser(userDetails).getId();

        Trip trip = tripRepository.findByTripId(tripId)
                .orElseThrow(() -> new AppException.NotFoundException(
                        "Transport leg not found: " + tripId));

        if (!userId.equals(trip.getCustomerId())) {
            throw new AppException.UnauthorizedException(
                    "Not your transport leg");
        }

        Map<String, Object> detail = new HashMap<>(toSummary(trip));

        // Add stops
        var stops = tripStopRepository
                .findByTripIdOrderBySequenceAsc(trip.getId());
        detail.put("stops", stops);

        return ResponseEntity.ok(detail);
    }

    @Operation(
            summary = "Get timeline events for a transport leg",
            description = "Returns ordered timeline visible to customer"
    )
    @GetMapping("/{tripId}/timeline")
    public ResponseEntity<List<Map<String, Object>>> getTimeline(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String tripId) {

        Long userId = resolveUser(userDetails).getId();

        Trip trip = tripRepository.findByTripId(tripId)
                .orElseThrow(() -> new AppException.NotFoundException(
                        "Transport leg not found: " + tripId));

        if (!userId.equals(trip.getCustomerId())) {
            throw new AppException.UnauthorizedException("Not your transport leg");
        }

        var stops = tripStopRepository
                .findByTripIdOrderBySequenceAsc(trip.getId());

        var timeline = stops.stream().map(stop -> {
            Map<String, Object> event = new HashMap<>();
            event.put("sequence",    stop.getSequence());
            event.put("type",        stop.getStopType());
            event.put("location",    stop.getLocation());
            event.put("status",      stop.getStatus());
            event.put("plannedEta",  stop.getPlannedEta());
            event.put("arrivedAt",   stop.getActualArrivalAt());
            event.put("departedAt",  stop.getActualDepartureAt());
            return event;
        }).toList();

        return ResponseEntity.ok(timeline);
    }

    @Operation(
            summary = "Get latest telemetry for a transport leg",
            description = "Returns current temperature, GPS and stale status"
    )
    @GetMapping("/{tripId}/telemetry/latest")
    public ResponseEntity<Map<String, Object>> getLatestTelemetry(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String tripId) {

        Long userId = resolveUser(userDetails).getId();

        Trip trip = tripRepository.findByTripId(tripId)
                .orElseThrow(() -> new AppException.NotFoundException(
                        "Transport leg not found: " + tripId));

        if (!userId.equals(trip.getCustomerId())) {
            throw new AppException.UnauthorizedException("Not your transport leg");
        }

        var readings = sensorRepository
                .findByAssetIdOrderByTimestampDesc(tripId);

        Map<String, Object> telemetry = new HashMap<>();

        if (readings.isEmpty()) {
            telemetry.put("available",   false);
            telemetry.put("message",     "Temperature data not available yet");
            telemetry.put("isStale",     true);
        } else {
            var latest = readings.get(0);
            boolean stale = latest.getTimestamp() != null
                    && latest.getTimestamp().isBefore(
                    java.time.LocalDateTime.now().minusMinutes(30));

            double tempC = latest.getTempC() != null ? latest.getTempC() : 0;
            String tempStatus = tempC > 8.0 || tempC < 0.0 ? "WARNING"
                    : tempC > 6.0 ? "CAUTION" : "SAFE";

            telemetry.put("available",    true);
            telemetry.put("tempC",        tempC);
            telemetry.put("tempStatus",   tempStatus);
            telemetry.put("targetMinC",   trip.getTargetTempMin());
            telemetry.put("targetMaxC",   trip.getTargetTempMax());
            telemetry.put("lastUpdateAt", latest.getTimestamp());
            telemetry.put("isStale",      stale);
            telemetry.put("dataSource",   "SENSOR");
        }

        return ResponseEntity.ok(telemetry);
    }

    @Operation(summary = "Confirm delivery receipt")
    @PostMapping("/{tripId}/confirm-delivery")
    public ResponseEntity<Map<String, Object>> confirmDelivery(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String tripId) {

        Long userId = resolveUser(userDetails).getId();

        Trip trip = tripRepository.findByTripId(tripId)
                .orElseThrow(() -> new AppException.NotFoundException(
                        "Transport leg not found: " + tripId));

        if (!userId.equals(trip.getCustomerId())) {
            throw new AppException.UnauthorizedException("Not your transport leg");
        }

        trip.setStatus(Trip.TripStatus.COMPLETED);
        tripRepository.save(trip);

        return ResponseEntity.ok(Map.of(
                "message",  "Delivery confirmed",
                "tripId",   tripId,
                "status",   "COMPLETED"
        ));
    }

    private Map<String, Object> toSummary(Trip trip) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("tripId",      trip.getTripId());
        summary.put("bookingId",   trip.getBookingId());
        summary.put("status",      trip.getStatus());
        summary.put("driverName",  trip.getDriverName());
        summary.put("vehiclePlate",trip.getVehiclePlate());
        summary.put("origin",      trip.getOrigin());
        summary.put("destination", trip.getDestination());
        summary.put("targetTempMin", trip.getTargetTempMin());
        summary.put("targetTempMax", trip.getTargetTempMax());
        summary.put("startedAt",   trip.getStartedAt());
        summary.put("eta",         trip.getEta());
        return summary;
    }
}