package com.coldconnect.service;

import com.coldconnect.entity.User;
import com.coldconnect.exception.AppException;
import com.coldconnect.i18n.AppMessages;
import com.coldconnect.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class OtpService {

    private final UserRepository userRepository;
    private final SmsService     smsService;
    private final AppMessages    messages;

    public OtpService(UserRepository userRepository,
                      SmsService smsService,
                      AppMessages messages) {
        this.userRepository = userRepository;
        this.smsService     = smsService;
        this.messages       = messages;
    }

    @Transactional
    public String requestOtp(String phone, String purpose, String language) {

        // Input validation
        if (phone == null || phone.isBlank()) {
            throw new AppException.BadRequestException("Phone number is required");
        }
        if (!phone.matches("^\\+?[0-9]{7,15}$")) {
            throw new AppException.BadRequestException(
                    "Invalid phone number format");
        }

        String otp = String.format("%06d", new Random().nextInt(999999));

        // DB validation — phone must exist
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new AppException.NotFoundException(
                        messages.get(AppMessages.Key.PHONE_NOT_REGISTERED, language)));

        // Check account is enabled
        if (!user.isEnabled()) {
            throw new AppException.UnauthorizedException(
                    "Account is disabled. Please contact support.");
        }

        // Rate limit — prevent OTP spam: block if last OTP was sent < 60 seconds ago
        if (user.getOtpExpiry() != null &&
                user.getOtpExpiry().isAfter(LocalDateTime.now().plusMinutes(9))) {
            throw new AppException.TooManyRequestsException(
                    "Please wait before requesting another OTP.");
        }

        user.setOtpCode(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        smsService.sendOtp(phone, otp, language != null ? language : "en");

        String masked = phone.length() > 4
                ? phone.substring(0, phone.length() - 4) + "****"
                : "****";

        return messages.get(AppMessages.Key.OTP_SENT, language) + " (" + masked + ")";
    }

    @Transactional
    public User verifyOtp(String phone, String code, String language) {

        // Input validation
        if (phone == null || phone.isBlank()) {
            throw new AppException.BadRequestException("Phone number is required");
        }
        if (code == null || code.isBlank()) {
            throw new AppException.BadRequestException("OTP code is required");
        }
        if (!code.matches("^[0-9]{4,6}$")) {
            throw new AppException.BadRequestException(
                    messages.get(AppMessages.Key.OTP_INVALID, language));
        }

        // DB validation — phone must exist
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new AppException.NotFoundException(
                        messages.get(AppMessages.Key.PHONE_NOT_REGISTERED, language)));

        // Check account is enabled
        if (!user.isEnabled()) {
            throw new AppException.UnauthorizedException(
                    "Account is disabled. Please contact support.");
        }

        // Master test OTP — remove when Termii is wired
        boolean isMasterCode = "1234".equals(code);

        if (!isMasterCode) {
            // Check OTP exists
            if (user.getOtpCode() == null) {
                throw new AppException.BadRequestException(
                        "No active OTP found. Please request a new one.");
            }
            // Check OTP matches
            if (!user.getOtpCode().equals(code)) {
                throw new AppException.BadRequestException(
                        messages.get(AppMessages.Key.OTP_INVALID, language));
            }
            // Check OTP not expired
            if (user.getOtpExpiry() == null ||
                    user.getOtpExpiry().isBefore(LocalDateTime.now())) {
                throw new AppException.BadRequestException(
                        messages.get(AppMessages.Key.OTP_EXPIRED, language));
            }
        }

        // Clear OTP after successful verification
        user.setOtpCode(null);
        user.setOtpExpiry(null);
        return userRepository.save(user);
    }
}