package com.coldconnect.controller;

import com.coldconnect.entity.SupportCase;
import com.coldconnect.exception.AppException;
import com.coldconnect.i18n.AppMessages;
import com.coldconnect.repository.SupportCaseRepository;
import com.coldconnect.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/support")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Support", description = "Raise and track support cases")
public class SupportController extends BaseController {

    private final SupportCaseRepository caseRepository;
    private final AppMessages           messages;

    public SupportController(UserRepository userRepository,
                             SupportCaseRepository caseRepository,
                             AppMessages messages) {
        super(userRepository);
        this.caseRepository = caseRepository;
        this.messages       = messages;
    }

    public record CaseRequest(
            @Schema(example = "FEE_ISSUE",
                    description = "DISPUTE · MISSING_CRATE · FEE_ISSUE · TEMP_CONCERN · DRIVER_INCIDENT")
            @NotBlank String type,

            @Schema(example = "MEDIUM",
                    description = "CRITICAL (2h SLA) · HIGH (4h) · MEDIUM (24h) · LOW (72h)")
            String severity,

            @Schema(example = "1", description = "Related booking ID if applicable")
            Long bookingId,

            @Schema(example = "CRT-001", description = "Related crate ID if applicable")
            String crateId,

            @Schema(example = "1", description = "Related trip ID if applicable")
            Long tripId,

            @Schema(example = "My crate was not found at the hub after 3 days.")
            @NotBlank String message,

            @Schema(example = "https://storage.example.com/photo.jpg",
                    description = "Optional photo URL — upload photo first then pass URL here")
            String photoUri
    ) {}

    @Operation(
            summary = "Raise a support case",
            description = """
            Types: DISPUTE · MISSING_CRATE · FEE_ISSUE · TEMP_CONCERN · DRIVER_INCIDENT
            SLA is auto-set based on severity:
            CRITICAL=2h · HIGH=4h · MEDIUM=24h · LOW=72h
            """
    )
    @PostMapping("/cases")
    public ResponseEntity<Map<String, Object>> createCase(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CaseRequest req) {

        String lang   = resolveLanguage(userDetails);
        Long   userId = resolveUser(userDetails).getId();

        SupportCase c = new SupportCase();
        c.setRequesterId(userId);
        c.setType(req.type().toUpperCase());
        c.setSeverity(req.severity() != null ? req.severity().toUpperCase() : "MEDIUM");
        c.setBookingId(req.bookingId());
        c.setCrateId(req.crateId());
        c.setTripId(req.tripId());
        c.setMessage(req.message());
        c.setPhotoUri(req.photoUri());
        c.setStatus("OPEN");
        caseRepository.save(c);

        return ResponseEntity.ok(Map.of(
                "message", messages.get(AppMessages.Key.SUPPORT_CASE_CREATED, lang),
                "caseId",  c.getId(),
                "status",  c.getStatus(),
                "sla",     c.getSla()
        ));
    }

    @Operation(summary = "Get my support cases")
    @GetMapping("/cases")
    public ResponseEntity<List<SupportCase>> getMyCases(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = resolveUser(userDetails).getId();
        return ResponseEntity.ok(
                caseRepository.findByRequesterIdOrderByCreatedAtDesc(userId));
    }

    @Operation(summary = "Get support case detail")
    @GetMapping("/cases/{caseId}")
    public ResponseEntity<SupportCase> getCase(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long caseId) {
        Long userId = resolveUser(userDetails).getId();
        SupportCase c = caseRepository.findById(caseId)
                .orElseThrow(() -> new AppException.NotFoundException(
                        "Case not found: " + caseId));
        if (!c.getRequesterId().equals(userId)) {
            throw new AppException.UnauthorizedException("Not your case");
        }
        return ResponseEntity.ok(c);
    }
}