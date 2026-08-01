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

    @Operation(
            summary = "Get all bookings — filterable by status",
            description = "Status: PENDING · CONFIRMED · IN_PROGRESS · COMPLETED · CANCELLED · DISPUTED"
    )
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllBookings(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false)    String status,
            @RequestParam(required = false)    String hubId,
            @RequestParam(required = false)    String serviceType) {

        var all = bookingRepository.findAll();

        // Filter by status
        if (status != null && !status.isBlank()) {
            try {
                Booking.BookingStatus s =
                        Booking.BookingStatus.valueOf(status.toUpperCase());
                all = all.stream().filter(b -> b.getStatus() == s).toList();
            } catch (IllegalArgumentException e) {
                throw new AppException.BadRequestException(
                        "Invalid status. Must be: PENDING, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED");
            }
        }

        // Filter by hubId
        if (hubId != null && !hubId.isBlank()) {
            try {
                Long hid = Long.valueOf(hubId);
                all = all.stream().filter(b -> hid.equals(b.getHubId())).toList();
            } catch (NumberFormatException ignored) {}
        }

        // Filter by serviceType
        if (serviceType != null && !serviceType.isBlank()) {
            all = all.stream()
                    .filter(b -> serviceType.equalsIgnoreCase(b.getServiceType()))
                    .toList();
        }

        // Sort newest first
        all = all.stream()
                .sorted((a, b) -> {
                    if (a.getCreatedAt() == null) return 1;
                    if (b.getCreatedAt() == null) return -1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .toList();

        // Manual pagination
        int total     = all.size();
        int fromIndex = Math.min(page * size, total);
        int toIndex   = Math.min(fromIndex + size, total);
        var pageItems = all.subList(fromIndex, toIndex);

        return ResponseEntity.ok(Map.of(
                "bookings",    pageItems,
                "total",       total,
                "page",        page,
                "size",        size,
                "totalPages",  (int) Math.ceil((double) total / size),
                "hasNext",     toIndex < total,
                "hasPrev",     page > 0
        ));
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