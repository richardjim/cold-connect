package com.coldconnect.controller;

import com.coldconnect.entity.User;
import com.coldconnect.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import com.coldconnect.exception.AppException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/me")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Profile", description = "Customer profile and preferences")
public class ProfileController extends BaseController {

    public ProfileController(UserRepository userRepository) {
        super(userRepository);
    }

    @Operation(summary = "Get my profile")
    @GetMapping
    public ResponseEntity<User> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(resolveUser(userDetails));
    }

    @Operation(summary = "Update profile, language, consents")
    @PatchMapping
    public ResponseEntity<User> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> updates) {

        User user = resolveUser(userDetails);

        if (updates.containsKey("fullName")) {
            String name = updates.get("fullName");
            if (name == null || name.isBlank() || name.length() < 2) {
                throw new AppException.BadRequestException("Full name must be at least 2 characters");
            }
            if (!name.matches("^[a-zA-Z\\s'-]+$")) {
                throw new AppException.BadRequestException("Name must contain letters only");
            }
            user.setFullName(name);
        }

        if (updates.containsKey("language")) {
            String lang = updates.get("language");
            if (!lang.matches("^(en|ha|yo|ig|pcm)$")) {
                throw new AppException.BadRequestException("Language must be one of: en, ha, yo, ig, pcm");
            }
            user.setLanguage(lang);
        }

        if (updates.containsKey("consentStatus")) {
            String consent = updates.get("consentStatus");
            if (!consent.matches("^(accepted|declined)$")) {
                throw new AppException.BadRequestException("Consent status must be: accepted or declined");
            }
            user.setConsentStatus(consent);
        }

        if (updates.containsKey("preferredHubId")) {
            user.setPreferredHubId(updates.get("preferredHubId"));
        }

        return ResponseEntity.ok(userRepository.save(user));
    }
}
