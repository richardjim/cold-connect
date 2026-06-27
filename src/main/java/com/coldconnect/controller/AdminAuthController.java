package com.coldconnect.controller;

import com.coldconnect.entity.User;
import com.coldconnect.ratelimit.RateLimitService;
import com.coldconnect.repository.UserRepository;
import com.coldconnect.service.AuthService;
import com.coldconnect.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Admin Auth", description = "Email/password auth for admin and operator accounts")
public class AdminAuthController extends BaseController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final RateLimitService rateLimitService;

    public AdminAuthController(UserRepository userRepository, AuthService authService,
                                JwtUtil jwtUtil, RateLimitService rateLimitService) {
        super(userRepository);
        this.authService = authService;
        this.jwtUtil = jwtUtil;
        this.rateLimitService = rateLimitService;
    }

    public record RegisterRequest(@NotBlank @Email String email,
                                   @NotBlank @Size(min = 8) String password,
                                   @NotBlank String fullName) {}

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

    public record ForgotRequest(@NotBlank @Email String email) {}

    public record ResetRequest(@NotBlank String token, @NotBlank @Size(min = 8) String newPassword) {}

    @Operation(summary = "Register admin account")
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest req,
                                                         HttpServletRequest http) {
        rateLimitService.checkAuthLimit(getIp(http));
        String msg = authService.register(req.email(), req.password(), req.fullName());
        return ResponseEntity.ok(Map.of("message", msg));
    }

    @Operation(summary = "Login with email and password")
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest req,
                                                      HttpServletRequest http) {
        rateLimitService.checkAuthLimit(getIp(http));
        User user = authService.login(req.email(), req.password());
        String access = jwtUtil.generateAccessToken(user);
        String refresh = jwtUtil.generateRefreshToken(user);
        return ResponseEntity.ok(Map.of(
                "accessToken", access,
                "refreshToken", refresh,
                "tokenType", "Bearer",
                "role", user.getRole().name(),
                "email", user.getEmail()
        ));
    }

    @Operation(summary = "Verify email from link")
    @GetMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(@RequestParam String token) {
        return ResponseEntity.ok(Map.of("message", authService.verifyEmail(token)));
    }

    @Operation(summary = "Refresh access token")
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh(@Valid @RequestBody RefreshRequest req,
                                                        HttpServletRequest http) {
        rateLimitService.checkApiLimit(getIp(http));
        User user = authService.refreshToken(req.refreshToken());
        return ResponseEntity.ok(Map.of(
                "accessToken", jwtUtil.generateAccessToken(user),
                "refreshToken", jwtUtil.generateRefreshToken(user),
                "tokenType", "Bearer"
        ));
    }

    @Operation(summary = "Request password reset email")
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotRequest req,
                                                               HttpServletRequest http) {
        rateLimitService.checkAuthLimit(getIp(http));
        return ResponseEntity.ok(Map.of("message", authService.forgotPassword(req.email())));
    }

    @Operation(summary = "Reset password with token")
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetRequest req,
                                                              HttpServletRequest http) {
        rateLimitService.checkAuthLimit(getIp(http));
        return ResponseEntity.ok(Map.of("message", authService.resetPassword(req.token(), req.newPassword())));
    }

    @Operation(summary = "Logout")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(Map.of("message", authService.logout(userDetails.getUsername())));
    }

    private String getIp(HttpServletRequest req) {
        String forwarded = req.getHeader("X-Forwarded-For");
        return (forwarded != null && !forwarded.isEmpty())
                ? forwarded.split(",")[0].trim()
                : req.getRemoteAddr();
    }
}
