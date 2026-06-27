package com.coldconnect.service;

import com.coldconnect.entity.*;
import com.coldconnect.exception.AppException;
import com.coldconnect.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class MarketplaceService {

    private final MarketLotRepository lotRepository;
    private final MarketOrderRepository orderRepository;
    private final OrderItemRepository itemRepository;
    private final BuyerProfileRepository buyerRepository;
    private final ChainEventRepository chainRepository;

    public MarketplaceService(MarketLotRepository lotRepository,
                               MarketOrderRepository orderRepository,
                               OrderItemRepository itemRepository,
                               BuyerProfileRepository buyerRepository,
                               ChainEventRepository chainRepository) {
        this.lotRepository = lotRepository;
        this.orderRepository = orderRepository;
        this.itemRepository = itemRepository;
        this.buyerRepository = buyerRepository;
        this.chainRepository = chainRepository;
    }

    public List<MarketLot> browseLots(String commodity) {
        return commodity != null
                ? lotRepository.findByCommodityIdAndStatus(commodity, MarketLot.LotStatus.LIVE)
                : lotRepository.findByStatus(MarketLot.LotStatus.LIVE);
    }

    public MarketLot getLot(String lotId) {
        return lotRepository.findByLotId(lotId)
                .orElseThrow(() -> new AppException.NotFoundException("Lot not found: " + lotId));
    }

    public List<ChainEvent> getLotChain(String lotId) {
        return chainRepository.findByLotIdOrderByStartedAtAsc(lotId);
    }

    @Transactional
    public MarketLot listLot(Long sellerId, String crateIds, String commodityId,
                              String grade, Double kg, BigDecimal pricePerKg, Double minOrderKg) {
        MarketLot lot = new MarketLot();
        lot.setLotId("LOT-" + System.currentTimeMillis());
        lot.setSellerId(sellerId);
        lot.setCrateIds(crateIds);
        lot.setCommodityId(commodityId);
        lot.setGrade(grade);
        lot.setKgAvailable(kg);
        lot.setPricePerKg(pricePerKg);
        lot.setMinOrderKg(minOrderKg != null ? minOrderKg : 1.0);
        lot.setStatus(MarketLot.LotStatus.LIVE);
        lot.setTraceabilityScore(80);
        return lotRepository.save(lot);
    }

    @Transactional
    public MarketOrder placeOrder(Long buyerId, List<OrderItem> items,
                                   String fulfilmentType, String destAddress,
                                   String paymentPreference) {
        BuyerProfile buyer = buyerRepository.findByBuyerId(buyerId)
                .orElseThrow(() -> new AppException.NotFoundException("Buyer profile not found"));

        if ("UNVERIFIED".equals(buyer.getKybStatus())) {
            throw new AppException.UnauthorizedException("Complete KYB verification before placing orders");
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderItem item : items) {
            MarketLot lot = lotRepository.findByLotId(item.getLotId())
                    .orElseThrow(() -> new AppException.NotFoundException("Lot not found: " + item.getLotId()));

            if (lot.getKgAvailable() < item.getKg()) {
                throw new AppException.BadRequestException("Insufficient stock for lot: " + item.getLotId());
            }
            if (item.getKg() < lot.getMinOrderKg()) {
                throw new AppException.BadRequestException(
                        "Minimum order is " + lot.getMinOrderKg() + "kg for lot: " + item.getLotId());
            }

            item.setPricePerKgAtOrder(lot.getPricePerKg());
            subtotal = subtotal.add(lot.getPricePerKg().multiply(BigDecimal.valueOf(item.getKg())));
            lot.setKgAvailable(lot.getKgAvailable() - item.getKg());
            lotRepository.save(lot);
        }

        BigDecimal deliveryFee = "DELIVERY".equals(fulfilmentType)
                ? BigDecimal.valueOf(2000) : BigDecimal.ZERO;

        MarketOrder order = new MarketOrder();
        order.setBuyerId(buyerId);
        order.setFulfilmentType(fulfilmentType);
        order.setDestAddress(destAddress);
        order.setStatus(MarketOrder.OrderStatus.PENDING);
        order.setSubtotal(subtotal);
        order.setDeliveryFee(deliveryFee);
        order.setTotal(subtotal.add(deliveryFee));
        order = orderRepository.save(order);

        final Long orderId = order.getId();
        for (OrderItem item : items) {
            item.setOrderId(orderId);
            itemRepository.save(item);
        }
        return order;
    }

    public List<MarketOrder> getBuyerOrders(Long buyerId) {
        return orderRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId);
    }

    public MarketOrder getOrder(String orderId) {
        return orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new AppException.NotFoundException("Order not found: " + orderId));
    }

    public List<OrderItem> getOrderItems(String orderId) {
        MarketOrder order = getOrder(orderId);
        return itemRepository.findByOrderId(order.getId());
    }
}
