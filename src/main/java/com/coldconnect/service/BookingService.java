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
import org.springframework.context.annotation.Lazy;
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
    private final ImpactService                   impactService;
    private final AppMessages                     messages;

    public BookingService(BookingRepository bookingRepository,
                          BookingQuoteRepository quoteRepository,
                          ServiceRateRepository rateRepository,
                          BookingIdempotencyKeyRepository idempotencyRepository,
                          HubRepository hubRepository,
                          TenantRegionRepository regionRepository,
                          @Lazy ImpactService impactService,
                          AppMessages messages) {
        this.bookingRepository     = bookingRepository;
        this.quoteRepository       = quoteRepository;
        this.rateRepository        = rateRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.hubRepository         = hubRepository;
        this.regionRepository      = regionRepository;
        this.impactService         = impactService;
        this.messages              = messages;
    }

    public List<Booking> getCustomerBookings(Long customerId) {
        return bookingRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    public Booking getBooking(String bookingId, String language) {
        if (bookingId == null || bookingId.isBlank()) {
            throw new AppException.BadRequestException("Booking ID is required");
        }
        return bookingRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new AppException.NotFoundException(
                        messages.get(AppMessages.Key.BOOKING_NOT_FOUND, language)));
    }

    @Transactional
    public Booking createBooking(Long customerId, String serviceType, Long hubId,
                                 String region, Double quantityKg, Integer days,
                                 LocalDateTime windowStart, LocalDateTime windowEnd,
                                 String channel, String idempotencyKey,
                                 String pickupAddress, String dropoffAddress,
                                 Integer crateCount, String packagingType,
                                 String paymentMethod, String routeType,
                                 String loadSize, String commodityId,
                                 boolean operatorCallbackRequested,
                                 String language) {

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
        var hub = hubRepository.findById(hubId)
                .orElseThrow(() -> new AppException.NotFoundException(
                        messages.get(AppMessages.Key.HUB_NOT_FOUND, language)));

        if (hub.getStatus() != com.coldconnect.entity.Hub.HubStatus.ACTIVE) {
            throw new AppException.BadRequestException(
                    "Hub is not currently accepting bookings. Status: " + hub.getStatus());
        }

        double available = hub.getCapacityKg() - hub.getCurrentLoadKg();
        if (quantityKg > available) {
            throw new AppException.BadRequestException(
                    "Insufficient hub capacity. Available: " + available + "kg");
        }

        regionRepository.findByRegionId(region)
                .orElseThrow(() -> new AppException.NotFoundException(
                        "Region not found: " + region));

        // ── Duplicate prevention ──────────────────────────────────────────────
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = idempotencyRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                return bookingRepository.findById(existing.get().getBookingId())
                        .orElseThrow(() -> new AppException.NotFoundException(
                                messages.get(AppMessages.Key.BOOKING_NOT_FOUND, language)));
            }
        }

        // ── Calculate quote with fee breakdown ────────────────────────────────
        List<ServiceRate> rates = rateRepository.findByRegionAndServiceType(
                region, serviceType.toUpperCase());

        if (rates.isEmpty()) {
            throw new AppException.BadRequestException(
                    "No service rate configured for region '" + region
                            + "' and service type '" + serviceType + "'. Contact admin.");
        }

        ServiceRate rate   = rates.get(0);
        int         crates = crateCount != null ? crateCount : 1;

        BigDecimal storageFee      = rate.getBaseFee()
                .multiply(BigDecimal.valueOf(days));
        BigDecimal handlingFee     = rate.getStorageDayFee()
                .multiply(BigDecimal.valueOf(crates));
        BigDecimal weightChargeFee = rate.getBaseFee()
                .multiply(BigDecimal.valueOf(quantityKg / 100));
        BigDecimal total           = storageFee.add(handlingFee).add(weightChargeFee);

        // ── Create booking ────────────────────────────────────────────────────
        Booking booking = new Booking();
        booking.setCustomerId(customerId);
        booking.setServiceType(serviceType.toUpperCase());
        booking.setHubId(hubId);
        booking.setCommodityId(commodityId);
        booking.setStatus(Booking.BookingStatus.PENDING);
        booking.setScheduledWindowStart(windowStart);
        booking.setScheduledWindowEnd(windowEnd);
        booking.setPaymentStatus(Booking.PaymentStatus.UNPAID);
        booking.setSourceChannel(channel);
        booking.setPickupAddress(pickupAddress);
        booking.setDropoffAddress(dropoffAddress);
        booking.setCrateCount(crateCount);
        booking.setPackagingType(packagingType);
        booking.setPaymentMethod(paymentMethod);
        booking.setRouteType(routeType);
        booking.setLoadSize(loadSize);
        booking.setOperatorCallbackRequested(operatorCallbackRequested);
        booking = bookingRepository.save(booking);

        // ── Save quote with breakdown ─────────────────────────────────────────
        BookingQuote quote = new BookingQuote();
        quote.setBookingId(booking.getId());
        quote.setQuantityEstimateKg(quantityKg);
        quote.setDays(days);
        quote.setStorageFee(storageFee);
        quote.setHandlingFee(handlingFee);
        quote.setWeightChargeFee(weightChargeFee);
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
            throw new AppException.BadRequestException("Cannot confirm a cancelled booking");
        }
        if (booking.getStatus() == Booking.BookingStatus.COMPLETED) {
            throw new AppException.BadRequestException("Booking is already completed");
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

        if (!booking.getCustomerId().equals(userId)) {
            throw new AppException.UnauthorizedException("Not your booking");
        }
        if (booking.getStatus() == Booking.BookingStatus.COMPLETED) {
            throw new AppException.BadRequestException("Cannot cancel a completed booking");
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

    @Transactional
    public Booking completeBooking(String bookingId, Long userId, String language) {
        Booking booking = getBooking(bookingId, language);

        if (!booking.getCustomerId().equals(userId)) {
            throw new AppException.UnauthorizedException("Not your booking");
        }
        if (booking.getStatus() == Booking.BookingStatus.CANCELLED) {
            throw new AppException.BadRequestException("Cannot complete a cancelled booking");
        }
        if (booking.getStatus() == Booking.BookingStatus.COMPLETED) {
            throw new AppException.BadRequestException("Booking is already completed");
        }

        booking.setStatus(Booking.BookingStatus.COMPLETED);
        booking = bookingRepository.save(booking);

        // Auto-recalculate impact on completion
        impactService.recalculate(booking.getCustomerId());

        return booking;
    }

    @Transactional
    public Booking weighBooking(String bookingId, Double finalWeightKg,
                                Long userId, String language) {
        Booking booking = getBooking(bookingId, language);

        if (!booking.getCustomerId().equals(userId)) {
            throw new AppException.UnauthorizedException("Not your booking");
        }
        if (booking.getStatus() != Booking.BookingStatus.CONFIRMED
                && booking.getStatus() != Booking.BookingStatus.IN_PROGRESS) {
            throw new AppException.BadRequestException(
                    "Booking must be CONFIRMED or IN_PROGRESS to record weight");
        }
        if (finalWeightKg <= 0) {
            throw new AppException.BadRequestException(
                    "Final weight must be greater than zero");
        }

        BookingQuote quote = quoteRepository.findByBookingId(booking.getId())
                .orElseThrow(() -> new AppException.NotFoundException("Quote not found"));

        List<ServiceRate> rates = rateRepository.findByServiceType(
                booking.getServiceType());

        BigDecimal finalTotal = BigDecimal.ZERO;
        if (!rates.isEmpty()) {
            ServiceRate rate = rates.get(0);
            finalTotal = rate.getBaseFee()
                    .add(rate.getStorageDayFee()
                            .multiply(BigDecimal.valueOf(quote.getDays())))
                    .multiply(BigDecimal.valueOf(finalWeightKg / 100));
        }

        booking.setFinalWeightKg(finalWeightKg);
        booking.setFinalTotal(finalTotal);
        booking.setWeighedAt(LocalDateTime.now());
        booking = bookingRepository.save(booking);

        // Auto-recalculate impact after weighing
        impactService.recalculate(booking.getCustomerId());

        return booking;
    }

    public BookingQuote getQuote(String bookingId, String language) {
        Booking booking = getBooking(bookingId, language);
        return quoteRepository.findByBookingId(booking.getId())
                .orElseThrow(() -> new AppException.NotFoundException(
                        "Quote not found for booking: " + bookingId));
    }
}