package com.coldconnect.controller;

import com.coldconnect.entity.Notification;
import com.coldconnect.entity.SupportCase;
import com.coldconnect.exception.AppException;
import com.coldconnect.repository.NotificationRepository;
import com.coldconnect.repository.SupportCaseRepository;
import com.coldconnect.repository.UserRepository;
import com.coldconnect.service.SmsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/support")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Support", description = "Support queue management — Admin only")
public class AdminSupportController extends BaseController {

    private final SupportCaseRepository caseRepository;
    private final NotificationRepository notificationRepository;
    private final SmsService             smsService;

    public AdminSupportController(UserRepository userRepository,
                                  SupportCaseRepository caseRepository,
                                  NotificationRepository notificationRepository,
                                  SmsService smsService) {
        super(userRepository);
        this.caseRepository       = caseRepository;
        this.notificationRepository = notificationRepository;
        this.smsService           = smsService;
    }

    public record UpdateCaseRequest(
            String ownerId,
            @Schema(example = "IN_REVIEW",
                    description = "OPEN · IN_REVIEW · RESOLVED · CLOSED")
            String status,
            String note,
            String resolution
    ) {}

    public record MessageCustomerRequest(
            @Schema(example = "Your crate CRT-001 has been found and is ready for collection.")
            String message,
            @Schema(example = "true", description = "Also send via SMS")
            boolean sendSms
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
                        .orElseThrow(() -> new AppException.NotFoundException(
                                "Case not found: " + caseId))
        );
    }

    @Operation(
            summary = "Update support case — assign owner, change status, add resolution"
    )
    @PatchMapping("/cases/{caseId}")
    public ResponseEntity<Map<String, Object>> updateCase(
            @PathVariable Long caseId,
            @RequestBody UpdateCaseRequest req) {

        SupportCase c = caseRepository.findById(caseId)
                .orElseThrow(() -> new AppException.NotFoundException(
                        "Case not found: " + caseId));

        if (req.status() != null)     c.setStatus(req.status());
        if (req.resolution() != null) c.setResolution(req.resolution());
        caseRepository.save(c);

        return ResponseEntity.ok(Map.of(
                "message", "Case updated",
                "caseId",  caseId,
                "status",  c.getStatus()
        ));
    }

    @Operation(
            summary = "Message customer from admin",
            description = """
            Sends an in-app notification to the customer who raised the case.
            Optionally also sends an SMS via Termii.
            """
    )
    @PostMapping("/cases/{caseId}/message")
    public ResponseEntity<Map<String, Object>> messageCustomer(
            @PathVariable Long caseId,
            @RequestBody MessageCustomerRequest req) {

        SupportCase c = caseRepository.findById(caseId)
                .orElseThrow(() -> new AppException.NotFoundException(
                        "Case not found: " + caseId));

        // Get customer
        var customer = userRepository.findById(c.getRequesterId())
                .orElseThrow(() -> new AppException.NotFoundException(
                        "Customer not found"));

        // Save in-app notification
        Notification notification = new Notification();
        notification.setUserId(c.getRequesterId());
        notification.setTitle("Support Update — Case #" + caseId);
        notification.setBody(req.message());
        notification.setType("SUPPORT");
        notification.setRead(false);
        notificationRepository.save(notification);

        // Optionally send SMS
        if (req.sendSms() && customer.getPhone() != null) {
            smsService.sendSms(customer.getPhone(), req.message());
        }

        return ResponseEntity.ok(Map.of(
                "message",   "Customer notified",
                "caseId",    caseId,
                "customerId", c.getRequesterId(),
                "smsSent",   req.sendSms() && customer.getPhone() != null
        ));
    }
}