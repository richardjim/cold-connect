package com.coldconnect.repository;

import com.coldconnect.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByUserId(Long userId);
    Optional<CartItem> findByUserIdAndLotId(Long userId, String lotId);
    void deleteByUserId(Long userId);
    void deleteByIdAndUserId(Long id, Long userId);
}