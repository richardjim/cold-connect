package com.coldconnect.repository;

import com.coldconnect.entity.SafetyCheckItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SafetyCheckItemRepository extends JpaRepository<SafetyCheckItem, Long> {
    List<SafetyCheckItem> findByCheckId(Long checkId);
    List<SafetyCheckItem> findByCheckIdAndMark(Long checkId, String mark);
}