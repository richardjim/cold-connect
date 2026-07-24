package com.coldconnect.controller;

import com.coldconnect.entity.User;
import com.coldconnect.enums.Role;
import com.coldconnect.exception.AppException;
import com.coldconnect.i18n.AppMessages;
import com.coldconnect.ratelimit.RateLimitService;
import com.coldconnect.repository.UserRepository;
import com.coldconnect.service.OtpService;
import com.coldconnect.service.SmsService;
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

    private final OtpService       otpService;
    private final JwtUtil          jwtUtil;
    private final RateLimitService rateLimitService;
    private final SmsService       smsService;
    private final AppMessages      messages;

    public OtpController(UserRepository userRepository,
                         OtpService otpService,
                         JwtUtil jwtUtil,
                         RateLimitService rateLimitService,
                         SmsService smsService,
                         AppMessages messages) {
        super(userRepository);
        this.otpService       = otpService;
        this.jwtUtil          = jwtUtil;
        this.rateLimitService = rateLimitService;
        this.smsService       = smsService;
        this.messages         = messages;
    }

    public record SignupRequest(
            @NotBlank
            @Pattern(regexp = "^\\+?[0-9]{7,15}$",
                    message = "Phone number must contain digits only, 7-15 characters")
            String phone,
            @NotBlank
            @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
            @Pattern(regexp = "^[a-zA-Z\\s'-]+$", message = "Name must contain letters only")
            String fullName,
            @Pattern(regexp = "^(en|ha|yo|ig|pcm)$",
                    message = "Language must be one of: en, ha, yo, ig, pcm")
            String language,
            Long customerTypeId  // optional — e.g. 1=FARMER, 2=BUYER
    ) {}

    public record OtpRequestBody(
            @NotBlank
            @Pattern(regexp = "^\\+?[0-9]{7,15}$",
                    message = "Phone number must contain digits only, 7-15 characters")
            String phone,
            String purpose,
            @Pattern(regexp = "^(en|ha|yo|ig|pcm)$",
                    message = "Language must be one of: en, ha, yo, ig, pcm")
            String preferredLanguage
    ) {}

    public record OtpVerifyBody(
            @NotBlank
            @Pattern(regexp = "^\\+?[0-9]{7,15}$",
                    message = "Phone number must contain digits only")
            String phone,
            @NotBlank
            @Pattern(regexp = "^[0-9]{4,6}$", message = "OTP must be numeric digits only")
            String code,
            @Pattern(regexp = "^(en|ha|yo|ig|pcm)$",
                    message = "Language must be one of: en, ha, yo, ig, pcm")
            String preferredLanguage
    ) {}

    // ── Signup ────────────────────────────────────────────────────────────────
    @Operation(summary = "Customer signup — register account and send OTP")
    @PostMapping("/signup")
    public ResponseEntity<Map<String, String>> signup(
            @Valid @RequestBody SignupRequest req,
            HttpServletRequest http) {
        rateLimitService.checkAuthLimit(getIp(http));
        String lang = req.language() != null ? req.language() : "en";

        if (userRepository.existsByPhone(req.phone())) {
            throw new AppException.ConflictException(
                    messages.get(AppMessages.Key.PHONE_ALREADY_REGISTERED, lang));
        }

        User user = new User();
        user.setPhone(req.phone());
        user.setFullName(req.fullName());
        user.setLanguage(lang);
        user.setRole(Role.CUSTOMER);
        user.setEnabled(true);
        user.setCustomerTypeId(req.customerTypeId());
        userRepository.save(user);

        otpService.requestOtp(req.phone(), "signup", lang);
        return ResponseEntity.ok(Map.of(
                "message", messages.get(AppMessages.Key.SIGNUP_SUCCESS, lang),
                "next",    "POST /v1/auth/otp/verify"
        ));
    }

    // ── Login ─────────────────────────────────────────────────────────────────
    @Operation(summary = "Customer login — send OTP to registered phone")
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(
            @Valid @RequestBody OtpRequestBody req,
            HttpServletRequest http) {
        rateLimitService.checkAuthLimit(getIp(http));
        String lang = req.preferredLanguage() != null ? req.preferredLanguage() : "en";

        if (!userRepository.existsByPhone(req.phone())) {
            throw new AppException.NotFoundException(
                    messages.get(AppMessages.Key.PHONE_NOT_REGISTERED, lang));
        }

        otpService.requestOtp(req.phone(), "login", lang);
        return ResponseEntity.ok(Map.of(
                "message", messages.get(AppMessages.Key.LOGIN_NEXT_STEP, lang),
                "next",    "POST /v1/auth/otp/verify"
        ));
    }

    // ── Verify OTP ────────────────────────────────────────────────────────────
    @Operation(summary = "Verify OTP — returns JWT tokens")
    @PostMapping("/otp/verify")
    public ResponseEntity<Map<String, Object>> verifyOtp(
            @Valid @RequestBody OtpVerifyBody req,
            HttpServletRequest http) {
        rateLimitService.checkAuthLimit(getIp(http));
        String lang = req.preferredLanguage() != null ? req.preferredLanguage() : "en";

        User user = otpService.verifyOtp(req.phone(), req.code(), lang);
        return ResponseEntity.ok(Map.of(
                "message",      messages.get(AppMessages.Key.OTP_VERIFIED, lang),
                "accessToken",  jwtUtil.generateAccessToken(user),
                "refreshToken", jwtUtil.generateRefreshToken(user),
                "tokenType",    "Bearer",
                "role",         user.getRole().name(),
                "userId",       user.getId(),
                "fullName",     user.getFullName() != null ? user.getFullName() : "",
                "language",     user.getLanguage() != null ? user.getLanguage() : "en"
        ));
    }

    // ── Resend OTP ────────────────────────────────────────────────────────────
    @Operation(summary = "Resend OTP — for any registered phone")
    @PostMapping("/otp/request")
    public ResponseEntity<Map<String, String>> requestOtp(
            @Valid @RequestBody OtpRequestBody req,
            HttpServletRequest http) {
        rateLimitService.checkAuthLimit(getIp(http));
        String lang = req.preferredLanguage() != null ? req.preferredLanguage() : "en";
        String msg  = otpService.requestOtp(req.phone(), req.purpose(), lang);
        return ResponseEntity.ok(Map.of("message", msg));
    }

    // ── Call Me Instead ───────────────────────────────────────────────────────
    @Operation(
            summary = "Call Me Instead — trigger voice OTP call",
            description = "Sends OTP via phone call. Requires Termii integration."
    )
    @PostMapping("/otp/call")
    public ResponseEntity<Map<String, String>> voiceOtp(
            @Valid @RequestBody OtpRequestBody req,
            HttpServletRequest http) {
        rateLimitService.checkAuthLimit(getIp(http));
        String lang = req.preferredLanguage() != null ? req.preferredLanguage() : "en";

        if (!userRepository.existsByPhone(req.phone())) {
            throw new AppException.NotFoundException(
                    messages.get(AppMessages.Key.PHONE_NOT_REGISTERED, lang));
        }

        otpService.requestOtp(req.phone(), "voice", lang);
        smsService.sendOtpVoiceCall(req.phone(), "");

        return ResponseEntity.ok(Map.of(
                "message", messages.get(AppMessages.Key.VOICE_OTP_INITIATED, lang)
        ));
    }

    private String getIp(HttpServletRequest req) {
        String forwarded = req.getHeader("X-Forwarded-For");
        return (forwarded != null && !forwarded.isEmpty())
                ? forwarded.split(",")[0].trim()
                : req.getRemoteAddr();
    }

    public record RefreshRequest(@NotBlank String refreshToken) {}

    @Operation(summary = "Refresh customer access token")
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh(
            @Valid @RequestBody RefreshRequest req,
            HttpServletRequest http) {

        rateLimitService.checkApiLimit(getIp(http));

        String phone = jwtUtil.extractUsername(req.refreshToken());

        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new AppException.UnauthorizedException(
                        "Invalid refresh token"));

        if (!jwtUtil.isTokenValid(req.refreshToken(), user)) {
            throw new AppException.UnauthorizedException(
                    "Refresh token is invalid or expired. Please log in again.");
        }

        return ResponseEntity.ok(Map.of(
                "accessToken",  jwtUtil.generateAccessToken(user),
                "refreshToken", jwtUtil.generateRefreshToken(user),
                "tokenType",    "Bearer",
                "userId",       user.getId(),
                "role",         user.getRole().name()
        ));
    }
}