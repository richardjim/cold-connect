package com.coldconnect.controller;

import com.coldconnect.entity.ImpactMetric;
import com.coldconnect.repository.ImpactMetricRepository;
import com.coldconnect.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/impact")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Impact", description = "DARES evidence export and impact metrics — Admin only")
public class AdminImpactController extends BaseController {

    private final ImpactMetricRepository impactRepository;

    public AdminImpactController(UserRepository userRepository,
                                 ImpactMetricRepository impactRepository) {
        super(userRepository);
        this.impactRepository = impactRepository;
    }

    @Operation(
            summary = "Get platform-wide impact overview",
            description = "Aggregates all user impact metrics"
    )
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getOverview() {

        List<ImpactMetric> metrics = impactRepository.findAll();

        double totalFoodSaved  = metrics.stream()
                .mapToDouble(m -> m.getFoodSavedKg() != null ? m.getFoodSavedKg() : 0)
                .sum();

        double totalCo2Avoided = metrics.stream()
                .mapToDouble(m -> m.getCo2AvoidedKg() != null ? m.getCo2AvoidedKg() : 0)
                .sum();

        double totalSolarKwh   = metrics.stream()
                .mapToDouble(m -> m.getSolarCoolingKwh() != null ? m.getSolarCoolingKwh() : 0)
                .sum();

        int totalBookings      = metrics.stream()
                .mapToInt(m -> m.getTotalBookings() != null ? m.getTotalBookings() : 0)
                .sum();

        return ResponseEntity.ok(Map.of(
                "totalFoodSavedKg",  totalFoodSaved,
                "totalCo2AvoidedKg", totalCo2Avoided,
                "totalSolarKwh",     totalSolarKwh,
                "totalBookings",     totalBookings,
                "treesEquivalent",   totalCo2Avoided / 21.0,
                "kmNotDriven",       totalCo2Avoided / 0.21,
                "usersImpacted",     metrics.size()
        ));
    }

    @Operation(
            summary = "DARES evidence export",
            description = """
            Exports impact evidence for grant reporting.
            Includes: tonnes protected, kWh solar, users served,
            completeness score and assumptions used.
            Labels pilots vs projections as required by DARES.
            """
    )
    @GetMapping("/daresexport")
    public ResponseEntity<Map<String, Object>> getDaresExport(
            @RequestParam(required = false) String region,
            @RequestParam(defaultValue = "false") boolean includeEvidence) {

        List<ImpactMetric> metrics = impactRepository.findAll();

        double foodSaved   = metrics.stream()
                .mapToDouble(m -> m.getFoodSavedKg() != null ? m.getFoodSavedKg() : 0)
                .sum();

        double co2Avoided  = metrics.stream()
                .mapToDouble(m -> m.getCo2AvoidedKg() != null ? m.getCo2AvoidedKg() : 0)
                .sum();

        double solarKwh    = metrics.stream()
                .mapToDouble(m -> m.getSolarCoolingKwh() != null ? m.getSolarCoolingKwh() : 0)
                .sum();

        int totalBookings  = metrics.stream()
                .mapToInt(m -> m.getTotalBookings() != null ? m.getTotalBookings() : 0)
                .sum();

        int storageDays    = metrics.stream()
                .mapToInt(m -> m.getTotalStorageDays() != null ? m.getTotalStorageDays() : 0)
                .sum();

        return ResponseEntity.ok(Map.of(
                "exportType",           "DARES_EVIDENCE",
                "period",               "2026",
                "region",               region != null ? region : "ALL",
                "generatedAt",          LocalDateTime.now().toString(),
                "dataCompletenessScore", 0.85,
                "pilotStatus",          "PILOT",
                "assumptions",          "Food loss proxy: 30% of stored produce saved. CO2: 2.5kg per kg food saved.",
                "metrics", Map.of(
                        "tonnesProtected",  foodSaved / 1000,
                        "kWhSolar",         solarKwh,
                        "usersServed",      metrics.size(),
                        "bookings",         totalBookings,
                        "storageDays",      storageDays,
                        "co2AvoidedTonnes", co2Avoided / 1000
                )
        ));
    }
}