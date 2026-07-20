package com.coldconnect.controller;

import com.coldconnect.entity.User;
import com.coldconnect.enums.Role;
import com.coldconnect.exception.AppException;
import com.coldconnect.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Users", description = "User management — Admin only")
public class AdminUserController extends BaseController {

    public AdminUserController(UserRepository userRepository) {
        super(userRepository);
    }

    public record RoleChangeRequest(String role) {}

    @Operation(summary = "List all users — paginated")
    @GetMapping
    public ResponseEntity<Page<User>> getAllUsers(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size,
                Sort.by("id").descending());
        return ResponseEntity.ok(userRepository.findAll(pageable));
    }

    @Operation(summary = "Get user by ID")
    @GetMapping("/{userId}")
    public ResponseEntity<User> getUser(@PathVariable Long userId) {
        return ResponseEntity.ok(
                userRepository.findById(userId)
                        .orElseThrow(() -> new AppException.NotFoundException("User not found"))
        );
    }

    @Operation(summary = "Change user role")
    @PatchMapping("/{userId}/role")
    public ResponseEntity<Map<String, Object>> changeRole(
            @PathVariable Long userId,
            @RequestBody RoleChangeRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException.NotFoundException("User not found"));
        try {
            user.setRole(Role.valueOf(req.role().toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new AppException.BadRequestException(
                    "Invalid role. Must be: CUSTOMER, DRIVER, OPERATOR, ADMIN");
        }
        userRepository.save(user);
        return ResponseEntity.ok(Map.of(
                "message", "Role updated to " + req.role().toUpperCase(),
                "userId",  userId,
                "role",    user.getRole().name()
        ));
    }

    @Operation(summary = "Enable or disable a user account")
    @PatchMapping("/{userId}/status")
    public ResponseEntity<Map<String, Object>> changeStatus(
            @PathVariable Long userId,
            @RequestBody Map<String, Boolean> body) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException.NotFoundException("User not found"));
        boolean enabled = body.getOrDefault("enabled", true);
        user.setEnabled(enabled);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of(
                "message", enabled ? "Account enabled" : "Account disabled",
                "userId",  userId,
                "enabled", enabled
        ));
    }

//    @Operation(summary = "Delete a user account")
//    @DeleteMapping("/{userId}")
//    public ResponseEntity<Map<String, String>> deleteUser(
//            @PathVariable Long userId) {
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new AppException.NotFoundException("User not found"));
//        userRepository.delete(user);
//        return ResponseEntity.ok(Map.of("message", "User " + userId + " deleted"));
//    }
}