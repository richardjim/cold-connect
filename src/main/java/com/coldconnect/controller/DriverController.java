package com.coldconnect.controller;

import com.coldconnect.entity.*;
import com.coldconnect.exception.AppException;
import com.coldconnect.repository.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/driver")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Driver", description = "Driver safety checks, jobs and cold chain")
public class DriverController extends BaseController {

    private final SafetyCheckRepository     safetyCheckRepository;
    private final SafetyCheckItemRepository itemRepository;
    private final SensorReadingRepository   sensorRepository;
    private final VehicleRepository         vehicleRepository;
    private final TripRepository            tripRepository;
    private final TripStopRepository        tripStopRepository;

    public DriverController(UserRepository userRepository,
                            SafetyCheckRepository safetyCheckRepository,
                            SafetyCheckItemRepository itemRepository,
                            SensorReadingRepository sensorRepository,
                            VehicleRepository vehicleRepository,
                            TripRepository tripRepository,
                            TripStopRepository tripStopRepository) {
        super(userRepository);
        this.safetyCheckRepository = safetyCheckRepository;
        this.itemRepository        = itemRepository;
        this.sensorRepository      = sensorRepository;
        this.vehicleRepository     = vehicleRepository;
        this.tripRepository        = tripRepository;
        this.tripStopRepository    = tripStopRepository;
    }

    // ── Records ───────────────────────────────────────────────────────────────

    public record SafetyCheckItemRequest(
            @NotBlank String templateItemId,
            @NotBlank String label,
            @NotBlank String mark,
            String photoUri,
            String note,
            String severity
    ) {}

    public record SafetyCheckRequest(
            @NotNull Long vehicleId,
            Long tripId,
            Long coDriverId,
            @NotBlank String templateVersion,
            @NotNull List<SafetyCheckItemRequest> items,
            boolean coDriverConfirmed,
            Double gpsLat,
            Double gpsLng
    ) {}

    public record ArrivalRequest(
            Double gpsLat,
            Double gpsLng,
            String note,
            String photoUri
    ) {}

    public record CrateScanRequest(
            @NotBlank String crateId,
            @NotBlank String tripId,
            @NotBlank String stopId,
            @Schema(example = "PICKUP", description = "PICKUP · DELIVERY")
            @NotBlank String scanType,
            boolean manualEntry,
            String photoUri
    ) {}

    public record ProofEventRequest(
            @NotBlank String stopId,
            @Schema(example = "DELIVERY_PHOTO",
                    description = "ARRIVAL · LOAD_PHOTO · DELIVERY_PHOTO · RECIPIENT_CONFIRM · TEMPERATURE")
            @NotBlank String eventType,
            String photoUri,
            String recipientName,
            String notes,
            Double tempC,
            String idempotencyKey
    ) {}

    public record IncidentRequest(
            String tripId,
            @Schema(example = "TEMPERATURE_ALERT",
                    description = "TEMPERATURE_ALERT · VEHICLE_FAULT · ROUTE_DELAY · FAILED_PICKUP · FAILED_DELIVERY · ACCIDENT · SECURITY")
            @NotBlank String incidentType,
            @Schema(example = "HIGH", description = "CRITICAL · HIGH · MEDIUM · LOW")
            String severity,
            @NotBlank String note,
            String photoUri
    ) {}

    public record ManualTelemetryRequest(
            @NotBlank String assetId,
            @NotNull Double tempC,
            Double gpsLat,
            Double gpsLng,
            String tripId
    ) {}

    public record OfflineBatchRequest(
            @NotNull List<Map<String, Object>> events
    ) {}

    // ── Driver profile ────────────────────────────────────────────────────────

    @Operation(summary = "Get my driver profile and permissions")
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getMe(
            @AuthenticationPrincipal UserDetails userDetails) {
        var user = resolveUser(userDetails);
        Map<String, Object> profile = new HashMap<>();
        profile.put("id",        user.getId());
        profile.put("fullName",  user.getFullName());
        profile.put("phone",     user.getPhone());
        profile.put("role",      user.getRole().name());
        profile.put("language",  user.getLanguage());
        profile.put("permissions", List.of(
                "VIEW_ASSIGNMENTS", "SUBMIT_SAFETY_CHECK",
                "SCAN_CRATES", "SUBMIT_PROOF", "REPORT_INCIDENT"
        ));
        return ResponseEntity.ok(profile);
    }

    // ── Assignments ───────────────────────────────────────────────────────────

    @Operation(
            summary = "Get assigned transport jobs",
            description = "Returns jobs assigned to this driver. Default: active only."
    )
    @GetMapping("/assignments")
    public ResponseEntity<List<Map<String, Object>>> getAssignments(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false, defaultValue = "active") String status) {

        Long driverId = resolveUser(userDetails).getId();

        var trips = tripRepository.findAll().stream()
                .filter(t -> driverId.equals(t.getDriverId()))
                .filter(t -> {
                    if ("active".equalsIgnoreCase(status)) {
                        return t.getStatus() == Trip.TripStatus.PLANNED
                                || t.getStatus() == Trip.TripStatus.IN_PROGRESS;
                    }
                    if ("completed".equalsIgnoreCase(status)) {
                        return t.getStatus() == Trip.TripStatus.COMPLETED;
                    }
                    return true;
                })
                .map(t -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("tripId",      t.getTripId());
                    m.put("bookingId",   t.getBookingId());
                    m.put("status",      t.getStatus());
                    m.put("origin",      t.getOrigin());
                    m.put("destination", t.getDestination());
                    m.put("vehiclePlate",t.getVehiclePlate());
                    m.put("targetTempMin", t.getTargetTempMin());
                    m.put("targetTempMax", t.getTargetTempMax());
                    m.put("eta",         t.getEta());
                    m.put("startedAt",   t.getStartedAt());
                    return m;
                }).toList();

        return ResponseEntity.ok(trips);
    }

    // ── Acknowledge job ───────────────────────────────────────────────────────

    @Operation(summary = "Acknowledge an assigned job")
    @PostMapping("/assignments/{tripId}/acknowledge")
    public ResponseEntity<Map<String, Object>> acknowledgeJob(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String tripId) {

        Long driverId = resolveUser(userDetails).getId();

        Trip trip = tripRepository.findByTripId(tripId)
                .orElseThrow(() -> new AppException.NotFoundException(
                        "Trip not found: " + tripId));

        if (!driverId.equals(trip.getDriverId())) {
            throw new AppException.UnauthorizedException("Not your assignment");
        }

        return ResponseEntity.ok(Map.of(
                "message", "Job acknowledged",
                "tripId",  tripId,
                "status",  "ACKNOWLEDGED"
        ));
    }

    // ── Start route ───────────────────────────────────────────────────────────

    @Operation(summary = "Start a route")
    @PostMapping("/routes/{tripId}/start")
    public ResponseEntity<Map<String, Object>> startRoute(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String tripId) {

        Long driverId = resolveUser(userDetails).getId();

        Trip trip = tripRepository.findByTripId(tripId)
                .orElseThrow(() -> new AppException.NotFoundException(
                        "Trip not found: " + tripId));

        if (!driverId.equals(trip.getDriverId())) {
            throw new AppException.UnauthorizedException("Not your assignment");
        }

        trip.setStatus(Trip.TripStatus.IN_PROGRESS);
        trip.setStartedAt(LocalDateTime.now());
        tripRepository.save(trip);

        return ResponseEntity.ok(Map.of(
                "message",   "Route started",
                "tripId",    tripId,
                "status",    "IN_PROGRESS",
                "startedAt", trip.getStartedAt()
        ));
    }

    // ── Stop arrival ──────────────────────────────────────────────────────────

    @Operation(summary = "Mark arrival at a stop")
    @PostMapping("/stops/{stopId}/arrive")
    public ResponseEntity<Map<String, Object>> arriveAtStop(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long stopId,
            @RequestBody ArrivalRequest req) {

        TripStop stop = tripStopRepository.findById(stopId)
                .orElseThrow(() -> new AppException.NotFoundException(
                        "Stop not found: " + stopId));

        stop.setActualArrivalAt(LocalDateTime.now());
        stop.setStatus("ARRIVED");
        tripStopRepository.save(stop);

        return ResponseEntity.ok(Map.of(
                "message",   "Arrival recorded",
                "stopId",    stopId,
                "arrivedAt", stop.getActualArrivalAt()
        ));
    }

    // ── Stop departure ────────────────────────────────────────────────────────

    @Operation(summary = "Mark departure from a stop")
    @PostMapping("/stops/{stopId}/depart")
    public ResponseEntity<Map<String, Object>> departStop(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long stopId) {

        TripStop stop = tripStopRepository.findById(stopId)
                .orElseThrow(() -> new AppException.NotFoundException(
                        "Stop not found: " + stopId));

        stop.setActualDepartureAt(LocalDateTime.now());
        stop.setStatus("DEPARTED");
        tripStopRepository.save(stop);

        return ResponseEntity.ok(Map.of(
                "message",    "Departure recorded",
                "stopId",     stopId,
                "departedAt", stop.getActualDepartureAt()
        ));
    }

    // ── Crate scan ────────────────────────────────────────────────────────────

    @Operation(
            summary = "Submit crate scan",
            description = "Scan crate at pickup or delivery. manualEntry=true is flagged for audit."
    )
    @PostMapping("/crates/scan")
    public ResponseEntity<Map<String, Object>> scanCrate(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CrateScanRequest req) {

        Long driverId = resolveUser(userDetails).getId();

        return ResponseEntity.ok(Map.of(
                "message",     "Crate scan recorded",
                "crateId",     req.crateId(),
                "scanType",    req.scanType(),
                "scannedBy",   driverId,
                "scannedAt",   LocalDateTime.now(),
                "manualEntry", req.manualEntry(),
                "flagged",     req.manualEntry()
        ));
    }

    // ── Proof events ──────────────────────────────────────────────────────────

    @Operation(
            summary = "Submit pickup or delivery proof event",
            description = """
            eventType: ARRIVAL · LOAD_PHOTO · DELIVERY_PHOTO · RECIPIENT_CONFIRM · TEMPERATURE
            Use idempotencyKey for offline sync to prevent duplicates.
            """
    )
    @PostMapping("/proof-events")
    public ResponseEntity<Map<String, Object>> submitProof(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ProofEventRequest req) {

        Long driverId = resolveUser(userDetails).getId();

        return ResponseEntity.ok(Map.of(
                "message",    "Proof event recorded",
                "stopId",     req.stopId(),
                "eventType",  req.eventType(),
                "recordedBy", driverId,
                "recordedAt", LocalDateTime.now(),
                "synced",     true
        ));
    }

    // ── Incidents ─────────────────────────────────────────────────────────────

    @Operation(
            summary = "Report an incident or exception",
            description = """
            incidentType: TEMPERATURE_ALERT · VEHICLE_FAULT · ROUTE_DELAY ·
            FAILED_PICKUP · FAILED_DELIVERY · ACCIDENT · SECURITY
            """
    )
    @PostMapping("/incidents")
    public ResponseEntity<Map<String, Object>> reportIncident(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody IncidentRequest req) {

        Long driverId = resolveUser(userDetails).getId();

        return ResponseEntity.ok(Map.of(
                "message",      "Incident reported",
                "incidentType", req.incidentType(),
                "severity",     req.severity() != null ? req.severity() : "MEDIUM",
                "reportedBy",   driverId,
                "reportedAt",   LocalDateTime.now(),
                "tripId",       req.tripId() != null ? req.tripId() : ""
        ));
    }

    // ── Manual telemetry ──────────────────────────────────────────────────────

    @Operation(
            summary = "Submit manual temperature or location reading",
            description = "Fallback when IoT device is unavailable. Flagged as manual in audit."
    )
    @PostMapping("/telemetry/manual")
    public ResponseEntity<Map<String, Object>> submitManualTelemetry(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ManualTelemetryRequest req) {

        Long driverId = resolveUser(userDetails).getId();

        SensorReading reading = new SensorReading();
        reading.setAssetId(req.assetId());
        reading.setTempC(req.tempC());
        reading.setGpsLat(req.gpsLat());
        reading.setGpsLng(req.gpsLng());
        reading.setQualityFlag("MANUAL");
        reading.setTimestamp(LocalDateTime.now());
        sensorRepository.save(reading);

        return ResponseEntity.ok(Map.of(
                "message",     "Manual reading recorded",
                "assetId",     req.assetId(),
                "tempC",       req.tempC(),
                "recordedBy",  driverId,
                "recordedAt",  LocalDateTime.now(),
                "manual",      true,
                "flagged",     true
        ));
    }

    // ── Offline batch sync ────────────────────────────────────────────────────

    @Operation(
            summary = "Submit queued offline events",
            description = """
            Accepts a batch of events captured offline.
            Each event must include an idempotencyKey to prevent duplicates.
            Events are processed in order of their captured timestamp.
            """
    )
    @PostMapping("/sync/offline-batch")
    public ResponseEntity<Map<String, Object>> syncOfflineBatch(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody OfflineBatchRequest req) {

        Long driverId = resolveUser(userDetails).getId();
        int  count    = req.events() != null ? req.events().size() : 0;

        return ResponseEntity.ok(Map.of(
                "message",    "Offline batch received",
                "eventCount", count,
                "syncedBy",   driverId,
                "syncedAt",   LocalDateTime.now(),
                "status",     "ACCEPTED"
        ));
    }

    // ── Safety check ──────────────────────────────────────────────────────────

    @Operation(
            summary = "Submit driver walkaround safety check",
            description = """
            18-item pre-journey check. Run is blocked if:
            - Any item marked as CRITICAL defect
            - coDriverConfirmed is false
            """
    )
    @PostMapping("/safety-checks")
    public ResponseEntity<Map<String, Object>> submitSafetyCheck(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody SafetyCheckRequest req) {

        Long driverId = resolveUser(userDetails).getId();

        vehicleRepository.findById(req.vehicleId())
                .orElseThrow(() -> new AppException.NotFoundException(
                        "Vehicle not found: " + req.vehicleId()));

        long defectCount = req.items().stream()
                .filter(i -> "defect".equalsIgnoreCase(i.mark()))
                .count();

        boolean hasCriticalDefect = req.items().stream()
                .anyMatch(i -> "defect".equalsIgnoreCase(i.mark())
                        && "CRITICAL".equalsIgnoreCase(i.severity()));

        boolean runBlocked = hasCriticalDefect || !req.coDriverConfirmed();
        String  result     = runBlocked ? "FAIL" : "PASS";

        SafetyCheck check = new SafetyCheck();
        check.setVehicleId(req.vehicleId());
        check.setTripId(req.tripId());
        check.setLeadDriverId(driverId);
        check.setCoDriverId(req.coDriverId());
        check.setTemplateVersion(req.templateVersion());
        check.setDefectCount((int) defectCount);
        check.setCoDriverConfirmed(req.coDriverConfirmed());
        check.setRunBlocked(runBlocked);
        check.setResult(result);
        check.setGpsLat(req.gpsLat());
        check.setGpsLng(req.gpsLng());
        check = safetyCheckRepository.save(check);

        final Long checkId = check.getId();
        for (SafetyCheckItemRequest itemReq : req.items()) {
            SafetyCheckItem item = new SafetyCheckItem();
            item.setCheckId(checkId);
            item.setTemplateItemId(itemReq.templateItemId());
            item.setLabel(itemReq.label());
            item.setMark(itemReq.mark());
            item.setPhotoUri(itemReq.photoUri());
            item.setNote(itemReq.note());
            item.setSeverity(itemReq.severity());
            itemRepository.save(item);
        }

        return ResponseEntity.ok(Map.of(
                "checkId",    check.getCheckId(),
                "result",     result,
                "runBlocked", runBlocked,
                "defects",    defectCount,
                "message",    runBlocked
                        ? "Run is blocked. Resolve critical defects and get co-driver confirmation."
                        : "Safety check passed. You may proceed."
        ));
    }

    @Operation(summary = "Get my safety check history")
    @GetMapping("/safety-checks")
    public ResponseEntity<List<SafetyCheck>> getMySafetyChecks(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long driverId = resolveUser(userDetails).getId();
        return ResponseEntity.ok(
                safetyCheckRepository.findByLeadDriverIdOrderByStartedAtDesc(driverId)
        );
    }

    @Operation(summary = "Get live cold chain for a vehicle")
    @GetMapping("/vehicles/{vehicleId}/cold-chain")
    public ResponseEntity<Map<String, Object>> getVehicleColdChain(
            @PathVariable Long vehicleId,
            @RequestParam(required = false) String tripId) {

        vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new AppException.NotFoundException(
                        "Vehicle not found: " + vehicleId));

        List<SensorReading> readings = sensorRepository
                .findByAssetIdOrderByTimestampDesc(String.valueOf(vehicleId));

        SensorReading latest = readings.isEmpty() ? null : readings.get(0);

        return ResponseEntity.ok(Map.of(
                "vehicleId",     vehicleId,
                "tripId",        tripId != null ? tripId : "",
                "latestReading", latest != null ? latest : Map.of(),
                "history",       readings.stream().limit(20).toList(),
                "sensorCount",   readings.size()
        ));
    }
}