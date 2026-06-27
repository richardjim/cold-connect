package com.coldconnect.repository;

import com.coldconnect.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TripRepository extends JpaRepository<Trip, Long> {
    List<Trip> findByDriverId(Long driverId);
    Optional<Trip> findByTripId(String tripId);
}
