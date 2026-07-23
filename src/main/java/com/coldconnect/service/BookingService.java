package com.coldconnect.service;

import com.coldconnect.entity.Booking;
import com.coldconnect.entity.BookingIdempotencyKey;
import com.coldconnect.entity.BookingQuote;
import com.coldconnect.entity.ServiceRate;
import com.coldconnect.exception.AppException;
import com.coldconnect.i18n.AppMessages;
import com.coldconnect.repository.BookingIdempotencyKeyRepository;
import com.coldconnect.repository.BookingQuoteRepository;
import com.coldconnect.repository.BookingRepository;
import com.coldconnect.repository.HubRepository;
import com.coldconnect.repository.ServiceRateRepository;
import com.coldconnect.repository.TenantRegionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class BookingService {

    private static final Set<String> VALID_SERVICE_TYPES =
            Set.of("STORAGE", "TRANSPORT", "PICKUP", "BUNDLE");

    private final BookingRepository               bookingRepository;
    private final BookingQuoteRepository          quoteRepository;
    private final ServiceRateRepository           rateRepository;
    private final BookingIdempotencyKeyRepository idempotencyRepository;
    private final HubRepository                   hubRepository;
    private final TenantRegionRepository          regionRepository;
    private final AppMessages                     messages;

    public BookingService(BookingRepository bookingRepository,
                          BookingQuoteRepository quoteRepository,
                          ServiceRateRepository rateRepository,
                          BookingIdempotencyKeyRepository idempotencyRepository,
                          HubRepository hubRepository,
                          TenantRegionRepository regionRepository,
                          AppMessages messages) {
        this.bookingRepository     = bookingRepository;
        this.quoteRepository       = quoteRepository;
        this.rateRepository        = rateRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.hubRepository         = hubRepository;
        this.regionRepository      = regionRepository;
        this.messages              = messages;
    }

    public List<Booking> getCustomerBookings(Long customerId) {
        return bookingRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    public Booking getBooking(String bookingId, String language) {
        // Input validation
        if (bookingId == null || bookingId.isBlank()) {
            throw new AppException.BadRequestException("Booking ID is required");
        }
        // DB validation
        return bookingRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new AppException.NotFoundException(
                        messages.get(AppMessages.Key.BOOKING_NOT_FOUND, language)));
    }

    @Transactional
    public Booking createBooking(Long customerId, String serviceType, Long hubId,
                                 String region, Double quantityKg, Integer days,
                                 LocalDateTime windowStart, LocalDateTime windowEnd,
                                 String channel, String idempotencyKey, String language) {

        // ── Input validation ──────────────────────────────────────────────────

        if (serviceType == null || serviceType.isBlank()) {
            throw new AppException.BadRequestException("Service type is required");
        }
        if (!VALID_SERVICE_TYPES.contains(serviceType.toUpperCase())) {
            throw new AppException.BadRequestException(
                    "Invalid service type. Must be one of: " + VALID_SERVICE_TYPES);
        }
        if (hubId == null) {
            throw new AppException.BadRequestException("Hub ID is required");
        }
        if (region == null || region.isBlank()) {
            throw new AppException.BadRequestException("Region is required");
        }
        if (quantityKg == null || quantityKg <= 0) {
            throw new AppException.BadRequestException(
                    "Quantity must be greater than zero");
        }
        if (days == null || days <= 0) {
            throw new AppException.BadRequestException(
                    "Days must be greater than zero");
        }
        if (days > 365) {
            throw new AppException.BadRequestException(
                    "Storage duration cannot exceed 365 days");
        }
        if (windowStart != null && windowStart.isBefore(LocalDateTime.now())) {
            throw new AppException.BadRequestException(
                    "Window start cannot be in the past");
        }
        if (windowStart != null && windowEnd != null
                && windowEnd.isBefore(windowStart)) {
            throw new AppException.BadRequestException(
                    "Window end must be after window start");
        }

        // ── DB validation ─────────────────────────────────────────────────────

        // Validate hub exists and is active
        var hub = hubRepository.findById(hubId)
                .orElseThrow(() -> new AppException.NotFoundException(
                        messages.get(AppMessages.Key.HUB_NOT_FOUND, language)));

        if (hub.getStatus() != com.coldconnect.entity.Hub.HubStatus.ACTIVE) {
            throw new AppException.BadRequestException(
                    "Hub is not currently accepting bookings. Status: " + hub.getStatus());
        }

        // Validate hub has enough capacity
        double available = hub.getCapacityKg() - hub.getCurrentLoadKg();
        if (quantityKg > available) {
            throw new AppException.BadRequestException(
                    "Insufficient hub capacity. Available: " + available + "kg");
        }

        // Validate region exists
        regionRepository.findByRegionId(region)
                .orElseThrow(() -> new AppException.NotFoundException(
                        "Region not found: " + region));


        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = idempotencyRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                return bookingRepository.findById(existing.get().getBookingId())
                        .orElseThrow(() -> new AppException.NotFoundException(
                                messages.get(AppMessages.Key.BOOKING_NOT_FOUND, language)));
            }
        }

        // ── Calculate quote ───────────────────────────────────────────────────

        List<ServiceRate> rates = rateRepository.findByRegionAndServiceType(
                region, serviceType.toUpperCase());
        BigDecimal total = BigDecimal.ZERO;
        if (!rates.isEmpty()) {
            ServiceRate rate = rates.get(0);
            total = rate.getBaseFee()
                    .add(rate.getStorageDayFee().multiply(BigDecimal.valueOf(days)))
                    .multiply(BigDecimal.valueOf(quantityKg / 100));
        }

        Booking booking = new Booking();
        booking.setCustomerId(customerId);
        booking.setServiceType(serviceType.toUpperCase());
        booking.setHubId(hubId);
        booking.setStatus(Booking.BookingStatus.PENDING);
        booking.setScheduledWindowStart(windowStart);
        booking.setScheduledWindowEnd(windowEnd);
        booking.setPaymentStatus(Booking.PaymentStatus.UNPAID);
        booking.setSourceChannel(channel);
        booking = bookingRepository.save(booking);

        BookingQuote quote = new BookingQuote();
        quote.setBookingId(booking.getId());
        quote.setQuantityEstimateKg(quantityKg);
        quote.setDays(days);
        quote.setTotal(total);
        quote.setExpiry(LocalDateTime.now().plusHours(24));
        quote.setAssumptions("Rate version: v1");
        quoteRepository.save(quote);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            BookingIdempotencyKey key = new BookingIdempotencyKey();
            key.setIdempotencyKey(idempotencyKey);
            key.setUserId(customerId);
            key.setBookingId(booking.getId());
            idempotencyRepository.save(key);
        }

        return booking;
    }

    @Transactional
    public Booking confirmBooking(String bookingId, String language) {
        Booking booking = getBooking(bookingId, language);

        if (booking.getStatus() == Booking.BookingStatus.CANCELLED) {
            throw new AppException.BadRequestException(
                    "Cannot confirm a cancelled booking");
        }
        if (booking.getStatus() == Booking.BookingStatus.COMPLETED) {
            throw new AppException.BadRequestException(
                    "Booking is already completed");
        }
        if (booking.getStatus() != Booking.BookingStatus.PENDING) {
            throw new AppException.BadRequestException(
                    messages.get(AppMessages.Key.BOOKING_NOT_PENDING, language));
        }

        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        return bookingRepository.save(booking);
    }

    @Transactional
    public Booking cancelBooking(String bookingId, Long userId, String language) {
        Booking booking = getBooking(bookingId, language);

        // Ownership check
        if (!booking.getCustomerId().equals(userId)) {
            throw new AppException.UnauthorizedException("Not your booking");
        }

        // Status check
        if (booking.getStatus() == Booking.BookingStatus.COMPLETED) {
            throw new AppException.BadRequestException(
                    "Cannot cancel a completed booking");
        }
        if (booking.getStatus() == Booking.BookingStatus.CANCELLED) {
            throw new AppException.BadRequestException(
                    messages.get(AppMessages.Key.BOOKING_CANCELLED, language));
        }
        if (booking.getStatus() == Booking.BookingStatus.IN_PROGRESS) {
            throw new AppException.BadRequestException(
                    "Cannot cancel a booking that is in progress");
        }

        booking.setStatus(Booking.BookingStatus.CANCELLED);
        return bookingRepository.save(booking);
    }

    public BookingQuote getQuote(String bookingId, String language) {
        Booking booking = getBooking(bookingId, language);
        return quoteRepository.findByBookingId(booking.getId())
                .orElseThrow(() -> new AppException.NotFoundException(
                        "Quote not found for booking: " + bookingId));
    }
}