package com.coldconnect.controller;

import com.coldconnect.entity.*;
import com.coldconnect.repository.UserRepository;
import com.coldconnect.service.MarketplaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/v1/marketplace")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Marketplace", description = "Browse lots, place orders, sell stored crates")
public class MarketplaceController extends BaseController {

    private final MarketplaceService marketplaceService;

    public MarketplaceController(UserRepository userRepository, MarketplaceService marketplaceService) {
        super(userRepository);
        this.marketplaceService = marketplaceService;
    }

    public record ListLotRequest(
            @NotBlank String crateIds,
            @NotBlank String commodityId,
            String grade,
            @NotNull Double kg,
            @NotNull BigDecimal pricePerKg,
            Double minOrderKg) {}

    public record PlaceOrderRequest(
            @NotNull List<OrderItem> items,
            @NotBlank String fulfilmentType,
            String destAddress,
            String paymentPreference) {}

    @Operation(summary = "Browse live lots — filter by commodity")
    @GetMapping("/lots")
    public ResponseEntity<List<MarketLot>> browseLots(@RequestParam(required = false) String commodity) {
        return ResponseEntity.ok(marketplaceService.browseLots(commodity));
    }

    @Operation(summary = "Get lot detail with cold-chain summary")
    @GetMapping("/lots/{lotId}")
    public ResponseEntity<MarketLot> getLot(@PathVariable String lotId) {
        return ResponseEntity.ok(marketplaceService.getLot(lotId));
    }

    @Operation(summary = "Get full cold-chain for a lot")
    @GetMapping("/lots/{lotId}/chain")
    public ResponseEntity<List<ChainEvent>> getLotChain(@PathVariable String lotId) {
        return ResponseEntity.ok(marketplaceService.getLotChain(lotId));
    }

    @Operation(summary = "List stored crates for sale")
    @PostMapping("/lots")
    public ResponseEntity<MarketLot> listLot(@AuthenticationPrincipal UserDetails userDetails,
                                              @Valid @RequestBody ListLotRequest req) {
        Long sellerId = resolveUser(userDetails).getId();
        return ResponseEntity.ok(marketplaceService.listLot(
                sellerId, req.crateIds(), req.commodityId(), req.grade(),
                req.kg(), req.pricePerKg(), req.minOrderKg()));
    }

    @Operation(summary = "Place an order from cart")
    @PostMapping("/orders")
    public ResponseEntity<MarketOrder> placeOrder(@AuthenticationPrincipal UserDetails userDetails,
                                                   @Valid @RequestBody PlaceOrderRequest req) {
        Long buyerId = resolveUser(userDetails).getId();
        return ResponseEntity.ok(marketplaceService.placeOrder(
                buyerId, req.items(), req.fulfilmentType(), req.destAddress(), req.paymentPreference()));
    }

    @Operation(summary = "Get my orders")
    @GetMapping("/orders")
    public ResponseEntity<List<MarketOrder>> getMyOrders(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(marketplaceService.getBuyerOrders(resolveUser(userDetails).getId()));
    }

    @Operation(summary = "Get order detail with tracking")
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<MarketOrder> getOrder(@PathVariable String orderId) {
        return ResponseEntity.ok(marketplaceService.getOrder(orderId));
    }
}
