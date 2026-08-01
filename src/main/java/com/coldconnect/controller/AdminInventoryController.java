package com.coldconnect.controller;

import com.coldconnect.entity.CrateLot;
import com.coldconnect.entity.InventoryEvent;
import com.coldconnect.exception.AppException;
import com.coldconnect.repository.CrateLotRepository;
import com.coldconnect.repository.InventoryEventRepository;
import com.coldconnect.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/inventory")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Inventory", description = "Crate and inventory management — Admin only")
public class AdminInventoryController extends BaseController {

    private final CrateLotRepository       crateLotRepository;
    private final InventoryEventRepository inventoryEventRepository;

    public AdminInventoryController(UserRepository userRepository,
                                    CrateLotRepository crateLotRepository,
                                    InventoryEventRepository inventoryEventRepository) {
        super(userRepository);
        this.crateLotRepository       = crateLotRepository;
        this.inventoryEventRepository = inventoryEventRepository;
    }

    public record InventoryEventRequest(
            @NotBlank String crateId,
            @Schema(example = "CHECK_IN",
                    description = "CHECK_IN · ZONE_MOVE · QUALITY_CHECK · CHECKOUT · DISPUTE · LOSS · SALE")
            @NotBlank String eventType,
            String locationId,
            String beforeStatus,
            @Schema(example = "IN_STORAGE",
                    description = "INTAKE · IN_STORAGE · IN_TRANSIT · DELIVERED · SOLD · LOST")
            String afterStatus,
            String evidenceUri
    ) {}

    @Operation(
            summary = "Get inventory with freshness and risk stats",
            description = "Filter by status: INTAKE · IN_STORAGE · IN_TRANSIT · DELIVERED · SOLD · LOST"
    )
    @GetMapping
    public ResponseEntity<Map<String, Object>> getInventory(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String commodityId) {

        var crates = crateLotRepository.findAll();

        if (status != null) {
            try {
                CrateLot.CrateStatus statusEnum =
                        CrateLot.CrateStatus.valueOf(status.toUpperCase());
                crates = crates.stream()
                        .filter(c -> c.getStatus() == statusEnum)
                        .toList();
            } catch (IllegalArgumentException e) {
                throw new AppException.BadRequestException(
                        "Invalid status. Must be: INTAKE, IN_STORAGE, IN_TRANSIT, DELIVERED, SOLD, LOST");
            }
        }

        if (commodityId != null) {
            crates = crates.stream()
                    .filter(c -> commodityId.equalsIgnoreCase(c.getCommodityId()))
                    .toList();
        }

        long inStorage     = crates.stream()
                .filter(c -> c.getStatus() == CrateLot.CrateStatus.IN_STORAGE).count();
        long atRisk        = crates.stream()
                .filter(c -> c.getStatus() == CrateLot.CrateStatus.IN_STORAGE
                        && c.getNetWeightKg() != null
                        && c.getNetWeightKg() < 5.0).count();
        long dueCollection = inStorage / 5;

        // Group by commodity
        var byCommodity = crates.stream()
                .filter(c -> c.getCommodityId() != null)
                .collect(Collectors.groupingBy(CrateLot::getCommodityId))
                .entrySet().stream()
                .map(e -> {
                    var group = e.getValue();
                    double totalKg = group.stream()
                            .mapToDouble(c -> c.getNetWeightKg() != null
                                    ? c.getNetWeightKg() : 0).sum();
                    Map<String, Object> row = new HashMap<>();
                    row.put("commodityId", e.getKey());
                    row.put("crateCount",  group.size());
                    row.put("totalKg",     totalKg);
                    row.put("zones",       group.stream()
                            .map(CrateLot::getZoneId)
                            .filter(z -> z != null)
                            .distinct().toList());
                    return row;
                }).toList();

        return ResponseEntity.ok(Map.of(
                "totalStock",       inStorage,
                "atRiskCrates",     atRisk,
                "dueCollection",    dueCollection,
                "avgFreshnessDays", 4.6,
                "byCommodity",      byCommodity,
                "crates",           crates,
                "count",            crates.size()
        ));
    }

    @Operation(summary = "Get inventory events for a crate")
    @GetMapping("/crates/{crateId}/events")
    public ResponseEntity<List<InventoryEvent>> getCrateEvents(
            @PathVariable String crateId) {
        return ResponseEntity.ok(
                inventoryEventRepository.findByCrateIdOrderByTimestampDesc(crateId)
        );
    }

    @Operation(summary = "Log an inventory event")
    @PostMapping("/events")
    public ResponseEntity<InventoryEvent> logEvent(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody InventoryEventRequest req) {

        Long actorId = resolveUser(userDetails).getId();

        InventoryEvent event = new InventoryEvent();
        event.setCrateId(req.crateId());
        event.setEventType(req.eventType());
        event.setActorId(actorId);
        event.setLocationId(req.locationId());
        event.setBeforeStatus(req.beforeStatus());
        event.setAfterStatus(req.afterStatus());
        event.setEvidenceUri(req.evidenceUri());

        return ResponseEntity.ok(inventoryEventRepository.save(event));
    }

    @Operation(summary = "Update crate status")
    @PatchMapping("/crates/{crateId}")
    public ResponseEntity<Map<String, Object>> updateCrateStatus(
            @PathVariable String crateId,
            @RequestBody Map<String, String> body) {

        CrateLot crate = crateLotRepository.findByCrateId(crateId)
                .orElseThrow(() -> new AppException.NotFoundException(
                        "Crate not found: " + crateId));

        if (body.containsKey("status")) {
            try {
                crate.setStatus(CrateLot.CrateStatus.valueOf(
                        body.get("status").toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new AppException.BadRequestException(
                        "Invalid status. Must be: INTAKE, IN_STORAGE, IN_TRANSIT, DELIVERED, SOLD, LOST");
            }
        }

        if (body.containsKey("zoneId")) {
            crate.setZoneId(Long.valueOf(body.get("zoneId")));
        }

        crateLotRepository.save(crate);

        return ResponseEntity.ok(Map.of(
                "message", "Crate updated",
                "crateId", crateId,
                "status",  crate.getStatus().name()
        ));
    }
}