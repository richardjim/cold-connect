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
                              String consentStatus, String preferredHubId) {

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
            // Validate hub exists using EntityManager
            Long hubCount = (Long) entityManager
                    .createQuery("SELECT COUNT(h) FROM Hub h WHERE h.hubId = :hubId")
                    .setParameter("hubId", preferredHubId)
                    .getSingleResult();

            if (hubCount == 0) {
                throw new AppException.NotFoundException("Hub not found: " + preferredHubId);
            }

            user.setPreferredHubId(preferredHubId);
        }

        return userRepository.save(user);
    }

    public User getProfile(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException.NotFoundException("User not found"));
    }
}