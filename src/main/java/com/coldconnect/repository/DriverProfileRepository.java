package com.coldconnect.repository;

import com.coldconnect.entity.DriverProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DriverProfileRepository extends JpaRepository<DriverProfile, Long> {
    Optional<DriverProfile> findByUserId(Long userId);
    Optional<DriverProfile> findByDriverId(String driverId);
    List<DriverProfile> findByVettingStatus(String vettingStatus);
    List<DriverProfile> findByAssignedVehicleId(Long vehicleId);
}