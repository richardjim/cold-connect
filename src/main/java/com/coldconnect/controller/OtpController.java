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
import io.swagger.v3.oas.annotations.media.Schema;
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
            @NotBlank(message = "Phone number is required")
            @Pattern(regexp = "^\\+?[0-9]{7,15}$",
                    message = "Phone number must contain digits only, 7-15 characters")
            String phone,

            @NotBlank(message = "Full name is required")
            @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
            @Pattern(regexp = "^[a-zA-Z\\s'-]+$", message = "Name must contain letters only")
            String fullName,

            @Schema(example = "en", description = "en · ha · yo · ig · pcm")
            String language,

            @Schema(example = "1", description = "Customer type ID from GET /v1/customer-types")
            Long customerTypeId,

            @Schema(example = "Jos, Plateau State")
            String location,

            @Schema(example = "HUB-JOS-01")
            String preferredHubId,

            @Schema(example = "accepted", description = "accepted · declined")
            @Pattern(regexp = "^(accepted|declined)$",
                    message = "Consent must be: accepted or declined")
            String consentStatus,

            @Schema(example = "FARMER",
                    description = "FARMER · MARKET_TRADER · COOPERATIVE · BUYER · PROCESSOR")
            String persona,

            @Schema(example = "optional-org-id")
            String organizationId,

            @Schema(example = "FEMALE",
                    description = "MALE · FEMALE · OTHER · PREFER_NOT_TO_SAY")
            String gender,

            @Schema(example = "true",
                    description = "true if user is under 35")
            Boolean youth
    ) {}

    public record OtpRequestBody(
            @NotBlank(message = "Phone number is required")
            @Pattern(regexp = "^\\+?[0-9]{7,15}$",
                    message = "Phone number must contain digits only, 7-15 characters")
            String phone,
            String purpose,
            @Schema(example = "en", description = "en · ha · yo · ig · pcm")
            String preferredLanguage
    ) {}

    public record OtpVerifyBody(
            @NotBlank(message = "Phone number is required")
            @Pattern(regexp = "^\\+?[0-9]{7,15}$",
                    message = "Phone number must contain digits only")
            String phone,
            @NotBlank(message = "OTP code is required")
            @Pattern(regexp = "^[0-9]{4,6}$", message = "OTP must be numeric digits only")
            String code,
            @Schema(example = "en", description = "en · ha · yo · ig · pcm")
            String preferredLanguage
    ) {}

    public record RefreshRequest(
            @NotBlank(message = "Refresh token is required")
            String refreshToken
    ) {}

    private static final java.util.Set<String> VALID_LANGUAGES =
            java.util.Set.of("en", "ha", "yo", "ig", "pcm");

    private String resolveLanguage(String lang) {
        if (lang == null || lang.isBlank()) return "en";
        if (!VALID_LANGUAGES.contains(lang.toLowerCase())) {
            throw new AppException.BadRequestException(
                    "Invalid language. Must be one of: en · ha · yo · ig · pcm");
        }
        return lang.toLowerCase();
    }

    private String getIp(HttpServletRequest req) {
        String forwarded = req.getHeader("X-Forwarded-For");
        return (forwarded != null && !forwarded.isEmpty())
                ? forwarded.split(",")[0].trim()
                : req.getRemoteAddr();
    }

    // ── Signup ────────────────────────────────────────────────────────────────
    @Operation(
            summary = "Customer signup — register account and send OTP",
            description = """
            Creates account and sends OTP.
            **Required:** phone, fullName
            **Optional:** language (defaults to en), customerTypeId, location,
            preferredHubId, consentStatus, persona, organizationId, gender, youth
            """
    )
    @PostMapping("/signup")
    public ResponseEntity<Map<String, String>> signup(
            @Valid @RequestBody SignupRequest req,
            HttpServletRequest http) {
        rateLimitService.checkAuthLimit(getIp(http));

        // Validate and resolve language
        String lang = resolveLanguage(req.language());

        // Trim and validate name
        String cleanName = req.fullName() != null ? req.fullName().trim() : null;
        if (cleanName == null || cleanName.isBlank()) {
            throw new AppException.BadRequestException(
                    "Full name cannot be empty or contain only spaces");
        }

        // Trim phone
        String cleanPhone = req.phone() != null ? req.phone().trim() : null;
        if (cleanPhone == null || !cleanPhone.matches("^\\+?[0-9]{7,15}$")) {
            throw new AppException.BadRequestException(
                    "Invalid phone number format. Must contain digits only, 7-15 characters");
        }

        if (userRepository.existsByPhone(cleanPhone)) {
            throw new AppException.ConflictException(
                    messages.get(AppMessages.Key.PHONE_ALREADY_REGISTERED, lang));
        }

        User user = new User();
        user.setPhone(cleanPhone);
        user.setFullName(cleanName);
        user.setLanguage(lang);
        user.setRole(Role.CUSTOMER);
        user.setEnabled(true);

        if (req.customerTypeId() != null)                          user.setCustomerTypeId(req.customerTypeId());
        if (req.location() != null && !req.location().isBlank())   user.setLocation(req.location().trim());
        if (req.preferredHubId() != null && !req.preferredHubId().isBlank()) user.setPreferredHubId(req.preferredHubId().trim());
        if (req.consentStatus() != null && !req.consentStatus().isBlank())   user.setConsentStatus(req.consentStatus());
        if (req.persona() != null && !req.persona().isBlank())     user.setPersona(req.persona());
        if (req.organizationId() != null && !req.organizationId().isBlank()) user.setOrganizationId(req.organizationId().trim());
        if (req.gender() != null)                                  user.setGender(req.gender());
        if (req.youth() != null)                                   user.setYouth(req.youth());

        userRepository.save(user);
        otpService.requestOtp(cleanPhone, "signup", lang);

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

        String lang       = resolveLanguage(req.preferredLanguage());
        String cleanPhone = req.phone() != null ? req.phone().trim() : null;

        if (cleanPhone == null || !cleanPhone.matches("^\\+?[0-9]{7,15}$")) {
            throw new AppException.BadRequestException(
                    "Invalid phone number format. Must contain digits only, 7-15 characters");
        }

        if (!userRepository.existsByPhone(cleanPhone)) {
            throw new AppException.NotFoundException(
                    messages.get(AppMessages.Key.PHONE_NOT_REGISTERED, lang));
        }

        otpService.requestOtp(cleanPhone, "login", lang);
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

        String lang       = resolveLanguage(req.preferredLanguage());
        String cleanPhone = req.phone() != null ? req.phone().trim() : null;

        if (cleanPhone == null || !cleanPhone.matches("^\\+?[0-9]{7,15}$")) {
            throw new AppException.BadRequestException(
                    "Invalid phone number format");
        }

        User user = otpService.verifyOtp(cleanPhone, req.code(), lang);
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

        String lang       = resolveLanguage(req.preferredLanguage());
        String cleanPhone = req.phone() != null ? req.phone().trim() : null;

        if (cleanPhone == null || !cleanPhone.matches("^\\+?[0-9]{7,15}$")) {
            throw new AppException.BadRequestException(
                    "Invalid phone number format");
        }

        String msg = otpService.requestOtp(cleanPhone, req.purpose(), lang);
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

        String lang       = resolveLanguage(req.preferredLanguage());
        String cleanPhone = req.phone() != null ? req.phone().trim() : null;

        if (cleanPhone == null || !cleanPhone.matches("^\\+?[0-9]{7,15}$")) {
            throw new AppException.BadRequestException(
                    "Invalid phone number format");
        }

        if (!userRepository.existsByPhone(cleanPhone)) {
            throw new AppException.NotFoundException(
                    messages.get(AppMessages.Key.PHONE_NOT_REGISTERED, lang));
        }

        otpService.requestOtp(cleanPhone, "voice", lang);
        smsService.sendOtpVoiceCall(cleanPhone, "");

        return ResponseEntity.ok(Map.of(
                "message", messages.get(AppMessages.Key.VOICE_OTP_INITIATED, lang)
        ));
    }

    // ── Refresh Token ─────────────────────────────────────────────────────────
    @Operation(summary = "Refresh customer access token")
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh(
            @Valid @RequestBody RefreshRequest req,
            HttpServletRequest http) {
        rateLimitService.checkApiLimit(getIp(http));

        try {
            String username = jwtUtil.extractUsername(req.refreshToken());

            if (username == null || username.isBlank()) {
                throw new AppException.UnauthorizedException("Invalid refresh token");
            }

            var user = userRepository.findByPhone(username)
                    .or(() -> userRepository.findByEmail(username))
                    .orElseThrow(() -> new AppException.UnauthorizedException(
                            "User not found for token"));

            if (!user.isEnabled()) {
                throw new AppException.UnauthorizedException("Account is disabled");
            }

            if (!jwtUtil.isTokenValid(req.refreshToken(), user)) {
                throw new AppException.UnauthorizedException(
                        "Refresh token expired. Please log in again.");
            }

            return ResponseEntity.ok(Map.of(
                    "accessToken",  jwtUtil.generateAccessToken(user),
                    "refreshToken", jwtUtil.generateRefreshToken(user),
                    "tokenType",    "Bearer",
                    "userId",       user.getId(),
                    "role",         user.getRole().name(),
                    "language",     user.getLanguage() != null ? user.getLanguage() : "en"
            ));

        } catch (AppException.UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException.UnauthorizedException(
                    "Invalid or expired refresh token. Please log in again.");
        }
    }
}