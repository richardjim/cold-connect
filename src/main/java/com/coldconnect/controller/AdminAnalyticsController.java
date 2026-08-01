package com.coldconnect.controller;

import com.coldconnect.entity.Booking;
import com.coldconnect.entity.CrateLot;
import com.coldconnect.entity.Payment;
import com.coldconnect.entity.SupportCase;
import com.coldconnect.repository.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/analytics")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Analytics", description = "Platform analytics — Admin only")
public class AdminAnalyticsController extends BaseController {

    private final BookingRepository     bookingRepository;
    private final MarketOrderRepository orderRepository;
    private final HubRepository         hubRepository;
    private final PaymentRepository     paymentRepository;
    private final SupportCaseRepository supportCaseRepository;
    private final CrateLotRepository    crateLotRepository;

    public AdminAnalyticsController(UserRepository userRepository,
                                    BookingRepository bookingRepository,
                                    MarketOrderRepository orderRepository,
                                    HubRepository hubRepository,
                                    PaymentRepository paymentRepository,
                                    SupportCaseRepository supportCaseRepository,
                                    CrateLotRepository crateLotRepository) {
        super(userRepository);
        this.bookingRepository     = bookingRepository;
        this.orderRepository       = orderRepository;
        this.hubRepository         = hubRepository;
        this.paymentRepository     = paymentRepository;
        this.supportCaseRepository = supportCaseRepository;
        this.crateLotRepository    = crateLotRepository;
    }

    @Operation(summary = "Admin overview — KPIs, revenue chart, issues, hubs")
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getOverview(
            @RequestParam(required = false) String region) {

        long totalCratesInStorage = crateLotRepository.findAll().stream()
                .filter(c -> c.getStatus() == CrateLot.CrateStatus.IN_STORAGE)
                .count();

        var bookings = bookingRepository.findAll();
        var payments = paymentRepository.findAll();
        var cases    = supportCaseRepository.findAll();
        var hubs     = hubRepository.findAll();

        // Revenue last 7 days
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        BigDecimal revenue7d = payments.stream()
                .filter(p -> "CAPTURED".equals(p.getStatus())
                        && p.getCreatedAt() != null
                        && p.getCreatedAt().isAfter(sevenDaysAgo))
                .map(Payment::getAmount)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long openIssues = cases.stream()
                .filter(c -> !"CLOSED".equalsIgnoreCase(c.getStatus())
                        && !"RESOLVED".equalsIgnoreCase(c.getStatus()))
                .count();

        // Issues by type
        Map<String, Long> issuesByType = cases.stream()
                .filter(c -> c.getType() != null)
                .collect(Collectors.groupingBy(
                        SupportCase::getType,
                        Collectors.counting()));

        // Hub table
        var hubTable = hubs.stream().map(hub -> {
            Map<String, Object> h = new HashMap<>();
            h.put("hubId",        hub.getHubId());
            h.put("name",         hub.getName());
            h.put("capacityPct",  hub.getCapacityKg() != null && hub.getCapacityKg() > 0
                    ? Math.round((hub.getCurrentLoadKg() / hub.getCapacityKg()) * 100) : 0);
            h.put("tempCurrentC", hub.getTempCurrentC());
            h.put("powerStatus",  hub.getPowerStatus());
            h.put("solarKw",      hub.getSolarCapacityKw() != null ? hub.getSolarCapacityKw() : 0);
            h.put("status",       hub.getStatus() != null ? hub.getStatus().name() : "UNKNOWN");
            return h;
        }).toList();

        // Revenue by day chart
        var revenueByDay = new LinkedHashMap<String, BigDecimal>();
        for (int i = 6; i >= 0; i--) {
            LocalDate  day      = LocalDate.now().minusDays(i);
            String     label    = day.getDayOfWeek()
                    .getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            BigDecimal dayRevenue = payments.stream()
                    .filter(p -> "CAPTURED".equals(p.getStatus())
                            && p.getCreatedAt() != null
                            && p.getCreatedAt().toLocalDate().equals(day))
                    .map(Payment::getAmount)
                    .filter(a -> a != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            revenueByDay.put(label, dayRevenue);
        }

        long pending   = bookings.stream()
                .filter(b -> b.getStatus() == Booking.BookingStatus.PENDING).count();
        long confirmed = bookings.stream()
                .filter(b -> b.getStatus() == Booking.BookingStatus.CONFIRMED).count();
        long completed = bookings.stream()
                .filter(b -> b.getStatus() == Booking.BookingStatus.COMPLETED).count();
        long cancelled = bookings.stream()
                .filter(b -> b.getStatus() == Booking.BookingStatus.CANCELLED).count();

        return ResponseEntity.ok(Map.of(
                "cratesInStorage",  totalCratesInStorage,
                "revenue7d",        revenue7d,
                "openIssues",       openIssues,
                "issuesByType",     issuesByType,
                "hubs",             hubTable,
                "revenueByDay",     revenueByDay,
                "totalBookings",    bookings.size(),
                "bookingsByStatus", Map.of(
                        "PENDING",   pending,
                        "CONFIRMED", confirmed,
                        "COMPLETED", completed,
                        "CANCELLED", cancelled
                )
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
                "utilizationPct", hub.getCapacityKg() != null && hub.getCapacityKg() > 0
                        ? Math.round((hub.getCurrentLoadKg() / hub.getCapacityKg()) * 100) : 0,
                "status",         hub.getStatus() != null ? hub.getStatus().name() : "UNKNOWN"
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