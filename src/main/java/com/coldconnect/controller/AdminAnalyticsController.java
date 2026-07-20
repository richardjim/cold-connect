package com.coldconnect.controller;

import com.coldconnect.entity.Booking;
import com.coldconnect.repository.BookingRepository;
import com.coldconnect.repository.HubRepository;
import com.coldconnect.repository.MarketOrderRepository;
import com.coldconnect.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/analytics")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Analytics", description = "Platform analytics — Admin only")
public class AdminAnalyticsController extends BaseController {

    private final BookingRepository     bookingRepository;
    private final MarketOrderRepository orderRepository;
    private final HubRepository         hubRepository;

    public AdminAnalyticsController(UserRepository userRepository,
                                    BookingRepository bookingRepository,
                                    MarketOrderRepository orderRepository,
                                    HubRepository hubRepository) {
        super(userRepository);
        this.bookingRepository = bookingRepository;
        this.orderRepository   = orderRepository;
        this.hubRepository     = hubRepository;
    }

    @Operation(summary = "Platform overview — users, bookings, orders, hubs")
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getOverview() {
        long totalUsers    = userRepository.count();
        long totalBookings = bookingRepository.count();
        long totalOrders   = orderRepository.count();
        long totalHubs     = hubRepository.count();

        var bookings = bookingRepository.findAll();
        long pending   = bookings.stream().filter(b -> b.getStatus() == Booking.BookingStatus.PENDING).count();
        long confirmed = bookings.stream().filter(b -> b.getStatus() == Booking.BookingStatus.CONFIRMED).count();
        long completed = bookings.stream().filter(b -> b.getStatus() == Booking.BookingStatus.COMPLETED).count();
        long cancelled = bookings.stream().filter(b -> b.getStatus() == Booking.BookingStatus.CANCELLED).count();

        return ResponseEntity.ok(Map.of(
                "totalUsers",        totalUsers,
                "totalBookings",     totalBookings,
                "pendingBookings",   pending,
                "confirmedBookings", confirmed,
                "completedBookings", completed,
                "cancelledBookings", cancelled,
                "totalOrders",       totalOrders,
                "totalHubs",         totalHubs
        ));
    }

    @Operation(summary = "Hub capacity and utilization overview")
    @GetMapping("/hubs")
    public ResponseEntity<Object> getHubAnalytics() {
        var hubs = hubRepository.findAll().stream().map(hub -> Map.of(
                "hubId",          hub.getHubId(),
                "name",           hub.getName(),
                "capacityKg",     hub.getCapacityKg(),
                "currentLoadKg",  hub.getCurrentLoadKg(),
                "utilizationPct", hub.getCapacityKg() > 0
                        ? Math.round((hub.getCurrentLoadKg() / hub.getCapacityKg()) * 100)
                        : 0,
                "status",         hub.getStatus()
        )).toList();
        return ResponseEntity.ok(hubs);
    }

    @Operation(summary = "Booking status breakdown")
    @GetMapping("/bookings")
    public ResponseEntity<Object> getBookingAnalytics() {
        var bookings = bookingRepository.findAll();
        return ResponseEntity.ok(Map.of(
                "total", bookings.size(),
                "byStatus", Map.of(
                        "PENDING",     bookings.stream().filter(b -> b.getStatus() == Booking.BookingStatus.PENDING).count(),
                        "CONFIRMED",   bookings.stream().filter(b -> b.getStatus() == Booking.BookingStatus.CONFIRMED).count(),
                        "IN_PROGRESS", bookings.stream().filter(b -> b.getStatus() == Booking.BookingStatus.IN_PROGRESS).count(),
                        "COMPLETED",   bookings.stream().filter(b -> b.getStatus() == Booking.BookingStatus.COMPLETED).count(),
                        "CANCELLED",   bookings.stream().filter(b -> b.getStatus() == Booking.BookingStatus.CANCELLED).count()
                )
        ));
    }

    @Operation(summary = "User count by role")
    @GetMapping("/users")
    public ResponseEntity<Object> getUserAnalytics() {
        var users = userRepository.findAll();
        return ResponseEntity.ok(Map.of(
                "total", users.size(),
                "byRole", Map.of(
                        "CUSTOMER", users.stream().filter(u -> u.getRole().name().equals("CUSTOMER")).count(),
                        "DRIVER",   users.stream().filter(u -> u.getRole().name().equals("DRIVER")).count(),
                        "OPERATOR", users.stream().filter(u -> u.getRole().name().equals("OPERATOR")).count(),
                        "ADMIN",    users.stream().filter(u -> u.getRole().name().equals("ADMIN")).count()
                )
        ));
    }
}