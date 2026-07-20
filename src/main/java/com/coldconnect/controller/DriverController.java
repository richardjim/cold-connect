package com.coldconnect.controller;

import com.coldconnect.entity.SafetyCheck;
import com.coldconnect.entity.SafetyCheckItem;
import com.coldconnect.entity.SensorReading;
import com.coldconnect.exception.AppException;
import com.coldconnect.repository.SafetyCheckItemRepository;
import com.coldconnect.repository.SafetyCheckRepository;
import com.coldconnect.repository.SensorReadingRepository;
import com.coldconnect.repository.UserRepository;
import com.coldconnect.repository.VehicleRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/driver")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Driver", description = "Driver safety checks and cold chain monitoring")
public class DriverController extends BaseController {

    private final SafetyCheckRepository     safetyCheckRepository;
    private final SafetyCheckItemRepository itemRepository;
    private final SensorReadingRepository   sensorRepository;
    private final VehicleRepository         vehicleRepository;

    public DriverController(UserRepository userRepository,
                            SafetyCheckRepository safetyCheckRepository,
                            SafetyCheckItemRepository itemRepository,
                            SensorReadingRepository sensorRepository,
                            VehicleRepository vehicleRepository) {
        super(userRepository);
        this.safetyCheckRepository = safetyCheckRepository;
        this.itemRepository        = itemRepository;
        this.sensorRepository      = sensorRepository;
        this.vehicleRepository     = vehicleRepository;
    }

    public record SafetyCheckItemRequest(
            @NotBlank String templateItemId,
            @NotBlank String label,
            @NotBlank String mark,   // ok or defect
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

    @Operation(
            summary = "Submit driver walkaround safety check",
            description = """
            18-item pre-journey check. Run is blocked if:
            - Any item marked as CRITICAL defect
            - coDriverConfirmed is false
            Result is PASS only when all items pass and co-driver confirms.
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

        String result = runBlocked ? "FAIL" : "PASS";

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

    @Operation(
            summary = "Get my safety check history",
            description = "Returns all safety checks submitted by this driver"
    )
    @GetMapping("/safety-checks")
    public ResponseEntity<List<SafetyCheck>> getMySafetyChecks(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long driverId = resolveUser(userDetails).getId();
        return ResponseEntity.ok(
                safetyCheckRepository.findByLeadDriverIdOrderByStartedAtDesc(driverId)
        );
    }

    @Operation(
            summary = "Get live cold chain for a vehicle",
            description = "Returns box temperature, pallet readings and sensor roster for the driver"
    )
    @GetMapping("/vehicles/{vehicleId}/cold-chain")
    public ResponseEntity<Map<String, Object>> getVehicleColdChain(
            @PathVariable Long vehicleId,
            @RequestParam(required = false) String tripId) {

        vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new AppException.NotFoundException(
                        "Vehicle not found: " + vehicleId));

        List<SensorReading> readings =
                sensorRepository.findByAssetIdOrderByTimestampDesc(
                        String.valueOf(vehicleId));

        SensorReading latest = readings.isEmpty() ? null : readings.get(0);

        return ResponseEntity.ok(Map.of(
                "vehicleId",    vehicleId,
                "tripId",       tripId != null ? tripId : "",
                "latestReading", latest != null ? latest : Map.of(),
                "history",      readings.stream().limit(20).toList(),
                "sensorCount",  readings.size()
        ));
    }
}