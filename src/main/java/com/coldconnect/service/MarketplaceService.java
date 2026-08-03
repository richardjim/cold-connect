package com.coldconnect.service;

import com.coldconnect.entity.ChainEvent;
import com.coldconnect.entity.MarketLot;
import com.coldconnect.entity.MarketOrder;
import com.coldconnect.entity.OrderItem;
import com.coldconnect.exception.AppException;
import com.coldconnect.repository.ChainEventRepository;
import com.coldconnect.repository.MarketLotRepository;
import com.coldconnect.repository.MarketOrderRepository;
import com.coldconnect.repository.OrderItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class MarketplaceService {

    private final MarketLotRepository lotRepository;
    private final MarketOrderRepository orderRepository;
    private final OrderItemRepository itemRepository;
    private final ChainEventRepository chainRepository;

    public MarketplaceService(
            MarketLotRepository lotRepository,
            MarketOrderRepository orderRepository,
            OrderItemRepository itemRepository,
            ChainEventRepository chainRepository) {

        this.lotRepository = lotRepository;
        this.orderRepository = orderRepository;
        this.itemRepository = itemRepository;
        this.chainRepository = chainRepository;
    }

    public List<MarketLot> browseLots(String commodity) {
        if (commodity != null && !commodity.isBlank()) {
            return lotRepository.findByCommodityIdAndStatus(
                    commodity,
                    MarketLot.LotStatus.LIVE
            );
        }

        return lotRepository.findByStatus(MarketLot.LotStatus.LIVE);
    }

    public MarketLot getLot(String lotId) {
        return lotRepository.findByLotId(lotId)
                .orElseThrow(() ->
                        new AppException.NotFoundException(
                                "Lot not found: " + lotId));
    }

    public List<ChainEvent> getLotChain(String lotId) {
        return chainRepository.findByLotIdOrderByStartedAtAsc(lotId);
    }

    @Transactional
    public MarketLot listLot(
            Long sellerId,
            String crateIds,
            String commodityId,
            String grade,
            Double kg,
            BigDecimal pricePerKg,
            Double minOrderKg) {

        MarketLot lot = new MarketLot();

        lot.setLotId("LOT-" + System.currentTimeMillis());
        lot.setSellerId(sellerId);
        lot.setCrateIds(crateIds);
        lot.setCommodityId(commodityId);
        lot.setGrade(grade);
        lot.setKgAvailable(kg);
        lot.setPricePerKg(pricePerKg);
        lot.setMinOrderKg(minOrderKg == null ? 1D : minOrderKg);
        lot.setTraceabilityScore(80);
        lot.setStatus(MarketLot.LotStatus.LIVE);

        return lotRepository.save(lot);
    }

    @Transactional
    public MarketOrder placeOrder(
            Long buyerId,
            List<OrderItem> items,
            String fulfilmentType,
            String destAddress,
            String paymentPreference) {

        if (items == null || items.isEmpty()) {
            throw new AppException.BadRequestException(
                    "Order must contain at least one item.");
        }

        BigDecimal subtotal = BigDecimal.ZERO;

        for (OrderItem item : items) {

            MarketLot lot = lotRepository.findByLotId(item.getLotId())
                    .orElseThrow(() ->
                            new AppException.NotFoundException(
                                    "Lot not found: " + item.getLotId()));

            if (lot.getStatus() != MarketLot.LotStatus.LIVE) {
                throw new AppException.BadRequestException(
                        "Lot is no longer available.");
            }

            if (lot.getKgAvailable() < item.getKg()) {
                throw new AppException.BadRequestException(
                        "Insufficient stock for lot: " + item.getLotId());
            }

            if (item.getKg() < lot.getMinOrderKg()) {
                throw new AppException.BadRequestException(
                        "Minimum order is "
                                + lot.getMinOrderKg()
                                + "kg for lot: "
                                + item.getLotId());
            }

            item.setPricePerKgAtOrder(lot.getPricePerKg());

            subtotal = subtotal.add(
                    lot.getPricePerKg()
                            .multiply(BigDecimal.valueOf(item.getKg()))
            );

            lot.setKgAvailable(lot.getKgAvailable() - item.getKg());

            if (lot.getKgAvailable() <= 0) {
                lot.setStatus(MarketLot.LotStatus.SOLD);
            }

            lotRepository.save(lot);
        }

        BigDecimal deliveryFee =
                "DELIVERY".equalsIgnoreCase(fulfilmentType)
                        ? BigDecimal.valueOf(2000)
                        : BigDecimal.ZERO;

        MarketOrder order = new MarketOrder();

//        order.setOrderId("ORD-" + System.currentTimeMillis());
        order.setBuyerId(buyerId);
        order.setFulfilmentType(fulfilmentType);
        order.setDestAddress(destAddress);
        order.setPaymentPreference(paymentPreference);
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
                .orElseThrow(() ->
                        new AppException.NotFoundException(
                                "Order not found: " + orderId));
    }

    public List<OrderItem> getOrderItems(String orderId) {
        MarketOrder order = getOrder(orderId);
        return itemRepository.findByOrderId(order.getId());
    }
}