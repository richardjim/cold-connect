package com.coldconnect.controller;

import com.coldconnect.entity.User;
import com.coldconnect.ratelimit.RateLimitService;
import com.coldconnect.repository.UserRepository;
import com.coldconnect.service.OtpService;
import com.coldconnect.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/auth")
@Tag(name = "Customer Auth", description = "Phone OTP login for customers")
public class OtpController extends BaseController {

    private final OtpService otpService;
    private final JwtUtil jwtUtil;
    private final RateLimitService rateLimitService;

    public OtpController(UserRepository userRepository, OtpService otpService,
                          JwtUtil jwtUtil, RateLimitService rateLimitService) {
        super(userRepository);
        this.otpService = otpService;
        this.jwtUtil = jwtUtil;
        this.rateLimitService = rateLimitService;
    }

    public record OtpRequestBody(@NotBlank String phone, String purpose, String preferredLanguage) {}
    public record OtpVerifyBody(@NotBlank String phone, @NotBlank String code) {}

    @Operation(summary = "Request OTP — sends SMS to phone number")
    @PostMapping("/otp/request")
    public ResponseEntity<Map<String, String>> requestOtp(@Valid @RequestBody OtpRequestBody req,
                                                           HttpServletRequest http) {
        rateLimitService.checkAuthLimit(getIp(http));
        String msg = otpService.requestOtp(req.phone(), req.purpose(), req.preferredLanguage());
        return ResponseEntity.ok(Map.of("message", msg));
    }

    @Operation(summary = "Verify OTP — returns JWT tokens")
    @PostMapping("/otp/verify")
    public ResponseEntity<Map<String, Object>> verifyOtp(@Valid @RequestBody OtpVerifyBody req,
                                                          HttpServletRequest http) {
        rateLimitService.checkAuthLimit(getIp(http));
        User user = otpService.verifyOtp(req.phone(), req.code());
        return ResponseEntity.ok(Map.of(
                "accessToken", jwtUtil.generateAccessToken(user),
                "refreshToken", jwtUtil.generateRefreshToken(user),
                "tokenType", "Bearer",
                "role", user.getRole().name(),
                "userId", user.getId(),
                "isNewUser", user.getFullName() == null || user.getFullName().isBlank()
        ));
    }

    private String getIp(HttpServletRequest req) {
        String forwarded = req.getHeader("X-Forwarded-For");
        return (forwarded != null && !forwarded.isEmpty())
                ? forwarded.split(",")[0].trim()
                : req.getRemoteAddr();
    }
}
