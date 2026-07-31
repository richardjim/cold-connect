package com.coldconnect.controller;

import com.coldconnect.entity.Booking;
import com.coldconnect.entity.Payment;
import com.coldconnect.entity.Receipt;
import com.coldconnect.exception.AppException;
import com.coldconnect.i18n.AppMessages;
import com.coldconnect.repository.BookingRepository;
import com.coldconnect.repository.PaymentRepository;
import com.coldconnect.repository.ReceiptRepository;
import com.coldconnect.repository.UserRepository;
import com.coldconnect.thirdparty.PaystackService;
import com.coldconnect.service.WalletLedgerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/payments")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Wallet and Payments", description = "Payments and receipts")
public class PaymentController extends BaseController {

    private final PaymentRepository   paymentRepository;
    private final ReceiptRepository   receiptRepository;
    private final BookingRepository   bookingRepository;
    private final WalletLedgerService walletLedgerService;
    private final PaystackService     paystackService;
    private final AppMessages         messages;

    @Value("${app.base-url:https://cold-connect.onrender.com}")
    private String appBaseUrl;

    public PaymentController(UserRepository userRepository,
                             PaymentRepository paymentRepository,
                             ReceiptRepository receiptRepository,
                             BookingRepository bookingRepository,
                             WalletLedgerService walletLedgerService,
                             PaystackService paystackService,
                             AppMessages messages) {
        super(userRepository);
        this.paymentRepository  = paymentRepository;
        this.receiptRepository  = receiptRepository;
        this.bookingRepository  = bookingRepository;
        this.walletLedgerService = walletLedgerService;
        this.paystackService    = paystackService;
        this.messages           = messages;
    }

    public record PaymentRequest(
            @Schema(example = "BK-1234567890")
            @NotBlank String bookingId,

            @Schema(example = "3500.00")
            @NotNull @Positive BigDecimal amount,

            @Schema(example = "CARD",
                    description = "CARD · BANK_TRANSFER · WALLET · CASH_AT_DROP_OFF")
            @NotBlank String method
    ) {}

    public record VerifyRequest(
            @Schema(example = "CCT-ABC12345",
                    description = "Paystack transaction reference")
            @NotBlank String reference
    ) {}

    @Operation(
            summary = "Initiate a payment for a booking",
            description = """
            **Methods:**
            - `CARD` or `BANK_TRANSFER` → returns Paystack `checkoutUrl` — redirect customer there
            - `WALLET` → deducted from Cold Connect wallet immediately, no redirect needed
            - `CASH_AT_DROP_OFF` → logged as pending, confirmed by hub operator
            
            **Flow for CARD/BANK_TRANSFER:**
            1. Call this endpoint → get `checkoutUrl`
            2. Redirect customer to `checkoutUrl`
            3. Customer pays on Paystack
            4. Call `POST /v1/payments/verify` with the reference
            """
    )
    @PostMapping
    public ResponseEntity<Map<String, Object>> initiatePayment(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody PaymentRequest req) {

        String lang   = resolveLanguage(userDetails);
        var    user   = resolveUser(userDetails);
        Long   userId = user.getId();

        // Validate booking
        Booking booking = bookingRepository.findByBookingId(req.bookingId())
                .orElseThrow(() -> new AppException.NotFoundException(
                        "Booking not found: " + req.bookingId()));

        if (!booking.getCustomerId().equals(userId)) {
            throw new AppException.UnauthorizedException("Not your booking");
        }
        if (booking.getStatus() == Booking.BookingStatus.CANCELLED) {
            throw new AppException.BadRequestException(
                    "Cannot pay for a cancelled booking");
        }

        String reference = "CCT-" + UUID.randomUUID()
                .toString().substring(0, 8).toUpperCase();

        return switch (req.method()) {

            case "CARD", "BANK_TRANSFER" -> {
                // Initialize Paystack transaction
                String callbackUrl = appBaseUrl + "/v1/payments/callback";

                Map<String, Object> metadata = Map.of(
                        "bookingId", req.bookingId(),
                        "userId",    userId,
                        "method",    req.method()
                );

                String email = user.getEmail() != null
                        ? user.getEmail()
                        : user.getPhone() + "@coldconnect.app";

                var ps = paystackService.initializeTransaction(
                        email, req.amount(), reference, callbackUrl, metadata);

                if (!ps.success) {
                    throw new AppException.BadRequestException(
                            "Payment initialization failed: " + ps.message);
                }

                // Save pending payment
                Payment payment = new Payment();
                payment.setBookingId(booking.getId());
                payment.setPayerId(userId);
                payment.setAmount(req.amount());
                payment.setMethod(req.method());
                payment.setProviderRef(ps.reference);
                payment.setStatus("PENDING");
                paymentRepository.save(payment);

                yield ResponseEntity.ok(Map.of(
                        "message",      "Redirect customer to checkoutUrl to complete payment",
                        "checkoutUrl",  ps.authorizationUrl,
                        "reference",    ps.reference,
                        "accessCode",   ps.accessCode,
                        "method",       req.method(),
                        "amount",       req.amount(),
                        "next",         "POST /v1/payments/verify with the reference after payment"
                ));
            }

            case "WALLET" -> {
                // Deduct from wallet immediately
                walletLedgerService.debit(userId, req.amount(),
                        "Payment for booking " + req.bookingId(), lang);

                booking.setPaymentStatus(Booking.PaymentStatus.PAID);
                bookingRepository.save(booking);

                Payment payment = new Payment();
                payment.setBookingId(booking.getId());
                payment.setPayerId(userId);
                payment.setAmount(req.amount());
                payment.setMethod("WALLET");
                payment.setProviderRef(reference);
                payment.setStatus("CAPTURED");
                payment = paymentRepository.save(payment);

                Receipt receipt = generateReceipt(payment, booking.getId(), userId);

                yield ResponseEntity.ok(Map.of(
                        "message",   "Payment successful. Receipt generated.",
                        "paymentId", payment.getId(),
                        "receiptId", receipt.getId(),
                        "status",    "CAPTURED",
                        "amount",    req.amount()
                ));
            }

            case "CASH_AT_DROP_OFF" -> {
                Payment payment = new Payment();
                payment.setBookingId(booking.getId());
                payment.setPayerId(userId);
                payment.setAmount(req.amount());
                payment.setMethod("CASH_AT_DROP_OFF");
                payment.setProviderRef(reference);
                payment.setStatus("PENDING");
                payment = paymentRepository.save(payment);

                yield ResponseEntity.ok(Map.of(
                        "message",   "Cash payment logged. Confirm with hub operator at drop-off.",
                        "paymentId", payment.getId(),
                        "status",    "PENDING",
                        "amount",    req.amount()
                ));
            }

            default -> throw new AppException.BadRequestException(
                    "Invalid method. Use: CARD · BANK_TRANSFER · WALLET · CASH_AT_DROP_OFF");
        };
    }

    @Operation(
            summary = "Verify a Paystack payment",
            description = """
            Call this after the customer completes payment on Paystack.
            Pass the reference returned from POST /v1/payments.
            On success: booking is marked PAID and receipt is generated.
            """
    )
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyPayment(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody VerifyRequest req) {

        Long userId = resolveUser(userDetails).getId();

        var verify = paystackService.verifyTransaction(req.reference());

        if (!verify.success) {
            throw new AppException.BadRequestException(
                    "Payment verification failed: " + verify.message);
        }

        if (!"success".equals(verify.status)) {
            throw new AppException.BadRequestException(
                    "Payment not successful. Status: " + verify.status);
        }

        // Find and update payment record
        Payment payment = paymentRepository.findAll().stream()
                .filter(p -> req.reference().equals(p.getProviderRef()))
                .findFirst()
                .orElseThrow(() -> new AppException.NotFoundException(
                        "Payment record not found for reference: " + req.reference()));

        if (!payment.getPayerId().equals(userId)) {
            throw new AppException.UnauthorizedException("Not your payment");
        }

        payment.setStatus("CAPTURED");
        paymentRepository.save(payment);

        // Mark booking as paid
        Booking booking = bookingRepository.findById(payment.getBookingId())
                .orElseThrow(() -> new AppException.NotFoundException("Booking not found"));
        booking.setPaymentStatus(Booking.PaymentStatus.PAID);
        bookingRepository.save(booking);

        // Generate receipt
        Receipt receipt = generateReceipt(payment, booking.getId(), userId);

        return ResponseEntity.ok(Map.of(
                "message",   "Payment verified. Receipt generated.",
                "paymentId", payment.getId(),
                "receiptId", receipt.getId(),
                "status",    "CAPTURED",
                "amount",    verify.amount,
                "channel",   verify.channel != null ? verify.channel : "",
                "bookingId", booking.getBookingId()
        ));
    }

    @Operation(
            summary = "Paystack webhook — do not call manually",
            description = "Receives Paystack payment events. Automatically updates payment status."
    )
    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(
            @RequestHeader(value = "x-paystack-signature", required = false) String signature,
            @RequestBody String payload) {

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode json = mapper.readTree(payload);

            String event = json.get("event").asText();

            if ("charge.success".equals(event)) {
                String reference = json.get("data").get("reference").asText();

                paymentRepository.findAll().stream()
                        .filter(p -> reference.equals(p.getProviderRef()))
                        .findFirst()
                        .ifPresent(payment -> {
                            payment.setStatus("CAPTURED");
                            paymentRepository.save(payment);

                            bookingRepository.findById(payment.getBookingId())
                                    .ifPresent(booking -> {
                                        booking.setPaymentStatus(Booking.PaymentStatus.PAID);
                                        bookingRepository.save(booking);
                                        generateReceipt(payment, booking.getId(),
                                                payment.getPayerId());
                                    });
                        });
            }
        } catch (Exception e) {
            // Log but always return 200 to Paystack
        }

        return ResponseEntity.ok("OK");
    }

    @Operation(summary = "Get my payment history")
    @GetMapping
    public ResponseEntity<List<Payment>> getMyPayments(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = resolveUser(userDetails).getId();
        return ResponseEntity.ok(
                paymentRepository.findAll().stream()
                        .filter(p -> userId.equals(p.getPayerId()))
                        .toList()
        );
    }

    @Operation(summary = "Get payment by ID")
    @GetMapping("/{paymentId}")
    public ResponseEntity<Payment> getPayment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long paymentId) {
        Long userId = resolveUser(userDetails).getId();
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new AppException.NotFoundException(
                        "Payment not found"));
        if (!payment.getPayerId().equals(userId)) {
            throw new AppException.UnauthorizedException("Not your payment");
        }
        return ResponseEntity.ok(payment);
    }

    private Receipt generateReceipt(Payment payment, Long bookingId, Long userId) {
        Receipt receipt = new Receipt();
        receipt.setPaymentId(payment.getId());
        receipt.setBookingId(bookingId);
        receipt.setAmount(payment.getAmount());
        receipt.setIssuedTo(userId);
        receipt.setReceiptUri("RCT-" + payment.getId());
        receipt.setShareChannels("APP,SMS");
        return receiptRepository.save(receipt);
    }
}