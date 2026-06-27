package com.coldconnect.service;

import com.coldconnect.entity.*;
import com.coldconnect.exception.AppException;
import com.coldconnect.repository.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TrackingService {

    private final CrateLotRepository crateRepository;
    private final TripRepository tripRepository;
    private final TripStopRepository tripStopRepository;
    private final SensorReadingRepository sensorRepository;
    private final ChainEventRepository chainRepository;

    public TrackingService(CrateLotRepository crateRepository,
                            TripRepository tripRepository,
                            TripStopRepository tripStopRepository,
                            SensorReadingRepository sensorRepository,
                            ChainEventRepository chainRepository) {
        this.crateRepository = crateRepository;
        this.tripRepository = tripRepository;
        this.tripStopRepository = tripStopRepository;
        this.sensorRepository = sensorRepository;
        this.chainRepository = chainRepository;
    }

    public CrateLot getCrate(String crateId) {
        return crateRepository.findByCrateId(crateId)
                .orElseThrow(() -> new AppException.NotFoundException("Crate not found: " + crateId));
    }

    public List<CrateLot> getCustomerCrates(Long ownerId) {
        return crateRepository.findByOwnerId(ownerId);
    }

    public Trip getTrip(String tripId) {
        return tripRepository.findByTripId(tripId)
                .orElseThrow(() -> new AppException.NotFoundException("Trip not found: " + tripId));
    }

    public List<TripStop> getTripStops(String tripId) {
        Trip trip = getTrip(tripId);
        return tripStopRepository.findByTripIdOrderBySequenceAsc(trip.getId());
    }

    public List<SensorReading> getSensorHistory(String assetId) {
        return sensorRepository.findByAssetIdOrderByTimestampDesc(assetId);
    }

    public List<ChainEvent> getChain(String lotId) {
        return chainRepository.findByLotIdOrderByStartedAtAsc(lotId);
    }
}
