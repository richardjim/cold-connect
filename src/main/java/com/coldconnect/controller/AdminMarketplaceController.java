package com.coldconnect.controller;

import com.coldconnect.entity.BuyerProfile;
import com.coldconnect.entity.MarketLot;
import com.coldconnect.entity.MarketOrder;
import com.coldconnect.exception.AppException;
import com.coldconnect.repository.BuyerProfileRepository;
import com.coldconnect.repository.MarketLotRepository;
import com.coldconnect.repository.MarketOrderRepository;
import com.coldconnect.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/marketplace")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Marketplace", description = "Marketplace operations — Admin only")
public class AdminMarketplaceController extends BaseController {

    private final MarketOrderRepository orderRepository;
    private final MarketLotRepository   lotRepository;
    private final BuyerProfileRepository buyerRepository;

    public AdminMarketplaceController(UserRepository userRepository,
                                      MarketOrderRepository orderRepository,
                                      MarketLotRepository lotRepository,
                                      BuyerProfileRepository buyerRepository) {
        super(userRepository);
        this.orderRepository  = orderRepository;
        this.lotRepository    = lotRepository;
        this.buyerRepository  = buyerRepository;
    }

    @Operation(
            summary = "Marketplace KPI summary — GMV, orders, lots, buyers",
            description = "Admin marketplace ops dashboard"
    )
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary() {

        var orders = orderRepository.findAll();
        var lots   = lotRepository.findAll();
        var buyers = buyerRepository.findAll();

        BigDecimal gmv = orders.stream()
                .filter(o -> o.getStatus() != MarketOrder.OrderStatus.CANCELLED)
                .map(MarketOrder::getTotal)
                .filter(t -> t != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long liveLots = lots.stream()
                .filter(l -> l.getStatus() == MarketLot.LotStatus.LIVE)
                .count();

        long pendingOrders = orders.stream()
                .filter(o -> o.getStatus() == MarketOrder.OrderStatus.PENDING)
                .count();

        long verifiedBuyers = buyers.stream()
                .filter(b -> !"UNVERIFIED".equals(b.getKybStatus()))
                .count();

        return ResponseEntity.ok(Map.of(
                "gmv",            gmv,
                "totalOrders",    orders.size(),
                "pendingOrders",  pendingOrders,
                "liveLots",       liveLots,
                "totalLots",      lots.size(),
                "totalBuyers",    buyers.size(),
                "verifiedBuyers", verifiedBuyers
        ));
    }

    @Operation(summary = "Get all marketplace orders — filterable by status")
    @GetMapping("/orders")
    public ResponseEntity<List<MarketOrder>> getOrders(
            @RequestParam(required = false) String status) {

        var orders = orderRepository.findAll();

        if (status != null) {
            try {
                MarketOrder.OrderStatus s =
                        MarketOrder.OrderStatus.valueOf(status.toUpperCase());
                orders = orders.stream()
                        .filter(o -> o.getStatus() == s)
                        .toList();
            } catch (IllegalArgumentException e) {
                throw new AppException.BadRequestException("Invalid status: " + status);
            }
        }

        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "Update marketplace order status")
    @PatchMapping("/orders/{orderId}/status")
    public ResponseEntity<Map<String, Object>> updateOrderStatus(
            @PathVariable String orderId,
            @RequestBody Map<String, String> body) {

        MarketOrder order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new AppException.NotFoundException(
                        "Order not found: " + orderId));

        try {
            order.setStatus(
                    MarketOrder.OrderStatus.valueOf(body.get("status").toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new AppException.BadRequestException(
                    "Invalid status: " + body.get("status"));
        }

        orderRepository.save(order);

        return ResponseEntity.ok(Map.of(
                "message", "Order status updated",
                "orderId", orderId,
                "status",  order.getStatus().name()
        ));
    }

    @Operation(summary = "Get all lots — filterable by status")
    @GetMapping("/lots")
    public ResponseEntity<List<MarketLot>> getLots(
            @RequestParam(required = false) String status) {

        var lots = lotRepository.findAll();

        if (status != null) {
            try {
                MarketLot.LotStatus s = MarketLot.LotStatus.valueOf(status.toUpperCase());
                lots = lots.stream()
                        .filter(l -> l.getStatus() == s)
                        .toList();
            } catch (IllegalArgumentException e) {
                throw new AppException.BadRequestException("Invalid status: " + status);
            }
        }

        return ResponseEntity.ok(lots);
    }

    @Operation(summary = "Get all buyer profiles — filterable by KYB status")
    @GetMapping("/buyers")
    public ResponseEntity<List<BuyerProfile>> getBuyers(
            @RequestParam(required = false) String kybStatus) {

        var buyers = buyerRepository.findAll();

        if (kybStatus != null) {
            buyers = buyers.stream()
                    .filter(b -> kybStatus.equalsIgnoreCase(b.getKybStatus()))
                    .toList();
        }

        return ResponseEntity.ok(buyers);
    }
}