package com.coldconnect.controller;

import com.coldconnect.entity.Payment;
import com.coldconnect.entity.Receipt;
import com.coldconnect.repository.UserRepository;
import com.coldconnect.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/v1")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Wallet and Payments", description = "Wallet balance, payments, receipts")
public class WalletController extends BaseController {

    private final WalletService walletService;

    public WalletController(UserRepository userRepository, WalletService walletService) {
        super(userRepository);
        this.walletService = walletService;
    }

    public record PaymentRequest(Long bookingId, Long orderId, BigDecimal amount, String method) {}

    @Operation(summary = "Get wallet balance and transaction history")
    @GetMapping("/wallet")
    public ResponseEntity<WalletService.WalletSummary> getWallet(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = resolveUser(userDetails).getId();
        return ResponseEntity.ok(walletService.getWallet(userId));
    }

    @Operation(summary = "Initiate a payment")
    @PostMapping("/payments")
    public ResponseEntity<Payment> pay(@AuthenticationPrincipal UserDetails userDetails,
                                        @RequestBody PaymentRequest req) {
        Long userId = resolveUser(userDetails).getId();
        return ResponseEntity.ok(walletService.initiatePayment(
                userId, req.bookingId(), req.orderId(), req.amount(), req.method()));
    }

    @Operation(summary = "Get all receipts")
    @GetMapping("/receipts")
    public ResponseEntity<List<Receipt>> getReceipts(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(walletService.getReceipts(resolveUser(userDetails).getId()));
    }

    @Operation(summary = "Get receipt detail")
    @GetMapping("/receipts/{receiptId}")
    public ResponseEntity<Receipt> getReceipt(@PathVariable Long receiptId) {
        return ResponseEntity.ok(walletService.getReceipt(receiptId));
    }
}
