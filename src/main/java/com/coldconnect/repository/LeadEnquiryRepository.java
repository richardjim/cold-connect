package com.coldconnect.repository;

import com.coldconnect.entity.LeadEnquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LeadEnquiryRepository extends JpaRepository<LeadEnquiry, Long> {
    List<LeadEnquiry> findBySourceTypeOrderByCreatedAtDesc(String sourceType);
    List<LeadEnquiry> findByStatusOrderByCreatedAtDesc(String status);
    List<LeadEnquiry> findByPersonaOrderByCreatedAtDesc(String persona);
}