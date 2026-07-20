package com.coldconnect.controller;

import com.coldconnect.entity.AppEvent;
import com.coldconnect.repository.UserRepository;
import com.coldconnect.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/events")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Events", description = "Analytics and evidence event logging")
public class EventController extends BaseController {

    private final EventService eventService;

    public EventController(UserRepository userRepository,
                           EventService eventService) {
        super(userRepository);
        this.eventService = eventService;
    }

    public record EventRequest(
            @NotBlank String eventName,
            String screenId,
            String entityType,
            String entityId,
            String region,
            String networkState,
            String deviceInfo,
            String featureFlagState
    ) {}

    @Operation(
            summary = "Log an analytics or evidence event",
            description = """
            Use for: booking_created, lot_viewed, screen_viewed,
            order_placed, chain_report_shared, safety_check_completed etc.
            Every create/update action should fire an event for audit trail.
            """
    )
    @PostMapping
    public ResponseEntity<Map<String, Object>> logEvent(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody EventRequest req) {

        var user = resolveUser(userDetails);

        AppEvent event = eventService.logEvent(
                user.getId(),
                req.eventName(),
                req.screenId(),
                req.entityType(),
                req.entityId(),
                req.region(),
                user.getRole().name(),
                req.networkState(),
                req.deviceInfo(),
                req.featureFlagState()
        );

        return ResponseEntity.ok(Map.of(
                "eventId",    event.getId(),
                "acceptedAt", event.getOccurredAt()
        ));
    }
}