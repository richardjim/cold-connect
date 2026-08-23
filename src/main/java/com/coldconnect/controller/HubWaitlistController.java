package com.coldconnect.controller;

import com.coldconnect.entity.HubWaitlist;
import com.coldconnect.exception.AppException;
import com.coldconnect.i18n.AppMessages;
import com.coldconnect.repository.HubRepository;
import com.coldconnect.repository.UserRepository;
import com.coldconnect.service.HubWaitlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/hubs")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Hub Waitlist", description = "Join waitlist for full hubs")
public class HubWaitlistController extends BaseController {

    private final HubWaitlistService waitlistService;
    private final HubRepository      hubRepository;
    private final AppMessages        messages;

    public HubWaitlistController(UserRepository userRepository,
                                 HubWaitlistService waitlistService,
                                 HubRepository hubRepository,
                                 AppMessages messages) {
        super(userRepository);
        this.waitlistService = waitlistService;
        this.hubRepository   = hubRepository;
        this.messages        = messages;
    }

    public record WaitlistRequest(
            String commodityId,
            @NotNull @Positive Double quantityKg
    ) {}

    @Operation(summary = "Join waitlist for a full hub")
    @PostMapping("/{hubId}/waitlist")
    public ResponseEntity<Map<String, Object>> joinWaitlist(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long hubId,
            @Valid @RequestBody WaitlistRequest req) {

        String lang   = resolveLanguage(userDetails);
        Long   userId = resolveUser(userDetails).getId();

        // Validate hub exists — fixes #479 #480
        hubRepository.findById(hubId)
                .orElseThrow(() -> new AppException.NotFoundException(
                        "Hub not found: " + hubId));

        HubWaitlist entry = waitlistService.joinWaitlist(
                userId, hubId, req.commodityId(), req.quantityKg(), lang);

        return ResponseEntity.ok(Map.of(
                "message", messages.get(AppMessages.Key.WAITLIST_JOINED, lang),
                "entry",   entry
        ));
    }

    @Operation(
            summary = "Get my waitlist status for a hub",
            description = "Returns onWaitlist=false if user is not on the waitlist. Returns 404 if hub does not exist."
    )
    @GetMapping("/{hubId}/waitlist")
    public ResponseEntity<Map<String, Object>> getWaitlistStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long hubId) {

        Long userId = resolveUser(userDetails).getId();

        // Validate hub exists — fixes #493
        hubRepository.findById(hubId)
                .orElseThrow(() -> new AppException.NotFoundException(
                        "Hub not found: " + hubId));

        // Null-safe entry lookup — fixes #492
        var entryOpt = waitlistService.getWaitlistStatus(userId, hubId);
        int count    = waitlistService.getWaitlistCount(hubId);

        if (entryOpt.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "onWaitlist",   false,
                    "hubId",        hubId,
                    "totalWaiting", count,
                    "message",      "You are not on the waitlist for this hub"
            ));
        }

        return ResponseEntity.ok(Map.of(
                "onWaitlist",   true,
                "hubId",        hubId,
                "totalWaiting", count,
                "entry",        entryOpt.get()
        ));
    }

    @Operation(summary = "Get all my waitlist entries")
    @GetMapping("/waitlist/mine")
    public ResponseEntity<List<HubWaitlist>> getMyWaitlists(
            @AuthenticationPrincipal UserDetails userDetails) {
        // Requires auth — fixed by SecurityConfig removing /v1/hubs/** from PUBLIC_PATHS
        Long userId = resolveUser(userDetails).getId();
        return ResponseEntity.ok(waitlistService.getMyWaitlists(userId));
    }

    @Operation(summary = "Cancel a waitlist entry")
    @DeleteMapping("/waitlist/{waitlistId}")
    public ResponseEntity<Map<String, Object>> cancelWaitlist(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long waitlistId) {

        String lang   = resolveLanguage(userDetails);
        Long   userId = resolveUser(userDetails).getId();

        HubWaitlist entry = waitlistService.cancelWaitlist(userId, waitlistId, lang);
        return ResponseEntity.ok(Map.of(
                "message", messages.get(AppMessages.Key.WAITLIST_CANCELLED, lang),
                "entry",   entry
        ));
    }
}