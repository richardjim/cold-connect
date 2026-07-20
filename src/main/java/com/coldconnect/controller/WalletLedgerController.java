package com.coldconnect.controller;

import com.coldconnect.entity.WalletTransaction;
import com.coldconnect.i18n.AppMessages;
import com.coldconnect.repository.UserRepository;
import com.coldconnect.service.WalletLedgerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/v1/wallet")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Wallet", description = "Wallet balance, top-up and withdrawals")
public class WalletLedgerController extends BaseController {

    private final WalletLedgerService walletLedgerService;
    private final AppMessages         messages;

    public WalletLedgerController(UserRepository userRepository,
                                  WalletLedgerService walletLedgerService,
                                  AppMessages messages) {
        super(userRepository);
        this.walletLedgerService = walletLedgerService;
        this.messages            = messages;
    }

    public record TopUpRequest(
            @NotNull @Positive(message = "Amount must be greater than zero") BigDecimal amount,
            @NotBlank String method
    ) {}

    public record WithdrawRequest(
            @NotNull @Positive(message = "Amount must be greater than zero") BigDecimal amount,
            @NotBlank String method
    ) {}

    @Operation(summary = "Get wallet balance and transaction history")
    @GetMapping
    public ResponseEntity<WalletLedgerService.WalletDetail> getWallet(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = resolveUser(userDetails).getId();
        return ResponseEntity.ok(walletLedgerService.getWalletDetail(userId));
    }

    @Operation(summary = "Top up wallet — methods: BANK_TRANSFER, CARD, CASH")
    @PostMapping("/topup")
    public ResponseEntity<Map<String, Object>> topUp(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody TopUpRequest req) {
        String lang   = resolveLanguage(userDetails);
        Long   userId = resolveUser(userDetails).getId();
        WalletTransaction tx =
                walletLedgerService.topUp(userId, req.amount(), req.method(), lang);
        return ResponseEntity.ok(Map.of(
                "message",     messages.get(AppMessages.Key.WALLET_TOPUP_SUCCESS, lang),
                "transaction", tx
        ));
    }

    @Operation(summary = "Withdraw from wallet — fails if balance insufficient")
    @PostMapping("/withdraw")
    public ResponseEntity<Map<String, Object>> withdraw(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody WithdrawRequest req) {
        String lang   = resolveLanguage(userDetails);
        Long   userId = resolveUser(userDetails).getId();
        WalletTransaction tx =
                walletLedgerService.withdraw(userId, req.amount(), req.method(), lang);
        return ResponseEntity.ok(Map.of(
                "message",     messages.get(AppMessages.Key.WALLET_WITHDRAW_SUCCESS, lang),
                "transaction", tx
        ));
    }
}