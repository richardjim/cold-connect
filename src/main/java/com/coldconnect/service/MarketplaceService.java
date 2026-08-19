package com.coldconnect.service;

import com.coldconnect.entity.ChainEvent;
import com.coldconnect.entity.MarketLot;
import com.coldconnect.entity.MarketOrder;
import com.coldconnect.entity.OrderItem;
import com.coldconnect.exception.AppException;
import com.coldconnect.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Service
public class MarketplaceService {

    private static final Set<String> VALID_GRADES       = Set.of("A", "B", "C");
    private static final Set<String> VALID_FULFILMENT   = Set.of("DELIVERY", "COLLECTION");
    private static final Set<String> VALID_PAYMENT_PREF = Set.of("PREPAID", "PAY_ON_DELIVERY", "CREDIT");

    private final MarketLotRepository   lotRepository;
    private final MarketOrderRepository orderRepository;
    private final OrderItemRepository   itemRepository;
    private final ChainEventRepository  chainRepository;
    private final CrateLotRepository    crateLotRepository;
    private final CommodityRepository   commodityRepository;
    private final HubRepository         hubRepository;
    private final UserRepository        userRepository;

    public MarketplaceService(MarketLotRepository lotRepository,
                              MarketOrderRepository orderRepository,
                              OrderItemRepository itemRepository,
                              ChainEventRepository chainRepository,
                              CrateLotRepository crateLotRepository,
                              CommodityRepository commodityRepository,
                              HubRepository hubRepository,
                              UserRepository userRepository) {
        this.lotRepository       = lotRepository;
        this.orderRepository     = orderRepository;
        this.itemRepository      = itemRepository;
        this.chainRepository     = chainRepository;
        this.crateLotRepository  = crateLotRepository;
        this.commodityRepository = commodityRepository;
        this.hubRepository       = hubRepository;
        this.userRepository      = userRepository;
    }

    public List<MarketLot> browseLots(String commodity) {
        if (commodity != null && !commodity.isBlank()) {
            return lotRepository.findByCommodityIdAndStatus(
                    commodity, MarketLot.LotStatus.LIVE);
        }
        return lotRepository.findByStatus(MarketLot.LotStatus.LIVE);
    }

    public MarketLot getLot(String lotId) {
        return lotRepository.findByLotId(lotId)
                .orElseThrow(() -> new AppException.NotFoundException(
                        "Lot not found: " + lotId));
    }

    public List<ChainEvent> getLotChain(String lotId) {
        return chainRepository.findByLotIdOrderByStartedAtAsc(lotId);
    }

    @Transactional
    public MarketLot listLot(Long sellerId, String crateIds, String commodityId,
                             String grade, Double kg, BigDecimal pricePerKg,
                             Double minOrderKg) {

        // ── Validation ────────────────────────────────────────────────────────
        if (crateIds == null || crateIds.isBlank()) {
            throw new AppException.BadRequestException("crateIds is required");
        }

        // Validate crate exists
        var crate = crateLotRepository.findByCrateId(crateIds)
                .orElseThrow(() -> new AppException.NotFoundException(
                        "Crate not found: " + crateIds));

        // Validate commodity exists
        var commodity = commodityRepository.findAll().stream()
                .filter(c -> commodityId.equalsIgnoreCase(c.getCommodityId()))
                .findFirst()
                .orElseThrow(() -> new AppException.NotFoundException(
                        "Commodity not found: " + commodityId));

        // Validate grade
        if (grade != null && !VALID_GRADES.contains(grade.toUpperCase())) {
            throw new AppException.BadRequestException(
                    "Invalid grade. Must be: A · B · C");
        }

        // Validate kg
        if (kg == null || kg <= 0) {
            throw new AppException.BadRequestException(
                    "kg must be greater than zero");
        }

        // Validate pricePerKg
        if (pricePerKg == null || pricePerKg.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException.BadRequestException(
                    "pricePerKg must be greater than zero");
        }

        // Validate minOrderKg
        if (minOrderKg != null && minOrderKg <= 0) {
            throw new AppException.BadRequestException(
                    "minOrderKg must be greater than zero");
        }

        // Check for duplicate active listing
        boolean duplicate = lotRepository.findAll().stream()
                .anyMatch(l -> crateIds.equals(l.getCrateIds())
                        && l.getStatus() == MarketLot.LotStatus.LIVE);
        if (duplicate) {
            throw new AppException.ConflictException(
                    "An active listing already exists for crate: " + crateIds);
        }

        // ── Resolve enrichment fields ─────────────────────────────────────────

        // Seller name
        var seller = userRepository.findById(sellerId).orElse(null);
        String sellerName = seller != null ? seller.getFullName() : "Unknown";

        // Commodity name + freshness
        String  commodityName = commodity.getName();
        Integer freshnessDays = commodity.getShelfLifeDays();

        // Hub from crate
        Long   hubId   = crate.getHubId();
        String hubName = null;
        if (hubId != null) {
            var hub = hubRepository.findById(hubId).orElse(null);
            hubName = hub != null ? hub.getName() : null;
        }

        // ── Create lot ────────────────────────────────────────────────────────
        MarketLot lot = new MarketLot();
        lot.setLotId("LOT-" + System.currentTimeMillis());
        lot.setSellerId(sellerId);
        lot.setSellerName(sellerName);
        lot.setCrateIds(crateIds);
        lot.setCommodityId(commodityId);
        lot.setCommodityName(commodityName);
        lot.setGrade(grade != null ? grade.toUpperCase() : null);
        lot.setKgAvailable(kg);
        lot.setPricePerKg(pricePerKg);
        lot.setMinOrderKg(minOrderKg != null ? minOrderKg : 1.0);
        lot.setHubId(hubId);
        lot.setHubName(hubName);
        lot.setFreshnessDays(freshnessDays);
        lot.setTraceabilityScore(80);
        lot.setStatus(MarketLot.LotStatus.LIVE);

        return lotRepository.save(lot);
    }

    @Transactional
    public MarketOrder placeOrder(Long buyerId, List<OrderItem> items,
                                  String fulfilmentType, String destAddress,
                                  String paymentPreference) {

        // ── Validation ────────────────────────────────────────────────────────
        if (items == null || items.isEmpty()) {
            throw new AppException.BadRequestException(
                    "Order must contain at least one item.");
        }

        if (fulfilmentType == null || fulfilmentType.isBlank()) {
            throw new AppException.BadRequestException(
                    "fulfilmentType is required. Must be: DELIVERY · COLLECTION");
        }
        if (!VALID_FULFILMENT.contains(fulfilmentType.toUpperCase())) {
            throw new AppException.BadRequestException(
                    "Invalid fulfilmentType. Must be: DELIVERY · COLLECTION");
        }

        if (paymentPreference == null || paymentPreference.isBlank()) {
            throw new AppException.BadRequestException(
                    "paymentPreference is required. Must be: PREPAID · PAY_ON_DELIVERY · CREDIT");
        }
        if (!VALID_PAYMENT_PREF.contains(paymentPreference.toUpperCase())) {
            throw new AppException.BadRequestException(
                    "Invalid paymentPreference. Must be: PREPAID · PAY_ON_DELIVERY · CREDIT");
        }

        if ("DELIVERY".equalsIgnoreCase(fulfilmentType)
                && (destAddress == null || destAddress.isBlank())) {
            throw new AppException.BadRequestException(
                    "destAddress is required for DELIVERY orders");
        }

        if (destAddress != null && destAddress.length() > 500) {
            throw new AppException.BadRequestException(
                    "destAddress cannot exceed 500 characters");
        }

        // ── Process items ─────────────────────────────────────────────────────
        BigDecimal subtotal = BigDecimal.ZERO;

        for (OrderItem item : items) {
            if (item.getKg() == null || item.getKg() <= 0) {
                throw new AppException.BadRequestException(
                        "Item kg must be greater than zero");
            }

            MarketLot lot = lotRepository.findByLotId(item.getLotId())
                    .orElseThrow(() -> new AppException.NotFoundException(
                            "Lot not found: " + item.getLotId()));

            if (lot.getStatus() != MarketLot.LotStatus.LIVE) {
                throw new AppException.BadRequestException(
                        "Lot is no longer available: " + item.getLotId());
            }

            if (lot.getKgAvailable() < item.getKg()) {
                throw new AppException.BadRequestException(
                        "Insufficient stock for lot: " + item.getLotId()
                                + ". Available: " + lot.getKgAvailable() + "kg");
            }

            if (item.getKg() < lot.getMinOrderKg()) {
                throw new AppException.BadRequestException(
                        "Minimum order is " + lot.getMinOrderKg()
                                + "kg for lot: " + item.getLotId());
            }

            // Server always calculates price — never trust client price
            item.setPricePerKgAtOrder(lot.getPricePerKg());

            subtotal = subtotal.add(
                    lot.getPricePerKg().multiply(BigDecimal.valueOf(item.getKg())));

            lot.setKgAvailable(lot.getKgAvailable() - item.getKg());
            if (lot.getKgAvailable() <= 0) {
                lot.setStatus(MarketLot.LotStatus.SOLD);
            }
            lotRepository.save(lot);
        }

        BigDecimal deliveryFee = "DELIVERY".equalsIgnoreCase(fulfilmentType)
                ? BigDecimal.valueOf(2000)
                : BigDecimal.ZERO;

        MarketOrder order = new MarketOrder();
        order.setBuyerId(buyerId);
        order.setFulfilmentType(fulfilmentType.toUpperCase());
        order.setDestAddress(destAddress);
        order.setPaymentPreference(paymentPreference.toUpperCase());
        order.setSubtotal(subtotal);
        order.setDeliveryFee(deliveryFee);
        order.setTotal(subtotal.add(deliveryFee));
        order.setStatus(MarketOrder.OrderStatus.PENDING);
        order = orderRepository.save(order);

        for (OrderItem item : items) {
            item.setOrderId(order.getId());
            itemRepository.save(item);
        }

        return order;
    }

    public List<MarketOrder> getBuyerOrders(Long buyerId) {
        return orderRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId);
    }

    public MarketOrder getOrder(String orderId) {
        return orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new AppException.NotFoundException(
                        "Order not found: " + orderId));
    }

    public List<OrderItem> getOrderItems(String orderId) {
        MarketOrder order = getOrder(orderId);
        return itemRepository.findByOrderId(order.getId());
    }
}