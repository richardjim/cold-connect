package com.coldconnect.repository;

import com.coldconnect.entity.TripStop;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TripStopRepository extends JpaRepository<TripStop, Long> {
    List<TripStop> findByTripIdOrderBySequenceAsc(Long tripId);
    List<TripStop> findByCustomerId(Long customerId);
}
