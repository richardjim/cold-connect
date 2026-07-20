package com.coldconnect.controller;

import com.coldconnect.entity.Booking;
import com.coldconnect.entity.BookingQuote;
import com.coldconnect.exception.AppException;
import com.coldconnect.repository.BookingQuoteRepository;
import com.coldconnect.repository.BookingRepository;
import com.coldconnect.repository.ServiceRateRepository;
import com.coldconnect.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/bookings")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Bookings", description = "Booking management — Admin only")
public class AdminBookingController extends BaseController {

    private final BookingRepository      bookingRepository;
    private final BookingQuoteRepository quoteRepository;
    private final ServiceRateRepository  rateRepository;

    public AdminBookingController(UserRepository userRepository,
                                  BookingRepository bookingRepository,
                                  BookingQuoteRepository quoteRepository,
                                  ServiceRateRepository rateRepository) {
        super(userRepository);
        this.bookingRepository = bookingRepository;
        this.quoteRepository   = quoteRepository;
        this.rateRepository    = rateRepository;
    }

    public record ManualBookingRequest(
            @NotNull  Long customerId,
            @NotBlank String serviceType,
            @NotNull  Long hubId,
            @NotBlank String region,
            @NotNull  Double quantityKg,
            @NotNull  Integer days,
            LocalDateTime windowStart,
            LocalDateTime windowEnd,
            String overrideReason
    ) {}

    @Operation(summary = "Get all bookings — paginated and filterable")
    @GetMapping
    public ResponseEntity<Page<Booking>> getAllBookings(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false)    String status) {

        PageRequest pageable = PageRequest.of(page, size,
                Sort.by("id").descending());

        Page<Booking> bookings = bookingRepository.findAll(pageable);

        if (status != null) {
            try {
                Booking.BookingStatus s = Booking.BookingStatus.valueOf(status.toUpperCase());
                var filtered = bookingRepository.findAll().stream()
                        .filter(b -> b.getStatus() == s).toList();
                return ResponseEntity.ok(
                        new org.springframework.data.domain.PageImpl<>(filtered, pageable, filtered.size())
                );
            } catch (IllegalArgumentException e) {
                throw new AppException.BadRequestException("Invalid status: " + status);
            }
        }

        return ResponseEntity.ok(bookings);
    }

    @Operation(summary = "Get booking detail")
    @GetMapping("/{bookingId}")
    public ResponseEntity<Booking> getBooking(@PathVariable String bookingId) {
        return ResponseEntity.ok(
                bookingRepository.findByBookingId(bookingId)
                        .orElseThrow(() -> new AppException.NotFoundException(
                                "Booking not found: " + bookingId))
        );
    }

    @Operation(
            summary = "Create manual booking",
            description = "Admin-created booking on behalf of customer. Requires override reason."
    )
    @PostMapping
    public ResponseEntity<Map<String, Object>> createManualBooking(
            @org.springframework.security.core.annotation.AuthenticationPrincipal
            org.springframework.security.core.userdetails.UserDetails userDetails,
            @RequestBody ManualBookingRequest req) {

        Long adminId = resolveUser(userDetails).getId();

        var rates = rateRepository.findByRegionAndServiceType(
                req.region(), req.serviceType());

        BigDecimal total = BigDecimal.ZERO;
        if (!rates.isEmpty()) {
            var rate = rates.get(0);
            total = rate.getBaseFee()
                    .add(rate.getStorageDayFee().multiply(BigDecimal.valueOf(req.days())))
                    .multiply(BigDecimal.valueOf(req.quantityKg() / 100));
        }

        Booking booking = new Booking();
        booking.setCustomerId(req.customerId());
        booking.setServiceType(req.serviceType());
        booking.setHubId(req.hubId());
        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        booking.setScheduledWindowStart(req.windowStart());
        booking.setScheduledWindowEnd(req.windowEnd());
        booking.setPaymentStatus(Booking.PaymentStatus.UNPAID);
        booking.setSourceChannel("ADMIN");
        booking = bookingRepository.save(booking);

        BookingQuote quote = new BookingQuote();
        quote.setBookingId(booking.getId());
        quote.setQuantityEstimateKg(req.quantityKg());
        quote.setDays(req.days());
        quote.setTotal(total);
        quote.setExpiry(LocalDateTime.now().plusHours(24));
        quote.setAssumptions("Admin manual booking — override: " + req.overrideReason());
        quoteRepository.save(quote);

        return ResponseEntity.ok(Map.of(
                "message",   "Manual booking created",
                "bookingId", booking.getBookingId(),
                "createdBy", adminId,
                "status",    "CONFIRMED"
        ));
    }

    @Operation(summary = "Update booking status — Admin override")
    @PatchMapping("/{bookingId}/status")
    public ResponseEntity<Map<String, Object>> updateStatus(
            @PathVariable String bookingId,
            @RequestBody Map<String, String> body) {

        Booking booking = bookingRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new AppException.NotFoundException(
                        "Booking not found: " + bookingId));

        try {
            booking.setStatus(
                    Booking.BookingStatus.valueOf(body.get("status").toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new AppException.BadRequestException("Invalid status: " + body.get("status"));
        }

        bookingRepository.save(booking);

        return ResponseEntity.ok(Map.of(
                "message",   "Booking status updated",
                "bookingId", bookingId,
                "status",    booking.getStatus().name()
        ));
    }
}