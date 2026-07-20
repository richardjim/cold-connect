package com.coldconnect.controller;

import com.coldconnect.entity.SupportCase;
import com.coldconnect.exception.AppException;
import com.coldconnect.repository.SupportCaseRepository;
import com.coldconnect.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/support")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Support", description = "Support queue management — Admin only")
public class AdminSupportController extends BaseController {

    private final SupportCaseRepository caseRepository;

    public AdminSupportController(UserRepository userRepository,
                                  SupportCaseRepository caseRepository) {
        super(userRepository);
        this.caseRepository = caseRepository;
    }

    public record UpdateCaseRequest(
            String ownerId,
            String status,
            String note,
            String resolution
    ) {}

    @Operation(summary = "Get all support cases — filterable by status and severity")
    @GetMapping("/cases")
    public ResponseEntity<List<SupportCase>> getAllCases(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity) {
        var cases = caseRepository.findAll();
        if (status != null) {
            cases = cases.stream()
                    .filter(c -> status.equalsIgnoreCase(c.getStatus()))
                    .toList();
        }
        if (severity != null) {
            cases = cases.stream()
                    .filter(c -> severity.equalsIgnoreCase(c.getSeverity()))
                    .toList();
        }
        return ResponseEntity.ok(cases);
    }

    @Operation(summary = "Get support case detail")
    @GetMapping("/cases/{caseId}")
    public ResponseEntity<SupportCase> getCase(@PathVariable Long caseId) {
        return ResponseEntity.ok(
                caseRepository.findById(caseId)
                        .orElseThrow(() -> new AppException.NotFoundException("Case not found"))
        );
    }

    @Operation(
            summary = "Update support case — assign owner, change status, add note or resolution"
    )
    @PatchMapping("/cases/{caseId}")
    public ResponseEntity<Map<String, Object>> updateCase(
            @PathVariable Long caseId,
            @RequestBody UpdateCaseRequest req) {

        SupportCase c = caseRepository.findById(caseId)
                .orElseThrow(() -> new AppException.NotFoundException("Case not found"));

        if (req.status() != null)     c.setStatus(req.status());
        if (req.resolution() != null) c.setResolution(req.resolution());

        caseRepository.save(c);

        return ResponseEntity.ok(Map.of(
                "message", "Case updated",
                "caseId",  caseId,
                "status",  c.getStatus()
        ));
    }
}