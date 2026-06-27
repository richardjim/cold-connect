package com.coldconnect.controller;

import com.coldconnect.entity.Commodity;
import com.coldconnect.repository.UserRepository;
import com.coldconnect.service.CommodityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/commodities")
@Tag(name = "Commodities", description = "Commodity catalogue per region")
public class CommodityController extends BaseController {

    private final CommodityService commodityService;

    public CommodityController(UserRepository userRepository, CommodityService commodityService) {
        super(userRepository);
        this.commodityService = commodityService;
    }

    @Operation(summary = "Get commodity catalogue")
    @GetMapping
    public ResponseEntity<List<Commodity>> getCommodities(@RequestParam(required = false) String region) {
        return ResponseEntity.ok(commodityService.getCommodities(region));
    }
}
