package com.coldconnect.controller;

import com.coldconnect.entity.HubWaitlist;
import com.coldconnect.i18n.AppMessages;
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
    private final AppMessages        messages;

    public HubWaitlistController(UserRepository userRepository,
                                 HubWaitlistService waitlistService,
                                 AppMessages messages) {
        super(userRepository);
        this.waitlistService = waitlistService;
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
        HubWaitlist entry = waitlistService.joinWaitlist(
                userId, hubId, req.commodityId(), req.quantityKg(), lang);
        return ResponseEntity.ok(Map.of(
                "message", messages.get(AppMessages.Key.WAITLIST_JOINED, lang),
                "entry",   entry
        ));
    }

    @Operation(summary = "Get my waitlist status for a hub")
    @GetMapping("/{hubId}/waitlist")
    public ResponseEntity<Map<String, Object>> getWaitlistStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long hubId) {
        Long userId = resolveUser(userDetails).getId();
        var  entry  = waitlistService.getWaitlistStatus(userId, hubId);
        int  count  = waitlistService.getWaitlistCount(hubId);
        return ResponseEntity.ok(Map.of(
                "onWaitlist",   entry.isPresent(),
                "totalWaiting", count,
                "entry",        entry.orElse(null)
        ));
    }

    @Operation(summary = "Get all my waitlist entries")
    @GetMapping("/waitlist/mine")
    public ResponseEntity<List<HubWaitlist>> getMyWaitlists(
            @AuthenticationPrincipal UserDetails userDetails) {
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