package com.coldconnect.controller;

import com.coldconnect.entity.Trip;
import com.coldconnect.exception.AppException;
import com.coldconnect.repository.DriverProfileRepository;
import com.coldconnect.repository.TripRepository;
import com.coldconnect.repository.UserRepository;
import com.coldconnect.repository.VehicleRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/dispatch")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Dispatch", description = "Trip dispatch and route management — Admin only")
public class AdminDispatchController extends BaseController {

    private final TripRepository          tripRepository;
    private final VehicleRepository       vehicleRepository;
    private final DriverProfileRepository driverRepository;

    public AdminDispatchController(UserRepository userRepository,
                                   TripRepository tripRepository,
                                   VehicleRepository vehicleRepository,
                                   DriverProfileRepository driverRepository) {
        super(userRepository);
        this.tripRepository   = tripRepository;
        this.vehicleRepository = vehicleRepository;
        this.driverRepository  = driverRepository;
    }

    public record AssignDriverRequest(
            Long driverId,
            Long vehicleId,
            String notes
    ) {}

    @Operation(summary = "Get all trips — filterable by status")
    @GetMapping("/trips")
    public ResponseEntity<Map<String, Object>> getTrips(
            @RequestParam(required = false) String status) {

        var trips = tripRepository.findAll();

        if (status != null) {
            trips = trips.stream()
                    .filter(t -> t.getStatus() != null
                            && status.equalsIgnoreCase(t.getStatus().name()))
                    .toList();
        }

        return ResponseEntity.ok(Map.of(
                "trips",    trips,
                "vehicles", vehicleRepository.findAll(),
                "drivers",  driverRepository.findAll(),
                "count",    trips.size()
        ));
    }

    @Operation(summary = "Get trip detail")
    @GetMapping("/trips/{tripId}")
    public ResponseEntity<Trip> getTrip(@PathVariable String tripId) {
        return ResponseEntity.ok(
                tripRepository.findByTripId(tripId)
                        .orElseThrow(() -> new AppException.NotFoundException(
                                "Trip not found: " + tripId))
        );
    }

    @Operation(summary = "Assign driver and vehicle to a trip")
    @PatchMapping("/trips/{tripId}/driver")
    public ResponseEntity<Map<String, Object>> assignDriver(
            @PathVariable String tripId,
            @RequestBody AssignDriverRequest req) {

        Trip trip = tripRepository.findByTripId(tripId)
                .orElseThrow(() -> new AppException.NotFoundException(
                        "Trip not found: " + tripId));

        if (req.driverId() != null) {
            driverRepository.findById(req.driverId())
                    .orElseThrow(() -> new AppException.NotFoundException(
                            "Driver not found: " + req.driverId()));
            trip.setDriverId(req.driverId());
        }

        if (req.vehicleId() != null) {
            vehicleRepository.findById(req.vehicleId())
                    .orElseThrow(() -> new AppException.NotFoundException(
                            "Vehicle not found: " + req.vehicleId()));
            trip.setVehicleId(req.vehicleId());
        }

        tripRepository.save(trip);

        return ResponseEntity.ok(Map.of(
                "message",   "Driver and vehicle assigned",
                "tripId",    tripId,
                "driverId",  req.driverId() != null ? req.driverId() : "",
                "vehicleId", req.vehicleId() != null ? req.vehicleId() : ""
        ));
    }

    @Operation(summary = "Update trip status")
    @PatchMapping("/trips/{tripId}/status")
    public ResponseEntity<Map<String, Object>> updateStatus(
            @PathVariable String tripId,
            @RequestBody Map<String, String> body) {

        Trip trip = tripRepository.findByTripId(tripId)
                .orElseThrow(() -> new AppException.NotFoundException(
                        "Trip not found: " + tripId));

        try {
            trip.setStatus(Trip.TripStatus.valueOf(
                    body.get("status").toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new AppException.BadRequestException(
                    "Invalid status: " + body.get("status"));
        }

        tripRepository.save(trip);

        return ResponseEntity.ok(Map.of(
                "message", "Trip status updated",
                "tripId",  tripId,
                "status",  trip.getStatus().name()
        ));
    }
}