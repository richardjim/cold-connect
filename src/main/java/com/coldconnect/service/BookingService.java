package com.coldconnect.service;

import com.coldconnect.entity.Booking;
import com.coldconnect.entity.BookingQuote;
import com.coldconnect.entity.ServiceRate;
import com.coldconnect.exception.AppException;
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

    private final BookingRepository bookingRepository;
    private final BookingQuoteRepository quoteRepository;
    private final ServiceRateRepository rateRepository;

    public BookingService(BookingRepository bookingRepository,
                          BookingQuoteRepository quoteRepository,
                          ServiceRateRepository rateRepository) {
        this.bookingRepository = bookingRepository;
        this.quoteRepository = quoteRepository;
        this.rateRepository = rateRepository;
    }

    public List<Booking> getCustomerBookings(Long customerId) {
        return bookingRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    public Booking getBooking(String bookingId) {
        return bookingRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new AppException.NotFoundException("Booking not found: " + bookingId));
    }

    @Transactional
    public Booking createBooking(Long customerId, String serviceType, Long hubId,
                                  String region, Double quantityKg, Integer days,
                                  LocalDateTime windowStart, LocalDateTime windowEnd,
                                  String channel) {
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

        return booking;
    }

    @Transactional
    public Booking confirmBooking(String bookingId) {
        Booking booking = getBooking(bookingId);
        if (booking.getStatus() != Booking.BookingStatus.PENDING) {
            throw new AppException.BadRequestException("Booking is not in PENDING state");
        }
        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        return bookingRepository.save(booking);
    }

    public BookingQuote getQuote(String bookingId) {
        Booking booking = getBooking(bookingId);
        return quoteRepository.findByBookingId(booking.getId())
                .orElseThrow(() -> new AppException.NotFoundException("Quote not found"));
    }
}
