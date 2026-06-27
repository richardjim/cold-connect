package com.coldconnect.repository;

import com.coldconnect.entity.SupportCase;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SupportCaseRepository extends JpaRepository<SupportCase, Long> {
    List<SupportCase> findByRequesterIdOrderByCreatedAtDesc(Long requesterId);
}
