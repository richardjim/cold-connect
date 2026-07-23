package com.coldconnect.controller;

import com.coldconnect.entity.CustomerType;
import com.coldconnect.exception.AppException;
import com.coldconnect.repository.CustomerTypeRepository;
import com.coldconnect.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/customer-types")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Customer Types", description = "Customer type management — Admin only")
public class AdminCustomerTypeController extends BaseController {

    private final CustomerTypeRepository customerTypeRepository;

    public AdminCustomerTypeController(UserRepository userRepository,
                                       CustomerTypeRepository customerTypeRepository) {
        super(userRepository);
        this.customerTypeRepository = customerTypeRepository;
    }

    public record CustomerTypeRequest(
            @NotBlank String name,
            String description
    ) {}

    @Operation(summary = "Get all customer types")
    @GetMapping
    public ResponseEntity<List<CustomerType>> getAll() {
        return ResponseEntity.ok(customerTypeRepository.findByActiveTrue());
    }

    @Operation(summary = "Create a customer type")
    @PostMapping
    public ResponseEntity<CustomerType> create(
            @Valid @RequestBody CustomerTypeRequest req) {

        customerTypeRepository.findByName(req.name().toUpperCase())
                .ifPresent(t -> { throw new AppException.ConflictException(
                        "Customer type already exists: " + req.name()); });

        CustomerType type = new CustomerType();
        type.setName(req.name().toUpperCase());
        type.setDescription(req.description());
        return ResponseEntity.ok(customerTypeRepository.save(type));
    }

    @Operation(summary = "Update a customer type")
    @PatchMapping("/{id}")
    public ResponseEntity<CustomerType> update(
            @PathVariable Long id,
            @RequestBody CustomerTypeRequest req) {

        CustomerType type = customerTypeRepository.findById(id)
                .orElseThrow(() -> new AppException.NotFoundException(
                        "Customer type not found"));

        if (req.name() != null) type.setName(req.name().toUpperCase());
        if (req.description() != null) type.setDescription(req.description());
        return ResponseEntity.ok(customerTypeRepository.save(type));
    }

    @Operation(summary = "Disable a customer type")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> disable(@PathVariable Long id) {
        CustomerType type = customerTypeRepository.findById(id)
                .orElseThrow(() -> new AppException.NotFoundException(
                        "Customer type not found"));
        type.setActive(false);
        customerTypeRepository.save(type);
        return ResponseEntity.ok(Map.of("message", "Customer type disabled"));
    }
}