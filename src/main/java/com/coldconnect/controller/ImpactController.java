package com.coldconnect.controller;

import com.coldconnect.repository.UserRepository;
import com.coldconnect.service.ImpactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/impact")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Impact", description = "Environmental impact metrics for the user")
public class ImpactController extends BaseController {

    private final ImpactService impactService;

    public ImpactController(UserRepository userRepository,
                            ImpactService impactService) {
        super(userRepository);
        this.impactService = impactService;
    }

    @Operation(
            summary = "Get my environmental impact",
            description = "Returns food saved, CO2 avoided, solar energy used and equivalences"
    )
    @GetMapping
    public ResponseEntity<ImpactService.ImpactResponse> getImpact(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = resolveUser(userDetails).getId();
        return ResponseEntity.ok(impactService.getImpact(userId));
    }

    @Operation(summary = "Recalculate impact from latest bookings")
    @PostMapping("/recalculate")
    public ResponseEntity<ImpactService.ImpactResponse> recalculate(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = resolveUser(userDetails).getId();
        return ResponseEntity.ok(impactService.recalculate(userId));
    }
}