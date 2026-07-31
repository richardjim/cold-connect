package com.coldconnect.controller;

import com.coldconnect.entity.IotThreshold;
import com.coldconnect.exception.AppException;
import com.coldconnect.repository.IotThresholdRepository;
import com.coldconnect.repository.UserRepository;
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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/iot/thresholds")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin IoT", description = "Cold chain IoT monitoring — Admin only")
public class AdminIotThresholdController extends BaseController {

    private final IotThresholdRepository thresholdRepository;

    public AdminIotThresholdController(UserRepository userRepository,
                                       IotThresholdRepository thresholdRepository) {
        super(userRepository);
        this.thresholdRepository = thresholdRepository;
    }

    public record ThresholdRequest(
            @Schema(example = "HUB-JOS-01")
            @NotBlank String hubId,

            @Schema(example = "COM-001", description = "Optional — if null applies to all commodities in hub")
            String commodityId,

            @Schema(example = "2.0", description = "Minimum safe temperature in °C")
            @NotNull Double tempMinC,

            @Schema(example = "8.0", description = "Maximum safe temperature in °C")
            @NotNull Double tempMaxC,

            @Schema(example = "40.0", description = "Minimum humidity %")
            Double humidityMinPct,

            @Schema(example = "85.0", description = "Maximum humidity %")
            Double humidityMaxPct,

            @Schema(example = "30", description = "Minutes before sensor is flagged as stale")
            Integer staleAfterMinutes
    ) {}

    @Operation(
            summary = "Get all IoT alert thresholds",
            description = "Filter by hubId or commodityId"
    )
    @GetMapping
    public ResponseEntity<List<IotThreshold>> getThresholds(
            @RequestParam(required = false) String hubId,
            @RequestParam(required = false) String commodityId) {

        if (hubId != null && commodityId != null) {
            return ResponseEntity.ok(
                    thresholdRepository.findByHubIdAndCommodityId(hubId, commodityId)
                            .map(List::of).orElse(List.of())
            );
        }
        if (hubId != null) {
            return ResponseEntity.ok(thresholdRepository.findByHubId(hubId));
        }
        if (commodityId != null) {
            return ResponseEntity.ok(thresholdRepository.findByCommodityId(commodityId));
        }
        return ResponseEntity.ok(thresholdRepository.findAll());
    }

    @Operation(
            summary = "Create IoT alert threshold",
            description = "Set min/max temp and humidity thresholds per hub and commodity"
    )
    @PostMapping
    public ResponseEntity<IotThreshold> createThreshold(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ThresholdRequest req) {

        String adminName = resolveUser(userDetails).getFullName();

        IotThreshold threshold = new IotThreshold();
        threshold.setHubId(req.hubId());
        threshold.setCommodityId(req.commodityId());
        threshold.setTempMinC(req.tempMinC());
        threshold.setTempMaxC(req.tempMaxC());
        threshold.setHumidityMinPct(req.humidityMinPct());
        threshold.setHumidityMaxPct(req.humidityMaxPct());
        threshold.setStaleAfterMinutes(
                req.staleAfterMinutes() != null ? req.staleAfterMinutes() : 30);
        threshold.setCreatedBy(adminName);

        return ResponseEntity.ok(thresholdRepository.save(threshold));
    }

    @Operation(summary = "Update an IoT threshold")
    @PatchMapping("/{id}")
    public ResponseEntity<IotThreshold> updateThreshold(
            @PathVariable Long id,
            @RequestBody ThresholdRequest req) {

        IotThreshold threshold = thresholdRepository.findById(id)
                .orElseThrow(() -> new AppException.NotFoundException(
                        "Threshold not found: " + id));

        if (req.tempMinC() != null) threshold.setTempMinC(req.tempMinC());
        if (req.tempMaxC() != null) threshold.setTempMaxC(req.tempMaxC());
        if (req.humidityMinPct() != null) threshold.setHumidityMinPct(req.humidityMinPct());
        if (req.humidityMaxPct() != null) threshold.setHumidityMaxPct(req.humidityMaxPct());
        if (req.staleAfterMinutes() != null) threshold.setStaleAfterMinutes(req.staleAfterMinutes());

        return ResponseEntity.ok(thresholdRepository.save(threshold));
    }

    @Operation(summary = "Delete an IoT threshold")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteThreshold(@PathVariable Long id) {
        thresholdRepository.findById(id)
                .orElseThrow(() -> new AppException.NotFoundException(
                        "Threshold not found: " + id));
        thresholdRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Threshold deleted"));
    }
}