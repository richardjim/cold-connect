package com.coldconnect.service;

import com.coldconnect.entity.HubWaitlist;
import com.coldconnect.exception.AppException;
import com.coldconnect.i18n.AppMessages;
import com.coldconnect.repository.HubRepository;
import com.coldconnect.repository.HubWaitlistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class HubWaitlistService {

    private final HubWaitlistRepository waitlistRepository;
    private final HubRepository         hubRepository;
    private final AppMessages           messages;

    public HubWaitlistService(HubWaitlistRepository waitlistRepository,
                              HubRepository hubRepository,
                              AppMessages messages) {
        this.waitlistRepository = waitlistRepository;
        this.hubRepository      = hubRepository;
        this.messages           = messages;
    }

    @Transactional
    public HubWaitlist joinWaitlist(Long userId, Long hubId,
                                    String commodityId, Double quantityKg,
                                    String language) {
        hubRepository.findById(hubId)
                .orElseThrow(() -> new AppException.NotFoundException(
                        messages.get(AppMessages.Key.HUB_NOT_FOUND, language)));

        Optional<HubWaitlist> existing =
                waitlistRepository.findByUserIdAndHubIdAndStatus(userId, hubId, "WAITING");

        if (existing.isPresent()) {
            throw new AppException.ConflictException(
                    messages.get(AppMessages.Key.WAITLIST_ALREADY_ON, language));
        }

        HubWaitlist entry = new HubWaitlist();
        entry.setUserId(userId);
        entry.setHubId(hubId);
        entry.setCommodityId(commodityId);
        entry.setQuantityKg(quantityKg);
        entry.setStatus("WAITING");
        return waitlistRepository.save(entry);
    }

    public List<HubWaitlist> getMyWaitlists(Long userId) {
        return waitlistRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Optional<HubWaitlist> getWaitlistStatus(Long userId, Long hubId) {
        return waitlistRepository.findByUserIdAndHubIdAndStatus(userId, hubId, "WAITING");
    }

    @Transactional
    public HubWaitlist cancelWaitlist(Long userId, Long waitlistId, String language) {
        HubWaitlist entry = waitlistRepository.findById(waitlistId)
                .orElseThrow(() -> new AppException.NotFoundException(
                        "Waitlist entry not found"));

        if (!entry.getUserId().equals(userId)) {
            throw new AppException.UnauthorizedException("Not your waitlist entry");
        }

        entry.setStatus("CANCELLED");
        return waitlistRepository.save(entry);
    }

    public int getWaitlistCount(Long hubId) {
        return waitlistRepository.findByHubIdAndStatus(hubId, "WAITING").size();
    }
}