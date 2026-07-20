package com.coldconnect.controller;

import com.coldconnect.entity.Payment;
import com.coldconnect.exception.AppException;
import com.coldconnect.repository.PaymentRepository;
import com.coldconnect.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/payments")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Payments", description = "Payments ledger and reconciliation — Admin only")
public class AdminPaymentController extends BaseController {

    private final PaymentRepository paymentRepository;

    public AdminPaymentController(UserRepository userRepository,
                                  PaymentRepository paymentRepository) {
        super(userRepository);
        this.paymentRepository = paymentRepository;
    }

    public record ReconcileRequest(
            String matchRef,
            String adjustmentReason
    ) {}

    @Operation(
            summary = "Get payments ledger",
            description = "Filter by status: PENDING, CAPTURED, FAILED, REFUNDED"
    )
    @GetMapping("/ledger")
    public ResponseEntity<Map<String, Object>> getLedger(
            @RequestParam(required = false) String status) {

        List<Payment> payments = paymentRepository.findAll();

        if (status != null) {
            payments = payments.stream()
                    .filter(p -> status.equalsIgnoreCase(p.getStatus()))
                    .toList();
        }

        BigDecimal totalCaptured = payments.stream()
                .filter(p -> "CAPTURED".equals(p.getStatus()))
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPending = payments.stream()
                .filter(p -> "PENDING".equals(p.getStatus()))
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ResponseEntity.ok(Map.of(
                "payments",      payments,
                "totalCaptured", totalCaptured,
                "totalPending",  totalPending,
                "count",         payments.size()
        ));
    }

    @Operation(summary = "Get single payment")
    @GetMapping("/{paymentId}")
    public ResponseEntity<Payment> getPayment(@PathVariable Long paymentId) {
        return ResponseEntity.ok(
                paymentRepository.findById(paymentId)
                        .orElseThrow(() -> new AppException.NotFoundException("Payment not found"))
        );
    }

    @Operation(
            summary = "Reconcile a payment",
            description = "Match payment to bank transfer ref. Audit trail mandatory."
    )
    @PostMapping("/{paymentId}/reconcile")
    public ResponseEntity<Map<String, Object>> reconcile(
            @PathVariable Long paymentId,
            @RequestBody ReconcileRequest req) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new AppException.NotFoundException("Payment not found"));

        payment.setStatus("CAPTURED");
        payment.setProviderRef(req.matchRef());
        paymentRepository.save(payment);

        return ResponseEntity.ok(Map.of(
                "message",   "Payment reconciled",
                "paymentId", paymentId,
                "matchRef",  req.matchRef(),
                "status",    "CAPTURED"
        ));
    }
}