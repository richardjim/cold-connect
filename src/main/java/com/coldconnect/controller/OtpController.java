package com.coldconnect.controller;

import com.coldconnect.entity.User;
import com.coldconnect.enums.Role;
import com.coldconnect.exception.AppException;
import com.coldconnect.ratelimit.RateLimitService;
import com.coldconnect.repository.UserRepository;
import com.coldconnect.service.OtpService;
import com.coldconnect.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/auth")
@Tag(name = "Customer Auth", description = "Phone OTP signup and login for customers")
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

    public record OtpRequestBody(
            @NotBlank
            @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Phone number must contain digits only, 7-15 characters")
            String phone,
            String purpose,
            @Pattern(regexp = "^(en|ha|yo|ig|pcm)$", message = "Language must be one of: en, ha, yo, ig, pcm")
            String preferredLanguage
    ) {}

    public record SignupRequest(
            @NotBlank
            @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Phone number must contain digits only, 7-15 characters")
            String phone,
            @NotBlank
            @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
            @Pattern(regexp = "^[a-zA-Z\\s'-]+$", message = "Name must contain letters only")
            String fullName,
            @Pattern(regexp = "^(en|ha|yo|ig|pcm)$", message = "Language must be one of: en, ha, yo, ig, pcm")
            String language
    ) {}

    public record OtpVerifyBody(
            @NotBlank
            @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Phone number must contain digits only")
            String phone,
            @NotBlank
            @Pattern(regexp = "^[0-9]{4,6}$", message = "OTP must be numeric digits only")
            String code
    ) {}

    @Operation(summary = "Customer signup — register account and send OTP")
    @PostMapping("/signup")
    public ResponseEntity<Map<String, String>> signup(
            @Valid @RequestBody SignupRequest req,
            HttpServletRequest http) {

        rateLimitService.checkAuthLimit(getIp(http));

        if (userRepository.existsByPhone(req.phone())) {
            throw new AppException.ConflictException(
                    "Phone already registered. Use POST /v1/auth/login to sign in.");
        }

        User user = new User();
        user.setPhone(req.phone());
        user.setFullName(req.fullName());
        user.setLanguage(req.language() != null ? req.language() : "en");
        user.setRole(Role.CUSTOMER);
        user.setEnabled(true);
        userRepository.save(user);

        String msg = otpService.requestOtp(req.phone(), "signup", req.language());
        return ResponseEntity.ok(Map.of(
                "message", msg,
                "next", "POST /v1/auth/otp/verify with your OTP code"
        ));
    }

    @Operation(summary = "Customer login — send OTP to registered phone")
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(
            @Valid @RequestBody OtpRequestBody req,
            HttpServletRequest http) {

        rateLimitService.checkAuthLimit(getIp(http));

        if (!userRepository.existsByPhone(req.phone())) {
            throw new AppException.NotFoundException(
                    "Phone not registered. Use POST /v1/auth/signup first.");
        }

        String msg = otpService.requestOtp(req.phone(), "login", req.preferredLanguage());
        return ResponseEntity.ok(Map.of(
                "message", msg,
                "next", "POST /v1/auth/otp/verify with your OTP code"
        ));
    }

    @Operation(summary = "Verify OTP — returns JWT tokens (use after signup or login)")
    @PostMapping("/otp/verify")
    public ResponseEntity<Map<String, Object>> verifyOtp(
            @Valid @RequestBody OtpVerifyBody req,
            HttpServletRequest http) {

        rateLimitService.checkAuthLimit(getIp(http));
        User user = otpService.verifyOtp(req.phone(), req.code());
        return ResponseEntity.ok(Map.of(
                "accessToken",  jwtUtil.generateAccessToken(user),
                "refreshToken", jwtUtil.generateRefreshToken(user),
                "tokenType",    "Bearer",
                "role",         user.getRole().name(),
                "userId",       user.getId(),
                "fullName",     user.getFullName() != null ? user.getFullName() : ""
        ));
    }

    @Operation(summary = "Request OTP — generic, sends SMS to any registered phone")
    @PostMapping("/otp/request")
    public ResponseEntity<Map<String, String>> requestOtp(
            @Valid @RequestBody OtpRequestBody req,
            HttpServletRequest http) {

        rateLimitService.checkAuthLimit(getIp(http));
        String msg = otpService.requestOtp(req.phone(), req.purpose(), req.preferredLanguage());
        return ResponseEntity.ok(Map.of("message", msg));
    }

    private String getIp(HttpServletRequest req) {
        String forwarded = req.getHeader("X-Forwarded-For");
        return (forwarded != null && !forwarded.isEmpty())
                ? forwarded.split(",")[0].trim()
                : req.getRemoteAddr();
    }
}