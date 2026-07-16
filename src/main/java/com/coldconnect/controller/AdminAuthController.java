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
import jakarta.validation.constraints.Pattern;
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

    public record RegisterRequest(
            @NotBlank @Email(message = "Must be a valid email address")
            String email,
            @NotBlank @Size(min = 8, message = "Password must be at least 8 characters")
            @Pattern(
                    regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
                    message = "Password must contain uppercase, lowercase and a number"
            )
            String password,
            @NotBlank
            @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
            @Pattern(regexp = "^[a-zA-Z\\s'-]+$", message = "Name must contain letters only")
            String fullName
    ) {}

    public record LoginRequest(
            @NotBlank @Email(message = "Must be a valid email address")
            String email,
            @NotBlank(message = "Password is required")
            String password
    ) {}

    public record ForgotRequest(
            @NotBlank @Email(message = "Must be a valid email address")
            String email
    ) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}


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
    public record VerifyEmailRequest(@NotBlank String email, @NotBlank String code) {}

    @Operation(summary = "Verify email with 6-digit code")
    @PostMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest req) {
        return ResponseEntity.ok(Map.of("message", authService.verifyEmail(req.email(), req.code())));
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

    public record ResetRequest(
            @NotBlank String email,
            @NotBlank String code,
            @NotBlank @Size(min = 8) String newPassword
    ) {}

    @Operation(summary = "Reset password with 6-digit code")
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetRequest req, HttpServletRequest http) {
        rateLimitService.checkAuthLimit(getIp(http));
        return ResponseEntity.ok(Map.of("message",
                authService.resetPassword(req.email(), req.code(), req.newPassword())));
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

    public record ResendVerificationRequest(@NotBlank @Email String email) {}

    @Operation(summary = "Resend email verification code")
    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, String>> resendVerification(
            @Valid @RequestBody ResendVerificationRequest req,
            HttpServletRequest http) {
        rateLimitService.checkAuthLimit(getIp(http));
        return ResponseEntity.ok(Map.of("message", authService.resendVerification(req.email())));
    }
}
