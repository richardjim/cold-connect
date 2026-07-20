package com.coldconnect.service;

import com.coldconnect.entity.CartItem;
import com.coldconnect.entity.MarketLot;
import com.coldconnect.exception.AppException;
import com.coldconnect.i18n.AppMessages;
import com.coldconnect.repository.CartItemRepository;
import com.coldconnect.repository.MarketLotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CartService {

    private final CartItemRepository  cartItemRepository;
    private final MarketLotRepository marketLotRepository;
    private final AppMessages         messages;

    public CartService(CartItemRepository cartItemRepository,
                       MarketLotRepository marketLotRepository,
                       AppMessages messages) {
        this.cartItemRepository  = cartItemRepository;
        this.marketLotRepository = marketLotRepository;
        this.messages            = messages;
    }

    public static class CartSummary {
        public final List<CartItem> items;
        public final int            totalLots;
        public final Double         totalKg;
        public final BigDecimal     totalAmount;

        public CartSummary(List<CartItem> items) {
            this.items       = items;
            this.totalLots   = items.size();
            this.totalKg     = items.stream().mapToDouble(CartItem::getKg).sum();
            this.totalAmount = items.stream()
                    .map(i -> i.getPricePerKg().multiply(BigDecimal.valueOf(i.getKg())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }

    public CartSummary getCart(Long userId) {
        return new CartSummary(cartItemRepository.findByUserId(userId));
    }

    @Transactional
    public CartItem addItem(Long userId, String lotId, Double kg, String language) {
        MarketLot lot = marketLotRepository.findByLotId(lotId)
                .orElseThrow(() -> new AppException.NotFoundException(
                        messages.get(AppMessages.Key.LOT_NOT_AVAILABLE, language)));

        if (lot.getStatus() != MarketLot.LotStatus.LIVE) {
            throw new AppException.BadRequestException(
                    messages.get(AppMessages.Key.LOT_NOT_AVAILABLE, language));
        }

        if (kg < lot.getMinOrderKg()) {
            throw new AppException.BadRequestException(
                    "Minimum order is " + lot.getMinOrderKg() + "kg");
        }

        if (kg > lot.getKgAvailable()) {
            throw new AppException.BadRequestException(
                    "Only " + lot.getKgAvailable() + "kg available");
        }

        CartItem item = cartItemRepository
                .findByUserIdAndLotId(userId, lotId)
                .orElse(new CartItem());

        item.setUserId(userId);
        item.setLotId(lotId);
        item.setCommodityName(lot.getCommodityId());
        item.setGrade(lot.getGrade());
        item.setKg(kg);
        item.setPricePerKg(lot.getPricePerKg());
        item.setMinOrderKg(lot.getMinOrderKg());
        return cartItemRepository.save(item);
    }

    @Transactional
    public CartItem updateItem(Long userId, Long itemId, Double kg, String language) {
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new AppException.NotFoundException("Cart item not found"));

        if (!item.getUserId().equals(userId)) {
            throw new AppException.UnauthorizedException("Not your cart item");
        }

        MarketLot lot = marketLotRepository.findByLotId(item.getLotId())
                .orElseThrow(() -> new AppException.NotFoundException(
                        messages.get(AppMessages.Key.LOT_NOT_AVAILABLE, language)));

        if (kg < lot.getMinOrderKg()) {
            throw new AppException.BadRequestException(
                    "Minimum order is " + lot.getMinOrderKg() + "kg");
        }

        if (kg > lot.getKgAvailable()) {
            throw new AppException.BadRequestException(
                    "Only " + lot.getKgAvailable() + "kg available");
        }

        item.setKg(kg);
        return cartItemRepository.save(item);
    }

    @Transactional
    public void removeItem(Long userId, Long itemId) {
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new AppException.NotFoundException("Cart item not found"));

        if (!item.getUserId().equals(userId)) {
            throw new AppException.UnauthorizedException("Not your cart item");
        }

        cartItemRepository.delete(item);
    }

    @Transactional
    public void clearCart(Long userId) {
        cartItemRepository.deleteByUserId(userId);
    }
}