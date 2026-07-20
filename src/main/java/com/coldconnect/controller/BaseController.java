package com.coldconnect.controller;

import com.coldconnect.entity.User;
import com.coldconnect.exception.AppException;
import com.coldconnect.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;

public abstract class BaseController {

    protected final UserRepository userRepository;

    protected BaseController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    protected User resolveUser(UserDetails userDetails) {
        String username = userDetails.getUsername();
        return userRepository.findByPhone(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new AppException.NotFoundException("User not found"));
    }

    protected String resolveLanguage(UserDetails userDetails) {
        try {
            String lang = resolveUser(userDetails).getLanguage();
            return (lang != null && !lang.isBlank()) ? lang : "en";
        } catch (Exception e) {
            return "en";
        }
    }
}