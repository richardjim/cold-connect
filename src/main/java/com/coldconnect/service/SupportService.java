package com.coldconnect.service;

import com.coldconnect.entity.SupportCase;
import com.coldconnect.exception.AppException;
import com.coldconnect.repository.SupportCaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SupportService {

    private final SupportCaseRepository caseRepository;

    public SupportService(SupportCaseRepository caseRepository) {
        this.caseRepository = caseRepository;
    }

    @Transactional
    public SupportCase createCase(Long requesterId, Long bookingId, String crateId,
                                   Long tripId, String type, String severity, String message) {
        SupportCase c = new SupportCase();
        c.setRequesterId(requesterId);
        c.setBookingId(bookingId);
        c.setCrateId(crateId);
        c.setTripId(tripId);
        c.setType(type);
        c.setSeverity(severity);
        c.setSla(resolveSla(severity));
        c.setStatus("OPEN");
        return caseRepository.save(c);
    }

    public List<SupportCase> getCustomerCases(Long requesterId) {
        return caseRepository.findByRequesterIdOrderByCreatedAtDesc(requesterId);
    }

    public SupportCase getCase(Long caseId) {
        return caseRepository.findById(caseId)
                .orElseThrow(() -> new AppException.NotFoundException("Case not found"));
    }

    private String resolveSla(String severity) {
        if ("CRITICAL".equals(severity)) return "2h";
        if ("HIGH".equals(severity)) return "4h";
        if ("MEDIUM".equals(severity)) return "24h";
        return "72h";
    }
}
