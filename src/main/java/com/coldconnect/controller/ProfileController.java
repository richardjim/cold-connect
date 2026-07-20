package com.coldconnect.controller;

import com.coldconnect.entity.User;
import com.coldconnect.exception.AppException;
import com.coldconnect.i18n.AppMessages;
import com.coldconnect.repository.UserRepository;
import com.coldconnect.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/profile")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Profile", description = "Customer profile and preferences")
public class ProfileController extends BaseController {

    private final ProfileService profileService;
    private final AppMessages    messages;

    public ProfileController(UserRepository userRepository,
                             ProfileService profileService,
                             AppMessages messages) {
        super(userRepository);
        this.profileService = profileService;
        this.messages       = messages;
    }

    @Schema(description = "Update profile request — all fields optional")
    public record UpdateProfileRequest(

            @Schema(example = "John Farmer", description = "Letters only, 2-100 characters")
            @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
            @Pattern(regexp = "^[a-zA-Z\\s'-]+$", message = "Name must contain letters only")
            String fullName,

            @Schema(example = "en", description = "Supported: en, ha, yo, ig, pcm")
            @Pattern(regexp = "^(en|ha|yo|ig|pcm)$",
                    message = "Language must be one of: en, ha, yo, ig, pcm")
            String language,

            @Schema(example = "accepted", description = "accepted or declined")
            @Pattern(regexp = "^(accepted|declined)$",
                    message = "Consent must be: accepted or declined")
            String consentStatus,

            @Schema(example = "HUB-JOS-01", description = "Preferred cold hub ID")
            String preferredHubId
    ) {}

    @Schema(description = "Profile response — sensitive fields excluded")
    public record ProfileResponse(
            Long   id,
            String phone,
            String email,
            String fullName,
            String role,
            String language,
            String consentStatus,
            String preferredHubId,
            String kycLevel
    ) {}

    @Operation(summary = "Get my profile")
    @GetMapping
    public ResponseEntity<ProfileResponse> getProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = resolveUser(userDetails);
        return ResponseEntity.ok(toResponse(user));
    }

    @Operation(
            summary = "Update my profile",
            description = "All fields optional. Only provided fields are updated."
    )
    @PatchMapping
    public ResponseEntity<Map<String, Object>> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest req) {
        String lang   = resolveLanguage(userDetails);
        User   user   = resolveUser(userDetails);
        User updated  = profileService.updateProfile(
                user.getId(), req.fullName(), req.language(),
                req.consentStatus(), req.preferredHubId());
        return ResponseEntity.ok(Map.of(
                "message", messages.get(AppMessages.Key.PROFILE_UPDATED, lang),
                "profile", toResponse(updated)
        ));
    }

    private ProfileResponse toResponse(User user) {
        return new ProfileResponse(
                user.getId(),
                user.getPhone(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().name(),
                user.getLanguage(),
                user.getConsentStatus(),
                user.getPreferredHubId(),
                user.getKycLevel()
        );
    }
}