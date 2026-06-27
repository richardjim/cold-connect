package com.coldconnect.controller;

import com.coldconnect.entity.User;
import com.coldconnect.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<User> updateProfile(@AuthenticationPrincipal UserDetails userDetails,
                                               @RequestBody Map<String, String> updates) {
        User user = resolveUser(userDetails);
        if (updates.containsKey("fullName")) user.setFullName(updates.get("fullName"));
        if (updates.containsKey("language")) user.setLanguage(updates.get("language"));
        if (updates.containsKey("consentStatus")) user.setConsentStatus(updates.get("consentStatus"));
        if (updates.containsKey("preferredHubId")) user.setPreferredHubId(updates.get("preferredHubId"));
        return ResponseEntity.ok(userRepository.save(user));
    }
}
