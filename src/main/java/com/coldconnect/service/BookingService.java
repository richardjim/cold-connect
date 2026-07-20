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
import com.coldconnect.repository.ServiceRateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingService {

    private final BookingRepository               bookingRepository;
    private final BookingQuoteRepository          quoteRepository;
    private final ServiceRateRepository           rateRepository;
    private final BookingIdempotencyKeyRepository idempotencyRepository;
    private final AppMessages                     messages;

    public BookingService(BookingRepository bookingRepository,
                          BookingQuoteRepository quoteRepository,
                          ServiceRateRepository rateRepository,
                          BookingIdempotencyKeyRepository idempotencyRepository,
                          AppMessages messages) {
        this.bookingRepository     = bookingRepository;
        this.quoteRepository       = quoteRepository;
        this.rateRepository        = rateRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.messages              = messages;
    }

    public List<Booking> getCustomerBookings(Long customerId) {
        return bookingRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    public Booking getBooking(String bookingId, String language) {
        return bookingRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new AppException.NotFoundException(
                        messages.get(AppMessages.Key.BOOKING_NOT_FOUND, language)));
    }

    @Transactional
    public Booking createBooking(Long customerId, String serviceType, Long hubId,
                                 String region, Double quantityKg, Integer days,
                                 LocalDateTime windowStart, LocalDateTime windowEnd,
                                 String channel, String idempotencyKey, String language) {

        // Duplicate prevention
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = idempotencyRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                return bookingRepository.findById(existing.get().getBookingId())
                        .orElseThrow(() -> new AppException.NotFoundException(
                                messages.get(AppMessages.Key.BOOKING_NOT_FOUND, language)));
            }
        }

        List<ServiceRate> rates = rateRepository.findByRegionAndServiceType(region, serviceType);
        BigDecimal total = BigDecimal.ZERO;
        if (!rates.isEmpty()) {
            ServiceRate rate = rates.get(0);
            total = rate.getBaseFee()
                    .add(rate.getStorageDayFee().multiply(BigDecimal.valueOf(days)))
                    .multiply(BigDecimal.valueOf(quantityKg / 100));
        }

        Booking booking = new Booking();
        booking.setCustomerId(customerId);
        booking.setServiceType(serviceType);
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
            throw new AppException.BadRequestException(
                    messages.get(AppMessages.Key.BOOKING_NOT_PENDING, language));
        }

        if (booking.getStatus() == Booking.BookingStatus.CANCELLED) {
            throw new AppException.BadRequestException(
                    messages.get(AppMessages.Key.BOOKING_CANCELLED, language));
        }

        booking.setStatus(Booking.BookingStatus.CANCELLED);
        return bookingRepository.save(booking);
    }

    public BookingQuote getQuote(String bookingId, String language) {
        Booking booking = getBooking(bookingId, language);
        return quoteRepository.findByBookingId(booking.getId())
                .orElseThrow(() -> new AppException.NotFoundException("Quote not found"));
    }
}