package com.coldconnect.controller;

import com.coldconnect.entity.SupportCase;
import com.coldconnect.repository.UserRepository;
import com.coldconnect.service.SupportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/support")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Support", description = "Raise and track support cases")
public class SupportController extends BaseController {

    private final SupportService supportService;

    public SupportController(UserRepository userRepository, SupportService supportService) {
        super(userRepository);
        this.supportService = supportService;
    }

    public record CaseRequest(Long bookingId, String crateId, Long tripId,
                               String type, String severity, String message) {}

    @Operation(summary = "Raise a support case")
    @PostMapping("/cases")
    public ResponseEntity<SupportCase> createCase(@AuthenticationPrincipal UserDetails userDetails,
                                                   @RequestBody CaseRequest req) {
        Long userId = resolveUser(userDetails).getId();
        return ResponseEntity.ok(supportService.createCase(
                userId, req.bookingId(), req.crateId(), req.tripId(),
                req.type(), req.severity(), req.message()));
    }

    @Operation(summary = "Get my support cases")
    @GetMapping("/cases")
    public ResponseEntity<List<SupportCase>> getMyCases(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(supportService.getCustomerCases(resolveUser(userDetails).getId()));
    }

    @Operation(summary = "Get case detail")
    @GetMapping("/cases/{caseId}")
    public ResponseEntity<SupportCase> getCase(@PathVariable Long caseId) {
        return ResponseEntity.ok(supportService.getCase(caseId));
    }
}
