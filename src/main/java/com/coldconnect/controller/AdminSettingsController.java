package com.coldconnect.controller;

import com.coldconnect.entity.Commodity;
import com.coldconnect.entity.ServiceRate;
import com.coldconnect.exception.AppException;
import com.coldconnect.repository.CommodityRepository;
import com.coldconnect.repository.ServiceRateRepository;
import com.coldconnect.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    private final CommodityRepository   commodityRepository;

    public AdminSettingsController(UserRepository userRepository,
                                   ServiceRateRepository rateRepository,
                                   CommodityRepository commodityRepository) {
        super(userRepository);
        this.rateRepository      = rateRepository;
        this.commodityRepository = commodityRepository;
    }

    // ── Service Rates ─────────────────────────────────────────────────────────

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
            description = "Four-eyes approval recommended. Provide effectiveDate and changeReason."
    )
    @PatchMapping("/rates/{rateId}")
    public ResponseEntity<Map<String, Object>> updateRate(
            @PathVariable Long rateId,
            @RequestBody RateUpdateRequest req) {

        ServiceRate rate = rateRepository.findById(rateId)
                .orElseThrow(() -> new AppException.NotFoundException(
                        "Rate not found: " + rateId));

        if (req.baseFee() != null)        rate.setBaseFee(req.baseFee());
        if (req.storageDayFee() != null)  rate.setStorageDayFee(req.storageDayFee());
        if (req.transportKmFee() != null) rate.setTransportKmFee(req.transportKmFee());
        rateRepository.save(rate);

        return ResponseEntity.ok(Map.of(
                "message",       "Rate updated successfully",
                "rateId",        rateId,
                "effectiveDate", req.effectiveDate(),
                "changeReason",  req.changeReason() != null ? req.changeReason() : "",
                "updatedRate",   rate
        ));
    }

    // ── Languages ─────────────────────────────────────────────────────────────

    public record LanguageToggleRequest(
            @Schema(example = "ha", description = "en · ha · yo · ig · pcm")
            @NotBlank String language,
            boolean active
    ) {}

    @Operation(summary = "Get active languages")
    @GetMapping("/languages")
    public ResponseEntity<Map<String, Object>> getLanguages() {
        return ResponseEntity.ok(Map.of(
                "activeLanguages", List.of(
                        Map.of("code", "en",  "name", "English",        "active", true),
                        Map.of("code", "pcm", "name", "Nigerian Pidgin", "active", true),
                        Map.of("code", "ha",  "name", "Hausa",           "active", true),
                        Map.of("code", "yo",  "name", "Yoruba",          "active", true),
                        Map.of("code", "ig",  "name", "Igbo",            "active", true)
                )
        ));
    }

    @Operation(summary = "Toggle a language on or off")
    @PatchMapping("/languages")
    public ResponseEntity<Map<String, String>> toggleLanguage(
            @RequestBody LanguageToggleRequest req) {
        return ResponseEntity.ok(Map.of(
                "message",  req.active()
                        ? req.language() + " enabled"
                        : req.language() + " disabled",
                "language", req.language(),
                "active",   String.valueOf(req.active())
        ));
    }

    // ── Commodities ───────────────────────────────────────────────────────────

    public record CommodityRequest(
            @Schema(example = "COM-011")
            @NotBlank String commodityId,

            @Schema(example = "Sweet Potatoes")
            @NotBlank String name,

            @Schema(example = "VEGETABLES",
                    description = "VEGETABLES · FRUITS · SEAFOOD · DAIRY · GRAINS")
            @NotBlank String category,

            @Schema(example = "jos-01")
            @NotBlank String region,

            @Schema(example = "7.0", description = "Min safe temp °C")
            @NotNull Double tempRangeMin,

            @Schema(example = "13.0", description = "Max safe temp °C")
            @NotNull Double tempRangeMax,

            @Schema(example = "21", description = "Shelf life in days")
            Integer shelfLifeDays,

            @Schema(example = "Plastic crates only")
            String packagingRules,

            @Schema(example = "Keep dry and away from ethylene-producing fruits")
            String handlingNotes
    ) {}

    @Operation(summary = "Get all commodities — admin view with filters")
    @GetMapping("/commodities")
    public ResponseEntity<List<Commodity>> getAllCommodities(
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String category) {

        var commodities = commodityRepository.findAll();
        if (region != null) {
            commodities = commodities.stream()
                    .filter(c -> region.equalsIgnoreCase(c.getRegion()))
                    .toList();
        }
        if (category != null) {
            commodities = commodities.stream()
                    .filter(c -> category.equalsIgnoreCase(c.getCategory()))
                    .toList();
        }
        return ResponseEntity.ok(commodities);
    }

    @Operation(summary = "Create a new commodity")
    @PostMapping("/commodities")
    public ResponseEntity<Commodity> createCommodity(
            @Valid @RequestBody CommodityRequest req) {

        commodityRepository.findAll().stream()
                .filter(c -> req.commodityId().equalsIgnoreCase(c.getCommodityId()))
                .findFirst()
                .ifPresent(c -> { throw new AppException.ConflictException(
                        "Commodity ID already exists: " + req.commodityId()); });

        Commodity commodity = new Commodity();
        commodity.setCommodityId(req.commodityId().toUpperCase());
        commodity.setName(req.name());
        commodity.setCategory(req.category().toUpperCase());
        commodity.setRegion(req.region());
        commodity.setTempRangeMin(req.tempRangeMin());
        commodity.setTempRangeMax(req.tempRangeMax());
        commodity.setShelfLifeDays(req.shelfLifeDays());
        commodity.setPackagingRules(req.packagingRules());
        commodity.setHandlingNotes(req.handlingNotes());
        return ResponseEntity.ok(commodityRepository.save(commodity));
    }

    @Operation(summary = "Update a commodity")
    @PatchMapping("/commodities/{id}")
    public ResponseEntity<Commodity> updateCommodity(
            @PathVariable Long id,
            @RequestBody CommodityRequest req) {

        Commodity commodity = commodityRepository.findById(id)
                .orElseThrow(() -> new AppException.NotFoundException(
                        "Commodity not found: " + id));

        if (req.name() != null)           commodity.setName(req.name());
        if (req.category() != null)       commodity.setCategory(req.category().toUpperCase());
        if (req.region() != null)         commodity.setRegion(req.region());
        if (req.tempRangeMin() != null)   commodity.setTempRangeMin(req.tempRangeMin());
        if (req.tempRangeMax() != null)   commodity.setTempRangeMax(req.tempRangeMax());
        if (req.shelfLifeDays() != null)  commodity.setShelfLifeDays(req.shelfLifeDays());
        if (req.packagingRules() != null) commodity.setPackagingRules(req.packagingRules());
        if (req.handlingNotes() != null)  commodity.setHandlingNotes(req.handlingNotes());

        return ResponseEntity.ok(commodityRepository.save(commodity));
    }

    @Operation(summary = "Delete a commodity")
    @DeleteMapping("/commodities/{id}")
    public ResponseEntity<Map<String, String>> deleteCommodity(@PathVariable Long id) {
        commodityRepository.findById(id)
                .orElseThrow(() -> new AppException.NotFoundException(
                        "Commodity not found: " + id));
        commodityRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Commodity deleted"));
    }
}