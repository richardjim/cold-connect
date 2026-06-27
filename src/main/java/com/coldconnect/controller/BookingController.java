package com.coldconnect.controller;

import com.coldconnect.entity.Booking;
import com.coldconnect.entity.BookingQuote;
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

@RestController
@RequestMapping("/v1/bookings")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Bookings", description = "Cold storage and transport bookings")
public class BookingController extends BaseController {

    private final BookingService bookingService;

    public BookingController(UserRepository userRepository, BookingService bookingService) {
        super(userRepository);
        this.bookingService = bookingService;
    }

    public record BookingRequest(
            @NotBlank String serviceType,
            @NotNull Long hubId,
            @NotBlank String region,
            @NotNull Double quantityKg,
            @NotNull Integer days,
            LocalDateTime windowStart,
            LocalDateTime windowEnd) {}

    @Operation(summary = "Get my bookings")
    @GetMapping
    public ResponseEntity<List<Booking>> getMyBookings(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = resolveUser(userDetails).getId();
        return ResponseEntity.ok(bookingService.getCustomerBookings(userId));
    }

    @Operation(summary = "Create a booking with auto-quote")
    @PostMapping
    public ResponseEntity<Booking> createBooking(@AuthenticationPrincipal UserDetails userDetails,
                                                  @Valid @RequestBody BookingRequest req) {
        Long userId = resolveUser(userDetails).getId();
        return ResponseEntity.ok(bookingService.createBooking(
                userId, req.serviceType(), req.hubId(), req.region(),
                req.quantityKg(), req.days(), req.windowStart(), req.windowEnd(), "APP"));
    }

    @Operation(summary = "Get booking detail")
    @GetMapping("/{bookingId}")
    public ResponseEntity<Booking> getBooking(@PathVariable String bookingId) {
        return ResponseEntity.ok(bookingService.getBooking(bookingId));
    }

    @Operation(summary = "Confirm a pending booking")
    @PostMapping("/{bookingId}/confirm")
    public ResponseEntity<Booking> confirmBooking(@PathVariable String bookingId) {
        return ResponseEntity.ok(bookingService.confirmBooking(bookingId));
    }

    @Operation(summary = "Get booking quote")
    @GetMapping("/{bookingId}/quote")
    public ResponseEntity<BookingQuote> getQuote(@PathVariable String bookingId) {
        return ResponseEntity.ok(bookingService.getQuote(bookingId));
    }
}
