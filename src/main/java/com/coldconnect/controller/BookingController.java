package com.coldconnect.controller;

import com.coldconnect.entity.Booking;
import com.coldconnect.entity.BookingQuote;
import com.coldconnect.entity.Hub;
import com.coldconnect.exception.AppException;
import com.coldconnect.i18n.AppMessages;
import com.coldconnect.repository.HubRepository;
import com.coldconnect.repository.UserRepository;
import com.coldconnect.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/bookings")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Bookings", description = "Cold storage and transport bookings")
public class BookingController extends BaseController {

    private final BookingService bookingService;
    private final AppMessages    messages;
    private final HubRepository  hubRepository;

    public BookingController(UserRepository userRepository,
                             BookingService bookingService,
                             AppMessages messages,
                             HubRepository hubRepository) {
        super(userRepository);
        this.bookingService = bookingService;
        this.messages       = messages;
        this.hubRepository  = hubRepository;
    }

    public record BookingRequest(
            @Schema(example = "STORAGE",
                    description = "STORAGE · TRANSPORT · PICKUP · BUNDLE")
            @NotBlank String serviceType,

            @Schema(example = "1")
            @NotNull Long hubId,

            @Schema(example = "jos-01")
            @NotBlank String region,

            @Schema(example = "100.0", description = "Estimated quantity in kg")
            @NotNull @Positive Double quantityKg,

            @Schema(example = "7", description = "Number of storage days")
            @NotNull @Positive Integer days,

            @Schema(example = "2026-08-01T08:00:00")
            LocalDateTime windowStart,

            @Schema(example = "2026-08-08T08:00:00")
            LocalDateTime windowEnd,

            @Schema(example = "idem-key-uuid-001",
                    description = "Optional — prevents duplicate bookings")
            String idempotencyKey,

            @Schema(example = "12 Ahmadu Bello Way, Jos",
                    description = "Required for TRANSPORT and PICKUP")
            String pickupAddress,

            @Schema(example = "Dawanau Market, Kano")
            String dropoffAddress,

            @Schema(example = "2", description = "Number of crates")
            Integer crateCount,

            @Schema(example = "Plastic crates",
                    description = "Plastic crates · Wooden crates · Sacks · Baskets")
            String packagingType,

            @Schema(example = "CASH_AT_DROP_OFF",
                    description = "CASH_AT_DROP_OFF · BANK_TRANSFER · WALLET")
            String paymentMethod,

            @Schema(example = "false",
                    description = "Request a callback from the hub operator")
            Boolean operatorCallbackRequested,

            @Schema(example = "FARM_TO_HUB",
                    description = "FARM_TO_HUB · HUB_TO_MARKET · HUB_TO_BUYER")
            String routeType,

            @Schema(example = "6 crates",
                    description = "6 crates · 12 crates · Half truck · Full truck")
            String loadSize,

            @Schema(example = "COM-001",
                    description = "Commodity ID from GET /v1/commodities")
            String commodityId
    ) {}

    public record WeighRequest(
            @Schema(example = "480.5",
                    description = "Actual weight confirmed at the scale in kg")
            @NotNull @Positive Double finalWeightKg
    ) {}

    @Operation(summary = "Get my bookings")
    @GetMapping
    public ResponseEntity<List<Booking>> getMyBookings(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = resolveUser(userDetails).getId();
        return ResponseEntity.ok(bookingService.getCustomerBookings(userId));
    }

    @Operation(
            summary = "Create a booking",
            description = """
            5-step booking wizard.
            Final price confirmed after weighing via PATCH /{bookingId}/weigh.
            windowStart and windowEnd are optional — defaults to now + days.
            """
    )
    @PostMapping
    public ResponseEntity<Map<String, Object>> createBooking(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody BookingRequest req) {
        String lang   = resolveLanguage(userDetails);
        Long   userId = resolveUser(userDetails).getId();

        // windowEnd validation — only if both provided
        if (req.windowStart() != null && req.windowEnd() != null
                && req.windowEnd().isBefore(req.windowStart())) {
            throw new AppException.BadRequestException(
                    "windowEnd must be after windowStart");
        }

        // Resolve effective window
        LocalDateTime effectiveStart = req.windowStart() != null
                ? req.windowStart() : LocalDateTime.now();
        LocalDateTime effectiveEnd   = req.windowEnd() != null
                ? req.windowEnd()
                : effectiveStart.plusDays(req.days());

        Booking booking = bookingService.createBooking(
                userId, req.serviceType(), req.hubId(), req.region(),
                req.quantityKg(), req.days(),
                effectiveStart, effectiveEnd,
                "APP", req.idempotencyKey(),
                req.pickupAddress(), req.dropoffAddress(),
                req.crateCount(), req.packagingType(),
                req.paymentMethod(), req.routeType(), req.loadSize(),
                req.commodityId(),
                req.operatorCallbackRequested() != null
                        && req.operatorCallbackRequested(),
                lang);

        Hub hub = hubRepository.findById(req.hubId()).orElse(null);

        Map<String, Object> response = new HashMap<>();
        response.put("message",              messages.get(AppMessages.Key.BOOKING_CREATED, lang));
        response.put("booking",              booking);
        response.put("hubName",              hub != null ? hub.getName() : "");
        response.put("hubAddress",           hub != null ? hub.getAddress() : "");
        response.put("expectedCheckoutDate", effectiveStart.plusDays(req.days()).toLocalDate().toString());
        response.put("checkoutBy",           "8:00pm");
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get booking detail")
    @GetMapping("/{bookingId}")
    public ResponseEntity<Map<String, Object>> getBooking(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String bookingId) {
        String lang    = resolveLanguage(userDetails);
        Long   userId  = resolveUser(userDetails).getId();
        Booking booking = bookingService.getBooking(bookingId, lang);

        // Ownership check
        if (!booking.getCustomerId().equals(userId)) {
            throw new AppException.UnauthorizedException("Not your booking");
        }

        // Enrich with hub info
        Hub hub = booking.getHubId() != null
                ? hubRepository.findById(booking.getHubId()).orElse(null)
                : null;

        Map<String, Object> response = new HashMap<>();
        response.put("booking",    booking);
        response.put("hubName",    hub != null ? hub.getName() : "");
        response.put("hubAddress", hub != null ? hub.getAddress() : "");
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get booking quote")
    @GetMapping("/{bookingId}/quote")
    public ResponseEntity<BookingQuote> getQuote(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String bookingId) {
        String lang   = resolveLanguage(userDetails);
        Long   userId = resolveUser(userDetails).getId();
        Booking booking = bookingService.getBooking(bookingId, lang);

        // Ownership check
        if (!booking.getCustomerId().equals(userId)) {
            throw new AppException.UnauthorizedException("Not your booking");
        }

        return ResponseEntity.ok(bookingService.getQuote(bookingId, lang));
    }

    @Operation(summary = "Confirm a pending booking")
    @PostMapping("/{bookingId}/confirm")
    public ResponseEntity<Map<String, Object>> confirmBooking(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String bookingId) {
        String lang   = resolveLanguage(userDetails);
        Long   userId = resolveUser(userDetails).getId();
        Booking booking = bookingService.confirmBooking(bookingId, userId, lang);
        return ResponseEntity.ok(Map.of(
                "message", messages.get(AppMessages.Key.BOOKING_CONFIRMED, lang),
                "booking", booking
        ));
    }

    @Operation(summary = "Cancel a booking — cannot cancel a completed booking")
    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelBooking(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String bookingId) {
        String lang   = resolveLanguage(userDetails);
        Long   userId = resolveUser(userDetails).getId();
        Booking booking = bookingService.cancelBooking(bookingId, userId, lang);
        return ResponseEntity.ok(Map.of(
                "message", messages.get(AppMessages.Key.BOOKING_CANCELLED, lang),
                "booking", booking
        ));
    }

    @Operation(
            summary = "Record final weight after weighing at hub",
            description = "Booking must be CONFIRMED or IN_PROGRESS."
    )
    @PatchMapping("/{bookingId}/weigh")
    public ResponseEntity<Map<String, Object>> weighBooking(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String bookingId,
            @Valid @RequestBody WeighRequest req) {
        String lang   = resolveLanguage(userDetails);
        Long   userId = resolveUser(userDetails).getId();
        Booking booking = bookingService.weighBooking(
                bookingId, req.finalWeightKg(), userId, lang);
        return ResponseEntity.ok(Map.of(
                "message",       "Final weight recorded. Price updated.",
                "bookingId",     booking.getBookingId(),
                "finalWeightKg", booking.getFinalWeightKg(),
                "finalTotal",    booking.getFinalTotal(),
                "weighedAt",     booking.getWeighedAt()
        ));
    }

    @Operation(
            summary = "Mark booking as completed",
            description = "Called when customer collects produce from hub. Triggers impact recalculation."
    )
    @PostMapping("/{bookingId}/complete")
    public ResponseEntity<Map<String, Object>> completeBooking(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String bookingId) {
        String lang   = resolveLanguage(userDetails);
        Long   userId = resolveUser(userDetails).getId();
        Booking booking = bookingService.completeBooking(bookingId, userId, lang);
        return ResponseEntity.ok(Map.of(
                "message",   "Booking completed",
                "bookingId", booking.getBookingId(),
                "status",    booking.getStatus().name()
        ));
    }
}