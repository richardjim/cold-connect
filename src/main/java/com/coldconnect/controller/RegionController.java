package com.coldconnect.controller;

import com.coldconnect.entity.TenantRegion;
import com.coldconnect.repository.UserRepository;
import com.coldconnect.service.RegionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/regions")
@Tag(name = "Regions", description = "Region config and feature flags")
public class RegionController extends BaseController {

    private final RegionService regionService;

    public RegionController(UserRepository userRepository, RegionService regionService) {
        super(userRepository);
        this.regionService = regionService;
    }

    @Operation(summary = "Get region config including feature flags")
    @GetMapping("/{regionId}/config")
    public ResponseEntity<TenantRegion> getConfig(@PathVariable String regionId) {
        return ResponseEntity.ok(regionService.getRegion(regionId));
    }
}
