package com.coldconnect.controller;

import com.coldconnect.entity.Booking;
import com.coldconnect.entity.BookingQuote;
import com.coldconnect.i18n.AppMessages;
import com.coldconnect.repository.UserRepository;
import com.coldconnect.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/bookings")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Bookings", description = "Cold storage and transport bookings")
public class BookingController extends BaseController {

    private final BookingService bookingService;
    private final AppMessages    messages;

    public BookingController(UserRepository userRepository,
                             BookingService bookingService,
                             AppMessages messages) {
        super(userRepository);
        this.bookingService = bookingService;
        this.messages       = messages;
    }

    public record BookingRequest(
            @NotBlank String serviceType,
            @NotNull  Long hubId,
            @NotBlank String region,
            @NotNull  Double quantityKg,
            @NotNull  Integer days,
            LocalDateTime windowStart,
            LocalDateTime windowEnd,
            String idempotencyKey
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
            description = "Provide idempotencyKey to prevent duplicate bookings from rapid taps"
    )
    @PostMapping
    public ResponseEntity<Map<String, Object>> createBooking(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody BookingRequest req) {
        String lang   = resolveLanguage(userDetails);
        Long   userId = resolveUser(userDetails).getId();
        Booking booking = bookingService.createBooking(
                userId, req.serviceType(), req.hubId(), req.region(),
                req.quantityKg(), req.days(), req.windowStart(),
                req.windowEnd(), "APP", req.idempotencyKey(), lang);
        return ResponseEntity.ok(Map.of(
                "message", messages.get(AppMessages.Key.BOOKING_CREATED, lang),
                "booking", booking
        ));
    }

    @Operation(summary = "Get booking detail")
    @GetMapping("/{bookingId}")
    public ResponseEntity<Booking> getBooking(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String bookingId) {
        String lang = resolveLanguage(userDetails);
        return ResponseEntity.ok(bookingService.getBooking(bookingId, lang));
    }

    @Operation(summary = "Get booking quote")
    @GetMapping("/{bookingId}/quote")
    public ResponseEntity<BookingQuote> getQuote(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String bookingId) {
        String lang = resolveLanguage(userDetails);
        return ResponseEntity.ok(bookingService.getQuote(bookingId, lang));
    }

    @Operation(summary = "Confirm a pending booking")
    @PostMapping("/{bookingId}/confirm")
    public ResponseEntity<Map<String, Object>> confirmBooking(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String bookingId) {
        String lang    = resolveLanguage(userDetails);
        Booking booking = bookingService.confirmBooking(bookingId, lang);
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
        String lang    = resolveLanguage(userDetails);
        Long   userId  = resolveUser(userDetails).getId();
        Booking booking = bookingService.cancelBooking(bookingId, userId, lang);
        return ResponseEntity.ok(Map.of(
                "message", messages.get(AppMessages.Key.BOOKING_CANCELLED, lang),
                "booking", booking
        ));
    }
}