package com.coldconnect.controller;

import com.coldconnect.entity.SafetyCheck;
import com.coldconnect.entity.SafetyCheckItem;
import com.coldconnect.exception.AppException;
import com.coldconnect.repository.SafetyCheckItemRepository;
import com.coldconnect.repository.SafetyCheckRepository;
import com.coldconnect.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/safety")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Safety", description = "Fleet safety walkaround compliance — Admin only")
public class AdminSafetyController extends BaseController {

    private final SafetyCheckRepository     safetyCheckRepository;
    private final SafetyCheckItemRepository itemRepository;

    public AdminSafetyController(UserRepository userRepository,
                                 SafetyCheckRepository safetyCheckRepository,
                                 SafetyCheckItemRepository itemRepository) {
        super(userRepository);
        this.safetyCheckRepository = safetyCheckRepository;
        this.itemRepository        = itemRepository;
    }

    @Operation(summary = "Get all safety checks — filterable by result and vehicle")
    @GetMapping("/checks")
    public ResponseEntity<Map<String, Object>> getChecks(
            @RequestParam(required = false) String result,
            @RequestParam(required = false) Long vehicleId) {

        var checks = safetyCheckRepository.findAll();

        if (result != null) {
            checks = checks.stream()
                    .filter(c -> result.equalsIgnoreCase(c.getResult()))
                    .toList();
        }

        if (vehicleId != null) {
            checks = checks.stream()
                    .filter(c -> vehicleId.equals(c.getVehicleId()))
                    .toList();
        }

        long passed  = checks.stream().filter(c -> "PASS".equals(c.getResult())).count();
        long failed  = checks.stream().filter(c -> "FAIL".equals(c.getResult())).count();
        long pending = checks.stream().filter(c -> "PENDING".equals(c.getResult())).count();
        long blocked = checks.stream().filter(SafetyCheck::isRunBlocked).count();

        return ResponseEntity.ok(Map.of(
                "checks", checks,
                "count",  checks.size(),
                "stats",  Map.of(
                        "passed",  passed,
                        "failed",  failed,
                        "pending", pending,
                        "blocked", blocked
                )
        ));
    }

    @Operation(summary = "Get safety check detail with all items")
    @GetMapping("/checks/{checkId}")
    public ResponseEntity<Map<String, Object>> getCheck(
            @PathVariable String checkId) {

        SafetyCheck check = safetyCheckRepository.findByCheckId(checkId)
                .orElseThrow(() -> new AppException.NotFoundException(
                        "Safety check not found: " + checkId));

        List<SafetyCheckItem> items = itemRepository.findByCheckId(check.getId());

        long defects = items.stream()
                .filter(i -> "defect".equalsIgnoreCase(i.getMark()))
                .count();

        return ResponseEntity.ok(Map.of(
                "check",   check,
                "items",   items,
                "defects", defects
        ));
    }

    @Operation(summary = "Override a blocked trip — admin approval")
    @PatchMapping("/checks/{checkId}/override")
    public ResponseEntity<Map<String, Object>> overrideBlock(
            @PathVariable String checkId,
            @RequestBody Map<String, String> body) {

        SafetyCheck check = safetyCheckRepository.findByCheckId(checkId)
                .orElseThrow(() -> new AppException.NotFoundException(
                        "Safety check not found: " + checkId));

        check.setRunBlocked(false);
        check.setResult("PASS");
        safetyCheckRepository.save(check);

        return ResponseEntity.ok(Map.of(
                "message", "Run block overridden by admin",
                "checkId", checkId,
                "reason",  body.getOrDefault("reason", "Admin override"),
                "blocked", false
        ));
    }
}