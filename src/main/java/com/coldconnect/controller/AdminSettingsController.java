package com.coldconnect.controller;

import com.coldconnect.entity.ServiceRate;
import com.coldconnect.exception.AppException;
import com.coldconnect.repository.ServiceRateRepository;
import com.coldconnect.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/settings")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Settings", description = "Rate and platform settings — Admin only")
public class AdminSettingsController extends BaseController {

    private final ServiceRateRepository rateRepository;

    public AdminSettingsController(UserRepository userRepository,
                                   ServiceRateRepository rateRepository) {
        super(userRepository);
        this.rateRepository = rateRepository;
    }

    public record RateUpdateRequest(
            BigDecimal baseFee,
            BigDecimal storageDayFee,
            BigDecimal transportKmFee,
            @NotBlank String effectiveDate,
            String changeReason
    ) {}

    @Operation(summary = "Get all service rates")
    @GetMapping("/rates")
    public ResponseEntity<List<ServiceRate>> getAllRates(
            @RequestParam(required = false) String region) {

        if (region != null) {
            return ResponseEntity.ok(
                    rateRepository.findAll().stream()
                            .filter(r -> region.equalsIgnoreCase(r.getRegion()))
                            .toList()
            );
        }
        return ResponseEntity.ok(rateRepository.findAll());
    }

    @Operation(summary = "Get rate by ID")
    @GetMapping("/rates/{rateId}")
    public ResponseEntity<ServiceRate> getRate(@PathVariable Long rateId) {
        return ResponseEntity.ok(
                rateRepository.findById(rateId)
                        .orElseThrow(() -> new AppException.NotFoundException(
                                "Rate not found: " + rateId))
        );
    }

    @Operation(
            summary = "Update a service rate",
            description = """
            Four-eyes approval recommended for production.
            Provide effectiveDate and changeReason for audit trail.
            Only provided fields are updated.
            """
    )
    @PatchMapping("/rates/{rateId}")
    public ResponseEntity<Map<String, Object>> updateRate(
            @PathVariable Long rateId,
            @RequestBody RateUpdateRequest req) {

        ServiceRate rate = rateRepository.findById(rateId)
                .orElseThrow(() -> new AppException.NotFoundException(
                        "Rate not found: " + rateId));

        if (req.baseFee() != null) {
            rate.setBaseFee(req.baseFee());
        }
        if (req.storageDayFee() != null) {
            rate.setStorageDayFee(req.storageDayFee());
        }
        if (req.transportKmFee() != null) {
            rate.setTransportKmFee(req.transportKmFee());
        }

        rateRepository.save(rate);

        return ResponseEntity.ok(Map.of(
                "message",        "Rate updated successfully",
                "rateId",         rateId,
                "effectiveDate",  req.effectiveDate(),
                "changeReason",   req.changeReason() != null ? req.changeReason() : "",
                "updatedRate",    rate
        ));
    }
}