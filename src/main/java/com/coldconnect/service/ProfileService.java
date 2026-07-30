package com.coldconnect.service;

import com.coldconnect.entity.User;
import com.coldconnect.exception.AppException;
import com.coldconnect.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

    private final UserRepository userRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public ProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User updateProfile(Long userId, String fullName, String language,
                              String consentStatus, String preferredHubId,
                              String location, String persona,
                              Long customerTypeId, String organizationId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException.NotFoundException("User not found"));

        if (fullName != null && !fullName.isBlank()) {
            user.setFullName(fullName);
        }

        if (language != null) {
            user.setLanguage(language);
        }

        if (consentStatus != null) {
            user.setConsentStatus(consentStatus);
        }

        if (preferredHubId != null) {
            Long hubCount = (Long) entityManager
                    .createQuery("SELECT COUNT(h) FROM Hub h WHERE h.hubId = :hubId")
                    .setParameter("hubId", preferredHubId)
                    .getSingleResult();
            if (hubCount == 0) {
                throw new AppException.NotFoundException(
                        "Hub not found: " + preferredHubId);
            }
            user.setPreferredHubId(preferredHubId);
        }

        if (location != null && !location.isBlank()) {
            user.setLocation(location);
        }

        if (persona != null && !persona.isBlank()) {
            user.setPersona(persona);
        }

        if (customerTypeId != null) {
            user.setCustomerTypeId(customerTypeId);
        }

        if (organizationId != null && !organizationId.isBlank()) {
            user.setOrganizationId(organizationId);
        }

        return userRepository.save(user);
    }

    public User getProfile(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException.NotFoundException("User not found"));
    }
}