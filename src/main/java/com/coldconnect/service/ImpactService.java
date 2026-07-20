package com.coldconnect.service;

import com.coldconnect.entity.Booking;
import com.coldconnect.entity.ImpactMetric;
import com.coldconnect.repository.BookingRepository;
import com.coldconnect.repository.ImpactMetricRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ImpactService {

    private final ImpactMetricRepository impactRepository;
    private final BookingRepository      bookingRepository;

    // Impact constants per PRD
    private static final double FOOD_SAVED_RATIO         = 0.30;
    private static final double CO2_PER_KG_FOOD_SAVED    = 2.50;
    private static final double SOLAR_KWH_PER_KG_PER_DAY = 0.10;

    public ImpactService(ImpactMetricRepository impactRepository,
                         BookingRepository bookingRepository) {
        this.impactRepository = impactRepository;
        this.bookingRepository = bookingRepository;
    }

    public static class ImpactResponse {
        public final Double        foodSavedKg;
        public final Double        co2AvoidedKg;
        public final Double        spoiledFoodPreventedKg;
        public final Double        solarCoolingKwh;
        public final Integer       totalBookings;
        public final Integer       totalStorageDays;
        public final Double        treesEquivalent;
        public final Double        kmNotDriven;
        public final LocalDateTime lastCalculatedAt;

        public ImpactResponse(ImpactMetric m) {
            this.foodSavedKg            = m.getFoodSavedKg() != null ? m.getFoodSavedKg() : 0.0;
            this.co2AvoidedKg           = m.getCo2AvoidedKg() != null ? m.getCo2AvoidedKg() : 0.0;
            this.spoiledFoodPreventedKg = m.getSpoiledFoodPreventedKg() != null ? m.getSpoiledFoodPreventedKg() : 0.0;
            this.solarCoolingKwh        = m.getSolarCoolingKwh() != null ? m.getSolarCoolingKwh() : 0.0;
            this.totalBookings          = m.getTotalBookings() != null ? m.getTotalBookings() : 0;
            this.totalStorageDays       = m.getTotalStorageDays() != null ? m.getTotalStorageDays() : 0;
            this.treesEquivalent        = this.co2AvoidedKg / 21.0;
            this.kmNotDriven            = this.co2AvoidedKg / 0.21;
            this.lastCalculatedAt       = m.getLastCalculatedAt();
        }
    }

    @Transactional
    public ImpactResponse getImpact(Long userId) {
        ImpactMetric metric = impactRepository.findByUserId(userId)
                .orElseGet(() -> calculateAndSave(userId));
        return new ImpactResponse(metric);
    }

    @Transactional
    public ImpactResponse recalculate(Long userId) {
        return new ImpactResponse(calculateAndSave(userId));
    }

    @Transactional
    public ImpactMetric calculateAndSave(Long userId) {
        List<Booking> bookings = bookingRepository
                .findByCustomerIdOrderByCreatedAtDesc(userId)
                .stream()
                .filter(b -> b.getStatus() == Booking.BookingStatus.COMPLETED)
                .toList();

        double totalKgStored = 0.0;
        int    totalDays     = 0;

        for (Booking b : bookings) {
            totalKgStored += 100.0; // default per booking
            totalDays     += (b.getScheduledWindowStart() != null
                    && b.getScheduledWindowEnd() != null)
                    ? (int) java.time.Duration.between(
                    b.getScheduledWindowStart(),
                    b.getScheduledWindowEnd()).toDays()
                    : 7;
        }

        double foodSaved  = totalKgStored * FOOD_SAVED_RATIO;
        double co2Avoided = foodSaved * CO2_PER_KG_FOOD_SAVED;
        double solarKwh   = totalKgStored * totalDays * SOLAR_KWH_PER_KG_PER_DAY;

        ImpactMetric metric = impactRepository.findByUserId(userId)
                .orElse(new ImpactMetric());

        metric.setUserId(userId);
        metric.setFoodSavedKg(foodSaved);
        metric.setCo2AvoidedKg(co2Avoided);
        metric.setSpoiledFoodPreventedKg(foodSaved);
        metric.setSolarCoolingKwh(solarKwh);
        metric.setTotalBookings(bookings.size());
        metric.setTotalStorageDays(totalDays);
        metric.setLastCalculatedAt(LocalDateTime.now());

        return impactRepository.save(metric);
    }
}