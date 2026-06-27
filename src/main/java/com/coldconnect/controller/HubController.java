package com.coldconnect.controller;

import com.coldconnect.entity.Hub;
import com.coldconnect.repository.UserRepository;
import com.coldconnect.service.HubService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/hubs")
@Tag(name = "Hubs", description = "Hub search and capacity")
public class HubController extends BaseController {

    private final HubService hubService;

    public HubController(UserRepository userRepository, HubService hubService) {
        super(userRepository);
        this.hubService = hubService;
    }

    @Operation(summary = "Search hubs by region")
    @GetMapping
    public ResponseEntity<List<Hub>> searchHubs(@RequestParam(required = false) String region) {
        return ResponseEntity.ok(hubService.searchHubs(region));
    }

    @Operation(summary = "Get hub capacity snapshot")
    @GetMapping("/{hubId}/capacity")
    public ResponseEntity<HubService.CapacitySnapshot> getCapacity(@PathVariable Long hubId) {
        return ResponseEntity.ok(hubService.getCapacity(hubId));
    }
}
